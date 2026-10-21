const express = require('express');
const cors = require('cors');
const paymentsRouter = require('./routes/payments');
const errorHandler = require('./middleware/errorHandler');

const app = express();

app.use(cors());
app.use(express.json());

// Health endpoint
app.get('/health', (req, res) => {
  res.status(200).json({
    status: 'UP',
    app: 'QuickPay Daraja Backend',
    timestamp: new Date().toISOString(),
  });
});

// Payments API routes
app.use('/api/payments', paymentsRouter);

// Centralized error handling
app.use(errorHandler);

module.exports = app;
