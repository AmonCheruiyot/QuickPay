const { describe, it, before, after } = require('node:test');
const assert = require('node:assert');
const path = require('path');
const fs = require('fs');

const testDbPath = path.join(__dirname, 'test_callback.db');
process.env.DB_PATH = testDbPath;

const { getDb, closeDb } = require('../src/database/db');
const paymentService = require('../src/services/paymentService');

describe('Daraja Callback Processing & Idempotency', () => {
  before(() => {
    if (fs.existsSync(testDbPath)) {
      fs.unlinkSync(testDbPath);
    }
    getDb(testDbPath);
  });

  after(() => {
    closeDb();
    if (fs.existsSync(testDbPath)) {
      fs.unlinkSync(testDbPath);
    }
  });

  it('processes a successful callback and stores MpesaReceiptNumber', () => {
    const payment = paymentService.createPendingPayment({
      phone: '0712345678',
      amount: 250,
    });

    const checkoutId = 'ws_CO_SUCCESS_123';
    paymentService.updatePaymentCheckoutDetails(payment.reference, {
      merchantRequestId: 'M_REQ_SUCCESS',
      checkoutRequestId: checkoutId,
    });

    const callbackPayload = {
      Body: {
        stkCallback: {
          MerchantRequestID: 'M_REQ_SUCCESS',
          CheckoutRequestID: checkoutId,
          ResultCode: 0,
          ResultDesc: 'The service request is processed successfully.',
          CallbackMetadata: {
            Item: [
              { Name: 'Amount', Value: 250 },
              { Name: 'MpesaReceiptNumber', Value: 'QPK1234567' },
              { Name: 'TransactionDate', Value: 20260330153000 },
              { Name: 'PhoneNumber', Value: 254712345678 },
            ],
          },
        },
      },
    };

    const result = paymentService.processCallback(callbackPayload);
    assert.strictEqual(result.success, true);
    assert.strictEqual(result.payment.status, 'SUCCESS');
    assert.strictEqual(result.payment.receiptNumber, 'QPK1234567');
    assert.strictEqual(result.payment.resultCode, 0);
  });

  it('processes a user cancellation callback (ResultCode 1032) setting status to CANCELLED', () => {
    const payment = paymentService.createPendingPayment({
      phone: '0712345678',
      amount: 100,
    });

    const checkoutId = 'ws_CO_CANCELLED_456';
    paymentService.updatePaymentCheckoutDetails(payment.reference, {
      merchantRequestId: 'M_REQ_CANCELLED',
      checkoutRequestId: checkoutId,
    });

    const callbackPayload = {
      Body: {
        stkCallback: {
          MerchantRequestID: 'M_REQ_CANCELLED',
          CheckoutRequestID: checkoutId,
          ResultCode: 1032,
          ResultDesc: 'Request cancelled by user',
        },
      },
    };

    const result = paymentService.processCallback(callbackPayload);
    assert.strictEqual(result.success, true);
    assert.strictEqual(result.payment.status, 'CANCELLED');
    assert.strictEqual(result.payment.resultCode, 1032);
  });

  it('prevents a duplicate or failed callback from overwriting an already SUCCESSFUL transaction', () => {
    // Attempting to process a failed callback for ws_CO_SUCCESS_123 from previous test
    const duplicateFailedCallback = {
      Body: {
        stkCallback: {
          MerchantRequestID: 'M_REQ_SUCCESS',
          CheckoutRequestID: 'ws_CO_SUCCESS_123',
          ResultCode: 1,
          ResultDesc: 'System error',
        },
      },
    };

    const result = paymentService.processCallback(duplicateFailedCallback);
    assert.strictEqual(result.success, true);
    assert.strictEqual(result.payment.status, 'SUCCESS'); // Preserved SUCCESS status!
  });

  it('handles callback for unknown CheckoutRequestID gracefully', () => {
    const unknownCallback = {
      Body: {
        stkCallback: {
          MerchantRequestID: 'UNKNOWN',
          CheckoutRequestID: 'ws_CO_UNKNOWN_999',
          ResultCode: 0,
          ResultDesc: 'Success',
        },
      },
    };

    const result = paymentService.processCallback(unknownCallback);
    assert.strictEqual(result.success, false);
    assert.strictEqual(result.reason, 'Transaction not found for CheckoutRequestID');
  });
});
