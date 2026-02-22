package com.project.e_commerce.android.presentation.viewModel.admin

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.project.e_commerce.android.data.repository.AdminRepository
import com.project.e_commerce.android.data.model.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pour la gestion des commandes admin
 */
class AdminOrderViewModel(
    private val repository: AdminRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminOrderUiState())
    val uiState: StateFlow<AdminOrderUiState> = _uiState.asStateFlow()

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "admin_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun getAdminToken(): String? {
        return encryptedPrefs.getString("admin_token", null)
    }

    /**
     * Charge toutes les commandes ou filtrées par statut
     */
    fun loadOrders(status: String? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")
                
                // Appel API - GET /api/orders/admin/all ou avec filtre status
                val orders = if (status != null) {
                    repository.getOrdersByStatus(token, status)
                } else {
                    repository.getAllOrders(token)
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    orders = orders,
                    selectedStatus = status,
                    error = null
                )
                
                Log.d("AdminOrderVM", "✅ Loaded ${orders.size} orders")
            } catch (e: Exception) {
                Log.e("AdminOrderVM", "❌ Error loading orders", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load orders"
                )
            }
        }
    }

    /**
     * Change le statut d'une commande
     */
    fun updateOrderStatus(orderId: Int, newStatus: String) {
        viewModelScope.launch {
            try {
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")
                
                // API PATCH /api/orders/{orderId}/status
                repository.updateOrderStatus(token, orderId, newStatus)
                
                // Reload orders after update
                loadOrders(_uiState.value.selectedStatus)
                
                Log.d("AdminOrderVM", "✅ Order #$orderId status updated to $newStatus")
            } catch (e: Exception) {
                Log.e("AdminOrderVM", "❌ Error updating order status", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update order status"
                )
            }
        }
    }

    /**
     * Sélectionne une commande pour voir les détails
     */
    fun selectOrder(order: Order?) {
        _uiState.value = _uiState.value.copy(selectedOrder = order)
    }

    /**
     * Filtre les commandes par statut
     */
    fun filterByStatus(status: String?) {
        loadOrders(status)
    }

    /**
     * Obtient les statistiques des commandes
     */
    fun getOrderStats(): Map<String, Int> {
        val orders = _uiState.value.orders
        return mapOf(
            "all" to orders.size,
            "pending" to orders.count { it.status == "pending" },
            "processing" to orders.count { it.status == "processing" },
            "shipped" to orders.count { it.status == "shipped" },
            "delivered" to orders.count { it.status == "delivered" },
            "cancelled" to orders.count { it.status == "cancelled" }
        )
    }
}

/**
 * UI State pour AdminOrderScreen
 */
data class AdminOrderUiState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val selectedOrder: Order? = null,
    val selectedStatus: String? = null,
    val error: String? = null
)
