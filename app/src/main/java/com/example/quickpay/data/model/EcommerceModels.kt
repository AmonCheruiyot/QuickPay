package com.example.quickpay.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("role") val role: String, // "customer" or "admin"
    @SerializedName("address") val address: String?
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("user") val user: User?,
    @SerializedName("error") val error: String?
)

data class RegisterRequest(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String,
    @SerializedName("address") val address: String?,
    @SerializedName("role") val role: String = "customer"
)

data class Product(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("price") val price: Int,
    @SerializedName("stockQuantity") val stockQuantity: Int,
    @SerializedName("category") val category: String,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("inStock") val inStock: Boolean
)

data class ProductListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("count") val count: Int,
    @SerializedName("products") val products: List<Product>?,
    @SerializedName("error") val error: String?
)

data class CartItem(
    val product: Product,
    var quantity: Int
) {
    val subtotal: Int get() = product.price * quantity
}

data class CheckoutCartItem(
    @SerializedName("productId") val productId: Int,
    @SerializedName("quantity") val quantity: Int
)

data class CheckoutRequest(
    @SerializedName("userId") val userId: Int?,
    @SerializedName("customerName") val customerName: String,
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("deliveryAddress") val deliveryAddress: String,
    @SerializedName("cartItems") val cartItems: List<CheckoutCartItem>
)

data class OrderItem(
    @SerializedName("id") val id: Int?,
    @SerializedName("productId") val productId: Int,
    @SerializedName("productName") val productName: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unitPrice") val unitPrice: Int,
    @SerializedName("subtotal") val subtotal: Int
)

data class Order(
    @SerializedName("id") val id: Int,
    @SerializedName("orderReference") val orderReference: String,
    @SerializedName("userId") val userId: Int?,
    @SerializedName("customerName") val customerName: String,
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("deliveryAddress") val deliveryAddress: String,
    @SerializedName("totalAmount") val totalAmount: Int,
    @SerializedName("orderStatus") val orderStatus: String, // 'Pending Payment', 'Paid', 'Processing', 'Shipped', 'Completed', 'Cancelled'
    @SerializedName("paymentStatus") val paymentStatus: String, // 'PENDING', 'SUCCESS', 'FAILED', 'CANCELLED'
    @SerializedName("receiptNumber") val receiptNumber: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("items") val items: List<OrderItem>?
)

data class CheckoutResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("order") val order: Order?,
    @SerializedName("error") val error: String?
)

data class OrderListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("count") val count: Int,
    @SerializedName("orders") val orders: List<Order>?,
    @SerializedName("error") val error: String?
)

data class SingleOrderResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("order") val order: Order?,
    @SerializedName("error") val error: String?
)

data class AdminDashboardStats(
    @SerializedName("totalRevenue") val totalRevenue: Int,
    @SerializedName("totalOrders") val totalOrders: Int,
    @SerializedName("pendingOrders") val pendingOrders: Int,
    @SerializedName("lowStockCount") val lowStockCount: Int,
    @SerializedName("lowStockProducts") val lowStockProducts: List<Product>?
)

data class AdminDashboardResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("stats") val stats: AdminDashboardStats?,
    @SerializedName("error") val error: String?
)
