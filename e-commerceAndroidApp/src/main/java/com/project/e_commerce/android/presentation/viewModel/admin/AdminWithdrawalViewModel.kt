package com.project.e_commerce.android.presentation.viewModel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.android.data.api.WithdrawalRequestResponse
import com.project.e_commerce.android.data.repository.WithdrawalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Phase 9: Admin Withdrawal Management ViewModel
 * Manages admin operations for withdrawal requests
 * MIGRATION: Firebase Auth → CurrentUserProvider (Backend)
 */
class AdminWithdrawalViewModel(
    private val withdrawalRepository: WithdrawalRepository,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AdminWithdrawalUiState())
    val uiState: StateFlow<AdminWithdrawalUiState> = _uiState.asStateFlow()
    
    init {
        loadWithdrawals()
    }
    
    /**
     * Load withdrawal requests with optional status filter
     */
    fun loadWithdrawals(statusFilter: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                selectedStatusFilter = statusFilter
            )
            
            withdrawalRepository.adminListWithdrawals(statusFilter)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        withdrawals = response.requests,
                        total = response.total
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load withdrawals"
                    )
                }
        }
    }
    
    /**
     * Refresh current list
     */
    fun refresh() {
        loadWithdrawals(_uiState.value.selectedStatusFilter)
    }
    
    /**
     * Filter by status
     */
    fun filterByStatus(status: String?) {
        loadWithdrawals(status)
    }
    
    /**
     * Select a withdrawal for detail view
     */
    fun selectWithdrawal(withdrawal: WithdrawalRequestResponse) {
        _uiState.value = _uiState.value.copy(selectedWithdrawal = withdrawal)
    }
    
    /**
     * Clear selected withdrawal
     */
    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedWithdrawal = null)
    }
    
    /**
     * Approve a withdrawal request
     */
    fun approveWithdrawal(withdrawalId: Int, adminNotes: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                processingError = null
            )
            
            withdrawalRepository.adminApproveWithdrawal(withdrawalId, adminNotes)
                .onSuccess { updatedRequest ->
                    // Update the withdrawal in the list
                    val updatedList = _uiState.value.withdrawals.map {
                        if (it.id == withdrawalId) updatedRequest else it
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        withdrawals = updatedList,
                        selectedWithdrawal = updatedRequest,
                        lastAction = "Withdrawal approved successfully"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        processingError = error.message ?: "Failed to approve withdrawal"
                    )
                }
        }
    }
    
    /**
     * Reject a withdrawal request
     */
    fun rejectWithdrawal(withdrawalId: Int, adminNotes: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                processingError = null
            )
            
            withdrawalRepository.adminRejectWithdrawal(withdrawalId, adminNotes)
                .onSuccess { updatedRequest ->
                    // Update the withdrawal in the list
                    val updatedList = _uiState.value.withdrawals.map {
                        if (it.id == withdrawalId) updatedRequest else it
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        withdrawals = updatedList,
                        selectedWithdrawal = updatedRequest,
                        lastAction = "Withdrawal rejected"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        processingError = error.message ?: "Failed to reject withdrawal"
                    )
                }
        }
    }
    
    /**
     * Complete a withdrawal request (mark as paid)
     */
    fun completeWithdrawal(withdrawalId: Int, transactionId: String, adminNotes: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                processingError = null
            )
            
            withdrawalRepository.adminCompleteWithdrawal(withdrawalId, transactionId, adminNotes)
                .onSuccess { updatedRequest ->
                    // Update the withdrawal in the list
                    val updatedList = _uiState.value.withdrawals.map {
                        if (it.id == withdrawalId) updatedRequest else it
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        withdrawals = updatedList,
                        selectedWithdrawal = updatedRequest,
                        lastAction = "Withdrawal marked as completed"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        processingError = error.message ?: "Failed to complete withdrawal"
                    )
                }
        }
    }
    
    /**
     * Clear last action message
     */
    fun clearLastAction() {
        _uiState.value = _uiState.value.copy(lastAction = null)
    }
    
    /**
     * Get count by status
     */
    fun getCountByStatus(status: String): Int {
        return _uiState.value.withdrawals.count { it.status == status }
    }
    
    /**
     * Get total pending amount
     */
    fun getTotalPendingAmount(): Double {
        return _uiState.value.withdrawals
            .filter { it.status == "pending" }
            .sumOf { it.amount }
    }
}

data class AdminWithdrawalUiState(
    // List state
    val isLoading: Boolean = true,
    val withdrawals: List<WithdrawalRequestResponse> = emptyList(),
    val total: Int = 0,
    val selectedStatusFilter: String? = null,
    val error: String? = null,
    
    // Detail state
    val selectedWithdrawal: WithdrawalRequestResponse? = null,
    
    // Action state
    val isProcessing: Boolean = false,
    val processingError: String? = null,
    val lastAction: String? = null
)
