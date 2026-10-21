/**
 * Normalizes a Kenyan phone number into standard 254XXXXXXXXX format.
 * Accepts formats like:
 * 0712345678, 0112345678, +254712345678, 254712345678, 712345678, 112345678
 */
export function normalizePhoneNumber(rawPhone: string): string {
  if (!rawPhone || typeof rawPhone !== 'string') {
    throw new Error('Phone number must be a non-empty string');
  }

  let cleaned = rawPhone.replace(/[\s\-\+]/g, '');

  if (cleaned.startsWith('0')) {
    cleaned = '254' + cleaned.substring(1);
  } else if (cleaned.startsWith('7') || cleaned.startsWith('1')) {
    cleaned = '254' + cleaned;
  }

  const kenyanPhoneRegex = /^254[71]\d{8}$/;
  if (!kenyanPhoneRegex.test(cleaned)) {
    throw new Error(
      'Invalid Kenyan phone number format. Must be a valid M-Pesa number (e.g. 0712345678 or 0112345678)'
    );
  }

  return cleaned;
}

/**
 * Masks phone number for public display (e.g., 2547****5678)
 */
export function maskPhoneNumber(phone: string): string {
  if (!phone || typeof phone !== 'string' || phone.length < 8) {
    return '****';
  }
  const prefix = phone.substring(0, 4);
  const suffix = phone.substring(phone.length - 4);
  return `${prefix}****${suffix}`;
}
