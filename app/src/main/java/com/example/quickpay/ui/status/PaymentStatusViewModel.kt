package com.example.quickpay.ui.status

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickpay.data.model.Transaction
import com.example.quickpay.data.repository.PaymentRepository
import com.example.quickpay.utils.Constants
import kotlinx.coroutines.launch

sealed class StatusUiState {
    object Loading : StatusUiState()
    data class Success(val transaction: Transaction) : StatusUiState()
    data class Error(val message: String) : StatusUiState()
}

class PaymentStatusViewModel(
    private val repository: PaymentRepository = PaymentRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<StatusUiState>(StatusUiState.Loading)
    val uiState: LiveData<StatusUiState> = _uiState

    fun fetchPaymentStatus(reference: String, baseUrl: String = Constants.DEFAULT_BASE_URL) {
        _uiState.value = StatusUiState.Loading
        viewModelScope.launch {
            val result = repository.getPaymentStatus(reference, baseUrl)
            result.fold(
                onSuccess = { transaction ->
                    _uiState.value = StatusUiState.Success(transaction)
                },
                onFailure = { error ->
                    _uiState.value = StatusUiState.Error(error.message ?: "Failed to fetch status")
                }
            )
        }
    }
}
