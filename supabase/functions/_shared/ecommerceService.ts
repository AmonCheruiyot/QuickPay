import { getSupabaseAdminClient } from './supabaseClient.ts';
import { normalizePhoneNumber } from './phone.ts';

// ---------------------------------------------------------------------------
// 1. User & Authentication Functions
// ---------------------------------------------------------------------------
export async function registerUser(data: {
  fullName: string;
  email: string;
  phone: string;
  password: string;
  address?: string;
  role?: string;
}) {
  const normalizedPhone = normalizePhoneNumber(data.phone);
  const emailClean = data.email.trim().toLowerCase();

  const supabase = getSupabaseAdminClient();

  // Check duplicate email
  const { data: existing } = await supabase
    .from('users')
    .select('id')
    .eq('email', emailClean)
    .maybeSingle();

  if (existing) {
    throw new Error('An account with this email address already exists');
  }

  const { data: newUser, error } = await supabase
    .from('users')
    .insert([
      {
        full_name: data.fullName.trim(),
        email: emailClean,
        phone: normalizedPhone,
        password_hash: data.password, // demo hash
        role: data.role || 'customer',
        address: data.address || '',
      },
    ])
    .select('id, full_name, email, phone, role, address, created_at')
    .single();

  if (error) {
    throw new Error(`Registration failed: ${error.message}`);
  }

  return formatUserRow(newUser);
}

export async function loginUser(email: string, password: string) {
  const emailClean = email.trim().toLowerCase();
  const supabase = getSupabaseAdminClient();

  const { data: user, error } = await supabase
    .from('users')
    .select('*')
    .eq('email', emailClean)
    .maybeSingle();

  if (error || !user) {
    throw new Error('Invalid email or password');
  }

  if (user.password_hash !== password) {
    throw new Error('Invalid email or password');
  }

  return formatUserRow(user);
}

export async function updateUserProfile(userId: number, data: { fullName?: string; phone?: string; address?: string }) {
  const supabase = getSupabaseAdminClient();
  const updates: Record<string, any> = { updated_at: new Date().toISOString() };

  if (data.fullName) updates.full_name = data.fullName.trim();
  if (data.phone) updates.phone = normalizePhoneNumber(data.phone);
  if (data.address !== undefined) updates.address = data.address;

  const { data: updated, error } = await supabase
    .from('users')
    .update(updates)
    .eq('id', userId)
    .select('id, full_name, email, phone, role, address, created_at')
    .single();

  if (error) {
    throw new Error(`Profile update failed: ${error.message}`);
  }

  return formatUserRow(updated);
}

// ---------------------------------------------------------------------------
// 2. Product Catalogue & Admin Inventory Functions
// ---------------------------------------------------------------------------
export async function getProducts(category?: string, query?: string) {
  const supabase = getSupabaseAdminClient();
  let dbQuery = supabase.from('products').select('*').eq('is_active', true).order('id', { ascending: true });

  if (category && category.toLowerCase() !== 'all') {
    dbQuery = dbQuery.eq('category', category);
  }

  if (query && query.trim().length > 0) {
    dbQuery = dbQuery.ilike('name', `%${query.trim()}%`);
  }

  const { data, error } = await dbQuery;
  if (error || !data) {
    return [];
  }

  return data.map(formatProductRow);
}

export async function getProductById(id: number) {
  const supabase = getSupabaseAdminClient();
  const { data, error } = await supabase.from('products').select('*').eq('id', id).maybeSingle();
  if (error || !data) {
    return null;
  }
  return formatProductRow(data);
}

export async function addProduct(data: {
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  category: string;
  imageUrl?: string;
}) {
  const supabase = getSupabaseAdminClient();
  const { data: newProd, error } = await supabase
    .from('products')
    .insert([
      {
        name: data.name.trim(),
        description: data.description.trim(),
        price: Math.round(data.price),
        stock_quantity: Math.round(data.stockQuantity),
        category: data.category.trim() || 'General',
        image_url: data.imageUrl || 'https://picsum.photos/seed/product/400/300',
      },
    ])
    .select()
    .single();

  if (error) {
    throw new Error(`Failed to add product: ${error.message}`);
  }

  // Record initial inventory transaction
  await supabase.from('inventory_transactions').insert([
    {
      product_id: newProd.id,
      quantity_changed: newProd.stock_quantity,
      transaction_type: 'INITIAL',
      notes: 'Initial stock entry',
    },
  ]);

  return formatProductRow(newProd);
}

