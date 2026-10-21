# QuickPay — Android M-Pesa Payment Application with Safaricom Daraja STK Push

QuickPay is a complete, runnable Android payment application integrated with Safaricom's Daraja Sandbox API using Lipa Na M-Pesa Express (STK Push). Built as an academic demonstration for university software engineering classes, QuickPay features an Express.js Node.js backend, SQLite persistence, and a modern Android client in Kotlin.

---

## 🚀 Core Features

- **Lipa Na M-Pesa STK Push**: Initiates instant M-Pesa payment prompts to Kenyan phone numbers.
- **Phone Number Normalization**: Automatically converts various Kenyan phone formats (`0712345678`, `0112345678`, `+2547...`, `7...`) into standard `254XXXXXXXXX` format.
- **Asynchronous Callback Processing**: Handles incoming Daraja STK callback webhooks, extracts M-Pesa receipt numbers, and updates transaction states.
- **Strict Idempotency**: Prevents duplicate or out-of-order callbacks from corrupting payment status.
- **SQLite Persistence**: Stores all transactions locally with initial `PENDING` state before external calls.
- **Android Material UI**: Clean white & green theme inspired by mobile money interfaces, with input validation, loading indicators, and status badges.
- **Transaction History**: Displays recent transactions in a `RecyclerView` with click-to-view status details.
- **Server URL Configuration**: Allows easy switching between Android Emulator (`http://10.0.2.2:3000`) and physical devices on local Wi-Fi LANs.
- **Comprehensive Automated Test Suite**: 100% passing tests for backend API, callback processing, idempotency, E2E flow, and Android utilities.

---

## 🏗 System Architecture & Technology Stack

### Android Application
- **Language**: Kotlin
- **Min SDK**: API 24 (Android 7.0) | **Target/Compile SDK**: API 37
- **UI Framework**: AndroidX AppCompat, Material Design 3, ViewBinding
- **Networking**: Retrofit 2, OkHttp 4, Gson
- **Concurrency**: Kotlin Coroutines, ViewModel, LiveData
- **List Rendering**: RecyclerView, CardView

### Backend Service (`/server`)
- **Runtime**: Node.js v18/v20+
- **Framework**: Express.js
- **Database**: SQLite3 (`better-sqlite3`) with WAL mode
- **HTTP Client**: Axios
- **Configuration**: dotenv
- **Testing**: Node.js Native Test Runner (`node --test`)

---

## 📁 Required Project Structure

```text
QuickPay/
├── README.md
├── .gitignore
├── docs/
│   ├── architecture.md
│   ├── setup.md
│   ├── api-reference.md
│   ├── testing.md
│   └── assignment-demo.md
├── server/
│   ├── package.json
│   ├── package-lock.json
│   ├── .env.example
│   ├── .gitignore
│   ├── src/
│   │   ├── app.js
│   │   ├── index.js
│   │   ├── config/
│   │   │   └── env.js
│   │   ├── database/
│   │   │   ├── db.js
│   │   │   └── migrations.js
│   │   ├── routes/
│   │   │   └── payments.js
│   │   ├── controllers/
│   │   │   └── paymentsController.js
│   │   ├── services/
│   │   │   ├── darajaService.js
│   │   │   └── paymentService.js
│   │   ├── middleware/
│   │   │   ├── errorHandler.js
│   │   │   └── requestValidation.js
│   │   └── utils/
│   │       ├── phone.js
│   │       └── timestamp.js
│   └── test/
│       ├── health.test.js
│       ├── validation.test.js
│       ├── payments.test.js
│       ├── callback.test.js
│       └── e2e.test.js
└── app/ (Android Studio Kotlin Project)
    ├── build.gradle.kts
    └── src/main/java/com/example/quickpay/
```

---

## ⚙️ Quick Start Setup Guide

### 1. Backend Setup & Startup
```bash
cd server
npm install
npm test          # Run automated backend test suite
npm start         # Starts Express server on http://localhost:3000
```

### 2. HTTPS Callback Setup (ngrok)
To receive callbacks from Safaricom Daraja Sandbox during local development:
```bash
ngrok http 3000
```
Copy the generated ngrok URL (e.g. `https://abc123.ngrok-free.app`) and update `CALLBACK_URL` in `server/.env`:
```env
CALLBACK_URL=https://abc123.ngrok-free.app/api/payments/callback
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
4. Run app on Android Emulator.
5. Enter test M-Pesa phone number (e.g., `0712345678`), amount `10`, and tap **Pay with M-Pesa**.

---

## 🔒 Security & Academic Limitations

1. **Sandbox Environment**: QuickPay is strictly configured for the Safaricom Daraja Sandbox environment (`https://sandbox.safaricom.co.ke`) and does not process real money.
2. **Credential Protection**: Consumer Key, Secret, and Passkey are kept strictly in server `.env` files and never compiled into the Android APK.
3. **M-Pesa PIN Collection**: QuickPay **never** prompts for or stores M-Pesa PINs. PINs are entered exclusively on the official Safaricom system prompt.
4. **Public Endpoint Authentication**: In a commercial production deployment, transaction query endpoints should be secured behind user session authentication or OAuth tokens.

---

## 📚 Detailed Documentation

For further technical details, inspect the documentation files in `/docs`:
- [Architecture & Diagrams](docs/architecture.md)
- [Installation & Setup Instructions](docs/setup.md)
- [API Reference](docs/api-reference.md)
- [Testing & Verification Report](docs/testing.md)
- [Lecturer Demonstration & Viva Q&A Guide](docs/assignment-demo.md)
