import { serve } from 'https://deno.land/std@0.168.0/http/server.ts';
import {
  createPendingPayment,
  updatePaymentCheckoutDetails,
  updatePaymentStatus,
  processCallback,
  getPaymentByReference,
  getPaymentHistory,
} from '../_shared/paymentService.ts';
import { initiateStkPush } from '../_shared/daraja.ts';
import { normalizePhoneNumber } from '../_shared/phone.ts';

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
};

serve(async (req: Request) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders });
  }

  const url = new URL(req.url);
  const pathname = url.pathname.replace(/^\/quickpay-api/, '').replace(/\/$/, '') || '/';

  try {
    // Health check endpoint
    if (req.method === 'GET' && (pathname === '/health' || pathname === '/')) {
      return jsonResponse({
        status: 'UP',
        app: 'QuickPay Supabase Edge Function',
        timestamp: new Date().toISOString(),
      });
    }

    // Initiate STK Push payment
    if (req.method === 'POST' && (pathname === '/stk-push' || pathname === '/api/payments/stk-push')) {
      let body: any = {};
      try {
        body = await req.json();
      } catch {
        return jsonResponse({ error: 'Invalid JSON', message: 'Request body must be valid JSON' }, 400);
      }

      const { phone, amount } = body;
      if (!phone || typeof phone !== 'string') {
        return jsonResponse({ error: 'Invalid Phone Number', message: 'Phone number is required' }, 400);
      }

      let normalizedPhone: string;
      try {
        normalizedPhone = normalizePhoneNumber(phone);
      } catch (err: any) {
        return jsonResponse({ error: 'Invalid Phone Number', message: err.message }, 400);
      }

      const parsedAmount = Math.round(Number(amount));
      if (isNaN(parsedAmount) || parsedAmount < 1 || parsedAmount > 300000) {
        return jsonResponse({ error: 'Invalid Amount', message: 'Amount must be a positive number between 1 and 300,000 KSh' }, 400);
      }

      // 1. Create PENDING payment in PostgreSQL before external network call
      const payment = await createPendingPayment(normalizedPhone, parsedAmount);

      // 2. Trigger STK Push via Safaricom Daraja
      try {
        const darajaResponse = await initiateStkPush({
          phone: normalizedPhone,
          amount: parsedAmount,
          accountReference: `QP-${payment.reference}`,
          transactionDesc: `QuickPay Payment ${payment.reference}`,
        });

        // 3. Update transaction record with CheckoutRequestID & MerchantRequestID
        const updatedPayment = await updatePaymentCheckoutDetails(payment.reference, {
          merchantRequestId: darajaResponse.MerchantRequestID,
          checkoutRequestId: darajaResponse.CheckoutRequestID,
        });

        return jsonResponse({
          success: true,
          message: darajaResponse.CustomerMessage || 'STK Push prompt sent to your phone. Please enter your M-Pesa PIN.',
          reference: updatedPayment.reference,
          checkoutRequestId: updatedPayment.checkoutRequestId,
          status: updatedPayment.status,
          maskedPhone: updatedPayment.maskedPhone,
          amount: updatedPayment.amount,
        });
      } catch (darajaError: any) {
        await updatePaymentStatus(payment.reference, 'FAILED', {
          resultCode: darajaError.statusCode || 500,
          resultDescription: darajaError.message,
        });

        return jsonResponse({
          error: 'STK Push Failed',
          message: darajaError.message || 'Failed to initiate M-Pesa payment prompt',
          details: darajaError.darajaResponse?.errorMessage || darajaError.darajaResponse?.ResponseDescription,
        }, 500);
      }
    }

    // Public Callback Webhook for Safaricom Daraja
    if (req.method === 'POST' && (pathname === '/callback' || pathname === '/api/payments/callback')) {
      let callbackBody: any = {};
      try {
        callbackBody = await req.json();
      } catch {
        console.warn('[Callback Webhook] Non-JSON payload received');
      }

      console.log('[Callback Received]', JSON.stringify(callbackBody));
      await processCallback(callbackBody);

      // Return standard Safaricom callback acknowledgment
      return jsonResponse({
        ResultCode: 0,
        ResultDesc: 'Callback processed successfully',
      });
    }

    // Get Payment Status by Reference
    if (req.method === 'GET' && (pathname.startsWith('/status/') || pathname.startsWith('/payments/') || pathname.startsWith('/api/payments/'))) {
      const parts = pathname.split('/');
      const reference = parts[parts.length - 1];

      if (!reference || reference === 'history' || reference === 'payments') {
        // Fallthrough to history if path is /history or /payments
      } else {
        const payment = await getPaymentByReference(reference);
        if (!payment) {
          return jsonResponse({ error: 'Not Found', message: `Transaction with reference '${reference}' was not found.` }, 404);
        }
        return jsonResponse({ success: true, payment });
      }
    }

    // Get Payment History
    if (req.method === 'GET' && (pathname === '/history' || pathname === '/payments' || pathname === '/api/payments')) {
      const limit = parseInt(url.searchParams.get('limit') || '50', 10);
      const offset = parseInt(url.searchParams.get('offset') || '0', 10);

      const history = await getPaymentHistory(limit, offset);
      return jsonResponse({
        success: true,
        count: history.length,
        payments: history,
      });
    }

    return jsonResponse({ error: 'Not Found', message: `Route '${req.method} ${pathname}' not found` }, 404);
  } catch (err: any) {
    console.error('[Edge Function Error]', err.stack || err.message);
    return jsonResponse({
      error: err.name || 'InternalServerError',
      message: err.message || 'An unexpected error occurred',
    }, 500);
  }
});

function jsonResponse(data: any, status: number = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      ...corsHeaders,
      'Content-Type': 'application/json',
    },
  });
}