export async function updateProduct(
  id: number,
  data: {
    name?: string;
    description?: string;
    price?: number;
    stockQuantity?: number;
    category?: string;
    imageUrl?: string;
  }
) {
  const supabase = getSupabaseAdminClient();
  const updates: Record<string, any> = { updated_at: new Date().toISOString() };

  if (data.name) updates.name = data.name.trim();
  if (data.description !== undefined) updates.description = data.description.trim();
  if (data.price !== undefined) updates.price = Math.round(data.price);
  if (data.category) updates.category = data.category.trim();
  if (data.imageUrl) updates.image_url = data.imageUrl;

  if (data.stockQuantity !== undefined) {
    const newStock = Math.round(data.stockQuantity);
    // Fetch old stock to calculate diff
    const { data: oldProd } = await supabase.from('products').select('stock_quantity').eq('id', id).single();
    if (oldProd) {
      const diff = newStock - oldProd.stock_quantity;
      if (diff !== 0) {
        updates.stock_quantity = newStock;
        await supabase.from('inventory_transactions').insert([
          {
            product_id: id,
            quantity_changed: diff,
            transaction_type: diff > 0 ? 'RESTOCK' : 'ADJUSTMENT',
            notes: `Admin stock update (${diff > 0 ? '+' : ''}${diff})`,
          },
        ]);
      }
    }
  }

  const { data: updated, error } = await supabase
    .from('products')
    .update(updates)
    .eq('id', id)
    .select()
    .single();

  if (error) {
    throw new Error(`Failed to update product: ${error.message}`);
  }

  return formatProductRow(updated);
}

// ---------------------------------------------------------------------------
// 3. Checkout, Order Management & Inventory Deductions
// ---------------------------------------------------------------------------
export async function createCheckoutOrder(data: {
  userId?: number;
  customerName: string;
  phoneNumber: string;
  deliveryAddress: string;
  cartItems: Array<{ productId: number; quantity: number }>;
}) {
  if (!data.cartItems || data.cartItems.length === 0) {
    throw new Error('Shopping cart is empty');
  }

  const normalizedPhone = normalizePhoneNumber(data.phoneNumber);
  const supabase = getSupabaseAdminClient();

  // 1. Fetch products and validate stock availability
  let calculatedTotal = 0;
  const validatedItems: Array<{
    productId: number;
    productName: string;
    quantity: number;
    unitPrice: number;
    subtotal: number;
  }> = [];

  for (const item of data.cartItems) {
    const { data: prod, error } = await supabase
      .from('products')
      .select('*')
      .eq('id', item.productId)
      .single();

    if (error || !prod) {
      throw new Error(`Product ID ${item.productId} not found`);
    }

    if (item.quantity > prod.stock_quantity) {
      throw new Error(
        `Insufficient stock for '${prod.name}'. Requested: ${item.quantity}, Available: ${prod.stock_quantity}`
      );
    }

    const subtotal = prod.price * item.quantity;
    calculatedTotal += subtotal;

    validatedItems.push({
      productId: prod.id,
      productName: prod.name,
      quantity: item.quantity,
      unitPrice: prod.price,
      subtotal,
    });
  }

  // 2. Generate unique order reference
  const timestamp = Date.now();
  const randomSuffix = Math.floor(1000 + Math.random() * 9000);
  const orderReference = `ORD-${timestamp}-${randomSuffix}`;

  // 3. Save order record in 'Pending Payment' state
  const { data: order, error: orderErr } = await supabase
    .from('orders')
    .insert([
      {
        order_reference: orderReference,
        user_id: data.userId || null,
        customer_name: data.customerName.trim(),
        phone_number: normalizedPhone,
        delivery_address: data.deliveryAddress.trim(),
        total_amount: calculatedTotal,
        order_status: 'Pending Payment',
        payment_status: 'PENDING',
      },
    ])
    .select()
    .single();

  if (orderErr) {
    throw new Error(`Failed to create order: ${orderErr.message}`);
  }

  // 4. Save order items
  const orderItemsRows = validatedItems.map((item) => ({
    order_id: order.id,
    product_id: item.productId,
    product_name: item.productName,
    quantity: item.quantity,
    unit_price: item.unitPrice,
    subtotal: item.subtotal,
  }));

  const { error: itemsErr } = await supabase.from('order_items').insert(orderItemsRows);
  if (itemsErr) {
    throw new Error(`Failed to save order items: ${itemsErr.message}`);
  }

  return {
    id: order.id,
    orderReference: order.order_reference,
    customerName: order.customer_name,
    phoneNumber: order.phone_number,
    deliveryAddress: order.delivery_address,
    totalAmount: order.total_amount,
    orderStatus: order.order_status,
    paymentStatus: order.payment_status,
    createdAt: order.created_at,
    items: validatedItems,
  };
}

