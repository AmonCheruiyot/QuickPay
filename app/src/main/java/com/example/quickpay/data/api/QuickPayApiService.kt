package com.example.quickpay.data.api

import com.example.quickpay.data.model.AdminDashboardResponse
import com.example.quickpay.data.model.CheckoutRequest
import com.example.quickpay.data.model.CheckoutResponse
import com.example.quickpay.data.model.LoginRequest
import com.example.quickpay.data.model.LoginResponse
import com.example.quickpay.data.model.OrderListResponse
import com.example.quickpay.data.model.PaymentRequest
import com.example.quickpay.data.model.PaymentResponse
import com.example.quickpay.data.model.ProductListResponse
import com.example.quickpay.data.model.RegisterRequest
import com.example.quickpay.data.model.SingleOrderResponse
import com.example.quickpay.data.model.SingleTransactionResponse
import com.example.quickpay.data.model.TransactionHistoryResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface QuickPayApiService {

    // Auth
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Product Catalogue
    @GET("products")
    suspend fun getProducts(
        @Query("category") category: String? = null,
        @Query("q") query: String? = null
    ): Response<ProductListResponse>

    // Orders & Checkout
    @POST("orders/checkout")
    suspend fun createCheckout(@Body request: CheckoutRequest): Response<CheckoutResponse>

    @GET("orders/history")
    suspend fun getOrderHistory(@Query("userId") userId: Int? = null): Response<OrderListResponse>

    @GET("orders/{reference}")
    suspend fun getOrderDetails(@Path("reference") reference: String): Response<SingleOrderResponse>

    // M-Pesa Payment
    @POST("stk-push")
    suspend fun initiateStkPush(@Body request: PaymentRequest): Response<PaymentResponse>

    @GET("status/{reference}")
    suspend fun getPaymentStatus(@Path("reference") reference: String): Response<SingleTransactionResponse>

    @GET("history")
    suspend fun getPaymentHistory(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<TransactionHistoryResponse>

    // Admin Dashboard
    @GET("admin/dashboard")
    suspend fun getAdminDashboard(): Response<AdminDashboardResponse>
}
