# QuickPay Express Backend API Reference

Base URL: `http://localhost:3000` or `http://10.0.2.2:3000`

---

## 1. Health Endpoint

### `GET /health`

Checks if the server is running and database is accessible.

#### Request
No parameters required.

#### Response `200 OK`
```json
{
  "status": "UP",
  "app": "QuickPay Daraja Backend",
  "timestamp": "2026-03-30T15:45:00.000Z"
}
```

---

## 2. Initiate Payment (STK Push)

### `POST /api/payments/stk-push`

Validates input, normalizes Kenyan phone number, saves PENDING transaction in SQLite, authenticates with Daraja, and triggers Lipa Na M-Pesa STK Push.

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
  "message": "STK Push prompt sent to your phone. Please enter your M-Pesa PIN.",
  "reference": "QP17926158733266391",
  "checkoutRequestId": "ws_CO_30032026154500123",
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

## 3. Daraja STK Push Callback Endpoint

### `POST /api/payments/callback`

Received asynchronously from Safaricom Daraja API after user completes or cancels prompt.

#### Success Callback Payload Example
```json
{
  "Body": {
    "stkCallback": {
      "MerchantRequestID": "29115-34620561-1",
      "CheckoutRequestID": "ws_CO_30032026154500123",
      "ResultCode": 0,
      "ResultDesc": "The service request is processed successfully.",
      "CallbackMetadata": {
        "Item": [
          { "Name": "Amount", "Value": 10 },
          { "Name": "MpesaReceiptNumber", "Value": "QPK1234567" },
          { "Name": "TransactionDate", "Value": 20260330154500 },
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

### `GET /api/payments/:reference`

Retrieves status for a specific transaction by internal reference.

#### Response `200 OK`
```json
{
  "success": true,
  "payment": {
    "id": 1,
    "reference": "QP17926158733266391",
    "phone": "254712345678",
    "maskedPhone": "2547****5678",
    "amount": 10,
    "status": "SUCCESS",
    "merchantRequestId": "29115-34620561-1",
    "checkoutRequestId": "ws_CO_30032026154500123",
    "receiptNumber": "QPK1234567",
    "resultCode": 0,
    "resultDescription": "The service request is processed successfully.",
    "transactionDate": "20260330154500",
    "createdAt": "2026-03-30 15:45:00",
    "updatedAt": "2026-03-30 15:45:05"
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

### `GET /api/payments?limit=50&offset=0`

Retrieves list of past transactions ordered by creation date descending.

#### Response `200 OK`
```json
{
  "success": true,
  "count": 1,
  "payments": [
    {
      "id": 1,
      "reference": "QP17926158733266391",
      "phone": "254712345678",
      "maskedPhone": "2547****5678",
      "amount": 10,
      "status": "SUCCESS",
      "receiptNumber": "QPK1234567",
      "createdAt": "2026-03-30 15:45:00"
    }
  ]
}
```
