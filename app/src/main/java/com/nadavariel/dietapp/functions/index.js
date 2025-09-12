const { onCall } = require("firebase-functions/v2/https");
const axios = require("axios");
const crypto = require("crypto");
const OAuth = require("oauth-1.0a");
const qs = require('qs');

const consumerKey = "";
const consumerSecret = "";

axios.interceptors.request.use(request => {
  console.log('Starting Request');
  console.log(JSON.stringify(request, null, 2));
  return request;
});

axios.interceptors.request.use(request => {
  console.log('Starting Request');
  console.log(JSON.stringify(request, null, 2));
  return request;
});

axios.interceptors.response.use(response => {
  console.log('Response:');
  console.log(JSON.stringify(response.data, null, 2));
  return response;
}, error => {
  console.log('Response Error:');
  console.log(JSON.stringify(error.response.data, null, 2));
  return Promise.reject(error);
});

exports.analyzeImage = onCall({
  region: "me-west1",
  vpcConnector: "diet-app-connector",
}, async (data) => {
  try {
    console.log("Full data object received:", data);

    const foodName = data.data.foodName;

    console.log("Received food name:", foodName);

    const url = 'https://platform.fatsecret.com/rest/server.api';
    const httpMethod = 'POST';

    const timestamp = Math.floor(Date.now() / 1000);
    const nonce = crypto.randomBytes(16).toString('hex');

    // 1. All parameters, including OAuth, are put in a single object
    const params = {
      method: "foods.search.v2",
      search_expression: foodName,
      format: "json",
      oauth_consumer_key: consumerKey,
      oauth_nonce: nonce,
      oauth_signature_method: 'HMAC-SHA1',
      oauth_timestamp: timestamp,
      oauth_version: '1.0',
    };

    // 2. Parameters are sorted lexicographically
    const sortedParams = Object.keys(params)
      .sort()
      .reduce((acc, key) => {
        acc[key] = params[key];
        return acc;
      }, {});

    // 3. Parameters are URL encoded and joined to form the signature base string
    const encodedParams = qs.stringify(sortedParams);
    const signatureBaseString = [
      httpMethod,
      encodeURIComponent(url),
      encodeURIComponent(encodedParams),
    ].join('&');

    // 4. A signing key is created from the consumer secret
    const signingKey = `${encodeURIComponent(consumerSecret)}&`;

    // 5. HMAC-SHA1 signature is generated and Base64 encoded
    const signature = crypto
      .createHmac('sha1', signingKey)
      .update(signatureBaseString)
      .digest('base64');

    // 6. The signature is added to the parameters
    params.oauth_signature = signature;

    // 7. The final request URL is built with all parameters in the query string
    const finalUrl = `${url}?${qs.stringify(params)}`;

    const response = await axios({
      method: httpMethod,
      url: finalUrl,
      // The headers and data are kept empty for this specific API request
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      data: null, // No body is sent with the POST request
    });

    return {
      success: true,
      data: response.data,
    };
  } catch (error) {
    console.error("An error occurred:", error);
    if (error.response) {
      console.error("FatSecret API responded with an error:", error.response.status, error.response.data);
    } else if (error.request) {
      console.error("No response received from FatSecret API.");
    } else {
      console.error('Error in Axios request setup:', error.message);
    }

    return {
      success: false,
      error: error.message,
    };
  }
});