export async function getOrderDetails(orderReference: string) {
  const supabase = getSupabaseAdminClient();
  const { data: order, error } = await supabase
    .from('orders')
    .select('*')
    .eq('order_reference', orderReference)
    .maybeSingle();

  if (error || !order) {
    return null;
  }

  const { data: items } = await supabase
    .from('order_items')
    .select('*')
    .eq('order_id', order.id);

  // Fetch linked payment receipt if available
  const { data: payment } = await supabase
    .from('payments')
    .select('receipt_number, result_description, updated_at')
    .eq('order_reference', orderReference)
    .eq('status', 'SUCCESS')
    .maybeSingle();

  return {
    id: order.id,
    orderReference: order.order_reference,
    userId: order.user_id,
    customerName: order.customer_name,
    phoneNumber: order.phone_number,
    deliveryAddress: order.delivery_address,
    totalAmount: order.total_amount,
    orderStatus: order.order_status,
    paymentStatus: order.payment_status,
    receiptNumber: payment?.receipt_number || null,
    createdAt: order.created_at,
    updatedAt: order.updated_at,
    items: (items || []).map((item: any) => ({
      id: item.id,
      productId: item.product_id,
      productName: item.product_name,
      quantity: item.quantity,
      unitPrice: item.unit_price,
      subtotal: item.subtotal,
    })),
  };
}

export async function getOrderHistoryForUser(userId?: number) {
  const supabase = getSupabaseAdminClient();
  let query = supabase.from('orders').select('*').order('created_at', { ascending: false });

  if (userId) {
    query = query.eq('user_id', userId);
  }

  const { data: orders, error } = await query;
  if (error || !orders) {
    return [];
  }

  const result = [];
  for (const order of orders) {
    const { data: items } = await supabase
      .from('order_items')
      .select('product_name, quantity, unit_price, subtotal')
      .eq('order_id', order.id);

    const { data: payment } = await supabase
      .from('payments')
      .select('receipt_number')
      .eq('order_reference', order.order_reference)
      .eq('status', 'SUCCESS')
      .maybeSingle();

    result.push({
      id: order.id,
      orderReference: order.order_reference,
      customerName: order.customer_name,
      phoneNumber: order.phone_number,
      deliveryAddress: order.delivery_address,
      totalAmount: order.total_amount,
      orderStatus: order.order_status,
      paymentStatus: order.payment_status,
      receiptNumber: payment?.receipt_number || null,
      createdAt: order.created_at,
      itemCount: (items || []).length,
      items: items || [],
    });
  }

  return result;
}

