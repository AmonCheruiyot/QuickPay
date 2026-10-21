/**
 * Normalizes a Kenyan phone number into standard 254XXXXXXXXX format.
 * Accepts formats like:
 * 0712345678, 0112345678, +254712345678, 254712345678, 712345678, 112345678
 *
 * @param {string} rawPhone
 * @returns {string} Normalized 12-digit phone number starting with 254
 */
function normalizePhoneNumber(rawPhone) {
  if (!rawPhone || typeof rawPhone !== 'string') {
    throw new Error('Phone number must be a non-empty string');
  }

  // Remove whitespace, dashes, plus signs
  let cleaned = rawPhone.replace(/[\s\-\+]/g, '');

  if (cleaned.startsWith('0')) {
    cleaned = '254' + cleaned.substring(1);
  } else if (cleaned.startsWith('7') || cleaned.startsWith('1')) {
    cleaned = '254' + cleaned;
  }

  // Validate Kenyan format: 254 followed by 9 digits starting with 7 or 1
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
 * @param {string} phone
 * @returns {string}
 */
function maskPhoneNumber(phone) {
  if (!phone || typeof phone !== 'string' || phone.length < 8) {
    return '****';
  }
  const prefix = phone.substring(0, 4);
  const suffix = phone.substring(phone.length - 4);
  return `${prefix}****${suffix}`;
}

module.exports = {
  normalizePhoneNumber,
  maskPhoneNumber,
};
