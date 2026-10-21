const express = require('express');
const router = express.Router();
const paymentsController = require('../controllers/paymentsController');
const { validateStkPushRequest } = require('../middleware/requestValidation');

// POST /api/payments/stk-push
router.post('/stk-push', validateStkPushRequest, paymentsController.initiatePayment);

// POST /api/payments/callback
router.post('/callback', paymentsController.handleCallback);

// GET /api/payments/:reference
router.get('/:reference', paymentsController.getPaymentStatus);

// GET /api/payments
router.get('/', paymentsController.getPaymentHistory);

module.exports = router;
