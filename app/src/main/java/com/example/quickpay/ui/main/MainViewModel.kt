package com.example.quickpay.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickpay.data.model.PaymentResponse
import com.example.quickpay.data.repository.PaymentRepository
import com.example.quickpay.utils.Constants
import com.example.quickpay.utils.PhoneUtils
import kotlinx.coroutines.launch

sealed class PaymentUiState {
    object Idle : PaymentUiState()
    object Loading : PaymentUiState()
    data class Success(val response: PaymentResponse) : PaymentUiState()
    data class Error(val message: String) : PaymentUiState()
}

class MainViewModel(
    private val repository: PaymentRepository = PaymentRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<PaymentUiState>(PaymentUiState.Idle)
    val uiState: LiveData<PaymentUiState> = _uiState

    private val _baseUrl = MutableLiveData<String>(Constants.DEFAULT_BASE_URL)
    val baseUrl: LiveData<String> = _baseUrl

    fun setBaseUrl(url: String) {
        val formatted = if (url.endsWith("/")) url else "$url/"
        _baseUrl.value = formatted
    }

    fun initiatePayment(phoneInput: String, amountInput: String) {
        val normalizedPhone = PhoneUtils.normalizePhoneNumber(phoneInput)
        if (normalizedPhone == null) {
            _uiState.value = PaymentUiState.Error("Invalid Kenyan phone number. Use format e.g. 0712345678")
            return
        }

        val amount = amountInput.toIntOrNull()
        if (amount == null || amount <= 0 || amount > 300000) {
            _uiState.value = PaymentUiState.Error("Amount must be between 1 and 300,000 KSh")
            return
        }

        _uiState.value = PaymentUiState.Loading

        viewModelScope.launch {
            val url = _baseUrl.value ?: Constants.DEFAULT_BASE_URL
            val result = repository.initiatePayment(normalizedPhone, amount, url)
            result.fold(
                onSuccess = { response ->
                    _uiState.value = PaymentUiState.Success(response)
                },
                onFailure = { error ->
                    _uiState.value = PaymentUiState.Error(error.message ?: "Failed to initiate payment")
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = PaymentUiState.Idle
    }
}
