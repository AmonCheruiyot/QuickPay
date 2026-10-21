# QuickPay University Assignment Demonstration & Viva Guide

This document provides a step-by-step demonstration script and sample Viva Q&A questions for presenting QuickPay to a university computer science / software engineering lecturer.

---

## 1. Step-by-Step Lecturer Demonstration Walkthrough

### Step 1: Explain Architecture & Security Setup
1. Show the project structure (`/app` Android client, `/server` Express Node.js backend, SQLite database).
2. Point out `.env` in `server/` and `.env.example` in repository.
3. **Key Viva Point**: Explain that no Daraja credentials or consumer secrets are stored in the Android app; all OAuth and signing occurs on the Express backend.

### Step 2: Show Backend Startup
1. Open terminal and run:
   ```bash
   cd server
   npm start
   ```
2. Point out database auto-migration on startup creating the `payments` table in SQLite (`quickpay.db`).

### Step 3: Run Backend & Android Test Suite
1. Run backend automated tests:
   ```bash
   npm test
   ```
2. Show that all 14 tests pass (validation, idempotency, callback, E2E flow).
3. Run Android unit tests:
   ```bash
   unset ANDROID_PREFS_ROOT && ./gradlew test
   ```

### Step 4: Live Payment Initiation Demo
1. Open QuickPay Android application on emulator or physical phone.
2. Enter M-Pesa phone number (e.g. `0712345678`).
3. Enter amount (e.g. `10` KSh).
4. Tap **Pay with M-Pesa**.
5. Show Android sending request to Express `POST /api/payments/stk-push`.
6. Show Express creating a `PENDING` record in SQLite, authenticating with Daraja, generating Base64 password (`Shortcode + Passkey + Timestamp`), and submitting STK Push request.
7. Observe Android UI updating to **Awaiting M-Pesa PIN input...** with transaction reference (`QP...`).

### Step 5: Simulate / Receive Daraja Callback
1. In development, simulate or receive the Daraja callback at `POST /api/payments/callback`.
2. Show server logs showing callback received with `CheckoutRequestID` and M-Pesa receipt number (`QPK1234567`).
3. Tap **Refresh Status** in Android app or check status view.
4. Show Android updating UI to **SUCCESS** badge with M-Pesa receipt displayed!

### Step 6: Transaction History
1. Tap **View Transaction History**.
2. Show `RecyclerView` fetching past transactions from `GET /api/payments`.
3. Tap any transaction row to open its status details.

---

## 2. Sample Viva Questions & Technical Answers

### Q1: How does STK Push (Lipa Na M-Pesa Online) work under the hood?
**Answer**:
STK Push (Sim ToolKit Push) is an asynchronous payment workflow.
1. The backend sends a `POST` request to Daraja containing the customer's phone number, amount, shortcode, password (Base64 of `Shortcode + Passkey + Timestamp`), and a public `CallBackURL`.
2. Safaricom's gateway receives the request, validates credentials, and sends a SIM network command to the customer's phone prompting them to enter their M-Pesa PIN.
3. When the user enters their PIN, Safaricom processes the transaction asynchronously and sends an HTTP `POST` callback payload to our backend's `CallBackURL` containing the status and M-Pesa receipt number.

### Q2: Why is the payment processed asynchronously instead of synchronously in a single API call?
**Answer**:
Because entering an M-Pesa PIN on a mobile phone requires human interaction that can take 5 to 60 seconds (or timeout if the user declines). Holding an HTTP connection open for that duration is prone to timeouts and poor performance. An asynchronous architecture allows the API to respond immediately with a `PENDING` status while Safaricom processes the PIN input in the background.

### Q3: How do you handle idempotency and prevent duplicate payment callbacks?
**Answer**:
In `paymentService.js`, each transaction is matched using Daraja's unique `CheckoutRequestID`. Before updating transaction status, the backend checks the existing status in SQLite. If a transaction is already marked `SUCCESS`, a duplicate callback or a subsequent failure notification is safely ignored and cannot overwrite the `SUCCESS` state.

### Q4: Why shouldn't payment credentials (Consumer Key/Secret) be stored in the Android application?
**Answer**:
Android APK files can be decompiled or reverse-engineered using tools like `jadx` or `apktool`. If API keys or secrets are embedded in Kotlin code or Android XML resources, attackers can extract them and abuse the Daraja account. All secret credentials must reside in a secure server-side environment (`.env`).

### Q5: How does QuickPay handle phone number normalization?
**Answer**:
Kenyan M-Pesa phone numbers can be entered in various formats (e.g. `0712345678`, `0112345678`, `+254712345678`, `712345678`). The utility function `normalizePhoneNumber` uses regular expressions to strip spaces/symbols and convert all valid formats into the standard 12-digit format `2547XXXXXXXX` or `2541XXXXXXXX` required by Safaricom Daraja.

### Q6: What happens if the server restarts while a payment is pending?
**Answer**:
Because QuickPay saves every payment in SQLite (`payments` table) with status `PENDING` *before* sending the request to Daraja, transaction records persist across server restarts. When Daraja eventually posts the callback, the backend looks up the record by `CheckoutRequestID` and updates its status correctly.
