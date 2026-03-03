package com.project.e_commerce.data.remote.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Commissions API Service (Ktor)
 * Migré depuis CommissionsApi (Retrofit) — 2.14 Unification Réseau.
 */
class CommissionsApiService(private val httpClient: HttpClient) {

    /**
     * Get commissions for the current logged-in user
     * GET /commissions/user/me
     */
    suspend fun getMyCommissions(
        skip: Int = 0,
        limit: Int = 20,
        status: String? = null
    ): List<CommissionItemDto> {
        return httpClient.get("commissions/user/me") {
            parameter("skip", skip)
            parameter("limit", limit)
            if (status != null) parameter("status", status)
        }.body()
    }
}

@Serializable
data class CommissionItemDto(
    val id: Int = 0,
    @SerialName("orderId") val orderId: Int = 0,
    @SerialName("userId") val promoterId: String = "",
    @SerialName("commissionAmount") val amount: Double = 0.0,
    @SerialName("commissionRate") val rate: Double = 0.0,
    val status: String = "",
    @SerialName("createdAt") val createdAt: String = "",
    @SerialName("updatedAt") val updatedAt: String = ""
)
