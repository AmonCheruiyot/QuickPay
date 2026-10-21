const { describe, it } = require('node:test');
const assert = require('node:assert');
const app = require('../src/app');
const http = require('http');

function makeRequest(path, method = 'GET') {
  return new Promise((resolve, reject) => {
    const server = app.listen(0, () => {
      const port = server.address().port;
      const req = http.request(
        {
          host: '127.0.0.1',
          port,
          path,
          method,
        },
        (res) => {
          let data = '';
          res.on('data', (chunk) => (data += chunk));
          res.on('end', () => {
            server.close();
            try {
              resolve({ statusCode: res.statusCode, body: JSON.parse(data) });
            } catch (e) {
              resolve({ statusCode: res.statusCode, body: data });
            }
          });
        }
      );
      req.on('error', (err) => {
        server.close();
        reject(err);
      });
      req.end();
    });
  });
}

describe('Health Endpoint', () => {
  it('GET /health returns 200 OK and status UP without credentials', async () => {
    const res = await makeRequest('/health');
    assert.strictEqual(res.statusCode, 200);
    assert.strictEqual(res.body.status, 'UP');
    assert.strictEqual(res.body.app, 'QuickPay Daraja Backend');
    assert.strictEqual(res.body.consumerKey, undefined);
    assert.strictEqual(res.body.consumerSecret, undefined);
  });
});
