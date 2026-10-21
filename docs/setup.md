# QuickPay Setup & Deployment Guide (Supabase Migration)

This document provides step-by-step instructions to configure, deploy, and run the QuickPay Supabase Edge Functions backend and Android application.

---

## 1. Prerequisites

- **Node.js**: Version 18.x or 20.x+
- **Supabase CLI**: Version 2.x+ (`npx supabase`)
- **Java Development Kit (JDK)**: JDK 17 or Java 21+
- **Android Studio**: Ladybug / Koala or newer with Android SDK Platform 35/37 installed.

---

## 2. Supabase Backend Setup & Deployment

### Step 1: Initialize local Supabase project
```bash
npx supabase init
```

### Step 2: Link local repository to remote Supabase project
```bash
npx supabase link --project-ref injsofnemhbedeswjyev --password "Quickpay3243"
```

### Step 3: Apply PostgreSQL Database Migrations
```bash
npx supabase db push
```

### Step 4: Configure Supabase Function Secrets
Set the Daraja Sandbox credentials and function environment variables on Supabase:
```bash
npx supabase secrets set \
  DARAJA_CONSUMER_KEY="G0EBdLj2qgQ0XxitveJiGsru5THgtEFvuOmDTBRFpHrthaaY" \
  DARAJA_CONSUMER_SECRET="aMy06cohopbG91UNaTMhLKDlx4i2DBioQTZJj6xKsojsbyCvCF88w8iyNfaHYJme" \
  DARAJA_PASSKEY="bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919" \
  DARAJA_SHORTCODE="174379" \
  DARAJA_BASE_URL="https://sandbox.safaricom.co.ke" \
  CALLBACK_URL="https://injsofnemhbedeswjyev.supabase.co/functions/v1/quickpay-api/callback"
```

### Step 5: Deploy Edge Functions
Deploy the `quickpay-api` Edge Function with public JWT verification disabled (allowing public webhook callbacks from Safaricom):
```bash
npx supabase functions deploy quickpay-api --no-verify-jwt
```

The deployed function URL is:
`https://injsofnemhbedeswjyev.supabase.co/functions/v1/quickpay-api`

---

## 3. Android Application Setup

### Running on Android Emulator or Physical Device
1. Open `/home/amon/AndroidStudioProjects/Quickpay` in Android Studio.
2. Build & run the app:
   ```bash
   unset ANDROID_PREFS_ROOT && ./gradlew assembleDebug
   ```
3. Run Android unit tests:
   ```bash
   unset ANDROID_PREFS_ROOT && ./gradlew test
   ```
4. The app default server URL is pre-configured to point to the live cloud backend:
   `https://injsofnemhbedeswjyev.supabase.co/functions/v1/quickpay-api`
5. No local Express server or ngrok tunnel is required.
