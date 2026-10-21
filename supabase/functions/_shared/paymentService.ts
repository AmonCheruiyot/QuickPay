import { getSupabaseAdminClient } from './supabaseClient.ts';
import { normalizePhoneNumber, maskPhoneNumber } from './phone.ts';

export interface PaymentRecord {
  id: number;
  reference: string;
  phone: string;
  maskedPhone: string;
  amount: number;
  status: string;
  merchantRequestId?: string | null;
  checkoutRequestId?: string | null;
  receiptNumber?: string | null;
  resultCode?: number | null;
  resultDescription?: string | null;
  transactionDate?: string | null;
  createdAt: string;
  updatedAt: string;
}

export async function createPendingPayment(phone: string, amount: number): Promise<PaymentRecord> {
  const normalizedPhone = normalizePhoneNumber(phone);
  const parsedAmount = Math.round(amount);

  if (isNaN(parsedAmount) || parsedAmount <= 0) {
    throw new Error('Amount must be a positive integer');
  }

  const timestamp = Date.now();
  const randomSuffix = Math.floor(1000 + Math.random() * 9000);
  const reference = `QP${timestamp}${randomSuffix}`;

  const supabase = getSupabaseAdminClient();
  const { data, error } = await supabase
    .from('payments')
    .insert([
      {
        reference,
        phone: normalizedPhone,
        amount: parsedAmount,
        status: 'PENDING',
      },
    ])
    .select()
    .single();

  if (error) {
    throw new Error(`Database error creating payment: ${error.message}`);
  }

  return formatPaymentRow(data);
}

export async function updatePaymentCheckoutDetails(
  reference: string,
  details: { merchantRequestId: string; checkoutRequestId: string }
): Promise<PaymentRecord> {
  const supabase = getSupabaseAdminClient();
  const { data, error } = await supabase
    .from('payments')
    .update({
      merchant_request_id: details.merchantRequestId,
      checkout_request_id: details.checkoutRequestId,
      updated_at: new Date().toISOString(),
    })
    .eq('reference', reference)
    .select()
    .single();

  if (error) {
    throw new Error(`Database error updating checkout details: ${error.message}`);
  }

  return formatPaymentRow(data);
}

export async function updatePaymentStatus(
  reference: string,
  status: string,
  details: {
    resultCode?: number | null;
    resultDescription?: string | null;
    receiptNumber?: string | null;
    transactionDate?: string | null;
  }
): Promise<PaymentRecord> {
  const supabase = getSupabaseAdminClient();
  const updatePayload: Record<string, any> = {
    status,
    updated_at: new Date().toISOString(),
  };

  if (details.resultCode !== undefined) updatePayload.result_code = details.resultCode;
  if (details.resultDescription !== undefined) updatePayload.result_description = details.resultDescription;
  if (details.receiptNumber) updatePayload.receipt_number = details.receiptNumber;
  if (details.transactionDate) updatePayload.transaction_date = details.transactionDate;

  const { data, error } = await supabase
    .from('payments')
    .update(updatePayload)
    .eq('reference', reference)
    .select()
    .single();

  if (error) {
    throw new Error(`Database error updating payment status: ${error.message}`);
  }

  return formatPaymentRow(data);
}

export async function processCallback(callbackPayload: any) {
  if (!callbackPayload || !callbackPayload.Body || !callbackPayload.Body.stkCallback) {
    throw new Error('Invalid Daraja callback payload structure');
  }

  const stkCallback = callbackPayload.Body.stkCallback;
  const checkoutRequestId = stkCallback.CheckoutRequestID;
  const resultCode = stkCallback.ResultCode;
  const resultDesc = stkCallback.ResultDesc || '';

  const supabase = getSupabaseAdminClient();
  const { data: existingPayment, error: fetchError } = await supabase
    .from('payments')
    .select('*')
    .eq('checkout_request_id', checkoutRequestId)
    .maybeSingle();

  if (fetchError || !existingPayment) {
    console.warn(`[Callback] Received callback for unknown CheckoutRequestID: ${checkoutRequestId}`);
    return { success: false, reason: 'Transaction not found for CheckoutRequestID' };
  }

  // Idempotency check: Do not allow a failed or duplicate callback to overwrite a confirmed success
  if (existingPayment.status === 'SUCCESS' && resultCode !== 0) {
    console.warn(`[Callback] Ignoring failed callback for already successful transaction: ${existingPayment.reference}`);
    return { success: true, payment: formatPaymentRow(existingPayment) };
  }

  let status = 'FAILED';
  let receiptNumber: string | null = null;
  let transactionDate: string | null = null;

  if (resultCode === 0) {
    status = 'SUCCESS';
    if (stkCallback.CallbackMetadata && Array.isArray(stkCallback.CallbackMetadata.Item)) {
      stkCallback.CallbackMetadata.Item.forEach((item: any) => {
        if (item.Name === 'MpesaReceiptNumber') {
          receiptNumber = String(item.Value);
        } else if (item.Name === 'TransactionDate') {
          transactionDate = String(item.Value);
        }
      });
    }
  } else if (resultCode === 1032 || resultDesc.toLowerCase().includes('cancel')) {
    status = 'CANCELLED';
  }

  const updatedPayment = await updatePaymentStatus(existingPayment.reference, status, {
    resultCode,
    resultDescription: resultDesc,
    receiptNumber,
    transactionDate,
  });

  return { success: true, payment: updatedPayment };
}

export async function getPaymentByReference(reference: string): Promise<PaymentRecord | null> {
  const supabase = getSupabaseAdminClient();
  const { data, error } = await supabase
    .from('payments')
    .select('*')
    .eq('reference', reference)
    .maybeSingle();

  if (error || !data) {
    return null;
  }

  return formatPaymentRow(data);
}

export async function getPaymentHistory(limit: number = 50, offset: number = 0): Promise<PaymentRecord[]> {
  const supabase = getSupabaseAdminClient();
  const { data, error } = await supabase
    .from('payments')
    .select('*')
    .order('created_at', { ascending: false })
    .range(offset, offset + limit - 1);

  if (error || !data) {
    return [];
  }

  return data.map(formatPaymentRow);
}

function formatPaymentRow(row: any): PaymentRecord {
  return {
    id: row.id,
    reference: row.reference,
    phone: row.phone,
    maskedPhone: maskPhoneNumber(row.phone),
    amount: row.amount,
    status: row.status,
    merchantRequestId: row.merchant_request_id,
    checkoutRequestId: row.checkout_request_id,
    receiptNumber: row.receipt_number,
    resultCode: row.result_code,
    resultDescription: row.result_description,
    transactionDate: row.transaction_date,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}
