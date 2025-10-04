const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { GoogleGenerativeAI } = require("@google/generative-ai");

// Define secret key
const geminiApiKeySecret = defineSecret("GEMINI_API_KEY");

/**
 * Converts a Base64 string to a GoogleGenerativeAI.Part object.
 * @param {string} base64String The image data as a Base64 string.
 * @param {string} mimeType The MIME type of the image (e.g., 'image/jpeg').
 * @returns {object} The correctly formatted Gemini Part object for the 'contents' array.
 */
function imageToGenerativePart(base64String, mimeType) {
  // CRITICAL FIX 1: The key for image data is 'inlineData' and its value is the nested object
  return {
    inlineData: {
      data: base64String,
      mimeType
    },
  };
}

/**
 * Extracts a JSON string from a markdown code block, or returns the text if not found.
 * @param {string} text The response text from the Gemini API.
 * @returns {string} The cleaned JSON string.
 */
function extractJsonFromMarkdown(text) {
  const jsonStart = text.indexOf('```json\n');
  const jsonEnd = text.indexOf('\n```', jsonStart + 7);

  if (jsonStart !== -1 && jsonEnd !== -1) {
    return text.substring(jsonStart + 8, jsonEnd).trim();
  }

  return text.trim();
}

exports.analyzeFoodWithGemini = onCall(
  {
    region: "me-west1",
    secrets: [geminiApiKeySecret],
    // Increase memory/timeout for multimodal requests
    // memory: "512MiB",
    // timeoutSeconds: 60,
  },
  async (data) => {
    const MAX_RETRIES = 3;
    let retries = 0;
    let lastError = null;

    while (retries < MAX_RETRIES) {
      try {
        let foodName = data.data.foodName;
        const imageB64 = data.data.imageB64;

        if (!foodName && !imageB64) {
          throw new HttpsError("invalid-argument", "Either 'foodName' or 'imageB64' must be provided.");
        }

        const geminiApiKey = geminiApiKeySecret.value();
        const genAI = new GoogleGenerativeAI(geminiApiKey);
        const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" });

        // --- STEP 1: Image Analysis (if image is present) ---
        if (imageB64) {
          const imagePart = imageToGenerativePart(imageB64, "image/jpeg");

          const descriptionPrompt = `
            Describe the main food item(s) on the plate, including estimated quantities.
            Keep the description concise (e.g., "1 bowl of rice with 1 chicken breast", "2 slices of pizza").
            DO NOT provide nutritional information or JSON.
          `;

          // CRITICAL FIX 2: Text parts must be wrapped correctly for the SDK/API version.
          // By default, the contents array is an array of objects, each containing a 'text' or 'inlineData' property.
          const multimodalContents = [
            {
              role: "user",
              parts: [
                imagePart,
                { text: descriptionPrompt },
              ],
            },
          ];

          // Note: In some older SDK versions or direct API calls, the key 'text' might be disallowed
          // and the API expects a simple string or a 'parts' array inside the content object.
          // We will use the standard object format { text: '...' }. If this fails, we will try the simple string.

          const descriptionResult = await model.generateContent({ contents: multimodalContents });
          const textDescription = descriptionResult.response.text().trim();

          console.log("Image description from Gemini:", textDescription);

          // Use the generated description as the foodName for the next step
          foodName = textDescription;
        }

        // --- STEP 2: Nutritional Analysis (using foodName/description) ---
        const nutritionalAnalysisPrompt = `
          You are a nutritional assistant.
          Analyze the nutritional content of the food item(s) and quantity specified in "${foodName}".
          For each distinct, main food item in the request, provide the calories, protein, carbs, fat, fiber, sugar, sodium, potassium, calcium, iron, and vitamin C for that exact quantity

          RULES AND CONSTRAINTS:
          1. DECOMPOSITION: Minor components (e.g., sauces, small garnishes, seasonings) that are typically consumed with a main dish should not be separated. For example, "pasta with tomato sauce" is one item. A named dish (like 'Caesar Salad' or 'Hummus Plate') should be treated as a SINGLE item and not decomposed unless the user explicitly lists multiple ingredients.
          2. SEPARATION: Main components should be separated. For example, "rice with chicken breast" contains two separate main food items.
          3. SERVING AMOUNT: If a number is not specified for a main food item, use a common, natural serving size (e.g., "1 piece", "1 bowl"). DO NOT use fractional amounts (e.g., "0.5 cup", "3 oz") unless the user explicitly specified a fraction in their entry.
          4. UNIT SELECTION: For whole fruits or vegetables without a specified unit, the unit should be "piece" or "unit" instead of "cup" or "grams"

          Return the data as a single JSON array containing an object for each main food item.
          If the food is not found, return an empty array: [].
          Example for "5 bananas":
          [
            {
              "food_name": "Banana",
              "serving_unit": "pieces",
              "serving_amount": "5",
              "calories": "525",
              "protein": "6.5",
              "carbohydrates": "135",
              "fat": "2",
              "fiber": "17.5",
              "sugar": "72.5",
              "sodium": "5",
              "potassium": "2100",
              "calcium": "30",
              "iron": "1.5",
              "vitamin_c": "51"
            }
          ]
          Example for "rice with chicken breast":
          [
            {
              "food_name": "Cooked Rice",
              "serving_unit": "cup",
              "serving_amount": "1",
              "calories": "205",
              "protein": "4.3",
              "carbohydrates": "44.5",
              "fat": "0.4",
              "fiber": "0.6",
              "sugar": "0",
              "sodium": "2",
              "potassium": "55",
              "calcium": "5",
              "iron": "0.2",
              "vitamin_c": "0"
            },
            {
              "food_name": "Chicken Breast",
              "serving_unit": "piece",
              "serving_amount": "1",
              "calories": "165",
              "protein": "31",
              "carbohydrates": "0",
              "fat": "3.6",
              "fiber": "0",
              "sugar": "0",
              "sodium": "75",
              "potassium": "330",
              "calcium": "5",
              "iron": "0.7",
              "vitamin_c": "0"
            }
          ]
        `;

        // The final prompt is a single string and is passed directly to generateContent
        const result = await model.generateContent(nutritionalAnalysisPrompt);
        const response = result.response;
        const text = response.text();

        const cleanedText = extractJsonFromMarkdown(text);
        console.log("Cleaned Gemini response:", cleanedText);

        let parsedJson;
        try {
          parsedJson = JSON.parse(cleanedText);
        } catch (parseError) {
          console.error("Failed to parse cleaned Gemini response as JSON:", parseError);
          throw new HttpsError("internal", "Gemini response was not a valid JSON string. Check function logs for details.");
        }

        return {
          success: true,
          data: parsedJson
        };

      } catch (error) {
        lastError = error;
        console.error(`Attempt ${retries + 1} failed:`, error.message);

        if (error.status === 503) {
          const delay = Math.pow(2, retries) * 1000;
          console.log(`Retrying in ${delay / 1000} seconds...`);
          await new Promise(resolve => setTimeout(resolve, delay));
          retries++;
        } else {
          // Check if the error is a FirebaseFunctionsException (e.g., from an HttpsError throw)
          if (error.code) {
            throw error;
          } else {
            // Re-throw a generic internal error for other unhandled exceptions
            throw new HttpsError("internal", "An unknown error occurred during the Gemini analysis. Please try again.");
          }
        }
      }
    }

    if (lastError) {
      if (lastError.code) {
        throw lastError;
      }
      throw new HttpsError("unavailable", "The Gemini API is currently unavailable. Please try again in a few moments.");
    }
  }
);
