const { describe, it, before, after } = require('node:test');
const assert = require('node:assert');
const http = require('http');
const path = require('path');
const fs = require('fs');

const testDbPath = path.join(__dirname, 'test_e2e.db');
process.env.DB_PATH = testDbPath;

const { getDb, closeDb } = require('../src/database/db');
const darajaService = require('../src/services/darajaService');
const app = require('../src/app');

function makeHttpRequest(port, method, path, body = null) {
  return new Promise((resolve, reject) => {
    const payload = body ? JSON.stringify(body) : '';
    const req = http.request(
      {
        host: '127.0.0.1',
        port,
        path,
        method,
        headers: body
          ? {
              'Content-Type': 'application/json',
              'Content-Length': Buffer.byteLength(payload),
            }
          : {},
      },
      (res) => {
        let data = '';
        res.on('data', (chunk) => (data += chunk));
        res.on('end', () => {
          try {
            resolve({ statusCode: res.statusCode, body: JSON.parse(data) });
          } catch (e) {
            resolve({ statusCode: res.statusCode, body: data });
          }
        });
      }
    );
    req.on('error', reject);
    if (body) req.write(payload);
    req.end();
  });
}

describe('End-to-End Payment Flow Integration Test', () => {
  let server;
  let serverPort;
  let originalInitiate;

  before(async () => {
    closeDb();
    if (fs.existsSync(testDbPath)) {
      fs.unlinkSync(testDbPath);
    }
    getDb(testDbPath);

    // Mock Daraja STK Push network call for deterministic E2E test
    originalInitiate = darajaService.initiateStkPush;
    darajaService.initiateStkPush = async ({ phone, amount }) => {
      return {
        MerchantRequestID: '29115-MOCK-12345',
        CheckoutRequestID: 'ws_CO_MOCK_67890',
        ResponseCode: '0',
        ResponseDescription: 'Success. Request accepted for processing',
        CustomerMessage: 'Success. Request accepted for processing',
      };
    };

    await new Promise((resolve) => {
      server = app.listen(0, () => {
        serverPort = server.address().port;
        resolve();
      });
    });
  });

  after(async () => {
    darajaService.initiateStkPush = originalInitiate;
    if (server) {
      await new Promise((resolve) => server.close(resolve));
    }
    closeDb();
    if (fs.existsSync(testDbPath)) {
      fs.unlinkSync(testDbPath);
    }
  });

  it('completes end-to-end flow: Initiate -> Pending -> Daraja Callback -> Success Status -> History', async () => {
    // 1. Initiate Payment via POST /api/payments/stk-push
    const initRes = await makeHttpRequest(serverPort, 'POST', '/api/payments/stk-push', {
      phone: '0712345678',
      amount: 100,
    });

    assert.strictEqual(initRes.statusCode, 200);
    assert.strictEqual(initRes.body.success, true);
    assert.strictEqual(initRes.body.status, 'PENDING');
    assert.strictEqual(initRes.body.checkoutRequestId, 'ws_CO_MOCK_67890');
    const reference = initRes.body.reference;
    assert.ok(reference);

    // 2. Query initial status via GET /api/payments/:reference
    const pendingStatusRes = await makeHttpRequest(serverPort, 'GET', `/api/payments/${reference}`);
    assert.strictEqual(pendingStatusRes.statusCode, 200);
    assert.strictEqual(pendingStatusRes.body.payment.status, 'PENDING');

    // 3. Simulate Daraja Callback POST /api/payments/callback
    const callbackPayload = {
      Body: {
        stkCallback: {
          MerchantRequestID: '29115-MOCK-12345',
          CheckoutRequestID: 'ws_CO_MOCK_67890',
          ResultCode: 0,
          ResultDesc: 'The service request is processed successfully.',
          CallbackMetadata: {
            Item: [
              { Name: 'Amount', Value: 100 },
              { Name: 'MpesaReceiptNumber', Value: 'QPK99887766' },
              { Name: 'TransactionDate', Value: 20260330160000 },
              { Name: 'PhoneNumber', Value: 254712345678 },
            ],
          },
        },
      },
    };

    const callbackRes = await makeHttpRequest(serverPort, 'POST', '/api/payments/callback', callbackPayload);
    assert.strictEqual(callbackRes.statusCode, 200);
    assert.strictEqual(callbackRes.body.ResultCode, 0);

    // 4. Query updated status via GET /api/payments/:reference
    const successStatusRes = await makeHttpRequest(serverPort, 'GET', `/api/payments/${reference}`);
    assert.strictEqual(successStatusRes.statusCode, 200);
    assert.strictEqual(successStatusRes.body.payment.status, 'SUCCESS');
    assert.strictEqual(successStatusRes.body.payment.receiptNumber, 'QPK99887766');

    // 5. Query transaction history via GET /api/payments
    const historyRes = await makeHttpRequest(serverPort, 'GET', '/api/payments');
    assert.strictEqual(historyRes.statusCode, 200);
    assert.ok(historyRes.body.payments.length >= 1);
    assert.strictEqual(historyRes.body.payments[0].reference, reference);
  });
});
