const { describe, it } = require('node:test');
const assert = require('node:assert');
const { normalizePhoneNumber, maskPhoneNumber } = require('../src/utils/phone');
const { getDarajaTimestamp } = require('../src/utils/timestamp');

describe('Phone & Validation Utilities', () => {
  it('normalizes various valid Kenyan phone number formats to 254XXXXXXXXX', () => {
    assert.strictEqual(normalizePhoneNumber('0712345678'), '254712345678');
    assert.strictEqual(normalizePhoneNumber('0112345678'), '254112345678');
    assert.strictEqual(normalizePhoneNumber('+254712345678'), '254712345678');
    assert.strictEqual(normalizePhoneNumber('254712345678'), '254712345678');
    assert.strictEqual(normalizePhoneNumber('712345678'), '254712345678');
    assert.strictEqual(normalizePhoneNumber('112345678'), '254112345678');
  });

  it('rejects invalid or non-Kenyan phone numbers', () => {
    assert.throws(() => normalizePhoneNumber('12345'), /Invalid Kenyan phone number/);
    assert.throws(() => normalizePhoneNumber('0201234567'), /Invalid Kenyan phone number/); // landline
    assert.throws(() => normalizePhoneNumber(''), /Phone number must be a non-empty string/);
  });

  it('masks phone numbers correctly', () => {
    assert.strictEqual(maskPhoneNumber('254712345678'), '2547****5678');
  });

  it('generates 14-character Daraja timestamp string', () => {
    const timestamp = getDarajaTimestamp(new Date('2026-03-30T15:45:00'));
    assert.strictEqual(timestamp.length, 14);
    assert.strictEqual(timestamp.substring(0, 4), '2026');
  });
});
