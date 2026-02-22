package com.project.e_commerce.android.presentation.viewModel.promoter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.data.remote.api.MarketplaceApiService
import com.project.e_commerce.domain.model.marketplace.AffiliateSale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Affiliate Sales Screen
 * Displays the promoter's affiliate sales history
 */
class AffiliateSalesViewModel(
    private val marketplaceApiService: MarketplaceApiService,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<AffiliateSalesUiState>(AffiliateSalesUiState.Loading)
    val uiState: StateFlow<AffiliateSalesUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow<String?>(null)
    val selectedFilter: StateFlow<String?> = _selectedFilter.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadSales()
    }

    fun loadSales(filter: String? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = AffiliateSalesUiState.Loading
                _selectedFilter.value = filter

                val userId = currentUserProvider.getCurrentUserId()
                if (userId == null) {
                    _uiState.value = AffiliateSalesUiState.Error("User not logged in")
                    return@launch
                }

                val sales = marketplaceApiService.getMySales(status = filter)
                
                if (sales.isEmpty()) {
                    _uiState.value = AffiliateSalesUiState.Empty
                } else {
                    val totalSales = sales.sumOf { it.saleAmount }
                    val totalCommissions = sales.sumOf { it.commissionAmount }
                    val pendingCommissions = sales
                        .filter { it.commissionStatus == "pending" }
                        .sumOf { it.commissionAmount }

                    _uiState.value = AffiliateSalesUiState.Success(
                        sales = sales,
                        totalSales = totalSales,
                        totalCommissions = totalCommissions,
                        pendingCommissions = pendingCommissions
                    )
                }

            } catch (e: Exception) {
                _uiState.value = AffiliateSalesUiState.Error(
                    e.message ?: "Error loading sales"
                )
            }
        }
    }

    fun onFilterSelected(filter: String?) {
        _selectedFilter.value = filter
        loadSales(filter)
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val userId = currentUserProvider.getCurrentUserId()
                if (userId != null) {
                    val filter = _selectedFilter.value
                    val sales = marketplaceApiService.getMySales(status = filter)
                    
                    if (sales.isEmpty()) {
                        _uiState.value = AffiliateSalesUiState.Empty
                    } else {
                        val totalSales = sales.sumOf { it.saleAmount }
                        val totalCommissions = sales.sumOf { it.commissionAmount }
                        val pendingCommissions = sales
                            .filter { it.commissionStatus == "pending" }
                            .sumOf { it.commissionAmount }

                        _uiState.value = AffiliateSalesUiState.Success(
                            sales = sales,
                            totalSales = totalSales,
                            totalCommissions = totalCommissions,
                            pendingCommissions = pendingCommissions
                        )
                    }
                }
            } catch (e: Exception) {
                // Keep current state on refresh error
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

/**
 * UI State for Affiliate Sales Screen
 */
sealed class AffiliateSalesUiState {
    object Loading : AffiliateSalesUiState()
    object Empty : AffiliateSalesUiState()
    data class Success(
        val sales: List<AffiliateSale>,
        val totalSales: Double,
        val totalCommissions: Double,
        val pendingCommissions: Double
    ) : AffiliateSalesUiState()
    data class Error(val message: String) : AffiliateSalesUiState()
}
