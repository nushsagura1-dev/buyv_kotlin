package com.project.e_commerce.android.data.api

import retrofit2.http.*

/**
 * Stripe Payments API
 * Handles payment intent creation for Stripe Payment Sheet integration
 */
interface PaymentsApi {
    
    /**
     * Create a Stripe Payment Intent
     * Returns client secret, ephemeral key, and customer ID for Stripe Payment Sheet
     * 
     * @param token User authentication token (Bearer)
     * @param request Payment intent request with amount and currency
     * @return Payment intent response with Stripe credentials
     */
    @POST("payments/create-payment-intent")
    suspend fun createPaymentIntent(
        @Header("Authorization") token: String,
        @Body request: PaymentIntentRequest
    ): PaymentIntentResponse
}

/**
 * Request body for creating a Payment Intent
 * 
 * @property amount Amount in cents (e.g., 5000 = $50.00)
 * @property currency Currency code (default: "usd")
 */
data class PaymentIntentRequest(
    val amount: Int,
    val currency: String = "usd"
)

/**
 * Response from Payment Intent creation
 * Contains all necessary data for Stripe Payment Sheet
 * 
 * @property clientSecret Stripe client secret for payment confirmation
 * @property ephemeralKey Ephemeral key for customer authentication
 * @property customer Stripe customer ID
 * @property publishableKey Stripe publishable key (optional, can be hardcoded in app)
 */
data class PaymentIntentResponse(
    val clientSecret: String,
    val ephemeralKey: String,
    val customer: String,
    val publishableKey: String? = null,
    val paymentIntentId: String? = null  // PaymentIntent ID for order verification
)
