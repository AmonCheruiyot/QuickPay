# QuickPay — Android M-Pesa Payment App (Supabase Edge Functions + Safaricom Daraja STK Push)

QuickPay is a complete, runnable Android payment application integrated with Safaricom's Daraja Sandbox API using Lipa Na M-Pesa Express (STK Push). Built as an academic demonstration for university software engineering classes, QuickPay features a serverless **Supabase Edge Functions** backend in TypeScript (Deno), a **Supabase PostgreSQL** cloud database, and a modern Android client in Kotlin.

---

## 🚀 Core Features

- **Lipa Na M-Pesa STK Push**: Initiates instant M-Pesa payment prompts to Kenyan phone numbers.
- **Supabase Cloud Backend**: Serverless Deno TypeScript Edge Functions (`quickpay-api`) deployed on Supabase infrastructure.
- **PostgreSQL Database**: Cloud-hosted transaction storage with Row Level Security (RLS) and automated `updated_at` triggers.
- **Permanent HTTPS Webhooks**: Public callback URL (`https://injsofnemhbedeswjyev.supabase.co/functions/v1/quickpay-api/callback`) for receiving Safaricom callbacks without needing local Express or ngrok.
- **Phone Number Normalization**: Automatically converts various Kenyan phone formats (`0712345678`, `0112345678`, `+2547...`, `7...`) into standard `254XXXXXXXXX` format.
- **Strict Idempotency**: Prevents duplicate or out-of-order callbacks from corrupting payment status.
- **Android Material UI**: Clean white & green theme inspired by mobile money interfaces, with input validation, loading indicators, and status badges.
- **Transaction History**: Displays recent transactions in a `RecyclerView` with click-to-view status details.
- **Zero Client Secrets**: All Daraja credentials reside strictly in Supabase Edge Function secrets.

---

## 🏗 System Architecture & Technology Stack

### Android Application
- **Language**: Kotlin
- **Min SDK**: API 24 (Android 7.0) | **Target/Compile SDK**: API 37
- **UI Framework**: AndroidX AppCompat, Material Design 3, ViewBinding
- **Networking**: Retrofit 2, OkHttp 4, Gson
- **Concurrency**: Kotlin Coroutines, ViewModel, LiveData
- **List Rendering**: RecyclerView, CardView

### Backend Service (Supabase Cloud)
- **Runtime**: Supabase Edge Functions (Deno TypeScript)
- **Database**: Supabase PostgreSQL with Row Level Security
- **Deployment URL**: `https://injsofnemhbedeswjyev.supabase.co/functions/v1/quickpay-api`
- **CLI Tooling**: Supabase CLI (`npx supabase`)

---

## 📁 Repository Structure

```text
QuickPay/
├── README.md
├── .gitignore
├── supabase/
│   ├── config.toml
│   ├── migrations/
│   │   └── 20260330000000_create_payments_table.sql
│   └── functions/
│       ├── _shared/
│       │   ├── daraja.ts
│       │   ├── paymentService.ts
│       │   ├── phone.ts
│       │   ├── supabaseClient.ts
│       │   └── timestamp.ts
│       └── quickpay-api/
│           └── index.ts
├── docs/
│   ├── architecture.md
│   ├── setup.md
│   ├── api-reference.md
│   ├── testing.md
│   └── assignment-demo.md
└── app/ (Android Studio Kotlin Project)
    ├── build.gradle.kts
    └── src/main/java/com/example/quickpay/
```

---

## ⚙️ Quick Start Setup & Deployment Guide

### 1. Supabase CLI Link & Migration
```bash
# Link local repository to remote Supabase project
npx supabase link --project-ref injsofnemhbedeswjyev --password "Quickpay3243"

# Push database migrations to remote PostgreSQL
npx supabase db push
```

### 2. Configure Function Secrets & Deploy
```bash
# Configure Daraja Sandbox credentials in Supabase Function Secrets
npx supabase secrets set \
  DARAJA_CONSUMER_KEY="G0EBdLj2qgQ0XxitveJiGsru5THgtEFvuOmDTBRFpHrthaaY" \
  DARAJA_CONSUMER_SECRET="aMy06cohopbG91UNaTMhLKDlx4i2DBioQTZJj6xKsojsbyCvCF88w8iyNfaHYJme" \
  DARAJA_PASSKEY="bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919" \
  DARAJA_SHORTCODE="174379" \
  DARAJA_BASE_URL="https://sandbox.safaricom.co.ke" \
  CALLBACK_URL="https://injsofnemhbedeswjyev.supabase.co/functions/v1/quickpay-api/callback"

# Deploy Edge Function
npx supabase functions deploy quickpay-api --no-verify-jwt
```

### 3. Android Application Setup
1. Open `/home/amon/AndroidStudioProjects/Quickpay` in Android Studio.
2. Build Debug APK:
   ```bash
   unset ANDROID_PREFS_ROOT && ./gradlew assembleDebug
   ```
3. Run Android Unit Tests:
   ```bash
   unset ANDROID_PREFS_ROOT && ./gradlew test
   ```
4. Run app on Android Emulator or test device.
5. Enter test M-Pesa phone number (e.g., `0712345678`), amount `10`, and tap **Pay with M-Pesa**.

---

## 🔒 Security & Academic Limitations

1. **Sandbox Environment**: QuickPay is strictly configured for the Safaricom Daraja Sandbox environment (`https://sandbox.safaricom.co.ke`) and does not process real money.
2. **Credential Protection**: Consumer Key, Secret, and Passkey are kept strictly in Supabase Edge Function secrets and never compiled into the Android APK.
3. **M-Pesa PIN Collection**: QuickPay **never** prompts for or stores M-Pesa PINs. PINs are entered exclusively on the official Safaricom system prompt.
4. **Row Level Security**: PostgreSQL `payments` table enables RLS, restricting direct mutations from public clients.

---

## 📚 Detailed Documentation

For further technical details, inspect the documentation files in `/docs`:
- [Architecture & Sequence Diagrams](docs/architecture.md)
- [Installation & Deployment Instructions](docs/setup.md)
- [API Reference](docs/api-reference.md)
- [Testing & Verification Report](docs/testing.md)
- [Lecturer Demonstration & Viva Q&A Guide](docs/assignment-demo.md)
