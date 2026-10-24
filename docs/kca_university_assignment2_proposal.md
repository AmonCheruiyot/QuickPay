# KCA UNIVERSITY
## SCHOOL OF TECHNOLOGY — DEPARTMENT OF COMPUTER SCIENCE
### COURSE UNIT: MOBILE APPLICATION DEVELOPMENT (ASSIGNMENT 2)
---

# SYSTEM PROPOSAL & ARCHITECTURE DOCUMENTATION

## PROJECT TITLE:
**DESIGN AND IMPLEMENTATION OF AN ANDROID-BASED E-COMMERCE SYSTEM WITH SAFARICOM DARAJA M-PESA STK PUSH INTEGRATION**

**Student Project**: QuickPay E-Commerce  
**Platform**: Android Mobile Application (Kotlin)  
**Backend Architecture**: Supabase Serverless Edge Functions (TypeScript/Deno)  
**Database**: Supabase PostgreSQL Cloud Database  
**Payment Gateway**: Safaricom Daraja API — Lipa Na M-Pesa Express (STK Push)  

---

## 1. Executive Summary & Scenario Overview

A Kenyan retail business intends to expand its operations by introducing a mobile e-commerce platform that enables customers to browse products, add items to a shopping cart, place orders, and make payments conveniently using M-Pesa.

Currently, the business receives orders through manual phone calls and social media platforms, leading to challenges with inventory tracking, payment reconciliation, and order status transparency.

**QuickPay** addresses these challenges by delivering a complete, secure, native Android e-commerce application integrated directly with Safaricom's Daraja Sandbox API via Supabase Cloud Edge Functions and PostgreSQL.

---

## 2. System Architecture & Asynchronous Workflow

QuickPay employs a decoupled, highly secure architecture that separates the Android mobile app, cloud REST API, payment gateway, and database:

```text
[Android Kotlin App] ---> [Supabase Edge Function API] ---> [Safaricom Daraja API]
                                     |                              |
                                     v                              v
                           [Supabase PostgreSQL] <--- [Async M-Pesa Callback Webhook]
```

### Asynchronous Payment Workflow
1. The customer selects products, manages their cart, and proceeds to Checkout.
2. The system generates a unique order reference (`ORD-XXXXXXXXXX`) and saves the order in `Pending Payment` state.
3. The Android client submits the order reference, total KES amount, and customer phone number to the backend `POST /stk-push`.
4. The backend validates stock, creates a `PENDING` payment record, generates the STK Push password (Base64 of `Shortcode + Passkey + Timestamp`), and submits the request to Daraja.
5. Safaricom sends an **M-Pesa STK Push Prompt** directly to the customer's mobile SIM screen.
6. The customer enters their M-Pesa PIN in the official system interface.
7. Safaricom sends an asynchronous HTTP `POST` callback to `https://injsofnemhbedeswjyev.supabase.co/functions/v1/quickpay-api/callback`.
8. The backend verifies the callback, extracts the M-Pesa receipt number, updates the order status to `Paid`, and automatically deducts product inventory stock.
9. The Android app fetches the updated status and renders the **SUCCESS** confirmation badge and receipt.

---

## 3. Entity-Relationship Diagram (ERD) & Database Design

QuickPay utilizes a relational PostgreSQL schema designed for integrity and financial auditability:

```text
+------------------+         +--------------------+         +-----------------------+
|      USERS       |         |      PRODUCTS      |         | INVENTORY_TRANSACTIONS|
+------------------+         +--------------------+         +-----------------------+
| PK id            |<---+    | PK id              |<---+    | PK id                 |
|    full_name     |    |    |    name            |    |    | FK product_id         |
|    email (UNIQUE)|    |    |    description     |    |    |    quantity_changed   |
|    phone         |    |    |    price (KES)     |    |    |    transaction_type   |
|    password_hash |    |    |    stock_quantity  |----+    |    created_at         |
|    role          |    |    |    category        |         +-----------------------+
|    address       |    |    |    image_url       |
+------------------+    |    +--------------------+
         |              |              |
         | 1:N          |              | 1:N
         v              |              v
+------------------+    |    +--------------------+
|      ORDERS      |    |    |    ORDER_ITEMS     |
+------------------+    |    +--------------------+
| PK id            |    |    | PK id              |
|    order_ref (UQ)|    |    | FK order_id        |
| FK user_id       |----+    | FK product_id      |
|    total_amount  |         |    quantity        |
|    order_status  |         |    unit_price      |
|    payment_status|         |    subtotal        |
+------------------+         +--------------------+
         |
         | 1:1 / 1:N
         v
+------------------+
|     PAYMENTS     |
+------------------+
| PK id            |
|    reference (UQ)|
| FK order_ref     |
|    phone         |
|    amount        |
|    status        |
|    checkout_id   |
|    receipt_number|
+------------------+
```

---

## 4. Test Matrix & Execution Verification Results

