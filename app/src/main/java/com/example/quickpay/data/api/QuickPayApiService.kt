package com.example.quickpay.data.api

import com.example.quickpay.data.model.PaymentRequest
import com.example.quickpay.data.model.PaymentResponse
import com.example.quickpay.data.model.SingleTransactionResponse
import com.example.quickpay.data.model.TransactionHistoryResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface QuickPayApiService {

    @POST("api/payments/stk-push")
    suspend fun initiateStkPush(
        @Body request: PaymentRequest
    ): Response<PaymentResponse>

    @GET("api/payments/{reference}")
    suspend fun getPaymentStatus(
        @Path("reference") reference: String
    ): Response<SingleTransactionResponse>

    @GET("api/payments")
    suspend fun getPaymentHistory(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<TransactionHistoryResponse>
}
