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
        const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });

        const prompt = `
          You are a nutritional assistant.
          Analyze the nutritional content of the food item(s) and quantity specified in "${foodName}".
          For each distinct, main food item in the request, provide the calories, protein, carbs, and fat for that exact quantity.
          Minor components (e.g., sauces, small garnishes, seasonings) that are typically consumed with a main dish should not be separated. For example, "pasta with tomato sauce" is one item, but "rice with chicken breast" contains two separate main food items.
          If a number is not specified for a main food item, use a common, natural serving size (e.g., "1 banana", "1 bowl").
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
              "fat": "2"
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
              "fat": "0.4"
            },
            {
              "food_name": "Chicken Breast",
              "serving_unit": "ounce",
              "serving_amount": "3",
              "calories": "165",
              "protein": "31",
              "carbohydrates": "0",
              "fat": "3.6"
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
              "fat": "6"
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
