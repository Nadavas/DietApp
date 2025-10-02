const { onCall } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { GoogleGenerativeAI } = require("@google/generative-ai");

// Define secret key
const geminiApiKeySecret = defineSecret("GEMINI_API_KEY");

function extractJsonFromMarkdown(text) {
  // Check if the response is a markdown code block for JSON
  const jsonStart = text.indexOf('```json\n');
  const jsonEnd = text.indexOf('\n```', jsonStart + 7);

  if (jsonStart !== -1 && jsonEnd !== -1) {
    // Extract the content between the markdown fences
    return text.substring(jsonStart + 8, jsonEnd).trim();
  }

  // If it's not a markdown block, assume it's a plain JSON string
  return text.trim();
}

exports.analyzeFoodWithGemini = onCall(
  {
    region: "me-west1",
    secrets: [geminiApiKeySecret],
  },
  async (data) => {
    const MAX_RETRIES = 3;
    let retries = 0;
    let lastError = null;

    while (retries < MAX_RETRIES) {
      try {
        const foodName = data.data.foodName;
        if (!foodName) {
          throw new onCall.HttpsError("invalid-argument", "The 'foodName' parameter is required.");
        }

        const geminiApiKey = geminiApiKeySecret.value();
        const genAI = new GoogleGenerativeAI(geminiApiKey);
        const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" });

        const prompt = `
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
          Example for "pasta with tomato sauce":
          [
            {
              "food_name": "Pasta with Tomato Sauce",
              "serving_unit": "cup",
              "serving_amount": "1",
              "calories": "300",
              "protein": "11",
              "carbohydrates": "50",
              "fat": "6",
              "fiber": "4",
              "sugar": "6",
              "sodium": "450",
              "potassium": "150",
              "calcium": "40",
              "iron": "2",
              "vitamin_c": "8"
            }
          ]
          Example for "caesar salad":
          [
            {
              "food_name": "Caesar Salad",
              "serving_unit": "bowl",
              "serving_amount": "1",
              "calories": "300",
              "protein": "8",
              "carbohydrates": "15",
              "fat": "23",
              "fiber": "2.5",
              "sugar": "4",
              "sodium": "420",
              "potassium": "180",
              "calcium": "100",
              "iron": "1",
              "vitamin_c": "15"
            }
          ]
        `;

        const result = await model.generateContent(prompt);
        const response = result.response;
        const text = response.text();

        const cleanedText = extractJsonFromMarkdown(text);
        console.log("Cleaned Gemini response:", cleanedText);

        let parsedJson;
        try {
          parsedJson = JSON.parse(cleanedText);
        } catch (parseError) {
          console.error("Failed to parse cleaned Gemini response as JSON:", parseError);
          // A specific error for JSON parsing issues
          throw new onCall.HttpsError("internal", "Gemini response was not a valid JSON string. Check function logs for details.");
        }

        return {
          success: true,
          data: parsedJson
        };

      } catch (error) {
        lastError = error;
        console.error(`Attempt ${retries + 1} failed:`, error.message);

        // Check if the error is a temporary 503 from Gemini
        if (error.status === 503) {
          const delay = Math.pow(2, retries) * 1000; // Exponential backoff (1s, 2s, 4s)
          console.log(`Retrying in ${delay / 1000} seconds...`);
          await new Promise(resolve => setTimeout(resolve, delay));
          retries++;
        } else {
          // For any other error (like the `TypeError` or a bad prompt),
          // don't retry and just rethrow the error to the client.
          if (error.code) { // This checks if it's an HttpsError
            throw error;
          } else {
            // Re-throw as a generic internal error to be safe
            throw new onCall.HttpsError("internal", "An unknown error occurred during the Gemini analysis. Please try again.");
          }
        }
      }
    }

    // If all retries fail, throw the last error
    if (lastError) {
      if (lastError.code) {
        throw lastError;
      }
      throw new onCall.HttpsError("unavailable", "The Gemini API is currently unavailable. Please try again in a few moments.");
    }
  }
);
