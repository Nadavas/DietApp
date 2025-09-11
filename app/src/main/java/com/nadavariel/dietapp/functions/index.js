const { onCall } = require("firebase-functions/v2/https");
const axios = require("axios");

exports.analyzeImage = onCall({
  region: "me-west1",
  vpcConnector: "diet-app-connector",
}, async (data) => {
  try {
    const imageUrl = data.imageUrl;
    const apiKey = process.env.EXTERNAL_API_KEY;

    const response = await axios.post("YOUR_EXTERNAL_API_URL", {
      imageUrl: imageUrl,
    }, {
      headers: {
        "Authorization": `Bearer ${apiKey}`,
      },
    });

    return {
      success: true,
      data: response.data,
    };
  } catch (error) {
    console.error("An error occurred:", error);
    return {
      success: false,
      error: error.message,
    };
  }
});
