/**
 * Generates timestamp string in YYYYMMDDHHmmss format required by Daraja.
 * @param {Date} [date] Optional date object (defaults to now)
 * @returns {string} e.g. "20260330153000"
 */
function getDarajaTimestamp(date = new Date()) {
  const pad = (num) => String(num).padStart(2, '0');
  const year = date.getFullYear();
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hours = pad(date.getHours());
  const minutes = pad(date.getMinutes());
  const seconds = pad(date.getSeconds());

  return `${year}${month}${day}${hours}${minutes}${seconds}`;
}

module.exports = {
  getDarajaTimestamp,
};