| Test Case ID | Test Scenario | Inputs / Action | Expected Result | Actual Result | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-01** | Customer Registration | Valid details: John Doe, `john@gmail.com`, `0713702378`, `password123` | Account created successfully; user session saved | Account created, redirected to Catalogue | **PASS** |
| **TC-02** | Invalid Phone Registration | Invalid phone `12345` | Displays validation error message | Error: "Invalid Kenyan phone number" displayed | **PASS** |
| **TC-03** | Browse Product Catalogue | Select category "Smartphones" | Displays list of smartphones with prices in KES | 2 smartphones returned and rendered | **PASS** |
| **TC-04** | Out-of-Stock Prevention | Add quantity 50 for item with stock 10 | Prevents adding item beyond available stock | Toast message: "Requested quantity exceeds available stock" | **PASS** |
| **TC-05** | Cart Total Calculation | Add Oraimo FreePods (4,200) + Power Bank (5,800) | Cart total automatically updates to 10,000 KES | Total calculated automatically as KES 10,000 | **PASS** |
| **TC-06** | Checkout & Order Creation | Submit order for Oraimo FreePods (4,200 KES) | Order created in `Pending Payment` state with ref `ORD-...` | Order `ORD-1792868935918-5864` created in PostgreSQL | **PASS** |
| **TC-07** | M-Pesa STK Push Trigger | Tap "Pay with M-Pesa" for 4,200 KES | STK Push request sent; M-Pesa SIM prompt delivered to phone | Prompt delivered on Infinix SIM screen: "Pay Kshs 4200?" | **PASS** |
| **TC-08** | M-Pesa Payment Callback | Safaricom posts `ResultCode: 0`, Receipt `QPK88776655` | Order updated to `Paid`, stock deducted from 30 to 29 | Order status updated to `Paid`, stock deducted to 29 | **PASS** |
| **TC-09** | Idempotency Verification | Submit duplicate failed callback for paid order | System ignores failed callback; keeps status `Paid` | Status `Paid` and receipt `QPK88776655` preserved | **PASS** |
| **TC-10** | Admin Dashboard Access | Login as `admin@quickpay.co.ke` | Access granted to Admin panel; stats & sales revenue displayed | Total revenue KES 4,200 and low stock list rendered | **PASS** |

---

## 5. Answers to Lecturer Demonstration Questions

### Q1: How does the application calculate the total amount payable?
**Answer**:
The `CartManager` utility in Android maintains a list of `CartItem` objects. For each item, `subtotal = unitPrice * quantity`. The total amount is dynamically computed as `sumOf { it.subtotal }`. When the user proceeds to Checkout, the backend independently re-validates item prices and stock in Supabase PostgreSQL, computing `calculatedTotal = sum(unitPrice * quantity)` to guarantee financial integrity before creating the order.

### Q2: Why should Daraja API credentials be stored on the backend server?
**Answer**:
Android APK binaries can easily be decompiled or reverse-engineered using tools like `jadx` or `apktool`. Hardcoding Consumer Keys, Secrets, or Passkeys in Kotlin or XML exposes them to malicious actors who could exhaust API quotas or compromise accounts. Keeping credentials in Supabase Edge Function Secrets guarantees they never touch the mobile device.

### Q3: What is the difference between an STK Push request being accepted and a payment being completed?
**Answer**:
When Daraja accepts an STK Push request, it merely confirms that the API request parameters were valid (`ResponseCode: 0`) and that a prompt has been queued for transmission to the customer's handset. The payment is **not completed** until the customer enters their M-Pesa PIN and Safaricom asynchronously posts a confirmed callback (`ResultCode: 0`) with an M-Pesa receipt number to the backend webhook.

### Q4: How does the backend associate a payment callback with the correct order?
**Answer**:
When the payment is initiated, the backend generates a payment record linking `order_reference` and stores Daraja's unique `CheckoutRequestID`. When Safaricom posts the callback webhook, the backend queries `payments` by `CheckoutRequestID`. Upon verifying `ResultCode === 0`, it retrieves the linked `order_reference`, marks the corresponding record in `orders` as `Paid`, and updates inventory stock.

### Q5: How does the application handle failed, delayed, or duplicate callbacks?
**Answer**:
- **Failed Callbacks**: If `ResultCode !== 0`, the payment status is updated to `FAILED` or `CANCELLED`, while the order remains `Pending Payment` so the user can re-try checkout.
- **Delayed Callbacks**: Orders remain in `Pending Payment` state until the callback arrives. The Android client polls or manually refreshes status via `GET /orders/:reference`.
- **Duplicate Callbacks**: Idempotency checks in `paymentService.ts` check if the transaction is already marked `SUCCESS`. Subsequent duplicate callbacks are ignored to prevent double processing or accidental status degradation.

### Q6: How are products and inventory updated after a successful order?
**Answer**:
Upon receiving a confirmed `ResultCode === 0` callback, `markOrderAsPaidAndDeductStock` retrieves all `order_items` for the order. For each item, it updates `products.stock_quantity = stock_quantity - quantity` and inserts a `DEDUCTION` record into `inventory_transactions` for audit purposes.

### Q7: What measures prevent unauthorized access to administrative functions?
**Answer**:
In `UserSessionManager`, users are assigned roles (`customer` or `admin`). Admin activities (such as `AdminDashboardActivity`) perform role checks (`user.role == 'admin'`). On the backend, administrative Edge Function routes check user permissions, and Row Level Security (RLS) policies on Supabase PostgreSQL restrict direct table writes.

### Q8: How would the application be prepared for deployment in a production environment?
**Answer**:
1. Replace Daraja Sandbox credentials with Production Lipa Na M-Pesa Paybill / Buy Goods credentials.
2. Enable production domain SSL certificates and strict CORS restrictions on Supabase Edge Functions.
3. Enforce Supabase Auth JWT tokens for user authentication.
4. Integrate push notifications (Firebase Cloud Messaging) to notify customers when order status changes to `Shipped` or `Completed`.
