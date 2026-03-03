package com.project.e_commerce.data.remote.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

/**
 * Stripe Payments API Service (Ktor)
 * Migré depuis PaymentsApi (Retrofit) — 2.14 Unification Réseau.
 */
class PaymentsApiService(private val httpClient: HttpClient) {

    /**
     * Create a Stripe Payment Intent
     * POST /payments/create-payment-intent
     */
    suspend fun createPaymentIntent(amount: Int, currency: String = "usd"): PaymentIntentResponseDto {
        return httpClient.post("payments/create-payment-intent") {
            contentType(ContentType.Application.Json)
            setBody(PaymentIntentRequestDto(amount = amount, currency = currency))
        }.body()
    }
}

@Serializable
data class PaymentIntentRequestDto(
    val amount: Int,
    val currency: String = "usd"
)

@Serializable
data class PaymentIntentResponseDto(
    val clientSecret: String = "",
    val ephemeralKey: String = "",
    val customer: String = "",
    val publishableKey: String? = null,
    val paymentIntentId: String? = null
)
