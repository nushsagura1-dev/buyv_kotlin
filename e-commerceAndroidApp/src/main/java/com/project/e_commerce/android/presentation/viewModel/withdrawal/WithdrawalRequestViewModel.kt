package com.project.e_commerce.android.presentation.viewModel.withdrawal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.android.data.api.WithdrawalRequestResponse
import com.project.e_commerce.android.data.api.WithdrawalStatsResponse
import com.project.e_commerce.android.data.repository.WithdrawalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Phase 8: Withdrawal Request ViewModel
 * Manages withdrawal request form and submission
 * MIGRATION: Firebase Auth → CurrentUserProvider (Backend)
 */
class WithdrawalRequestViewModel(
    private val withdrawalRepository: WithdrawalRepository,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WithdrawalRequestUiState())
    val uiState: StateFlow<WithdrawalRequestUiState> = _uiState.asStateFlow()
    
    init {
        loadWithdrawalStats()
    }
    
    /**
     * Load withdrawal statistics (available balance, etc.)
     */
    fun loadWithdrawalStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingStats = true, error = null)
            
            withdrawalRepository.getWithdrawalStats()
                .onSuccess { stats ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingStats = false,
                        stats = stats,
                        availableBalance = stats.available_balance
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingStats = false,
                        error = error.message ?: "Failed to load stats"
                    )
                }
        }
    }
    
    /**
     * Update withdrawal amount
     */
    fun updateAmount(amount: String) {
        val amountDouble = amount.toDoubleOrNull() ?: 0.0
        _uiState.value = _uiState.value.copy(
            amount = amount,
            amountError = null
        )
        validateAmount(amountDouble)
    }
    
    /**
     * Update payment method
     */
    fun updatePaymentMethod(method: String) {
        _uiState.value = _uiState.value.copy(
            paymentMethod = method,
            paymentMethodError = null,
            // Clear payment details when method changes
            paymentDetails = emptyMap()
        )
    }
    
    /**
     * Update payment details
     */
    fun updatePaymentDetails(key: String, value: String) {
        val currentDetails = _uiState.value.paymentDetails.toMutableMap()
        currentDetails[key] = value
        _uiState.value = _uiState.value.copy(
            paymentDetails = currentDetails,
            paymentDetailsError = null
        )
    }
    
    /**
     * Validate amount
     */
    private fun validateAmount(amount: Double) {
        val error = when {
            amount <= 0 -> "Please enter an amount"
            amount < 50 -> "Minimum withdrawal is \$50"
            amount > _uiState.value.availableBalance -> "Insufficient balance"
            amount > 10000 -> "Maximum withdrawal is \$10,000 per request"
            else -> null
        }
        _uiState.value = _uiState.value.copy(amountError = error)
    }
    
    /**
     * Validate payment details based on method
     */
    private fun validatePaymentDetails(): Boolean {
        val method = _uiState.value.paymentMethod
        val details = _uiState.value.paymentDetails
        
        when (method) {
            "paypal" -> {
                val email = details["email"] ?: ""
                if (email.isEmpty() || !email.contains("@")) {
                    _uiState.value = _uiState.value.copy(
                        paymentDetailsError = "Valid PayPal email is required"
                    )
                    return false
                }
            }
            "bank_transfer" -> {
                val requiredFields = listOf(
                    "account_holder_name",
                    "bank_name",
                    "account_number",
                    "routing_number"
                )
                val missingFields = requiredFields.filter {
                    details[it].isNullOrBlank()
                }
                if (missingFields.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        paymentDetailsError = "Please fill all bank details"
                    )
                    return false
                }
            }
            else -> {
                _uiState.value = _uiState.value.copy(
                    paymentMethodError = "Please select a payment method"
                )
                return false
            }
        }
        
        return true
    }
    
    /**
     * Submit withdrawal request
     */
    fun submitWithdrawalRequest() {
        viewModelScope.launch {
            // Validate all fields
            val amount = _uiState.value.amount.toDoubleOrNull() ?: 0.0
            validateAmount(amount)
            
            if (_uiState.value.amountError != null) {
                return@launch
            }
            
            if (!validatePaymentDetails()) {
                return@launch
            }
            
            // Submit request
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            
            withdrawalRepository.createWithdrawalRequest(
                amount = amount,
                paymentMethod = _uiState.value.paymentMethod,
                paymentDetails = _uiState.value.paymentDetails
            )
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submittedRequest = response,
                        isSuccess = true
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = error.message ?: "Failed to submit request",
                        isSuccess = false
                    )
                }
        }
    }
    
    /**
     * Reset form
     */
    fun resetForm() {
        _uiState.value = WithdrawalRequestUiState(
            availableBalance = _uiState.value.availableBalance,
            stats = _uiState.value.stats
        )
    }
}

data class WithdrawalRequestUiState(
    // Stats
    val isLoadingStats: Boolean = true,
    val stats: WithdrawalStatsResponse? = null,
    val availableBalance: Double = 0.0,
    
    // Form fields
    val amount: String = "",
    val paymentMethod: String = "",  // "paypal" or "bank_transfer"
    val paymentDetails: Map<String, String> = emptyMap(),
    
    // Validation errors
    val amountError: String? = null,
    val paymentMethodError: String? = null,
    val paymentDetailsError: String? = null,
    
    // Submission state
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val submittedRequest: WithdrawalRequestResponse? = null
)
