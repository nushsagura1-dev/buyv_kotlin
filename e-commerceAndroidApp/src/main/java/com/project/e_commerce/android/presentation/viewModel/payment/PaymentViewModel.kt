package com.project.e_commerce.android.presentation.viewModel.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.data.local.TokenManager
import com.project.e_commerce.data.remote.api.PaymentIntentResponseDto
import com.project.e_commerce.data.remote.api.PaymentsApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Stripe Payment Integration
 * Handles payment intent creation and payment flow state management
 * MIGRATION 2.14: Retrofit PaymentsApi → Ktor PaymentsApiService (shared)
 */
class PaymentViewModel(
    private val paymentsApiService: PaymentsApiService,
    private val currentUserProvider: CurrentUserProvider,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    /**
     * Create a Stripe Payment Intent
     *
     * @param amountInCents Total amount in cents (e.g., 5000 = $50.00)
     * @param currency Currency code (default: "usd")
     */
    fun createPaymentIntent(amountInCents: Int, currency: String = "usd") {
        viewModelScope.launch {
            try {
                _paymentState.value = PaymentState.Loading

                val userId = currentUserProvider.getCurrentUserId()
                if (userId == null) {
                    _paymentState.value = PaymentState.Error("User not authenticated")
                    return@launch
                }

                // Ktor client (authenticatedClient) adds Bearer token automatically
                val response = paymentsApiService.createPaymentIntent(
                    amount = amountInCents,
                    currency = currency
                )

                _paymentState.value = PaymentState.Success(response)

            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error(
                    e.message ?: "Failed to create payment intent"
                )
            }
        }
    }

    /**
     * Mock payment success — bypasses Stripe entirely (for commission/order flow testing)
     */
    fun mockPaymentSuccess() {
        _paymentState.value = PaymentState.PaymentCompleted
    }

    fun onPaymentSuccess() {
        _paymentState.value = PaymentState.PaymentCompleted
    }

    fun onPaymentCancelled() {
        _paymentState.value = PaymentState.Cancelled
    }

    fun onPaymentFailed(error: String) {
        _paymentState.value = PaymentState.Error(error)
    }

    fun resetPaymentState() {
        _paymentState.value = PaymentState.Idle
    }
}

/**
 * Payment flow states
 */
sealed class PaymentState {
    object Idle : PaymentState()
    object Loading : PaymentState()
    data class Success(val paymentIntent: PaymentIntentResponseDto) : PaymentState()
    object PaymentCompleted : PaymentState()
    object Cancelled : PaymentState()
    data class Error(val message: String) : PaymentState()
}

