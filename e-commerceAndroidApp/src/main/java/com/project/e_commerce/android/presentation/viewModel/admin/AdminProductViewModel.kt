package com.project.e_commerce.android.presentation.viewModel.admin

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.project.e_commerce.android.data.model.admin.AdminProduct
import com.project.e_commerce.android.data.repository.AdminRepository
import com.project.e_commerce.android.data.api.ProductUpdateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminProductViewModel(
    private val adminRepository: AdminRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminProductUiState())
    val uiState: StateFlow<AdminProductUiState> = _uiState.asStateFlow()

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
        loadProducts()
    }

    fun loadProducts(search: String? = null, featuredOnly: Boolean = false) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val token = getAdminToken()
                if (token == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Admin not logged in"
                    )
                    return@launch
                }

                // Get products from admin repository
                val response = adminRepository.getProducts(
                    page = 1,
                    limit = 100
                )

                // Filter by search and featured
                var products = response.products
                
                if (!search.isNullOrBlank()) {
                    products = products.filter { product ->
                        product.name.contains(search, ignoreCase = true)
                    }
                }

                if (featuredOnly) {
                    products = products.filter { it.isFeatured }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    products = products,
                    error = null
                )

            } catch (e: Exception) {
                Log.e("AdminProductVM", "Error loading products", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load products"
                )
            }
        }
    }

    fun updateProduct(
        productId: String, 
        price: Double, 
        commissionRate: Double,
        name: String,
        description: String?
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val token = getAdminToken() ?: throw Exception("Not logged in as admin")

                // Call backend API to update product
                val request = com.project.e_commerce.android.data.api.ProductUpdateRequest(
                    selling_price = price,
                    commission_rate = commissionRate,
                    name = name,
                    description = description
                )

                val response = adminRepository.updateProduct(
                    token = token,
                    productId = productId,
                    request = request
                )

                // Update local state with response
                val updatedProducts = _uiState.value.products.map { product ->
                    if (product.id == productId) {
                        product.copy(
                            sellingPrice = response.sellingPrice,
                            commissionRate = response.commissionRate,
                            name = response.name
                        )
                    } else {
                        product
                    }
                }

                _uiState.value = _uiState.value.copy(
                    products = updatedProducts,
                    isLoading = false,
                    error = null
                )

                Log.d("AdminProductVM", "Product updated successfully: $productId")
            } catch (e: Exception) {
                Log.e("AdminProductVM", "Error updating product", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update product"
                )
            }
        }
    }

    fun toggleFeatured(productId: String) {
        viewModelScope.launch {
            try {
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")

                // TODO: Call admin feature endpoint when available
                // For now, update local state
                val updatedProducts = _uiState.value.products.map { product ->
                    if (product.id == productId) {
                        product.copy(isFeatured = !product.isFeatured)
                    } else {
                        product
                    }
                }

                _uiState.value = _uiState.value.copy(products = updatedProducts)

                Log.d("AdminProductVM", "Product featured toggled: $productId")
            } catch (e: Exception) {
                Log.e("AdminProductVM", "Error toggling featured", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to toggle featured status"
                )
            }
        }
    }

    fun deleteProduct(productId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val token = getAdminToken() ?: throw Exception("Not logged in as admin")

                // Call backend API to delete product
                adminRepository.deleteProduct(
                    token = token,
                    productId = productId
                )

                // Remove product from local state
                val updatedProducts = _uiState.value.products.filter { 
                    it.id != productId 
                }

                _uiState.value = _uiState.value.copy(
                    products = updatedProducts,
                    isLoading = false,
                    error = null
                )

                Log.d("AdminProductVM", "Product deleted successfully: $productId")
                onSuccess()
            } catch (e: Exception) {
                Log.e("AdminProductVM", "Error deleting product", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete product"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class AdminProductUiState(
    val isLoading: Boolean = false,
    val products: List<com.project.e_commerce.android.data.model.admin.AdminProduct> = emptyList(),
    val error: String? = null
)
