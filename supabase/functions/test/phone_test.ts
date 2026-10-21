import { assertEquals, assertThrows } from 'https://deno.land/std@0.168.0/testing/asserts.ts';
import { normalizePhoneNumber, maskPhoneNumber } from '../_shared/phone.ts';

Deno.test('normalizePhoneNumber - converts valid Kenyan numbers to 254XXXXXXXXX', () => {
  assertEquals(normalizePhoneNumber('0712345678'), '254712345678');
  assertEquals(normalizePhoneNumber('0112345678'), '254112345678');
  assertEquals(normalizePhoneNumber('+254712345678'), '254712345678');
  assertEquals(normalizePhoneNumber('254712345678'), '254712345678');
  assertEquals(normalizePhoneNumber('712345678'), '254712345678');
});

Deno.test('normalizePhoneNumber - throws error for invalid numbers', () => {
  assertThrows(() => normalizePhoneNumber('12345'));
  assertThrows(() => normalizePhoneNumber('0201234567'));
  assertThrows(() => normalizePhoneNumber(''));
});

Deno.test('maskPhoneNumber - masks middle digits', () => {
  assertEquals(maskPhoneNumber('254712345678'), '2547****5678');
});
