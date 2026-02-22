package com.project.e_commerce.android.presentation.viewModel.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.domain.model.Order
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.usecase.order.GetOrderDetailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Track Order Screen
 * Loads order details from backend
 */
class TrackOrderViewModel(
    private val getOrderDetailsUseCase: GetOrderDetailsUseCase,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<TrackOrderUiState>(TrackOrderUiState.Loading)
    val uiState: StateFlow<TrackOrderUiState> = _uiState.asStateFlow()

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.value = TrackOrderUiState.Loading

            val userId = currentUserProvider.getCurrentUserId()
            if (userId == null) {
                _uiState.value = TrackOrderUiState.Error("User not logged in")
                return@launch
            }

            when (val result = getOrderDetailsUseCase(orderId)) {
                is Result.Success -> {
                    _uiState.value = TrackOrderUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = TrackOrderUiState.Error(
                        result.error.message ?: "Error loading order"
                    )
                }
                is Result.Loading -> {
                    _uiState.value = TrackOrderUiState.Loading
                }
            }
        }
    }

    fun refresh(orderId: String) {
        loadOrder(orderId)
    }
}

/**
 * UI State for Track Order Screen
 */
sealed class TrackOrderUiState {
    object Loading : TrackOrderUiState()
    data class Success(val order: Order) : TrackOrderUiState()
    data class Error(val message: String) : TrackOrderUiState()
}
