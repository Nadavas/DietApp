const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { GoogleGenerativeAI } = require("@google/generative-ai");

// Define secret key
const geminiApiKeySecret = defineSecret("GEMINI_API_KEY");

/**
 * Converts a Base64 string to a GoogleGenerativeAI.Part object.
 * @param {string} base64String The image data as a Base64 string.
 * @param {string} mimeType The MIME type of the image (e.g., 'image/jpeg').
 * @returns {object} The correctly formatted Gemini Part object.
 */
function imageToGenerativePart(base64String, mimeType) {
  return {
    inlineData: {
      data: base64String,
      mimeType
    },
  };
}

/**
 * Extracts a JSON string from a markdown code block.
 * @param {string} text The response text from the Gemini API.
 * @returns {string} The cleaned JSON string.
 */
function extractJsonFromMarkdown(text) {
  if (!text) return "{}";

  // Try to locate a JSON block (handles ```json ... ``` and plain JSON)
  const jsonMatch = text.match(/\{[\s\S]*\}|\[[\s\S]*\]/);
  if (jsonMatch) {
    const candidate = jsonMatch[0];
    try {
      JSON.parse(candidate); // sanity check
      return candidate;
    } catch {
      // If it's close to JSON but invalid, try some basic cleanup
      const cleaned = candidate
        .replace(/[\u0000-\u001F]+/g, '') // remove control chars
        .replace(/,\s*([}\]])/g, '$1');  // remove trailing commas
      return cleaned;
    }
  }

  // Fallback: try parsing the entire thing
  return text.trim();
}

// =================================================================
// EXISTING FUNCTION FOR FOOD ANALYSIS
// =================================================================
exports.analyzeFoodWithGemini = onCall(
  {
    region: "me-west1",
    secrets: [geminiApiKeySecret],
  },
  async (request) => { // <-- Corrected parameter name from 'data' to 'request'
    const MAX_RETRIES = 3;
    let retries = 0;
    let lastError = null;

    while (retries < MAX_RETRIES) {
      try {
        // Corrected to use request.data for v2 onCall functions
        let foodName = request.data.foodName;
        const imageB64 = request.data.imageB64;

        if (!foodName && !imageB64) {
          throw new HttpsError("invalid-argument", "Either 'foodName' or 'imageB64' must be provided.");
        }

        const geminiApiKey = geminiApiKeySecret.value();
        const genAI = new GoogleGenerativeAI(geminiApiKey);
        const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" }); // Updated to a newer model for consistency

        // --- STEP 1: Image Analysis (if image is present) ---
        if (imageB64) {
          const imagePart = imageToGenerativePart(imageB64, "image/jpeg");

          const descriptionPrompt = `
            Describe the main food item(s) on the plate, including estimated quantities.
            Keep the description concise (e.g., "1 bowl of rice with 1 chicken breast", "2 slices of pizza").
            DO NOT provide nutritional information or JSON.
          `;

          const multimodalContents = [
            {
              role: "user",
              parts: [
                imagePart,
                { text: descriptionPrompt },
              ],
            },
          ];

          const descriptionResult = await model.generateContent({ contents: multimodalContents });
          const textDescription = descriptionResult.response.text().trim();

          console.log("Image description from Gemini:", textDescription);
          foodName = textDescription;
        }

        // --- STEP 2: Nutritional Analysis (using foodName/description) ---
        const nutritionalAnalysisPrompt = `
          You are a nutritional assistant.
          Analyze the nutritional content of the food item(s) and quantity specified in "${foodName}".
          For each distinct, main food item in the request, provide the calories, protein, carbs, fat, fiber, sugar, sodium, potassium, calcium, iron, and vitamin C for that exact quantity.

          RULES AND CONSTRAINTS:
          1. DECOMPOSITION: Minor components (e.g., sauces, small garnishes) that are part of a main dish should not be separated. For example, "pasta with tomato sauce" is one item.
          2. SEPARATION: Main components should be separated. For example, "rice with chicken breast" contains two separate main food items.
          3. SERVING AMOUNT: If a number is not specified for a main food item, use a common, natural serving size (e.g., "1 piece", "1 bowl"). Do not use fractional amounts unless specified.
          4. UNIT SELECTION (Weight Priority: Grams): When specifying the weight unit, always use 'g' (grams) and never use 'ounce' or 'oz'. For whole items, the unit should be 'piece' or 'unit' instead of 'cup' or 'grams'.

          Return the data as a single JSON array containing an object for each main food item.
          If the food is not found, return an empty array: [].
          Example for "5 bananas":
          [
            {
              "food_name": "Banana", "serving_unit": "pieces", "serving_amount": "5", "calories": 525, "protein": 6.5, "carbohydrates": 135, "fat": 2, "fiber": 17.5, "sugar": 72.5, "sodium": 5, "potassium": 2100, "calcium": 30, "iron": 1.5, "vitamin_c": 51
            }
          ]
        `;

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

        if (error.status === 503 || (error.code && error.code === 'unavailable')) {
          const delay = Math.pow(2, retries) * 1000;
          console.log(`Retrying in ${delay / 1000} seconds...`);
          await new Promise(resolve => setTimeout(resolve, delay));
          retries++;
        } else {
          if (error.code) { // Re-throw HttpsError directly
            throw error;
          }
          throw new HttpsError("internal", "An unknown error occurred during Gemini analysis.");
        }
      }
    }

    if (lastError) {
      if (lastError.code) {
        throw lastError;
      }
      throw new HttpsError("unavailable", "The Gemini API is currently unavailable after multiple retries.");
    }
  }
);


// =================================================================
// NEW FUNCTION FOR DIET PLAN GENERATION
// =================================================================
exports.generateDietPlan = onCall(
  {
    region: "me-west1",
    secrets: [geminiApiKeySecret],
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError(
        "unauthenticated",
        "The function must be called while authenticated.",
      );
    }

    const userProfile = request.data.userProfile;
    if (!userProfile || typeof userProfile !== 'string') {
      throw new HttpsError(
        "invalid-argument",
        "The function must be called with a 'userProfile' string argument.",
      );
    }

    try {
        const geminiApiKey = geminiApiKeySecret.value();
        const genAI = new GoogleGenerativeAI(geminiApiKey);
        const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" });

        const prompt = `
          You are a helpful and experienced nutritionist.
          Analyze the following user's diet and health profile.
          Based on their answers, calculate a personalized daily diet plan.

          Your response MUST be a valid JSON object. Do not include any text, markdown, or formatting before or after the JSON object.

          The JSON object must have the following structure:
          {
            "dailyCalories": <integer>,
            "proteinGrams": <integer>,
            "carbsGrams": <integer>,
            "fatGrams": <integer>,
            "recommendations": "<string with 2-3 brief, actionable recommendations>",
            "disclaimer": "This diet plan is AI-generated. Consult with a healthcare professional before making significant dietary changes."
          }

          Here is the user's profile:
          ---
          ${userProfile}
          ---
        `;

        const result = await model.generateContent(prompt);
        const response = result.response;
        const text = response.text();

        const cleanedText = extractJsonFromMarkdown(text);
        console.log("Cleaned diet plan response:", cleanedText);

        const planData = JSON.parse(cleanedText);

        // Return in the format the client expects
        return { success: true, data: planData };

    } catch (error) {
        console.error("Error generating diet plan:", error);
        if (error instanceof SyntaxError) {
             throw new HttpsError(
                "internal",
                "Gemini response was not valid JSON.",
                error.message,
            );
        }
        throw new HttpsError(
            "internal",
            "Failed to generate diet plan from AI.",
            error.message,
        );
    }
  }
);