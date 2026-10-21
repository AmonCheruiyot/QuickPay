# QuickPay Automated & Integration Testing Report

## 1. Overview

Testing was performed across both the Express Node.js backend and the Android Kotlin client application to verify payment initiation, input validation, Daraja OAuth & STK Push formatting, SQLite database persistence, idempotent callback handling, and status queries.

---

## 2. Backend Automated Test Results

Executed using Node.js Native Test Runner (`node --test test/*.test.js`):

```bash
cd server
npm test
```

### Execution Output
```text
▶ Daraja Callback Processing & Idempotency
  ✔ processes a successful callback and stores MpesaReceiptNumber (4.8ms)
  ✔ processes a user cancellation callback (ResultCode 1032) setting status to CANCELLED (1.8ms)
  ✔ prevents a duplicate or failed callback from overwriting an already SUCCESSFUL transaction (0.9ms)
  ✔ handles callback for unknown CheckoutRequestID gracefully (2.7ms)
✔ Daraja Callback Processing & Idempotency (54.0ms)

▶ End-to-End Payment Flow Integration Test
  ✔ completes end-to-end flow: Initiate -> Pending -> Daraja Callback -> Success Status -> History (57.5ms)
✔ End-to-End Payment Flow Integration Test (95.5ms)

▶ Health Endpoint
  ✔ GET /health returns 200 OK and status UP without credentials (33.7ms)
✔ Health Endpoint (36.1ms)

▶ Payment Service & Database Persistence
  ✔ creates a pending payment in SQLite with unique reference and normalized phone (3.7ms)
  ✔ updates merchant and checkout request IDs (1.4ms)
  ✔ retrieves payment history in reverse chronological order (1.5ms)
  ✔ returns null for non-existent payment reference (4.6ms)
✔ Payment Service & Database Persistence (47.5ms)

▶ Phone & Validation Utilities
  ✔ normalizes various valid Kenyan phone number formats to 254XXXXXXXXX (2.4ms)
  ✔ rejects invalid or non-Kenyan phone numbers (1.0ms)
  ✔ masks phone numbers correctly (0.5ms)
  ✔ generates 14-character Daraja timestamp string (0.6ms)
✔ Phone & Validation Utilities (8.9ms)

ℹ tests 14
ℹ suites 5
ℹ pass 14
ℹ fail 0
ℹ cancelled 0
ℹ skipped 0
ℹ duration_ms 397.7ms
```

---

## 3. Android Automated Unit Test Results

Executed using Gradle (`./gradlew test`):

```bash
unset ANDROID_PREFS_ROOT && ./gradlew test
```

### Execution Output
```text
> Task :app:compileDebugKotlin
> Task :app:compileDebugUnitTestKotlin
> Task :app:testDebugUnitTest
> Task :app:test

BUILD SUCCESSFUL in 8s
27 actionable tasks: 7 executed, 20 up-to-date
```

---

## 4. Summary Matrix

| Domain | Scope | Test File | Test Count | Result |
| :--- | :--- | :--- | :--- | :--- |
| **Backend** | Health Endpoint | `test/health.test.js` | 1 | PASSED |
| **Backend** | Validation & Phone Normalization | `test/validation.test.js` | 4 | PASSED |
| **Backend** | Database & Persistence | `test/payments.test.js` | 4 | PASSED |
| **Backend** | Callback & Idempotency | `test/callback.test.js` | 4 | PASSED |
| **Backend** | End-to-End Payment Integration | `test/e2e.test.js` | 1 | PASSED |
| **Android** | Phone Utilities Unit Tests | `PhoneUtilsTest.kt` | 3 | PASSED |
| **Android** | Debug APK Build | `assembleDebug` | 1 | PASSED |
