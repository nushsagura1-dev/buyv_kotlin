package com.project.e_commerce.android.presentation.viewModel.promoter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.data.remote.api.MarketplaceApiService
import com.project.e_commerce.domain.model.marketplace.PromoterWallet
import com.project.e_commerce.domain.model.marketplace.WalletTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Wallet Screen
 * Displays the promoter's wallet balance and transaction history
 */
class WalletViewModel(
    private val marketplaceApiService: MarketplaceApiService,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<WalletUiState>(WalletUiState.Loading)
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val _transactions = MutableStateFlow<List<WalletTransaction>>(emptyList())
    val transactions: StateFlow<List<WalletTransaction>> = _transactions.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadWallet()
    }

    fun loadWallet() {
        viewModelScope.launch {
            try {
                _uiState.value = WalletUiState.Loading

                val userId = currentUserProvider.getCurrentUserId()
                if (userId == null) {
                    _uiState.value = WalletUiState.Error("User not logged in")
                    return@launch
                }

                val wallet = marketplaceApiService.getMyWallet()
                val transactions = marketplaceApiService.getWalletTransactions(limit = 50)
                
                _transactions.value = transactions
                _uiState.value = WalletUiState.Success(wallet)

            } catch (e: Exception) {
                _uiState.value = WalletUiState.Error(
                    e.message ?: "Error loading wallet"
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val userId = currentUserProvider.getCurrentUserId()
                if (userId != null) {
                    val wallet = marketplaceApiService.getMyWallet()
                    val transactions = marketplaceApiService.getWalletTransactions(limit = 50)
                    
                    _transactions.value = transactions
                    _uiState.value = WalletUiState.Success(wallet)
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
 * UI State for Wallet Screen
 */
sealed class WalletUiState {
    object Loading : WalletUiState()
    data class Success(val wallet: PromoterWallet) : WalletUiState()
    data class Error(val message: String) : WalletUiState()
}
