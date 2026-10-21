package com.example.quickpay.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickpay.data.model.Transaction
import com.example.quickpay.data.repository.PaymentRepository
import com.example.quickpay.utils.Constants
import kotlinx.coroutines.launch

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    data class Success(val transactions: List<Transaction>) : HistoryUiState()
    object Empty : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}

class TransactionHistoryViewModel(
    private val repository: PaymentRepository = PaymentRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<HistoryUiState>(HistoryUiState.Loading)
    val uiState: LiveData<HistoryUiState> = _uiState

    fun loadHistory(baseUrl: String = Constants.DEFAULT_BASE_URL) {
        _uiState.value = HistoryUiState.Loading
        viewModelScope.launch {
            val result = repository.getPaymentHistory(baseUrl)
            result.fold(
                onSuccess = { list ->
                    if (list.isEmpty()) {
                        _uiState.value = HistoryUiState.Empty
                    } else {
                        _uiState.value = HistoryUiState.Success(list)
                    }
                },
                onFailure = { error ->
                    _uiState.value = HistoryUiState.Error(error.message ?: "Failed to load history")
                }
            )
        }
    }
}
