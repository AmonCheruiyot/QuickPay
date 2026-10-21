# QuickPay University Assignment Demonstration & Viva Guide (Supabase Cloud)

This document provides a step-by-step demonstration script and sample Viva Q&A questions for presenting QuickPay to a university computer science / software engineering lecturer.

---

## 1. Step-by-Step Lecturer Demonstration Walkthrough

### Step 1: Explain Architecture & Cloud Setup
1. Show the project structure (`/app` Android client, `/supabase` Supabase Cloud configuration, migrations, and Edge Functions).
2. Point out that no laptop Express server or ngrok tunnel is required; the backend is deployed on **Supabase Edge Functions** (`https://injsofnemhbedeswjyev.supabase.co/functions/v1/quickpay-api`).
3. **Key Viva Point**: Explain that no Daraja credentials or consumer secrets are stored in the Android app; all OAuth and signing occurs on Supabase server-side secrets.

### Step 2: Show Supabase Database Schema & RLS
1. Open `supabase/migrations/20260330000000_create_payments_table.sql`.
2. Show the `payments` table definition with `reference`, `phone`, `amount`, `status`, `merchant_request_id`, `checkout_request_id`, `receipt_number`, `result_code`, `result_description`, and timestamps.
3. Show Row Level Security (RLS) policies allowing public read access while restricting write mutations to server-side functions.

### Step 3: Run Android Test Suite & Build
1. Run Android unit tests and assemble Debug APK:
   ```bash
   unset ANDROID_PREFS_ROOT && ./gradlew test assembleDebug
   ```
2. Show `BUILD SUCCESSFUL` output and unit test results.

### Step 4: Live Payment Initiation Demo
1. Open QuickPay Android application on emulator or physical phone.
2. Enter M-Pesa phone number (e.g. `0712345678`).
3. Enter amount (e.g. `10` KSh).
4. Tap **Pay with M-Pesa**.
5. Show Android sending request to Supabase Edge Function `POST /stk-push`.
6. Show Edge Function creating a `PENDING` record in Supabase PostgreSQL, authenticating with Daraja, generating Base64 password (`Shortcode + Passkey + Timestamp`), and submitting STK Push request to Safaricom.
7. Observe Android UI updating to **Awaiting M-Pesa PIN input...** with transaction reference (`QP...`).

### Step 5: Real Daraja Webhook & Receipt Verification
1. Show Safaricom posting real HTTP callback payload to `https://injsofnemhbedeswjyev.supabase.co/functions/v1/quickpay-api/callback`.
2. Show Edge Function parsing callback, extracting receipt number (`QPK...`), and updating transaction status in PostgreSQL.
3. Tap **Refresh Status** in Android app.
4. Show Android updating UI to **SUCCESS** badge with M-Pesa receipt displayed!

### Step 6: Transaction History
1. Tap **View Transaction History**.
2. Show `RecyclerView` fetching past transactions from Supabase PostgreSQL via `GET /history`.
3. Tap any transaction row to open its status details.

---

## 2. Sample Viva Questions & Technical Answers

### Q1: Why did you migrate from Node.js/Express + SQLite to Supabase Edge Functions + PostgreSQL?
**Answer**:
Migrating to Supabase Edge Functions (Deno TypeScript) and Supabase PostgreSQL offers serverless cloud scalability and eliminates local infrastructure dependencies. Unlike a local Express server requiring `ngrok` for webhooks, Supabase Edge Functions provide a permanent, public HTTPS URL for Safaricom Daraja callback webhooks, making the payment architecture production-ready and accessible from any mobile device on any network.

### Q2: How does STK Push (Lipa Na M-Pesa Online) work under the hood?
**Answer**:
STK Push (Sim ToolKit Push) is an asynchronous payment workflow:
1. The backend sends a `POST` request to Daraja containing the customer's phone number, amount, shortcode, password (Base64 of `Shortcode + Passkey + Timestamp`), and a public `CallBackURL`.
2. Safaricom's gateway receives the request, validates credentials, and sends a SIM network command to the customer's phone prompting them to enter their M-Pesa PIN.
3. When the user enters their PIN, Safaricom processes the transaction asynchronously and sends an HTTP `POST` callback payload to our backend's `CallBackURL` containing the status and M-Pesa receipt number.

### Q3: How do you handle idempotency and prevent duplicate payment callbacks?
**Answer**:
In `paymentService.ts`, each transaction is matched using Daraja's unique `CheckoutRequestID`. Before updating transaction status, the backend checks the existing status in PostgreSQL. If a transaction is already marked `SUCCESS`, a duplicate callback or a subsequent failure notification is safely ignored and cannot overwrite the `SUCCESS` state.

### Q4: How is security enforced on Supabase PostgreSQL?
**Answer**:
Row Level Security (RLS) is enabled on the `payments` table. Public/anonymous clients have `SELECT` permission to view payment statuses and history, while `INSERT` and `UPDATE` mutations are strictly restricted to server-side Edge Functions using the service role key or server-side security context.

### Q5: How does QuickPay handle phone number normalization?
**Answer**:
Kenyan M-Pesa phone numbers can be entered in various formats (e.g. `0712345678`, `0112345678`, `+254712345678`, `712345678`). The utility function `normalizePhoneNumber` uses regular expressions to strip spaces/symbols and convert all valid formats into the standard 12-digit format `2547XXXXXXXX` or `2541XXXXXXXX` required by Safaricom Daraja.
