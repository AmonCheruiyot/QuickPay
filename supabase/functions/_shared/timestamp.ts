/**
 * Generates timestamp string in YYYYMMDDHHmmss format required by Safaricom Daraja API.
 */
export function getDarajaTimestamp(date: Date = new Date()): string {
  const pad = (num: number) => String(num).padStart(2, '0');
  const year = date.getFullYear();
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hours = pad(date.getHours());
  const minutes = pad(date.getMinutes());
  const seconds = pad(date.getSeconds());

  return `${year}${month}${day}${hours}${minutes}${seconds}`;
}
