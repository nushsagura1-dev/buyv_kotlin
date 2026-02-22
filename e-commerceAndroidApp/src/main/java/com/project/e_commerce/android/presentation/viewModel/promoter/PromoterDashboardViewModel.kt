package com.project.e_commerce.android.presentation.viewModel.promoter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.android.data.api.PromoterAnalyticsResponse
import com.project.e_commerce.domain.usecase.tracking.GetPromoterAnalyticsUseCase
import com.project.e_commerce.data.local.CurrentUserProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Phase 7: ViewModel pour le Promoter Dashboard
 * Gère l'état des analytics, earnings, et performances
 * MIGRATION: Firebase Auth → CurrentUserProvider (Backend)
 */
class PromoterDashboardViewModel(
    private val getPromoterAnalyticsUseCase: GetPromoterAnalyticsUseCase,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PromoterDashboardUiState())
    val uiState: StateFlow<PromoterDashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadAnalytics()
    }
    
    fun loadAnalytics(days: Int = 30) {
        viewModelScope.launch {
            val userId = currentUserProvider.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "User not logged in"
                )}
                return@launch
            }
            
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            getPromoterAnalyticsUseCase(userId, days)
                .onSuccess { analytics ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        analytics = analytics,
                        selectedPeriod = days,
                        error = null
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load analytics"
                    )}
                }
        }
    }
    
    fun refreshAnalytics() {
        loadAnalytics(_uiState.value.selectedPeriod)
    }
    
    fun changePeriod(days: Int) {
        loadAnalytics(days)
    }
}

/**
 * UI State pour le Promoter Dashboard
 */
data class PromoterDashboardUiState(
    val isLoading: Boolean = true,
    val analytics: PromoterAnalyticsResponse? = null,
    val selectedPeriod: Int = 30,
    val error: String? = null
) {
    val hasData: Boolean
        get() = analytics != null && !isLoading
}
