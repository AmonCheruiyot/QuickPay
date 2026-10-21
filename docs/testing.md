# QuickPay Testing & Verification Report (Supabase Cloud Migration)

## 1. Overview

Testing was performed across both the live deployed **Supabase Edge Function** (`quickpay-api`), the **Supabase PostgreSQL** cloud database, and the **Android Kotlin Client** to verify payment initiation, input validation, Daraja OAuth & STK Push formatting, cloud database persistence, idempotent callback handling, and status queries.

---

## 2. Live Supabase Cloud API End-to-End Verification

Tested directly against `https://injsofnemhbedeswjyev.supabase.co/functions/v1/quickpay-api`:

### Test 1: Health Check Endpoint
```bash
curl -s -X GET https://injsofnemhbedeswjyev.supabase.co/functions/v1/quickpay-api/health
```
**Result**: `200 OK`
```json
{
  "status": "UP",
  "app": "QuickPay Supabase Edge Function",
  "timestamp": "2026-10-21T21:21:20.313Z"
}
```

### Test 2: STK Push Initiation & Live Daraja Request
```bash
curl -s -X POST https://injsofnemhbedeswjyev.supabase.co/functions/v1/quickpay-api/stk-push \
  -H "Content-Type: application/json" \
  -d '{"phone": "0712345678", "amount": 10}'
```
**Result**: `200 OK`
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

### Test 3: Real Webhook Delivery from Safaricom Daraja
Safaricom Sandbox processed the request and posted an asynchronous callback webhook back to `https://injsofnemhbedeswjyev.supabase.co/functions/v1/quickpay-api/callback`.

Querying status immediately after callback:
```bash
curl -s -X GET https://injsofnemhbedeswjyev.supabase.co/functions/v1/quickpay-api/status/QP17926176907704673
```
**Result**: Status updated in PostgreSQL from `PENDING` to `FAILED` (`ResultCode: 11`, `ResultDesc: "The DebitParty is in an invalid state."`).

### Test 4: Successful Webhook Processing & Receipt Storage
Simulating successful payment callback:
```bash
curl -s -X POST https://injsofnemhbedeswjyev.supabase.co/functions/v1/quickpay-api/callback \
  -H "Content-Type: application/json" \
  -d '{"Body":{"stkCallback":{"MerchantRequestID":"M_REQ_TEST","CheckoutRequestID":"ws_CO_221020260023025722000000","ResultCode":0,"ResultDesc":"The service request is processed successfully.","CallbackMetadata":{"Item":[{"Name":"Amount","Value":250},{"Name":"MpesaReceiptNumber","Value":"QPK99887766"},{"Name":"TransactionDate","Value":20261021212500},{"Name":"PhoneNumber","Value":254722000000}]}}}}'
```
Querying updated status:
**Result**: Status updated to `SUCCESS`, storing M-Pesa receipt `QPK99887766`.

### Test 5: Idempotency Protection
Submitting a subsequent failed callback for the already successful transaction `QP17926177813784082`.
**Result**: `SUCCESS` status and receipt `QPK99887766` preserved without corruption!

---

## 3. Android Automated Unit Tests & APK Build

Executed using Gradle (`./gradlew test assembleDebug`):

```bash
unset ANDROID_PREFS_ROOT && ./gradlew test assembleDebug
```

### Execution Output
```text
> Task :app:compileDebugKotlin
> Task :app:compileDebugUnitTestKotlin
> Task :app:testDebugUnitTest
> Task :app:assembleDebug

BUILD SUCCESSFUL in 44s
43 actionable tasks: 17 executed, 26 up-to-date
```

---

## 4. Summary Matrix

| Domain | Scope | Location | Result |
| :--- | :--- | :--- | :--- |
| **Supabase DB** | PostgreSQL Migration | `supabase/migrations` | APPLIED (`payments` table, indexes, RLS) |
| **Supabase Edge** | Health Endpoint | `https://injsofnemhbedeswjyev.supabase.co/.../health` | PASSED (200 OK) |
| **Supabase Edge** | STK Push Initiation | `https://injsofnemhbedeswjyev.supabase.co/.../stk-push` | PASSED (200 OK) |
| **Supabase Edge** | Live Daraja Callback Webhook | `https://injsofnemhbedeswjyev.supabase.co/.../callback` | PASSED (200 OK) |
| **Supabase Edge** | Idempotency Verification | `https://injsofnemhbedeswjyev.supabase.co/.../callback` | PASSED (Preserved `SUCCESS`) |
| **Supabase Edge** | Transaction History | `https://injsofnemhbedeswjyev.supabase.co/.../history` | PASSED (200 OK) |
| **Android** | Unit Tests & Build | `./gradlew test assembleDebug` | PASSED (`BUILD SUCCESSFUL`) |
