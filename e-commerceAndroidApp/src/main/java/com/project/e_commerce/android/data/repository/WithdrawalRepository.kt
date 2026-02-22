package com.project.e_commerce.android.data.repository

import com.project.e_commerce.android.data.api.WithdrawalApi
import com.project.e_commerce.android.data.api.CreateWithdrawalRequest
import com.project.e_commerce.android.data.api.ApproveWithdrawalRequest
import com.project.e_commerce.android.data.api.RejectWithdrawalRequest
import com.project.e_commerce.android.data.api.CompleteWithdrawalRequest
import com.project.e_commerce.android.data.api.WithdrawalRequestResponse
import com.project.e_commerce.android.data.api.WithdrawalHistoryResponse
import com.project.e_commerce.android.data.api.WithdrawalStatsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Phase 8: Withdrawal Repository
 * Handles withdrawal requests and management
 */
class WithdrawalRepository(
    private val withdrawalApi: WithdrawalApi
) {
    
    // ============ Promoter Methods ============
    
    /**
     * Create a new withdrawal request
     * 
     * @param amount Amount to withdraw (must be >= $50 and <= available balance)
     * @param paymentMethod "paypal" or "bank_transfer"
     * @param paymentDetails Payment account details (email for PayPal, bank info for bank transfer)
     * @return Result with withdrawal request response or error
     */
    suspend fun createWithdrawalRequest(
        amount: Double,
        paymentMethod: String,
        paymentDetails: Map<String, String>
    ): Result<WithdrawalRequestResponse> = withContext(Dispatchers.IO) {
        try {
            val request = CreateWithdrawalRequest(
                amount = amount,
                payment_method = paymentMethod,
                payment_details = paymentDetails
            )
            
            val response = withdrawalApi.createWithdrawalRequest(request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = when (response.code()) {
                    400 -> "Insufficient balance or invalid request"
                    404 -> "Promoter wallet not found"
                    else -> "Failed to create withdrawal request: ${response.message()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    /**
     * Get withdrawal history for current promoter
     * 
     * @return Result with list of all withdrawal requests
     */
    suspend fun getWithdrawalHistory(): Result<WithdrawalHistoryResponse> = withContext(Dispatchers.IO) {
        try {
            val response = withdrawalApi.getWithdrawalHistory()
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get withdrawal history: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    /**
     * Get withdrawal statistics
     * 
     * @return Result with balance and request counts
     */
    suspend fun getWithdrawalStats(): Result<WithdrawalStatsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = withdrawalApi.getWithdrawalStats()
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get withdrawal stats: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    // ============ Admin Methods ============
    
    /**
     * Admin: List all withdrawal requests with optional status filter
     * 
     * @param statusFilter Optional status filter: "pending", "approved", "rejected", "completed"
     * @return Result with list of withdrawal requests
     */
    suspend fun adminListWithdrawals(
        statusFilter: String? = null
    ): Result<WithdrawalHistoryResponse> = withContext(Dispatchers.IO) {
        try {
            val response = withdrawalApi.adminListWithdrawals(statusFilter)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to list withdrawals: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    /**
     * Admin: Approve a withdrawal request
     * 
     * @param withdrawalId ID of withdrawal request
     * @param adminNotes Optional notes from admin
     * @return Result with updated withdrawal request
     */
    suspend fun adminApproveWithdrawal(
        withdrawalId: Int,
        adminNotes: String? = null
    ): Result<WithdrawalRequestResponse> = withContext(Dispatchers.IO) {
        try {
            val request = ApproveWithdrawalRequest(admin_notes = adminNotes)
            val response = withdrawalApi.adminApproveWithdrawal(withdrawalId, request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = when (response.code()) {
                    404 -> "Withdrawal request not found"
                    400 -> "Cannot approve request in current status"
                    else -> "Failed to approve withdrawal: ${response.message()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    /**
     * Admin: Reject a withdrawal request
     * 
     * @param withdrawalId ID of withdrawal request
     * @param adminNotes Required reason for rejection
     * @return Result with updated withdrawal request
     */
    suspend fun adminRejectWithdrawal(
        withdrawalId: Int,
        adminNotes: String
    ): Result<WithdrawalRequestResponse> = withContext(Dispatchers.IO) {
        try {
            val request = RejectWithdrawalRequest(admin_notes = adminNotes)
            val response = withdrawalApi.adminRejectWithdrawal(withdrawalId, request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = when (response.code()) {
                    404 -> "Withdrawal request not found"
                    400 -> "Cannot reject request in current status"
                    else -> "Failed to reject withdrawal: ${response.message()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    /**
     * Admin: Mark withdrawal as completed (payment sent)
     * 
     * @param withdrawalId ID of withdrawal request
     * @param transactionId External payment transaction ID
     * @param adminNotes Optional notes
     * @return Result with updated withdrawal request
     */
    suspend fun adminCompleteWithdrawal(
        withdrawalId: Int,
        transactionId: String,
        adminNotes: String? = null
    ): Result<WithdrawalRequestResponse> = withContext(Dispatchers.IO) {
        try {
            val request = CompleteWithdrawalRequest(
                transaction_id = transactionId,
                admin_notes = adminNotes
            )
            val response = withdrawalApi.adminCompleteWithdrawal(withdrawalId, request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = when (response.code()) {
                    404 -> "Withdrawal request not found"
                    400 -> "Cannot complete request in current status"
                    else -> "Failed to complete withdrawal: ${response.message()}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
}
