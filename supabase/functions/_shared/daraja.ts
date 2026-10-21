import { getDarajaTimestamp } from './timestamp.ts';

let cachedToken: string | null = null;
let tokenExpiryTime = 0;

export async function getOAuthToken(): Promise<string> {
  const now = Date.now();
  if (cachedToken && now < tokenExpiryTime - 60000) {
    return cachedToken;
  }

  const consumerKey = Deno.env.get('DARAJA_CONSUMER_KEY') || 'G0EBdLj2qgQ0XxitveJiGsru5THgtEFvuOmDTBRFpHrthaaY';
  const consumerSecret = Deno.env.get('DARAJA_CONSUMER_SECRET') || 'aMy06cohopbG91UNaTMhLKDlx4i2DBioQTZJj6xKsojsbyCvCF88w8iyNfaHYJme';
  const baseUrl = Deno.env.get('DARAJA_BASE_URL') || 'https://sandbox.safaricom.co.ke';

  const auth = btoa(`${consumerKey}:${consumerSecret}`);
  const url = `${baseUrl}/oauth/v1/generate?grant_type=client_credentials`;

  const response = await fetch(url, {
    method: 'GET',
    headers: {
      Authorization: `Basic ${auth}`,
    },
  });

  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`Daraja OAuth Failed (${response.status}): ${errorBody}`);
  }

  const data = await response.json();
  if (data && data.access_token) {
    cachedToken = data.access_token;
    const expiresIn = parseInt(data.expires_in || '3599', 10) * 1000;
    tokenExpiryTime = now + expiresIn;
    return cachedToken;
  } else {
    throw new Error('Invalid OAuth response structure from Daraja');
  }
}

export function generatePassword(shortcode: string, passkey: string, timestamp: string): string {
  return btoa(`${shortcode}${passkey}${timestamp}`);
}

export interface StkPushParams {
  phone: string;
  amount: number;
  accountReference?: string;
  transactionDesc?: string;
  callbackUrl?: string;
}

export async function initiateStkPush({
  phone,
  amount,
  accountReference = 'QuickPay',
  transactionDesc = 'Payment for QuickPay',
  callbackUrl,
}: StkPushParams) {
  const token = await getOAuthToken();
  const timestamp = getDarajaTimestamp();

  const shortcode = Deno.env.get('DARAJA_SHORTCODE') || '174379';
  const passkey = Deno.env.get('DARAJA_PASSKEY') || 'bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919';
  const baseUrl = Deno.env.get('DARAJA_BASE_URL') || 'https://sandbox.safaricom.co.ke';

  const defaultCallback = `${Deno.env.get('SUPABASE_URL')}/functions/v1/quickpay-api/callback`;
  const finalCallbackUrl = callbackUrl || Deno.env.get('CALLBACK_URL') || defaultCallback;

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
    CallBackURL: finalCallbackUrl,
    AccountReference: accountReference,
    TransactionDesc: transactionDesc,
  };

  const url = `${baseUrl}/mpesa/stkpush/v1/processrequest`;
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  const responseData = await response.json();

  if (!response.ok) {
    const msg = responseData.errorMessage || responseData.ResponseDescription || 'STK Push rejected by Daraja';
    const err = new Error(msg);
    (err as any).darajaResponse = responseData;
    (err as any).statusCode = response.status;
    throw err;
  }

  return responseData;
}
