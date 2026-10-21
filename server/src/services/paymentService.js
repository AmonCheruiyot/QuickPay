const { getDb } = require('../database/db');
const { normalizePhoneNumber, maskPhoneNumber } = require('../utils/phone');

/**
 * Creates a new PENDING transaction record
 */
function createPendingPayment({ phone, amount }) {
  const normalizedPhone = normalizePhoneNumber(phone);
  const parsedAmount = parseInt(amount, 10);

  if (isNaN(parsedAmount) || parsedAmount <= 0) {
    throw new Error('Amount must be a positive integer');
  }

  const timestamp = Date.now();
  const randomSuffix = Math.floor(1000 + Math.random() * 9000);
  const reference = `QP${timestamp}${randomSuffix}`;

  const db = getDb();
  const stmt = db.prepare(`
    INSERT INTO payments (reference, phone, amount, status)
    VALUES (?, ?, ?, 'PENDING')
  `);

  stmt.run(reference, normalizedPhone, parsedAmount);

  return getPaymentByReference(reference);
}

/**
 * Updates merchant and checkout request IDs from Daraja response
 */
function updatePaymentCheckoutDetails(reference, { merchantRequestId, checkoutRequestId }) {
  const db = getDb();
  const stmt = db.prepare(`
    UPDATE payments
    SET merchant_request_id = ?, checkout_request_id = ?, updated_at = CURRENT_TIMESTAMP
    WHERE reference = ?
  `);

  stmt.run(merchantRequestId, checkoutRequestId, reference);
  return getPaymentByReference(reference);
}

/**
 * Updates payment status and result details
 */
function updatePaymentStatus(reference, status, { resultCode, resultDescription, receiptNumber, transactionDate }) {
  const db = getDb();
  const stmt = db.prepare(`
    UPDATE payments
    SET status = ?,
        result_code = ?,
        result_description = ?,
        receipt_number = COALESCE(?, receipt_number),
        transaction_date = COALESCE(?, transaction_date),
        updated_at = CURRENT_TIMESTAMP
    WHERE reference = ?
  `);

  stmt.run(status, resultCode, resultDescription, receiptNumber || null, transactionDate || null, reference);
  return getPaymentByReference(reference);
}

/**
 * Process incoming Daraja STK Push Callback payload
 */
function processCallback(callbackPayload) {
  if (!callbackPayload || !callbackPayload.Body || !callbackPayload.Body.stkCallback) {
    throw new Error('Invalid Daraja callback payload structure');
  }

  const stkCallback = callbackPayload.Body.stkCallback;
  const merchantRequestId = stkCallback.MerchantRequestID;
  const checkoutRequestId = stkCallback.CheckoutRequestID;
  const resultCode = stkCallback.ResultCode;
  const resultDesc = stkCallback.ResultDesc || '';

  const db = getDb();
  const existingStmt = db.prepare(`
    SELECT * FROM payments WHERE checkout_request_id = ?
  `);
  const payment = existingStmt.get(checkoutRequestId);

  if (!payment) {
    console.warn(`[Callback] Received callback for unknown CheckoutRequestID: ${checkoutRequestId}`);
    return { success: false, reason: 'Transaction not found for CheckoutRequestID' };
  }

  // Idempotency check: Do not allow a failed or duplicate callback to overwrite a confirmed success
  if (payment.status === 'SUCCESS' && resultCode !== 0) {
    console.warn(`[Callback] Ignoring failed callback for already successful transaction: ${payment.reference}`);
    return { success: true, payment: formatPayment(payment) };
  }

  let status = 'FAILED';
  let receiptNumber = null;
  let transactionDate = null;
  let callbackAmount = null;
  let callbackPhone = null;

  if (resultCode === 0) {
    status = 'SUCCESS';
    if (stkCallback.CallbackMetadata && Array.isArray(stkCallback.CallbackMetadata.Item)) {
      stkCallback.CallbackMetadata.Item.forEach((item) => {
        if (item.Name === 'MpesaReceiptNumber') {
          receiptNumber = String(item.Value);
        } else if (item.Name === 'TransactionDate') {
          receiptNumber = receiptNumber; // keep
          transactionDate = String(item.Value);
        } else if (item.Name === 'Amount') {
          callbackAmount = item.Value;
        } else if (item.Name === 'PhoneNumber') {
          callbackPhone = String(item.Value);
        }
      });
    }

    // Safety check: ensure callback amount matches requested amount if available
    if (callbackAmount !== null && Math.round(callbackAmount) !== payment.amount) {
      console.warn(`[Callback] Amount mismatch for ${payment.reference}. Stored: ${payment.amount}, Callback: ${callbackAmount}`);
    }
  } else if (resultCode === 1032 || resultDesc.toLowerCase().includes('cancel')) {
    status = 'CANCELLED';
  }

  const updatedPayment = updatePaymentStatus(payment.reference, status, {
    resultCode,
    resultDescription: resultDesc,
    receiptNumber,
    transactionDate,
  });

  return { success: true, payment: updatedPayment };
}

/**
 * Gets payment by internal reference
 */
function getPaymentByReference(reference) {
  const db = getDb();
  const stmt = db.prepare('SELECT * FROM payments WHERE reference = ?');
  const row = stmt.get(reference);
  if (!row) {
    return null;
  }
  return formatPayment(row);
}

/**
 * Retrieves payment history list
 */
function getPaymentHistory(limit = 50, offset = 0) {
  const db = getDb();
  const stmt = db.prepare(`
    SELECT * FROM payments
    ORDER BY created_at DESC
    LIMIT ? OFFSET ?
  `);
  const rows = stmt.all(limit, offset);
  return rows.map(formatPayment);
}

function formatPayment(row) {
  return {
    id: row.id,
    reference: row.reference,
    phone: row.phone,
    maskedPhone: maskPhoneNumber(row.phone),
    amount: row.amount,
    status: row.status,
    merchantRequestId: row.merchant_request_id,
    checkoutRequestId: row.checkout_request_id,
    receiptNumber: row.receipt_number,
    resultCode: row.result_code,
    resultDescription: row.result_description,
    transactionDate: row.transaction_date,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}

module.exports = {
  createPendingPayment,
  updatePaymentCheckoutDetails,
  updatePaymentStatus,
  processCallback,
  getPaymentByReference,
  getPaymentHistory,
};
