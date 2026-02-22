package com.project.e_commerce.android.presentation.viewModel.admin

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.project.e_commerce.android.data.model.User
import com.project.e_commerce.android.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminUserManagementViewModel(
    private val adminRepository: AdminRepository,
    private val context: Context
) : ViewModel() {
    
    companion object {
        private const val TAG = "AdminUserMgmtVM"
    }
    
    private val _recentUsers = MutableStateFlow<List<User>>(emptyList())
    val recentUsers: StateFlow<List<User>> = _recentUsers
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
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
    
    init {
        loadUsers()
    }
    
    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")
                Log.d(TAG, "Loading users with token: ${token.take(20)}...")
                val userResponses = adminRepository.getUsers(token)
                _recentUsers.value = userResponses.map { response ->
                    User(
                        id = response.id,
                        email = response.email,
                        username = response.username,
                        displayName = response.display_name,
                        profileImageUrl = null,
                        isVerified = response.is_verified,
                        isActive = true,
                        bio = null,
                        followersCount = response.followers_count,
                        followingCount = response.following_count,
                        postsCount = response.reels_count,
                        createdAt = response.created_at,
                        updatedAt = null
                    )
                }
                Log.d(TAG, "✅ Loaded ${_recentUsers.value.size} users")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load users: ${e.message}")
                _errorMessage.value = e.message ?: "Failed to load users"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadDashboardData() {
        loadUsers()
    }
    
    fun verifyUser(userId: String) {
        viewModelScope.launch {
            try {
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")
                adminRepository.verifyUsers(token, listOf(userId))
                loadUsers()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to verify user"
            }
        }
    }
    
    fun unverifyUser(userId: String) {
        viewModelScope.launch {
            try {
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")
                adminRepository.unverifyUsers(token, listOf(userId))
                loadUsers()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to unverify user"
            }
        }
    }
}
