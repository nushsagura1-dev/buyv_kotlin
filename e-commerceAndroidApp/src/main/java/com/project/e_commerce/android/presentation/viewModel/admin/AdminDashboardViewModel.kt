package com.project.e_commerce.android.presentation.viewModel.admin

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.project.e_commerce.android.data.api.*
import com.project.e_commerce.android.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminDashboardViewModel(
    private val repository: AdminRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

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

    private fun getAdminRole(): String? {
        return encryptedPrefs.getString("admin_role", null)
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")
                val role = getAdminRole()
                
                // Load stats
                val stats = repository.getDashboardStats(token)
                
                // Load recent users (only for super_admin and moderator)
                val recentUsers = if (role in listOf("super_admin", "moderator")) {
                    repository.getRecentUsers(token, 10)
                } else {
                    emptyList()
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    stats = stats,
                    recentUsers = recentUsers,
                    adminRole = role,
                    error = null
                )
            } catch (e: Exception) {
                Log.e("AdminDashboardVM", "Error loading dashboard", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard"
                )
            }
        }
    }
    
    // User Management methods
    fun verifyUsers(userIds: List<String>) {
        viewModelScope.launch {
            try {
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")
                
                userIds.forEach { userId ->
                    repository.verifyUser(userId)
                }
                
                // Reload dashboard data after verification
                loadDashboardData()
            } catch (e: Exception) {
                Log.e("AdminDashboardVM", "Error verifying users", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to verify users"
                )
            }
        }
    }
    
    fun unverifyUsers(userIds: List<String>) {
        viewModelScope.launch {
            try {
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")
                
                userIds.forEach { userId ->
                    repository.unverifyUser(userId)
                }
                
                // Reload dashboard data after unverification
                loadDashboardData()
            } catch (e: Exception) {
                Log.e("AdminDashboardVM", "Error unverifying users", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to unverify users"
                )
            }
        }
    }
    
    fun deleteUsers(userIds: List<String>) {
        viewModelScope.launch {
            try {
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")
                
                userIds.forEach { userId ->
                    repository.deleteUser(token, userId)
                }
                
                // Reload dashboard data after deletion
                loadDashboardData()
            } catch (e: Exception) {
                Log.e("AdminDashboardVM", "Error deleting users", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete users"
                )
            }
        }
    }
}

data class AdminDashboardUiState(
    val isLoading: Boolean = false,
    val stats: DashboardStatsResponse? = null,
    val recentUsers: List<RecentUserResponse> = emptyList(),
    val recentOrders: List<RecentOrderResponse> = emptyList(),
    val adminRole: String? = null,
    val error: String? = null
)
