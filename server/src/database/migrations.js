function runMigrations(db) {
  db.exec(`
    CREATE TABLE IF NOT EXISTS payments (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      reference TEXT NOT NULL UNIQUE,
      phone TEXT NOT NULL,
      amount INTEGER NOT NULL,
      status TEXT NOT NULL DEFAULT 'PENDING',
      merchant_request_id TEXT,
      checkout_request_id TEXT UNIQUE,
      receipt_number TEXT,
      result_code INTEGER,
      result_description TEXT,
      transaction_date TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

    CREATE INDEX IF NOT EXISTS idx_payments_reference ON payments(reference);
    CREATE INDEX IF NOT EXISTS idx_payments_checkout_id ON payments(checkout_request_id);
    CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
  `);
}

module.exports = {
  runMigrations,
};
