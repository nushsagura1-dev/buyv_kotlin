package com.project.e_commerce.android.presentation.viewModel.promoter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.data.local.TokenManager
import com.project.e_commerce.data.remote.api.CommissionItemDto
import com.project.e_commerce.data.remote.api.CommissionsApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for My Commissions Screen
 * MIGRATION 2.14: Retrofit CommissionsApi → Ktor CommissionsApiService (shared)
 */
class MyCommissionsViewModel(
    private val commissionsApi: CommissionsApiService,
    private val currentUserProvider: CurrentUserProvider,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyCommissionsUiState>(MyCommissionsUiState.Loading)
    val uiState: StateFlow<MyCommissionsUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow<String?>(null)
    val selectedFilter: StateFlow<String?> = _selectedFilter.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20
    private var hasMorePages = true
    private var isLoadingMore = false

    init {
        loadCommissions()
    }

    fun loadCommissions(filter: String? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = MyCommissionsUiState.Loading
                _selectedFilter.value = filter
                currentPage = 0
                hasMorePages = true

                val userId = currentUserProvider.getCurrentUserId()
                if (userId == null) {
                    _uiState.value = MyCommissionsUiState.Error("User not logged in")
                    return@launch
                }

                // Ktor authenticatedClient adds Bearer token automatically
                val commissions = commissionsApi.getMyCommissions(
                    skip = 0,
                    limit = pageSize,
                    status = filter
                )

                hasMorePages = commissions.size >= pageSize

                if (commissions.isEmpty()) {
                    _uiState.value = MyCommissionsUiState.Empty
                } else {
                    val totalEarned = commissions
                        .filter { it.status == "approved" || it.status == "paid" }
                        .sumOf { it.amount }
                    val pendingAmount = commissions
                        .filter { it.status == "pending" }
                        .sumOf { it.amount }

                    _uiState.value = MyCommissionsUiState.Success(
                        commissions = commissions,
                        totalEarned = totalEarned,
                        pendingAmount = pendingAmount,
                        canLoadMore = hasMorePages
                    )
                }
            } catch (e: Exception) {
                _uiState.value = MyCommissionsUiState.Error(
                    e.message ?: "Error loading commissions"
                )
            }
        }
    }

    fun loadMoreCommissions() {
        if (isLoadingMore || !hasMorePages) return
        val currentState = _uiState.value as? MyCommissionsUiState.Success ?: return

        viewModelScope.launch {
            try {
                isLoadingMore = true
                currentPage++

                val moreCommissions = commissionsApi.getMyCommissions(
                    skip = currentPage * pageSize,
                    limit = pageSize,
                    status = _selectedFilter.value
                )

                hasMorePages = moreCommissions.size >= pageSize

                val allCommissions = currentState.commissions + moreCommissions
                val totalEarned = allCommissions
                    .filter { it.status == "approved" || it.status == "paid" }
                    .sumOf { it.amount }
                val pendingAmount = allCommissions
                    .filter { it.status == "pending" }
                    .sumOf { it.amount }

                _uiState.value = currentState.copy(
                    commissions = allCommissions,
                    totalEarned = totalEarned,
                    pendingAmount = pendingAmount,
                    canLoadMore = hasMorePages
                )

                isLoadingMore = false
            } catch (e: Exception) {
                isLoadingMore = false
            }
        }
    }

    fun onFilterSelected(filter: String?) {
        if (filter == _selectedFilter.value) return
        loadCommissions(filter)
    }

    fun refresh() {
        loadCommissions(_selectedFilter.value)
    }
}

/**
 * UI State for My Commissions Screen
 */
sealed class MyCommissionsUiState {
    object Loading : MyCommissionsUiState()
    object Empty : MyCommissionsUiState()
    data class Success(
        val commissions: List<CommissionItemDto>,
        val totalEarned: Double,
        val pendingAmount: Double,
        val canLoadMore: Boolean
    ) : MyCommissionsUiState()
    data class Error(val message: String) : MyCommissionsUiState()
}
