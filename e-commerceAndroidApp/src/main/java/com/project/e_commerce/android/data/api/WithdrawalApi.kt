package com.project.e_commerce.android.data.api

import retrofit2.Response
import retrofit2.http.*

/**
 * Phase 8: Withdrawal API
 * Handles withdrawal requests for promoters and admin management
 */
interface WithdrawalApi {
    
    // ============ Promoter Endpoints ============
    
    @POST("api/marketplace/withdrawal/request")
    suspend fun createWithdrawalRequest(@Body request: CreateWithdrawalRequest): Response<WithdrawalRequestResponse>
    
    @GET("api/marketplace/withdrawal/history")
    suspend fun getWithdrawalHistory(): Response<WithdrawalHistoryResponse>
    
    @GET("api/marketplace/withdrawal/stats")
    suspend fun getWithdrawalStats(): Response<WithdrawalStatsResponse>
    
    // ============ Admin Endpoints ============
    
    @GET("api/marketplace/withdrawal/admin/list")
    suspend fun adminListWithdrawals(
        @Query("status_filter") statusFilter: String? = null
    ): Response<WithdrawalHistoryResponse>
    
    @POST("api/marketplace/withdrawal/admin/{withdrawal_id}/approve")
    suspend fun adminApproveWithdrawal(
        @Path("withdrawal_id") withdrawalId: Int,
        @Body request: ApproveWithdrawalRequest
    ): Response<WithdrawalRequestResponse>
    
    @POST("api/marketplace/withdrawal/admin/{withdrawal_id}/reject")
    suspend fun adminRejectWithdrawal(
        @Path("withdrawal_id") withdrawalId: Int,
        @Body request: RejectWithdrawalRequest
    ): Response<WithdrawalRequestResponse>
    
    @POST("api/marketplace/withdrawal/admin/{withdrawal_id}/complete")
    suspend fun adminCompleteWithdrawal(
        @Path("withdrawal_id") withdrawalId: Int,
        @Body request: CompleteWithdrawalRequest
    ): Response<WithdrawalRequestResponse>
}

// ============ Request Models ============

data class CreateWithdrawalRequest(
    val amount: Double,
    val payment_method: String,  // "paypal" or "bank_transfer"
    val payment_details: Map<String, String>
)

data class ApproveWithdrawalRequest(
    val admin_notes: String? = null
)

data class RejectWithdrawalRequest(
    val admin_notes: String
)

data class CompleteWithdrawalRequest(
    val transaction_id: String,
    val admin_notes: String? = null
)

// ============ Response Models ============

data class WithdrawalRequestResponse(
    val id: Int,
    val promoter_uid: String,
    val amount: Double,
    val payment_method: String,
    val payment_details: Map<String, String>,
    val status: String,  // "pending", "approved", "rejected", "completed"
    val admin_notes: String? = null,
    val created_at: String,
    val approved_at: String? = null,
    val completed_at: String? = null,
    val rejected_at: String? = null,
    val transaction_id: String? = null,
    val processed_by: String? = null,
    val promoter_name: String? = null
)

data class WithdrawalHistoryResponse(
    val total: Int,
    val requests: List<WithdrawalRequestResponse>
)

data class WithdrawalStatsResponse(
    val available_balance: Double,
    val pending_balance: Double,
    val total_withdrawn: Double,
    val pending_requests_count: Int,
    val approved_requests_count: Int,
    val total_requests_count: Int
)
