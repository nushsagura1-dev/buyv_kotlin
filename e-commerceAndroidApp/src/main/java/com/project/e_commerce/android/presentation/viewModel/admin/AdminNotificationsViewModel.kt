package com.project.e_commerce.android.presentation.viewModel.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.project.e_commerce.android.data.api.AdminNotificationResponse
import com.project.e_commerce.android.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminNotificationsViewModel(
    private val repository: AdminRepository,
    private val context: Context
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<AdminNotificationResponse>>(emptyList())
    val notifications: StateFlow<List<AdminNotificationResponse>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context, "admin_secure_prefs", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun getAdminToken(): String? = encryptedPrefs.getString("admin_token", null)

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")
                _notifications.value = repository.getAdminNotifications(token)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load notifications"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendNotification(title: String, body: String, type: String = "admin_broadcast") {
        viewModelScope.launch {
            _isSending.value = true
            _errorMessage.value = null
            try {
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")
                val result = repository.sendNotification(token, title, body, type)
                _successMessage.value = result.message
                loadNotifications()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to send notification"
            } finally {
                _isSending.value = false
            }
        }
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}
