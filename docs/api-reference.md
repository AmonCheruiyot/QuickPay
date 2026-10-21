# QuickPay Supabase Edge Function API Reference

Base URL: `https://injsofnemhbedeswjyev.supabase.co/functions/v1/quickpay-api`

---

## 1. Health Endpoint

### `GET /health`

Checks if the Supabase Edge Function is live and responsive.

#### Request
No parameters required.

#### Response `200 OK`
```json
{
  "status": "UP",
  "app": "QuickPay Supabase Edge Function",
  "timestamp": "2026-10-21T21:21:20.313Z"
}
```

---

## 2. Initiate Payment (STK Push)

### `POST /stk-push` (or `/api/payments/stk-push`)

Validates input, normalizes Kenyan phone number, saves PENDING transaction in Supabase PostgreSQL, authenticates with Daraja, and triggers Lipa Na M-Pesa STK Push.

#### Request Body
```json
{
  "phone": "0712345678",
  "amount": 10
}
```

#### Success Response `200 OK`
```json
{
  "success": true,
  "message": "Success. Request accepted for processing",
  "reference": "QP17926176907704673",
  "checkoutRequestId": "ws_CO_221020260021319712345678",
  "status": "PENDING",
  "maskedPhone": "2547****5678",
  "amount": 10
}
```

#### Error Response `400 Bad Request`
```json
{
  "error": "Invalid Phone Number",
  "message": "Invalid Kenyan phone number format. Must be a valid M-Pesa number (e.g. 0712345678 or 0112345678)"
}
```

---

## 3. Daraja STK Push Callback Webhook

### `POST /callback` (or `/api/payments/callback`)

Public webhook endpoint called asynchronously by Safaricom Daraja API after user completes or cancels payment prompt.

#### Success Callback Payload Example
```json
{
  "Body": {
    "stkCallback": {
      "MerchantRequestID": "29115-34620561-1",
      "CheckoutRequestID": "ws_CO_221020260021319712345678",
      "ResultCode": 0,
      "ResultDesc": "The service request is processed successfully.",
      "CallbackMetadata": {
        "Item": [
          { "Name": "Amount", "Value": 10 },
          { "Name": "MpesaReceiptNumber", "Value": "QPK1234567" },
          { "Name": "TransactionDate", "Value": 20261021212130 },
          { "Name": "PhoneNumber", "Value": 254712345678 }
        ]
      }
    }
  }
}
```

#### Safaricom Acknowledgment Response `200 OK`
```json
{
  "ResultCode": 0,
  "ResultDesc": "Callback processed successfully"
}
```

---

## 4. Get Transaction Status

### `GET /status/:reference` (or `/api/payments/:reference`)

Retrieves transaction details and status from Supabase PostgreSQL by internal reference.

#### Response `200 OK`
```json
{
  "success": true,
  "payment": {
    "id": 1,
    "reference": "QP17926176907704673",
    "phone": "254712345678",
    "maskedPhone": "2547****5678",
    "amount": 10,
    "status": "FAILED",
    "merchantRequestId": "727d-4bee-88dd-906bf2f5fdf1179596",
    "checkoutRequestId": "ws_CO_221020260021319712345678",
    "receiptNumber": null,
    "resultCode": 11,
    "resultDescription": "The DebitParty is in an invalid state.",
    "transactionDate": null,
    "createdAt": "2026-10-21T21:21:30.934901+00:00",
    "updatedAt": "2026-10-21T21:21:34.053063+00:00"
  }
}
```

#### Error Response `404 Not Found`
```json
{
  "error": "Not Found",
  "message": "Transaction with reference 'QP99999' was not found."
}
```

---

## 5. Get Transaction History

### `GET /history` (or `/api/payments?limit=50&offset=0`)

Retrieves list of past transactions from Supabase PostgreSQL ordered by creation date descending.

#### Response `200 OK`
```json
{
  "success": true,
  "count": 2,
  "payments": [
    {
      "id": 2,
      "reference": "QP17926177813784082",
      "phone": "254722000000",
      "maskedPhone": "2547****0000",
      "amount": 250,
      "status": "SUCCESS",
      "receiptNumber": "QPK99887766",
      "createdAt": "2026-10-21T21:23:01.55724+00:00"
    }
  ]
}
```
