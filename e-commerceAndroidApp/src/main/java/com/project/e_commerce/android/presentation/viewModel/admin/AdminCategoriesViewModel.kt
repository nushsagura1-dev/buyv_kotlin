package com.project.e_commerce.android.presentation.viewModel.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.project.e_commerce.android.data.api.AdminCategoryResponse
import com.project.e_commerce.android.data.api.CategoryCreateRequest
import com.project.e_commerce.android.data.api.CategoryUpdateRequest
import com.project.e_commerce.android.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sprint 21: Admin Categories Management ViewModel
 * Full CRUD operations on product categories
 */
class AdminCategoriesViewModel(
    private val repository: AdminRepository,
    private val context: Context
) : ViewModel() {

    private val _categories = MutableStateFlow<List<AdminCategoryResponse>>(emptyList())
    val categories: StateFlow<List<AdminCategoryResponse>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val adminToken: String by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                context,
                "admin_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.getString("admin_token", "") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = repository.getCategories()
                _categories.value = result
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading categories"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createCategory(
        name: String,
        nameAr: String?,
        slug: String,
        iconUrl: String?,
        parentId: String?,
        displayOrder: Int,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.createCategory(
                    adminToken,
                    CategoryCreateRequest(
                        name = name,
                        name_ar = nameAr,
                        slug = slug,
                        icon_url = iconUrl,
                        parent_id = parentId,
                        display_order = displayOrder,
                        is_active = isActive
                    )
                )
                _successMessage.value = "Category '$name' created"
                loadCategories()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error creating category"
                _isLoading.value = false
            }
        }
    }

    fun updateCategory(
        categoryId: String,
        name: String?,
        nameAr: String?,
        iconUrl: String?,
        displayOrder: Int?,
        isActive: Boolean?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.updateCategory(
                    adminToken,
                    categoryId,
                    CategoryUpdateRequest(
                        name = name,
                        name_ar = nameAr,
                        icon_url = iconUrl,
                        display_order = displayOrder,
                        is_active = isActive
                    )
                )
                _successMessage.value = "Category updated"
                loadCategories()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error updating category"
                _isLoading.value = false
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.deleteCategory(adminToken, categoryId)
                _successMessage.value = "Category deleted"
                loadCategories()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error deleting category"
                _isLoading.value = false
            }
        }
    }

    fun toggleCategoryActive(category: AdminCategoryResponse) {
        updateCategory(
            categoryId = category.id,
            name = null,
            nameAr = null,
            iconUrl = null,
            displayOrder = null,
            isActive = !category.is_active
        )
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}
