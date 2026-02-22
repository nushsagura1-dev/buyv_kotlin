package com.project.e_commerce.android.presentation.viewModel.admin

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.project.e_commerce.android.data.api.CJImportResponse
import com.project.e_commerce.android.data.api.CJProduct
import com.project.e_commerce.android.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pour l'import de produits CJ Dropshipping
 * 
 * Permet aux admins de :
 * - Rechercher des produits sur CJ Dropshipping
 * - Importer des produits dans le catalogue
 * - Synchroniser les prix/stocks avec CJ
 */
class AdminCJImportViewModel(
    private val repository: AdminRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminCJImportUiState())
    val uiState: StateFlow<AdminCJImportUiState> = _uiState.asStateFlow()

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
     * Recherche des produits sur CJ Dropshipping
     */
    fun searchProducts(query: String, category: String? = null) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter a search query")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null,
                    searchQuery = query,
                    selectedCategory = category
                )
                
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")
                
                val response = repository.searchCJProducts(
                    token = token,
                    query = query,
                    category = category,
                    page = 1
                )
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    searchResults = response.products,
                    totalResults = response.total,
                    currentPage = response.page,
                    error = null
                )
                
                Log.d("AdminCJImportVM", "✅ Found ${response.total} CJ products for '$query'")
            } catch (e: Exception) {
                Log.e("AdminCJImportVM", "❌ Error searching CJ products", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to search CJ products"
                )
            }
        }
    }

    /**
     * Charge la page suivante des résultats
     */
    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.searchQuery.isBlank()) return
        
        // Check if there are more pages
        val totalPages = (currentState.totalResults + 19) / 20 // page_size = 20
        if (currentState.currentPage >= totalPages) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoadingMore = true)
                
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")
                
                val response = repository.searchCJProducts(
                    token = token,
                    query = currentState.searchQuery,
                    category = currentState.selectedCategory,
                    page = currentState.currentPage + 1
                )
                
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    searchResults = currentState.searchResults + response.products,
                    currentPage = response.page,
                    error = null
                )
                
                Log.d("AdminCJImportVM", "✅ Loaded page ${response.page}")
            } catch (e: Exception) {
                Log.e("AdminCJImportVM", "❌ Error loading next page", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = e.message ?: "Failed to load more products"
                )
            }
        }
    }

    /**
     * Importe un produit CJ dans le catalogue
     */
    fun importProduct(
        cjProductId: String,
        cjVariantId: String? = null,
        commissionRate: Double = 10.0,
        categoryId: String? = null,
        customDescription: String? = null,
        sellingPrice: Double? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isImporting = true,
                    importingProductId = cjProductId,
                    importError = null
                )
                
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")
                
                val response = repository.importCJProduct(
                    token = token,
                    cjProductId = cjProductId,
                    cjVariantId = cjVariantId,
                    commissionRate = commissionRate,
                    categoryId = categoryId,
                    customDescription = customDescription,
                    sellingPrice = sellingPrice
                )
                
                // Add to imported products list
                val importedProducts = _uiState.value.importedProducts + response
                
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importingProductId = null,
                    importedProducts = importedProducts,
                    showImportSuccess = true,
                    lastImportedProduct = response,
                    importError = null
                )
                
                Log.d("AdminCJImportVM", "✅ Imported CJ product: ${response.name}")
            } catch (e: Exception) {
                Log.e("AdminCJImportVM", "❌ Error importing CJ product", e)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importingProductId = null,
                    importError = e.message ?: "Failed to import product"
                )
            }
        }
    }

    /**
     * Synchronise un produit avec CJ (met à jour prix/stock)
     */
    fun syncProduct(productId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isSyncing = true,
                    syncingProductId = productId
                )
                
                val token = getAdminToken() ?: throw Exception("Not logged in as admin")
                
                val response = repository.syncCJProduct(token, productId)
                
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncingProductId = null,
                    syncMessage = response.message
                )
                
                Log.d("AdminCJImportVM", "✅ Synced product: ${response.message}")
            } catch (e: Exception) {
                Log.e("AdminCJImportVM", "❌ Error syncing product", e)
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncingProductId = null,
                    error = e.message ?: "Failed to sync product"
                )
            }
        }
    }

    /**
     * Sélectionne un produit pour voir les détails / configurer l'import
     */
    fun selectProduct(product: CJProduct?) {
        _uiState.value = _uiState.value.copy(selectedProduct = product)
    }

    /**
     * Ferme le dialogue de succès d'import
     */
    fun dismissImportSuccess() {
        _uiState.value = _uiState.value.copy(
            showImportSuccess = false,
            lastImportedProduct = null
        )
    }

    /**
     * Efface l'erreur d'import
     */
    fun clearImportError() {
        _uiState.value = _uiState.value.copy(importError = null)
    }

    /**
     * Efface l'erreur générale
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Réinitialise la recherche
     */
    fun clearSearch() {
        _uiState.value = AdminCJImportUiState()
    }
}

/**
 * UI State pour AdminCJImportScreen
 */
data class AdminCJImportUiState(
    // Search state
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val searchResults: List<CJProduct> = emptyList(),
    val totalResults: Int = 0,
    val currentPage: Int = 1,
    
    // Product selection
    val selectedProduct: CJProduct? = null,
    
    // Import state
    val isImporting: Boolean = false,
    val importingProductId: String? = null,
    val importedProducts: List<CJImportResponse> = emptyList(),
    val showImportSuccess: Boolean = false,
    val lastImportedProduct: CJImportResponse? = null,
    val importError: String? = null,
    
    // Sync state
    val isSyncing: Boolean = false,
    val syncingProductId: String? = null,
    val syncMessage: String? = null,
    
    // General error
    val error: String? = null
)

/**
 * Categories CJ pour filtrage
 */
object CJCategories {
    val categories = listOf(
        "All" to null,
        "Electronics" to "electronics",
        "Clothing" to "clothing",
        "Home & Garden" to "home_garden",
        "Beauty" to "beauty",
        "Sports" to "sports",
        "Toys" to "toys",
        "Jewelry" to "jewelry",
        "Automotive" to "automotive",
        "Pet Supplies" to "pet_supplies"
    )
}
