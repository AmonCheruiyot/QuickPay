package com.example.quickpay.data.model

import com.google.gson.annotations.SerializedName

data class PaymentRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("amount") val amount: Int,
    @SerializedName("orderReference") val orderReference: String? = null
)

data class PaymentResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("reference") val reference: String?,
    @SerializedName("orderReference") val orderReference: String?,
    @SerializedName("checkoutRequestId") val checkoutRequestId: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("maskedPhone") val maskedPhone: String?,
    @SerializedName("amount") val amount: Int?,
    @SerializedName("error") val error: String?
)

data class Transaction(
    @SerializedName("id") val id: Int,
    @SerializedName("reference") val reference: String,
    @SerializedName("orderReference") val orderReference: String?,
    @SerializedName("phone") val phone: String,
    @SerializedName("maskedPhone") val maskedPhone: String?,
    @SerializedName("amount") val amount: Int,
    @SerializedName("status") val status: String,
    @SerializedName("merchantRequestId") val merchantRequestId: String?,
    @SerializedName("checkoutRequestId") val checkoutRequestId: String?,
    @SerializedName("receiptNumber") val receiptNumber: String?,
    @SerializedName("resultCode") val resultCode: Int?,
    @SerializedName("resultDescription") val resultDescription: String?,
    @SerializedName("transactionDate") val transactionDate: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class SingleTransactionResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("payment") val payment: Transaction?,
    @SerializedName("error") val error: String?,
    @SerializedName("message") val message: String?
)

data class TransactionHistoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("count") val count: Int,
    @SerializedName("payments") val payments: List<Transaction>?,
    @SerializedName("error") val error: String?,
    @SerializedName("message") val message: String?
)
