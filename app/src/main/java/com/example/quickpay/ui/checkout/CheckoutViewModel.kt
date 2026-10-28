package com.example.quickpay.ui.checkout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickpay.data.model.CheckoutCartItem
import com.example.quickpay.data.model.PaymentResponse
import com.example.quickpay.data.repository.EcommerceRepository
import com.example.quickpay.utils.CartManager
import com.example.quickpay.utils.Constants
import com.example.quickpay.utils.PhoneUtils
import kotlinx.coroutines.launch

sealed class CheckoutUiState {
    object Idle : CheckoutUiState()
    object Loading : CheckoutUiState()
    data class Success(val paymentResponse: PaymentResponse) : CheckoutUiState()
    data class Error(val message: String) : CheckoutUiState()
}

class CheckoutViewModel(
    private val repository: EcommerceRepository = EcommerceRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<CheckoutUiState>(CheckoutUiState.Idle)
    val uiState: LiveData<CheckoutUiState> = _uiState

    fun processCheckoutAndPay(
        userId: Int?,
        customerName: String,
        phoneInput: String,
        deliveryAddress: String,
        baseUrl: String = Constants.DEFAULT_BASE_URL
    ) {
        if (customerName.isBlank()) {
            _uiState.value = CheckoutUiState.Error("Please enter your full name")
            return
        }

        val normalizedPhone = PhoneUtils.normalizePhoneNumber(phoneInput)
        if (normalizedPhone == null) {
            _uiState.value = CheckoutUiState.Error("Invalid Kenyan M-Pesa phone number (e.g. 0712345678)")
            return
        }

        if (deliveryAddress.isBlank()) {
            _uiState.value = CheckoutUiState.Error("Please enter your delivery address")
            return
        }

        val cartItems = CartManager.getItems()
        if (cartItems.isEmpty()) {
            _uiState.value = CheckoutUiState.Error("Shopping cart is empty")
            return
        }

        _uiState.value = CheckoutUiState.Loading

        viewModelScope.launch {
            val checkoutCartItems = cartItems.map {
                CheckoutCartItem(productId = it.product.id, quantity = it.quantity)
            }

            // 1. Create order record on Supabase PostgreSQL
            val orderResult = repository.createCheckout(
                userId = userId,
                customerName = customerName,
                phoneNumber = normalizedPhone,
                deliveryAddress = deliveryAddress,
                cartItems = checkoutCartItems,
                baseUrl = baseUrl
            )

            orderResult.fold(
                onSuccess = { order ->
                    // 2. Initiate M-Pesa STK Push payment linked to the created order reference
                    val paymentResult = repository.initiateOrderPayment(
                        phone = normalizedPhone,
                        amount = order.totalAmount,
                        orderReference = order.orderReference,
                        baseUrl = baseUrl
                    )

                    paymentResult.fold(
                        onSuccess = { paymentRes ->
                            CartManager.clearCart() // Clear cart upon successful initiation
                            _uiState.value = CheckoutUiState.Success(paymentRes)
                        },
                        onFailure = { paymentErr ->
                            _uiState.value = CheckoutUiState.Error("Order created (${order.orderReference}) but STK Push failed: ${paymentErr.message}")
                        }
                    )
                },
                onFailure = { orderErr ->
                    _uiState.value = CheckoutUiState.Error(orderErr.message ?: "Failed to create order")
                }
            )
        }
    }
}
