const config = require('./config/env');
const { getDb } = require('./database/db');
const app = require('./app');

// Initialize DB schema on startup
getDb();

const PORT = config.port;

const server = app.listen(PORT, () => {
  console.log(`====================================================`);
  console.log(`  QuickPay Backend Server running on port ${PORT}`);
  console.log(`  Environment: ${config.nodeEnv}`);
  console.log(`  Callback URL: ${config.daraja.callbackUrl}`);
  console.log(`====================================================`);
});

module.exports = server;
