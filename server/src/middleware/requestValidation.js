const { normalizePhoneNumber } = require('../utils/phone');

function validateStkPushRequest(req, res, next) {
  const { phone, amount } = req.body || {};

  if (!phone || typeof phone !== 'string') {
    return res.status(400).json({
      error: 'Invalid Request',
      message: 'Phone number is required and must be a string.',
    });
  }

  let normalizedPhone;
  try {
    normalizedPhone = normalizePhoneNumber(phone);
  } catch (err) {
    return res.status(400).json({
      error: 'Invalid Phone Number',
      message: err.message,
    });
  }

  const parsedAmount = parseInt(amount, 10);
  if (isNaN(parsedAmount) || parsedAmount < 1 || parsedAmount > 300000) {
    return res.status(400).json({
      error: 'Invalid Amount',
      message: 'Amount must be a positive integer between 1 and 300,000 KSh.',
    });
  }

  // Attach normalized data to request
  req.normalizedPhone = normalizedPhone;
  req.parsedAmount = parsedAmount;
  next();
}

module.exports = {
  validateStkPushRequest,
};
