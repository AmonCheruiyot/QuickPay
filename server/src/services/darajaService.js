const axios = require('axios');
const config = require('../config/env');
const { getDarajaTimestamp } = require('../utils/timestamp');

let cachedToken = null;
let tokenExpiryTime = 0;

/**
 * Gets OAuth access token from Daraja API
 */
async function getOAuthToken() {
  const now = Date.now();
  // Return cached token if still valid (with 60s buffer)
  if (cachedToken && now < tokenExpiryTime - 60000) {
    return cachedToken;
  }

  const { consumerKey, consumerSecret, baseUrl } = config.daraja;
  const auth = Buffer.from(`${consumerKey}:${consumerSecret}`).toString('base64');

  try {
    const url = `${baseUrl}/oauth/v1/generate?grant_type=client_credentials`;
    const response = await axios.get(url, {
      headers: {
        Authorization: `Basic ${auth}`,
      },
      timeout: 10000,
    });

    if (response.data && response.data.access_token) {
      cachedToken = response.data.access_token;
      const expiresIn = parseInt(response.data.expires_in || '3599', 10) * 1000;
      tokenExpiryTime = now + expiresIn;
      return cachedToken;
    } else {
      throw new Error('Invalid OAuth response structure from Daraja');
    }
  } catch (error) {
    const errorDetails = error.response ? error.response.data : error.message;
    throw new Error(`Daraja OAuth Authentication Failed: ${JSON.stringify(errorDetails)}`);
  }
}

/**
 * Generates Base64 STK Push password
 */
function generatePassword(shortcode, passkey, timestamp) {
  return Buffer.from(`${shortcode}${passkey}${timestamp}`).toString('base64');
}

/**
 * Initiates STK Push request to Safaricom Daraja Sandbox
 */
async function initiateStkPush({
  phone,
  amount,
  accountReference = 'QuickPay',
  transactionDesc = 'Payment for QuickPay order',
  callbackUrl,
}) {
  const token = await getOAuthToken();
  const timestamp = getDarajaTimestamp();
  const { shortcode, passkey, baseUrl } = config.daraja;
  const password = generatePassword(shortcode, passkey, timestamp);

  const payload = {
    BusinessShortCode: shortcode,
    Password: password,
    Timestamp: timestamp,
    TransactionType: 'CustomerPayBillOnline',
    Amount: Math.round(amount),
    PartyA: phone,
    PartyB: shortcode,
    PhoneNumber: phone,
    CallBackURL: callbackUrl || config.daraja.callbackUrl,
    AccountReference: accountReference,
    TransactionDesc: transactionDesc,
  };

  try {
    const url = `${baseUrl}/mpesa/stkpush/v1/processrequest`;
    const response = await axios.post(url, payload, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      timeout: 15000,
    });

    return response.data;
  } catch (error) {
    if (error.response) {
      const darajaErr = error.response.data;
      const message = darajaErr.errorMessage || darajaErr.ResponseDescription || 'STK Push rejected by Daraja';
      const err = new Error(message);
      err.darajaResponse = darajaErr;
      err.statusCode = error.response.status;
      throw err;
    }
    throw new Error(`Daraja STK Push Network Error: ${error.message}`);
  }
}

function clearTokenCache() {
  cachedToken = null;
  tokenExpiryTime = 0;
}

module.exports = {
  getOAuthToken,
  generatePassword,
  initiateStkPush,
  clearTokenCache,
};
