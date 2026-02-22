package com.project.e_commerce.android.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.android.domain.usecase.GetUserOrdersUseCase
import com.project.e_commerce.android.domain.usecase.CancelOrderUseCase
import com.project.e_commerce.domain.model.Order
import com.project.e_commerce.domain.model.OrderStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log

data class OrderHistoryState(
    val allOrders: List<Order> = emptyList(),
    val filteredOrders: List<Order> = emptyList(),
    val selectedTabIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val orderCounts: Map<String, Int> = emptyMap()
)

class OrderHistoryViewModel(
    private val getUserOrdersUseCase: GetUserOrdersUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    companion object {
        private const val TAG = "OrderHistoryViewModel"
        private val TAB_FILTERS = listOf(
            "All" to null,
            "Completed" to OrderStatus.DELIVERED,
            "Pending" to OrderStatus.PENDING,
            "Canceled" to OrderStatus.CANCELED
        )
    }

    private val _state = MutableStateFlow(OrderHistoryState())
    val state: StateFlow<OrderHistoryState> = _state

    init {
        loadUserOrders()
    }

    fun onTabSelected(tabIndex: Int) {
        if (tabIndex in TAB_FILTERS.indices) {
            _state.update { currentState ->
                val newFilteredOrders = filterOrdersByTab(currentState.allOrders, tabIndex)
                currentState.copy(
                    selectedTabIndex = tabIndex,
                    filteredOrders = newFilteredOrders
                )
            }
        }
    }

    fun loadUserOrders() {
        viewModelScope.launch {
            val userId = currentUserProvider.getCurrentUserId()
            if (userId == null) {
                _state.update { it.copy(error = "User not authenticated") }
                return@launch
            }

            _state.update { it.copy(isLoading = true, error = null) }

            try {
                getUserOrdersUseCase(userId).collect { orders ->
                    Log.d(TAG, "Loaded ${orders.size} orders for user")

                    val orderCounts = calculateOrderCounts(orders)
                    val currentTabIndex = _state.value.selectedTabIndex
                    val filteredOrders = filterOrdersByTab(orders, currentTabIndex)

                    _state.update { currentState ->
                        currentState.copy(
                            allOrders = orders,
                            filteredOrders = filteredOrders,
                            orderCounts = orderCounts,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user orders: ${e.message}")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load orders: ${e.message}"
                    )
                }
            }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            try {
                cancelOrderUseCase(orderId)
                    .onSuccess {
                        Log.d(TAG, "Successfully canceled order: $orderId")
                        // Orders will be automatically updated through the Flow
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to cancel order: ${error.message}")
                        _state.update {
                            it.copy(error = "Failed to cancel order: ${error.message}")
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception canceling order: ${e.message}")
                _state.update {
                    it.copy(error = "Failed to cancel order: ${e.message}")
                }
            }
        }
    }

    fun refreshOrders() {
        loadUserOrders()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun filterOrdersByTab(orders: List<Order>, tabIndex: Int): List<Order> {
        if (tabIndex !in TAB_FILTERS.indices) return orders

        val (_, statusFilter) = TAB_FILTERS[tabIndex]

        return if (statusFilter == null) {
            orders // "All" tab
        } else {
            when (statusFilter) {
                OrderStatus.PENDING -> orders.filter {
                    it.status in listOf(
                        OrderStatus.PENDING,
                        OrderStatus.CONFIRMED,
                        OrderStatus.PROCESSING
                    )
                }

                OrderStatus.DELIVERED -> orders.filter {
                    it.status == OrderStatus.DELIVERED
                }

                OrderStatus.CANCELED -> orders.filter {
                    it.status in listOf(
                        OrderStatus.CANCELED,
                        OrderStatus.RETURNED,
                        OrderStatus.REFUNDED
                    )
                }

                else -> orders.filter { it.status == statusFilter }
            }
        }
    }

    private fun calculateOrderCounts(orders: List<Order>): Map<String, Int> {
        return mapOf(
            "All" to orders.size,
            "Completed" to orders.count { it.status == OrderStatus.DELIVERED },
            "Pending" to orders.count {
                it.status in listOf(
                    OrderStatus.PENDING,
                    OrderStatus.CONFIRMED,
                    OrderStatus.PROCESSING
                )
            },
            "Canceled" to orders.count {
                it.status in listOf(
                    OrderStatus.CANCELED,
                    OrderStatus.RETURNED,
                    OrderStatus.REFUNDED
                )
            }
        )
    }

    fun getTabsWithCounts(): List<Pair<String, Int>> {
        val counts = _state.value.orderCounts
        return TAB_FILTERS.map { (tabName, _) ->
            tabName to (counts[tabName] ?: 0)
        }
    }
}