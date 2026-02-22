package com.project.e_commerce.android.data.api

import retrofit2.http.*

/**
 * Commissions API
 * Handles affiliate commission tracking and withdrawals
 */
interface CommissionsApi {
    
    /**
     * Get commissions for the current logged-in user
     * GET /commissions/user/me?skip={skip}&limit={limit}&status={status}
     * 
     * @param token User authentication token (Bearer)
     * @param skip Pagination offset
     * @param limit Number of items per page
     * @param status Filter by status (pending, approved, paid, rejected)
     * @return List of user's commissions
     */
    @GET("commissions/user/me")
    suspend fun getMyCommissions(
        @Header("Authorization") token: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20,
        @Query("status") status: String? = null
    ): List<CommissionDto>
}

/**
 * Commission DTO
 * 
 * @property id Commission unique ID
 * @property orderId Related order ID
 * @property promoterId User ID of the promoter (affiliate)
 * @property amount Commission amount
 * @property rate Commission rate percentage
 * @property status Commission status (pending, approved, paid, rejected)
 * @property createdAt Creation timestamp
 * @property updatedAt Last update timestamp
 */
data class CommissionDto(
    val id: Int = 0,
    @com.google.gson.annotations.SerializedName("orderId") val orderId: Int = 0,
    @com.google.gson.annotations.SerializedName("userId") val promoterId: String = "",
    @com.google.gson.annotations.SerializedName("commissionAmount") val amount: Double = 0.0,
    @com.google.gson.annotations.SerializedName("commissionRate") val rate: Double = 0.0,
    val status: String = "",
    @com.google.gson.annotations.SerializedName("createdAt") val createdAt: String = "",
    @com.google.gson.annotations.SerializedName("updatedAt") val updatedAt: String = ""
)
