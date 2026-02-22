package com.project.e_commerce.android.presentation.viewModel.promoter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.data.repository.MarketplaceRepository
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.marketplace.ProductPromotion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for My Promotions Screen
 * Displays the promoter's product promotions (linked posts/reels to products)
 */
class MyPromotionsViewModel(
    private val marketplaceRepository: MarketplaceRepository,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyPromotionsUiState>(MyPromotionsUiState.Loading)
    val uiState: StateFlow<MyPromotionsUiState> = _uiState.asStateFlow()

    init {
        loadPromotions()
    }

    fun loadPromotions() {
        viewModelScope.launch {
            val userId = currentUserProvider.getCurrentUserId()
            if (userId == null) {
                _uiState.value = MyPromotionsUiState.Error("User not logged in")
                return@launch
            }

            marketplaceRepository.getMyPromotions(userId).collect { result ->
                _uiState.value = when (result) {
                    is Result.Loading -> MyPromotionsUiState.Loading
                    is Result.Success -> {
                        val promotions = result.data
                        if (promotions.isEmpty()) {
                            MyPromotionsUiState.Empty
                        } else {
                            val totalViews = promotions.sumOf { it.viewsCount }
                            val totalClicks = promotions.sumOf { it.clicksCount }
                            val totalSales = promotions.sumOf { it.salesCount }
                            val totalRevenue = promotions.sumOf { it.totalRevenue }
                            val totalCommission = promotions.sumOf { it.totalCommissionEarned }
                            
                            MyPromotionsUiState.Success(
                                promotions = promotions,
                                totalViews = totalViews,
                                totalClicks = totalClicks,
                                totalSales = totalSales,
                                totalRevenue = totalRevenue,
                                totalCommission = totalCommission
                            )
                        }
                    }
                    is Result.Error -> MyPromotionsUiState.Error(
                        result.error.message ?: "Error loading promotions"
                    )
                }
            }
        }
    }

    fun refresh() {
        loadPromotions()
    }
}

/**
 * UI State for My Promotions Screen
 */
sealed class MyPromotionsUiState {
    object Loading : MyPromotionsUiState()
    object Empty : MyPromotionsUiState()
    data class Success(
        val promotions: List<ProductPromotion>,
        val totalViews: Int,
        val totalClicks: Int,
        val totalSales: Int,
        val totalRevenue: Double,
        val totalCommission: Double
    ) : MyPromotionsUiState()
    data class Error(val message: String) : MyPromotionsUiState()
}
