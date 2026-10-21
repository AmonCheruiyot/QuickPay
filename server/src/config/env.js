const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../../.env') });

function validateEnv() {
  const required = [
    'DARAJA_CONSUMER_KEY',
    'DARAJA_CONSUMER_SECRET',
    'DARAJA_PASSKEY',
    'DARAJA_SHORTCODE',
  ];

  const missing = required.filter((key) => !process.env[key]);
  if (missing.length > 0) {
    throw new Error(`Missing required environment variables: ${missing.join(', ')}`);
  }
}

validateEnv();

module.exports = {
  port: parseInt(process.env.PORT || '3000', 10),
  nodeEnv: process.env.NODE_ENV || 'development',
  daraja: {
    consumerKey: process.env.DARAJA_CONSUMER_KEY,
    consumerSecret: process.env.DARAJA_CONSUMER_SECRET,
    passkey: process.env.DARAJA_PASSKEY,
    shortcode: process.env.DARAJA_SHORTCODE || '174379',
    baseUrl: process.env.DARAJA_BASE_URL || 'https://sandbox.safaricom.co.ke',
    callbackUrl: process.env.CALLBACK_URL || 'http://localhost:3000/api/payments/callback',
  },
  dbPath: process.env.DB_PATH || path.join(__dirname, '../../quickpay.db'),
};
