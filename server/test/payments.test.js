const { describe, it, before, after } = require('node:test');
const assert = require('node:assert');
const path = require('path');
const fs = require('fs');

const testDbPath = path.join(__dirname, 'test_payments.db');
process.env.DB_PATH = testDbPath;

const { getDb, closeDb } = require('../src/database/db');
const paymentService = require('../src/services/paymentService');

describe('Payment Service & Database Persistence', () => {
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

  it('creates a pending payment in SQLite with unique reference and normalized phone', () => {
    const payment = paymentService.createPendingPayment({
      phone: '0712345678',
      amount: 100,
    });

    assert.ok(payment.reference.startsWith('QP'));
    assert.strictEqual(payment.phone, '254712345678');
    assert.strictEqual(payment.maskedPhone, '2547****5678');
    assert.strictEqual(payment.amount, 100);
    assert.strictEqual(payment.status, 'PENDING');
  });

  it('updates merchant and checkout request IDs', () => {
    const payment = paymentService.createPendingPayment({
      phone: '0712345678',
      amount: 50,
    });

    const updated = paymentService.updatePaymentCheckoutDetails(payment.reference, {
      merchantRequestId: 'M_REQ_123',
      checkoutRequestId: 'ws_CO_456',
    });

    assert.strictEqual(updated.merchantRequestId, 'M_REQ_123');
    assert.strictEqual(updated.checkoutRequestId, 'ws_CO_456');
  });

  it('retrieves payment history in reverse chronological order', () => {
    const history = paymentService.getPaymentHistory(10, 0);
    assert.ok(Array.isArray(history));
    assert.ok(history.length >= 2);
    assert.ok(new Date(history[0].createdAt) >= new Date(history[1].createdAt));
  });

  it('returns null for non-existent payment reference', () => {
    const payment = paymentService.getPaymentByReference('NON_EXISTENT');
    assert.strictEqual(payment, null);
  });
});