export async function markOrderAsPaidAndDeductStock(orderReference: string, receiptNumber?: string) {
  const supabase = getSupabaseAdminClient();

  // 1. Fetch order
  const { data: order, error } = await supabase
    .from('orders')
    .select('*')
    .eq('order_reference', orderReference)
    .maybeSingle();

  if (error || !order) {
    console.warn(`[Order Payment] Order ${orderReference} not found`);
    return;
  }

  if (order.order_status === 'Paid') {
    console.log(`[Order Payment] Order ${orderReference} is already marked as Paid`);
    return;
  }

  // 2. Update order status to 'Paid' and payment_status to 'SUCCESS'
  await supabase
    .from('orders')
    .update({
      order_status: 'Paid',
      payment_status: 'SUCCESS',
      updated_at: new Date().toISOString(),
    })
    .eq('id', order.id);

  // 3. Fetch order items and deduct stock from products table
  const { data: items } = await supabase
    .from('order_items')
    .select('product_id, quantity')
    .eq('order_id', order.id);

  if (items && items.length > 0) {
    for (const item of items) {
      if (!item.product_id) continue;

      // Fetch current stock
      const { data: prod } = await supabase
        .from('products')
        .select('stock_quantity')
        .eq('id', item.product_id)
        .single();

      if (prod) {
        const newStock = Math.max(0, prod.stock_quantity - item.quantity);
        await supabase
          .from('products')
          .update({ stock_quantity: newStock })
          .eq('id', item.product_id);

        // Record stock deduction in inventory_transactions
        await supabase.from('inventory_transactions').insert([
          {
            product_id: item.product_id,
            quantity_changed: -item.quantity,
            transaction_type: 'DEDUCTION',
            notes: `Stock deducted for paid order ${orderReference}`,
          },
        ]);
      }
    }
  }

  console.log(`[Order Payment] Order ${orderReference} marked as Paid and stock deducted successfully!`);
}

export async function updateOrderStatusByAdmin(orderId: number, newStatus: string) {
  const validStatuses = ['Pending Payment', 'Paid', 'Processing', 'Shipped', 'Completed', 'Cancelled'];
  if (!validStatuses.includes(newStatus)) {
    throw new Error(`Invalid status '${newStatus}'`);
  }

  const supabase = getSupabaseAdminClient();
  const { data, error } = await supabase
    .from('orders')
    .update({ order_status: newStatus, updated_at: new Date().toISOString() })
    .eq('id', orderId)
    .select()
    .single();

  if (error) {
    throw new Error(`Failed to update order status: ${error.message}`);
  }

  return data;
}

// ---------------------------------------------------------------------------
// 4. Admin Dashboard Summary Stats
// ---------------------------------------------------------------------------
export async function getAdminDashboardStats() {
  const supabase = getSupabaseAdminClient();

  // Total sales revenue from paid orders
  const { data: paidOrders } = await supabase
    .from('orders')
    .select('total_amount')
    .eq('order_status', 'Paid');

  const totalRevenue = (paidOrders || []).reduce((sum: number, o: any) => sum + o.total_amount, 0);

  // Total orders count
  const { count: totalOrders } = await supabase
    .from('orders')
    .select('*', { count: 'exact', head: true });

  // Pending orders count
  const { count: pendingOrders } = await supabase
    .from('orders')
    .select('*', { count: 'exact', head: true })
    .eq('order_status', 'Pending Payment');

  // Low stock products count (stock < 5)
  const { data: lowStockProds } = await supabase
    .from('products')
    .select('id, name, stock_quantity')
    .lt('stock_quantity', 5);

  return {
    totalRevenue,
    totalOrders: totalOrders || 0,
    pendingOrders: pendingOrders || 0,
    lowStockCount: (lowStockProds || []).length,
    lowStockProducts: lowStockProds || [],
  };
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
function formatUserRow(row: any) {
  return {
    id: row.id,
    fullName: row.full_name,
    email: row.email,
    phone: row.phone,
    role: row.role,
    address: row.address,
    createdAt: row.created_at,
  };
}

function formatProductRow(row: any) {
  return {
    id: row.id,
    name: row.name,
    description: row.description,
    price: row.price,
    stockQuantity: row.stock_quantity,
    category: row.category,
    imageUrl: row.image_url,
    isActive: row.is_active,
    inStock: row.stock_quantity > 0,
    createdAt: row.created_at,
  };
}
