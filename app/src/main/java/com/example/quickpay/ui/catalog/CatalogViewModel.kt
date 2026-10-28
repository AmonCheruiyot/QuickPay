package com.example.quickpay.ui.catalog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quickpay.data.model.Product
import com.example.quickpay.data.repository.EcommerceRepository
import com.example.quickpay.utils.Constants
import kotlinx.coroutines.launch

sealed class CatalogUiState {
    object Loading : CatalogUiState()
    data class Success(val products: List<Product>) : CatalogUiState()
    object Empty : CatalogUiState()
    data class Error(val message: String) : CatalogUiState()
}

class CatalogViewModel(
    private val repository: EcommerceRepository = EcommerceRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<CatalogUiState>(CatalogUiState.Loading)
    val uiState: LiveData<CatalogUiState> = _uiState

    private var allProducts = listOf<Product>()
    private var currentCategory = "All"
    private var currentQuery = ""

    fun loadProducts(baseUrl: String = Constants.DEFAULT_BASE_URL) {
        _uiState.value = CatalogUiState.Loading
        viewModelScope.launch {
            val result = repository.getProducts(baseUrl = baseUrl)
            result.fold(
                onSuccess = { list ->
                    allProducts = list
                    applyFilters()
                },
                onFailure = { error ->
                    _uiState.value = CatalogUiState.Error(error.message ?: "Failed to load products")
                }
            )
        }
    }

    fun setCategoryFilter(category: String) {
        currentCategory = category
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        currentQuery = query
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = allProducts

        if (currentCategory != "All") {
            filtered = filtered.filter { it.category.equals(currentCategory, ignoreCase = true) }
        }

        if (currentQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(currentQuery, ignoreCase = true) ||
                        it.description.contains(currentQuery, ignoreCase = true)
            }
        }

        if (filtered.isEmpty()) {
            _uiState.value = CatalogUiState.Empty
        } else {
            _uiState.value = CatalogUiState.Success(filtered)
        }
    }
}
