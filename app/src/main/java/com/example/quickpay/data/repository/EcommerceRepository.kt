package com.example.quickpay.data.repository

import com.example.quickpay.data.api.RetrofitClient
import com.example.quickpay.data.model.AdminDashboardStats
import com.example.quickpay.data.model.CheckoutCartItem
import com.example.quickpay.data.model.CheckoutRequest
import com.example.quickpay.data.model.LoginRequest
import com.example.quickpay.data.model.Order
import com.example.quickpay.data.model.PaymentRequest
import com.example.quickpay.data.model.PaymentResponse
import com.example.quickpay.data.model.Product
import com.example.quickpay.data.model.RegisterRequest
import com.example.quickpay.data.model.User
import com.example.quickpay.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class EcommerceRepository {

    suspend fun login(email: String, password: String, baseUrl: String = Constants.DEFAULT_BASE_URL): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(baseUrl)
                val response = api.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body()?.user != null) {
                    Result.success(response.body()!!.user!!)
                } else {
                    val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network Error: ${e.localizedMessage ?: "Unable to connect"}"))
            }
        }

    suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        address: String?,
        baseUrl: String = Constants.DEFAULT_BASE_URL
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.getApiService(baseUrl)
            val req = RegisterRequest(fullName, email, phone, password, address)
            val response = api.register(req)
            if (response.isSuccessful && response.body()?.user != null) {
                Result.success(response.body()!!.user!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network Error: ${e.localizedMessage ?: "Unable to connect"}"))
        }
    }

    suspend fun getProducts(
        category: String? = null,
        query: String? = null,
        baseUrl: String = Constants.DEFAULT_BASE_URL
    ): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.getApiService(baseUrl)
            val response = api.getProducts(category, query)
            if (response.isSuccessful && response.body()?.products != null) {
                Result.success(response.body()!!.products!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network Error: ${e.localizedMessage ?: "Unable to fetch products"}"))
        }
    }

    suspend fun createCheckout(
        userId: Int?,
        customerName: String,
        phoneNumber: String,
        deliveryAddress: String,
        cartItems: List<CheckoutCartItem>,
        baseUrl: String = Constants.DEFAULT_BASE_URL
    ): Result<Order> = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.getApiService(baseUrl)
            val req = CheckoutRequest(userId, customerName, phoneNumber, deliveryAddress, cartItems)
            val response = api.createCheckout(req)
            if (response.isSuccessful && response.body()?.order != null) {
                Result.success(response.body()!!.order!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network Error: ${e.localizedMessage ?: "Checkout failed"}"))
        }
    }

    suspend fun initiateOrderPayment(
        phone: String,
        amount: Int,
        orderReference: String,
        baseUrl: String = Constants.DEFAULT_BASE_URL
    ): Result<PaymentResponse> = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.getApiService(baseUrl)
            val req = PaymentRequest(phone = phone, amount = amount, orderReference = orderReference)
            val response = api.initiateStkPush(req)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network Error: ${e.localizedMessage ?: "STK Push failed"}"))
        }
    }

    suspend fun getOrderHistory(
        userId: Int? = null,
        baseUrl: String = Constants.DEFAULT_BASE_URL
    ): Result<List<Order>> = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.getApiService(baseUrl)
            val response = api.getOrderHistory(userId)
            if (response.isSuccessful && response.body()?.orders != null) {
                Result.success(response.body()!!.orders!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network Error: ${e.localizedMessage ?: "Unable to fetch order history"}"))
        }
    }

    suspend fun getOrderDetails(
        orderReference: String,
        baseUrl: String = Constants.DEFAULT_BASE_URL
    ): Result<Order> = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.getApiService(baseUrl)
            val response = api.getOrderDetails(orderReference)
            if (response.isSuccessful && response.body()?.order != null) {
                Result.success(response.body()!!.order!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network Error: ${e.localizedMessage ?: "Unable to fetch order details"}"))
        }
    }

    suspend fun getAdminDashboard(baseUrl: String = Constants.DEFAULT_BASE_URL): Result<AdminDashboardStats> =
        withContext(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(baseUrl)
                val response = api.getAdminDashboard()
                if (response.isSuccessful && response.body()?.stats != null) {
                    Result.success(response.body()!!.stats!!)
                } else {
                    val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Network Error: ${e.localizedMessage ?: "Unable to fetch admin dashboard"}"))
            }
        }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody == null || errorBody.isBlank()) return "Request failed"
        return try {
            val json = JSONObject(errorBody)
            json.optString("message")
                .ifEmpty { json.optString("error", "An error occurred") }
        } catch (_: Exception) {
            "An error occurred"
        }
    }
}
