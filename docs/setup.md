# QuickPay Setup & Installation Guide

This document provides step-by-step instructions to set up, configure, and run both the Express backend server and the Android application for QuickPay.

---

## 1. Prerequisites

- **Node.js**: Version 18.x or 20.x+
- **npm**: Version 9.x+
- **Java Development Kit (JDK)**: JDK 17 or Java 21+
- **Android Studio**: Ladybug / Koala or newer with Android SDK Platform 35/37 installed.
- **ngrok** (or alternative HTTPS tunnel) for receiving Safaricom Daraja callbacks during local development.

---

## 2. Backend Installation & Setup

### Step 1: Navigate to server directory
```bash
cd server
```

### Step 2: Install dependencies
```bash
npm install
```

### Step 3: Configure Environment Variables
Create `.env` file inside `server/` (or copy from `.env.example`):
```bash
cp .env.example .env
```

Ensure `.env` contains the following Safaricom Daraja Sandbox credentials:
```env
PORT=3000
NODE_ENV=development
DARAJA_CONSUMER_KEY=G0EBdLj2qgQ0XxitveJiGsru5THgtEFvuOmDTBRFpHrthaaY
DARAJA_CONSUMER_SECRET=aMy06cohopbG91UNaTMhLKDlx4i2DBioQTZJj6xKsojsbyCvCF88w8iyNfaHYJme
DARAJA_PASSKEY=bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919
DARAJA_SHORTCODE=174379
DARAJA_BASE_URL=https://sandbox.safaricom.co.ke
CALLBACK_URL=http://localhost:3000/api/payments/callback
```

### Step 4: Run Backend Tests
Verify backend functionality and SQLite database setup:
```bash
npm test
```

### Step 5: Start Backend Server
```bash
npm start
```
The server will run on `http://localhost:3000`.

---

## 3. Configuring Safaricom Callback Tunnel (ngrok)

Safaricom Daraja requires a publicly accessible HTTPS URL for callback notifications.

### Step 1: Start ngrok tunnel
```bash
ngrok http 3000
```

### Step 2: Update `CALLBACK_URL` in `.env`
Copy the generated ngrok HTTPS URL (e.g., `https://abc1234.ngrok-free.app`) and update `.env`:
```env
CALLBACK_URL=https://abc1234.ngrok-free.app/api/payments/callback
```

Restart the Express backend server for changes to take effect.

---

## 4. Android Application Setup

### Option A: Running on Android Emulator
1. Open Android Studio and import `/home/amon/AndroidStudioProjects/Quickpay`.
2. Sync project with Gradle files (`./gradlew assembleDebug`).
3. Launch an Android Virtual Device (AVD).
4. Build & Run the app on the AVD.
5. In the QuickPay home screen, leave the **Backend Server URL** set to default: `http://10.0.2.2:3000` (which routes to the host computer's port 3000).

### Option B: Running on Physical Android Device (LAN)
1. Ensure your computer and Android phone are on the same Wi-Fi network.
2. Find your local IP address (e.g. `192.168.1.100`).
3. In QuickPay Android app, set the **Backend Server URL** to `http://192.168.1.100:3000`.
4. Ensure computer firewall allows incoming connections on port 3000.
