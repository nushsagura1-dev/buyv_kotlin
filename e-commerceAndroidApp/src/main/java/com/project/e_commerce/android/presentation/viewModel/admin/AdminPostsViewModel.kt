package com.project.e_commerce.android.presentation.viewModel.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.project.e_commerce.android.data.api.AdminPostResponse
import com.project.e_commerce.android.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminPostsViewModel(
    private val repository: AdminRepository,
    private val context: Context
) : ViewModel() {

    private val _posts = MutableStateFlow<List<AdminPostResponse>>(emptyList())
    val posts: StateFlow<List<AdminPostResponse>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

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
        loadPosts()
    }

    fun loadPosts(search: String? = null, postType: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")
                _posts.value = repository.getAdminPosts(token, search, postType)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load posts"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePost(postUid: String) {
        viewModelScope.launch {
            try {
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")
                val result = repository.deletePost(token, postUid)
                _successMessage.value = result.message
                loadPosts()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to delete post"
            }
        }
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}
