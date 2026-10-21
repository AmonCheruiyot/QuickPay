import { assertEquals } from 'https://deno.land/std@0.168.0/testing/asserts.ts';
import { getDarajaTimestamp } from '../_shared/timestamp.ts';

Deno.test('getDarajaTimestamp - formats date as YYYYMMDDHHmmss', () => {
  const testDate = new Date('2026-03-30T15:45:30Z');
  const timestamp = getDarajaTimestamp(testDate);
  assertEquals(timestamp.length, 14);
});
