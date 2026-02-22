package com.project.e_commerce.android.presentation.viewModel.admin

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.project.e_commerce.android.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminAuthViewModel(
    private val repository: AdminRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminAuthUiState())
    val uiState: StateFlow<AdminAuthUiState> = _uiState.asStateFlow()

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

    init {
        checkAdminLoginStatus()
    }

    private fun checkAdminLoginStatus() {
        viewModelScope.launch {
            try {
                val token = encryptedPrefs.getString("admin_token", null)
                val role = encryptedPrefs.getString("admin_role", null)
                val username = encryptedPrefs.getString("admin_username", null)

                if (token != null && role != null) {
                    _uiState.value = _uiState.value.copy(
                        isAdminLoggedIn = true,
                        adminRole = role,
                        adminUsername = username
                    )
                }
            } catch (e: Exception) {
                Log.e("AdminAuthVM", "Error checking login status", e)
            }
        }
    }

    fun adminLogin(email: String, password: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

                val response = repository.adminLogin(email, password)

                // Save admin token and info (encrypted)
                encryptedPrefs.edit().apply {
                    putString("admin_token", response.access_token)
                    putString("admin_role", response.admin.role)
                    putString("admin_username", response.admin.username)
                    putString("admin_email", response.admin.email)
                    putInt("admin_id", response.admin.id)
                    apply()
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAdminLoggedIn = true,
                    adminRole = response.admin.role,
                    adminUsername = response.admin.username,
                    error = null
                )

                Log.d("AdminAuthVM", "Admin login successful: ${response.admin.username} (${response.admin.role})")
            } catch (e: Exception) {
                Log.e("AdminAuthVM", "Admin login failed", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Login failed. Please check your credentials."
                )
            }
        }
    }

    fun adminLogout() {
        viewModelScope.launch {
            try {
                // Clear admin token (encrypted)
                encryptedPrefs.edit().clear().apply()

                _uiState.value = AdminAuthUiState() // Reset to initial state
                Log.d("AdminAuthVM", "Admin logged out")
            } catch (e: Exception) {
                Log.e("AdminAuthVM", "Error during logout", e)
            }
        }
    }

    fun getAdminToken(): String? {
        return encryptedPrefs.getString("admin_token", null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class AdminAuthUiState(
    val isLoading: Boolean = false,
    val isAdminLoggedIn: Boolean = false,
    val adminRole: String? = null,
    val adminUsername: String? = null,
    val error: String? = null
)
