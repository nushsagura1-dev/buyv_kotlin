package com.project.e_commerce.android.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

/**
 * Phase 8: Withdrawal API
 * Handles withdrawal requests for promoters and admin management
 */
class WithdrawalApi(private val httpClient: HttpClient) {

    // ============ Promoter Endpoints ============

    suspend fun createWithdrawalRequest(request: CreateWithdrawalRequest): WithdrawalRequestResponse =
        httpClient.post("api/marketplace/withdrawal/request") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun getWithdrawalHistory(): WithdrawalHistoryResponse =
        httpClient.get("api/marketplace/withdrawal/history").body()

    suspend fun getWithdrawalStats(): WithdrawalStatsResponse =
        httpClient.get("api/marketplace/withdrawal/stats").body()

    // ============ Admin Endpoints ============

    suspend fun adminListWithdrawals(statusFilter: String? = null): WithdrawalHistoryResponse =
        httpClient.get("api/marketplace/withdrawal/admin/list") {
            statusFilter?.let { parameter("status_filter", it) }
        }.body()

    suspend fun adminApproveWithdrawal(
        withdrawalId: Int,
        request: ApproveWithdrawalRequest
    ): WithdrawalRequestResponse =
        httpClient.post("api/marketplace/withdrawal/admin/$withdrawalId/approve") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun adminRejectWithdrawal(
        withdrawalId: Int,
        request: RejectWithdrawalRequest
    ): WithdrawalRequestResponse =
        httpClient.post("api/marketplace/withdrawal/admin/$withdrawalId/reject") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun adminCompleteWithdrawal(
        withdrawalId: Int,
        request: CompleteWithdrawalRequest
    ): WithdrawalRequestResponse =
        httpClient.post("api/marketplace/withdrawal/admin/$withdrawalId/complete") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}

// ============ Request Models ============

@Serializable
data class CreateWithdrawalRequest(
    val amount: Double,
    val payment_method: String,  // "paypal" or "bank_transfer"
    val payment_details: Map<String, String>
)

@Serializable
data class ApproveWithdrawalRequest(
    val admin_notes: String? = null
)

@Serializable
data class RejectWithdrawalRequest(
    val admin_notes: String
)

@Serializable
data class CompleteWithdrawalRequest(
    val transaction_id: String,
    val admin_notes: String? = null
)

// ============ Response Models ============

@Serializable
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

@Serializable
data class WithdrawalHistoryResponse(
    val total: Int,
    val requests: List<WithdrawalRequestResponse>
)

@Serializable
data class WithdrawalStatsResponse(
    val available_balance: Double,
    val pending_balance: Double,
    val total_withdrawn: Double,
    val pending_requests_count: Int,
    val approved_requests_count: Int,
    val total_requests_count: Int
)
