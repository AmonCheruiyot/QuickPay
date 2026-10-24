import { serve } from 'https://deno.land/std@0.168.0/http/server.ts';
import {
  createPendingPayment,
  updatePaymentCheckoutDetails,
  updatePaymentStatus,
  processCallback,
  getPaymentByReference,
  getPaymentHistory,
} from '../_shared/paymentService.ts';
import {
  registerUser,
  loginUser,
  updateUserProfile,
  getProducts,
  getProductById,
  addProduct,
  updateProduct,
  createCheckoutOrder,
  getOrderDetails,
  getOrderHistoryForUser,
  updateOrderStatusByAdmin,
  getAdminDashboardStats,
} from '../_shared/ecommerceService.ts';
import { initiateStkPush } from '../_shared/daraja.ts';
import { normalizePhoneNumber } from '../_shared/phone.ts';

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
};

serve(async (req: Request) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders });
  }

  const url = new URL(req.url);
  const pathname = url.pathname.replace(/^\/quickpay-api/, '').replace(/\/$/, '') || '/';

  try {
    // 1. Health check
    if (req.method === 'GET' && (pathname === '/health' || pathname === '/')) {
      return jsonResponse({
        status: 'UP',
        app: 'QuickPay E-Commerce Supabase Edge Function',
        timestamp: new Date().toISOString(),
      });
    }

    // -----------------------------------------------------------------------
    // 2. Authentication Routes
    // -----------------------------------------------------------------------
    if (req.method === 'POST' && pathname === '/auth/register') {
      const body = await req.json();
      const user = await registerUser(body);
      return jsonResponse({ success: true, message: 'Account registered successfully', user });
    }

    if (req.method === 'POST' && pathname === '/auth/login') {
      const body = await req.json();
      const user = await loginUser(body.email, body.password);
      return jsonResponse({ success: true, message: 'Login successful', user });
    }

    if (req.method === 'POST' && pathname === '/auth/profile') {
      const body = await req.json();
      const updatedUser = await updateUserProfile(body.userId, body);
      return jsonResponse({ success: true, message: 'Profile updated successfully', user: updatedUser });
    }

    // -----------------------------------------------------------------------
    // 3. Product Catalogue Routes
    // -----------------------------------------------------------------------
    if (req.method === 'GET' && pathname === '/products') {
      const category = url.searchParams.get('category') || undefined;
      const query = url.searchParams.get('q') || undefined;
      const products = await getProducts(category, query);
      return jsonResponse({ success: true, count: products.length, products });
    }

    if (req.method === 'GET' && pathname.startsWith('/products/')) {
      const id = parseInt(pathname.split('/')[2], 10);
      const product = await getProductById(id);
      if (!product) {
        return jsonResponse({ error: 'Not Found', message: 'Product not found' }, 404);
      }
      return jsonResponse({ success: true, product });
    }

    if (req.method === 'POST' && pathname === '/admin/products') {
      const body = await req.json();
      const product = await addProduct(body);
      return jsonResponse({ success: true, message: 'Product created successfully', product });
    }

    if (req.method === 'PUT' && pathname.startsWith('/admin/products/')) {
      const id = parseInt(pathname.split('/')[3], 10);
      const body = await req.json();
      const product = await updateProduct(id, body);
      return jsonResponse({ success: true, message: 'Product updated successfully', product });
    }

    // -----------------------------------------------------------------------
    // 4. Checkout & Order Management Routes
    // -----------------------------------------------------------------------
    if (req.method === 'POST' && pathname === '/orders/checkout') {
      const body = await req.json();
      const order = await createCheckoutOrder(body);
      return jsonResponse({ success: true, message: 'Order created successfully', order });
    }

    if (req.method === 'GET' && pathname === '/orders/history') {
      const userIdStr = url.searchParams.get('userId');
      const userId = userIdStr ? parseInt(userIdStr, 10) : undefined;
      const orders = await getOrderHistoryForUser(userId);
      return jsonResponse({ success: true, count: orders.length, orders });
    }

    if (req.method === 'GET' && pathname.startsWith('/orders/')) {
      const ref = pathname.split('/')[2];
      const order = await getOrderDetails(ref);
      if (!order) {
        return jsonResponse({ error: 'Not Found', message: 'Order not found' }, 404);
      }
      return jsonResponse({ success: true, order });
    }

    if (req.method === 'PUT' && pathname.includes('/admin/orders/') && pathname.endsWith('/status')) {
      const parts = pathname.split('/');
      const orderId = parseInt(parts[3], 10);
      const body = await req.json();
      const updatedOrder = await updateOrderStatusByAdmin(orderId, body.status);
      return jsonResponse({ success: true, message: 'Order status updated', order: updatedOrder });
    }

    if (req.method === 'GET' && pathname === '/admin/dashboard') {
      const stats = await getAdminDashboardStats();
      return jsonResponse({ success: true, stats });
    }

    // -----------------------------------------------------------------------
    // 5. Safaricom M-Pesa STK Push Payment Routes
    // -----------------------------------------------------------------------
    if (req.method === 'POST' && (pathname === '/stk-push' || pathname === '/api/payments/stk-push')) {
      let body: any = {};
      try {
        body = await req.json();
      } catch {
        return jsonResponse({ error: 'Invalid JSON', message: 'Request body must be valid JSON' }, 400);
      }

      const { phone, amount, orderReference } = body;
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

      // Save PENDING payment in database
      const payment = await createPendingPayment(normalizedPhone, parsedAmount, orderReference);

      // Trigger STK Push
      try {
        const darajaResponse = await initiateStkPush({
          phone: normalizedPhone,
          amount: parsedAmount,
          accountReference: orderReference || `QP-${payment.reference}`,
          transactionDesc: `QuickPay Payment ${orderReference || payment.reference}`,
        });

        const updatedPayment = await updatePaymentCheckoutDetails(payment.reference, {
          merchantRequestId: darajaResponse.MerchantRequestID,
          checkoutRequestId: darajaResponse.CheckoutRequestID,
        });

        return jsonResponse({
          success: true,
          message: darajaResponse.CustomerMessage || 'STK Push prompt sent to your phone. Please enter your M-Pesa PIN.',
          reference: updatedPayment.reference,
          orderReference: updatedPayment.orderReference,
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

    if (req.method === 'POST' && (pathname === '/callback' || pathname === '/api/payments/callback')) {
      let callbackBody: any = {};
      try {
        callbackBody = await req.json();
      } catch {
        console.warn('[Callback Webhook] Non-JSON payload received');
      }

      console.log('[Callback Received]', JSON.stringify(callbackBody));
      await processCallback(callbackBody);

      return jsonResponse({
        ResultCode: 0,
        ResultDesc: 'Callback processed successfully',
      });
    }

    if (req.method === 'GET' && (pathname.startsWith('/status/') || pathname.startsWith('/payments/') || pathname.startsWith('/api/payments/'))) {
      const parts = pathname.split('/');
      const reference = parts[parts.length - 1];

      if (!reference || reference === 'history' || reference === 'payments') {
        // Fallthrough
      } else {
        const payment = await getPaymentByReference(reference);
        if (!payment) {
          return jsonResponse({ error: 'Not Found', message: `Transaction with reference '${reference}' was not found.` }, 404);
        }
        return jsonResponse({ success: true, payment });
      }
    }

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
