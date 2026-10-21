package com.example.quickpay.data.repository

import com.example.quickpay.data.api.RetrofitClient
import com.example.quickpay.data.model.PaymentRequest
import com.example.quickpay.data.model.PaymentResponse
import com.example.quickpay.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class PaymentRepository {

    suspend fun initiatePayment(
        phone: String,
        amount: Int,
        baseUrl: String
    ): Result<PaymentResponse> = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.getApiService(baseUrl)
            val request = PaymentRequest(phone = phone, amount = amount)
            val response = api.initiateStkPush(request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network Error: ${e.localizedMessage ?: "Unable to connect to QuickPay backend"}"))
        }
    }

    suspend fun getPaymentStatus(
        reference: String,
        baseUrl: String
    ): Result<Transaction> = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.getApiService(baseUrl)
            val response = api.getPaymentStatus(reference)

            if (response.isSuccessful && response.body()?.payment != null) {
                Result.success(response.body()!!.payment!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network Error: ${e.localizedMessage ?: "Unable to connect to QuickPay backend"}"))
        }
    }

    suspend fun getPaymentHistory(
        baseUrl: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Transaction>> = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitClient.getApiService(baseUrl)
            val response = api.getPaymentHistory(limit, offset)

            if (response.isSuccessful && response.body()?.payments != null) {
                Result.success(response.body()!!.payments!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network Error: ${e.localizedMessage ?: "Unable to connect to QuickPay backend"}"))
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody == null || errorBody.isBlank()) return "Request failed"
        return try {
            val json = JSONObject(errorBody)
            json.optString("message")
                .ifEmpty { json.optString("error", "An error occurred on the server") }
        } catch (_: Exception) {
            "An error occurred on the server"
        }
    }
}
