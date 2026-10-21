const paymentService = require('../services/paymentService');
const darajaService = require('../services/darajaService');

async function initiatePayment(req, res, next) {
  try {
    const phone = req.normalizedPhone;
    const amount = req.parsedAmount;

    // 1. Save PENDING transaction in SQLite before calling external service
    const payment = paymentService.createPendingPayment({ phone, amount });

    // 2. Trigger STK Push via Daraja API
    try {
      const darajaResponse = await darajaService.initiateStkPush({
        phone,
        amount,
        accountReference: `QP-${payment.reference}`,
        transactionDesc: `QuickPay Payment ${payment.reference}`,
      });

      // 3. Store MerchantRequestID and CheckoutRequestID
      const updatedPayment = paymentService.updatePaymentCheckoutDetails(payment.reference, {
        merchantRequestId: darajaResponse.MerchantRequestID,
        checkoutRequestId: darajaResponse.CheckoutRequestID,
      });

      return res.status(200).json({
        success: true,
        message: darajaResponse.CustomerMessage || 'STK Push prompt sent to your phone. Please enter your M-Pesa PIN.',
        reference: updatedPayment.reference,
        checkoutRequestId: updatedPayment.checkoutRequestId,
        status: updatedPayment.status,
        maskedPhone: updatedPayment.maskedPhone,
        amount: updatedPayment.amount,
      });
    } catch (darajaError) {
      // If Daraja fails/rejects, update transaction status to FAILED in SQLite
      paymentService.updatePaymentStatus(payment.reference, 'FAILED', {
        resultCode: darajaError.statusCode || 500,
        resultDescription: darajaError.message,
      });
      throw darajaError;
    }
  } catch (error) {
    next(error);
  }
}

function handleCallback(req, res) {
  try {
    console.log('[Callback Received]', JSON.stringify(req.body));
    const result = paymentService.processCallback(req.body);

    // Safaricom Daraja callback acknowledgment response
    return res.status(200).json({
      ResultCode: 0,
      ResultDesc: 'Callback processed successfully',
    });
  } catch (error) {
    console.error('[Callback Error]', error.message);
    // Return 200 to Safaricom to acknowledge receipt even if parsing failed internally
    return res.status(200).json({
      ResultCode: 1,
      ResultDesc: 'Callback received with errors',
    });
  }
}

function getPaymentStatus(req, res, next) {
  try {
    const { reference } = req.params;
    const payment = paymentService.getPaymentByReference(reference);

    if (!payment) {
      return res.status(404).json({
        error: 'Not Found',
        message: `Transaction with reference '${reference}' was not found.`,
      });
    }

    return res.status(200).json({
      success: true,
      payment,
    });
  } catch (error) {
    next(error);
  }
}

function getPaymentHistory(req, res, next) {
  try {
    const limit = parseInt(req.query.limit || '50', 10);
    const offset = parseInt(req.query.offset || '0', 10);

    const history = paymentService.getPaymentHistory(limit, offset);
    return res.status(200).json({
      success: true,
      count: history.length,
      payments: history,
    });
  } catch (error) {
    next(error);
  }
}

module.exports = {
  initiatePayment,
  handleCallback,
  getPaymentStatus,
  getPaymentHistory,
};
