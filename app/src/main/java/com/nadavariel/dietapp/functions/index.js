const { onCall } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const axios = require("axios");
const crypto = require("crypto");
const qs = require("qs");

// Define secrets (must match what you set with `firebase functions:secrets:set`)
const consumerKeySecret = defineSecret("FATSECRET_KEY");
const consumerSecretSecret = defineSecret("FATSECRET_SECRET");

// Axios interceptors for debugging
axios.interceptors.request.use(request => {
  console.log("Starting Request");
  console.log(JSON.stringify(request, null, 2));
  return request;
});

axios.interceptors.response.use(
  response => {
    console.log("Response:");
    console.log(JSON.stringify(response.data, null, 2));
    return response;
  },
  error => {
    console.log("Response Error:");
    if (error.response) {
      console.log(JSON.stringify(error.response.data, null, 2));
    }
    return Promise.reject(error);
  }
);

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
