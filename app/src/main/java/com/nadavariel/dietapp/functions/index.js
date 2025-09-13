const { onCall } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const axios = require("axios");
const crypto = require("crypto");
const qs = require("qs");
const { GoogleGenerativeAI } = require("@google/generative-ai");

// Define secrets (must match what you set with `firebase functions:secrets:set`)
const consumerKeySecret = defineSecret("FATSECRET_KEY");
const consumerSecretSecret = defineSecret("FATSECRET_SECRET");
const geminiApiKeySecret = defineSecret("GEMINI_API_KEY");

exports.analyzeImage = onCall(
  {
    region: "me-west1",
    vpcConnector: "diet-app-connector",
    secrets: [consumerKeySecret, consumerSecretSecret], // allow access to secrets
  },
  async (data) => {
    try {
      console.log("Full data object received:", data);

      const foodName = data.data.foodName;
      console.log("Received food name:", foodName);

      // Retrieve secret values securely
      const consumerKey = consumerKeySecret.value();
      const consumerSecret = consumerSecretSecret.value();

      console.log("Using Consumer Key:", consumerKey);

      const url = "https://platform.fatsecret.com/rest/server.api";
      const httpMethod = "POST";

      const timestamp = Math.floor(Date.now() / 1000);
      const nonce = crypto.randomBytes(16).toString("hex");

      const params = {
        method: "foods.search.v2",
        search_expression: foodName,
        format: "json",
        max_results: "1",
        oauth_consumer_key: consumerKey,
        oauth_nonce: nonce,
        oauth_signature_method: "HMAC-SHA1",
        oauth_timestamp: timestamp,
        oauth_version: "1.0",
      };

      // Sort parameters
      const sortedParams = Object.keys(params)
        .sort()
        .reduce((acc, key) => {
          acc[key] = params[key];
          return acc;
        }, {});

      const encodedParams = qs.stringify(sortedParams);
      const signatureBaseString = [
        httpMethod,
        encodeURIComponent(url),
        encodeURIComponent(encodedParams),
      ].join("&");

      const signingKey = `${encodeURIComponent(consumerSecret)}&`;

      const signature = crypto
        .createHmac("sha1", signingKey)
        .update(signatureBaseString)
        .digest("base64");

      params.oauth_signature = signature;

      const finalUrl = `${url}?${qs.stringify(params)}`;

      const response = await axios({
        method: httpMethod,
        url: finalUrl,
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
        },
      });

      return {
        success: true,
        data: response.data,
      };
    } catch (error) {
      console.error("An error occurred:", error);
      if (error.response) {
        console.error(
          "FatSecret API responded with an error:",
          error.response.status,
          error.response.data
        );
      } else if (error.request) {
        console.error("No response received from FatSecret API.");
      } else {
        console.error("Error in Axios request setup:", error.message);
      }

      return {
        success: false,
        error: error.message,
      };
    }
  }
);

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
    vpcConnector: "diet-app-connector",
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
          Analyze the nutritional content of the food item and quantity specified in "${foodName}".
          Provide the calories, protein, carbs, and fat for that exact quantity.
          If a number is not specified, use a common, natural serving size (e.g., "1 banana", "1 bowl").
          Return the data as a single JSON object with the following structure:
          {
            "food_name": "...",
            "serving_unit": "...",
            "serving_amount": "...",
            "calories": "...",
            "protein": "...",
            "carbohydrates": "...",
            "fat": "..."
          }
          If the food is not found, return an empty object: {}.
          Example for "5 bananas":
          {
            "food_name": "Banana",
            "serving_unit": "pieces",
            "serving_amount": "5",
            "calories": "525",
            "protein": "6.5",
            "carbohydrates": "135",
            "fat": "2"
          }
          Example for "cooked rice":
          {
            "food_name": "Cooked Rice",
            "serving_unit": "cup",
            "serving_amount": "1",
            "calories": "205",
            "protein": "4.3",
            "carbohydrates": "44.5",
            "fat": "0.4"
          }
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
