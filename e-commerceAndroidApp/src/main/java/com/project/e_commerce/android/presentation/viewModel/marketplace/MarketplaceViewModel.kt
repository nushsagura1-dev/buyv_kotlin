package com.project.e_commerce.android.presentation.viewModel.marketplace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.project.e_commerce.android.data.paging.MarketplaceProductPagingSource
import com.project.e_commerce.domain.model.Category
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.marketplace.MarketplaceProduct
import com.project.e_commerce.domain.model.marketplace.ProductCategory
import com.project.e_commerce.domain.model.marketplace.ProductSortBy
import com.project.e_commerce.domain.usecase.marketplace.GetProductsUseCase
import com.project.e_commerce.domain.usecase.product.GetCategoriesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import android.util.Log

/**
 * ViewModel pour l'écran Marketplace avec Paging3.
 * 
 * Gère l'état de la liste des produits avec pagination infinie,
 * filtres, catégories et recherche.
 * 
 * Utilise Paging3 pour:
 * - Chargement progressif des données (infinite scroll)
 * - Gestion automatique de la pagination
 * - Cache en mémoire avec cachedIn(viewModelScope)
 * - Gestion de l'état loading/error/empty intégrée
 * - Prefetch automatique des pages suivantes
 */
class MarketplaceViewModel(
    private val getProductsUseCase: GetProductsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
        private const val PREFETCH_DISTANCE = 5
        private const val TAG = "MARKETPLACE_VM"
    }

    // Filtres actuels - trigger pour recréer le Pager
    private val _currentFilters = MutableStateFlow(ProductFilters())
    var currentFilters by mutableStateOf(ProductFilters())
        private set

    // Catégories disponibles
    private val _categoriesState = MutableStateFlow<List<Category>>(emptyList())
    val categoriesState: StateFlow<List<Category>> = _categoriesState.asStateFlow()

    // Catégories disponibles (legacy - ProductCategory)
    private val _categories = MutableStateFlow<List<ProductCategory>>(emptyList())
    val categories: StateFlow<List<ProductCategory>> = _categories.asStateFlow()

    /**
     * Flow Paging3 pour les produits.
     * 
     * Recréé automatiquement quand les filtres changent grâce à flatMapLatest.
     * Mis en cache avec cachedIn() pour survivre aux rotations d'écran.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val productsFlow: Flow<PagingData<MarketplaceProduct>> = _currentFilters
        .flatMapLatest { filters ->
            Log.d(TAG, "📦 Création Pager - Filtres: $filters")
            
            val sortBy = when (filters.sortBy) {
                "price_asc" -> ProductSortBy.PRICE_LOW_TO_HIGH
                "price_desc" -> ProductSortBy.PRICE_HIGH_TO_LOW
                "newest" -> ProductSortBy.NEWEST
                "popular" -> ProductSortBy.POPULAR
                else -> ProductSortBy.COMMISSION
            }

            Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    prefetchDistance = PREFETCH_DISTANCE,
                    enablePlaceholders = false,
                    initialLoadSize = PAGE_SIZE * 2 // Charger 2 pages au départ
                ),
                pagingSourceFactory = {
                    MarketplaceProductPagingSource(
                        getProductsUseCase = getProductsUseCase,
                        categoryId = filters.categoryId,
                        minPrice = filters.minPrice,
                        maxPrice = filters.maxPrice,
                        minCommission = filters.minCommission,
                        searchQuery = filters.searchQuery.takeIf { it.isNotEmpty() },
                        sortBy = sortBy
                    )
                }
            ).flow
        }
        .cachedIn(viewModelScope)

    init {
        Log.d(TAG, "🚀 Initialisation MarketplaceViewModel avec Paging3")
        loadCategories()
    }

    /**
     * Charge les catégories depuis le backend.
     */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                when (val result = getCategoriesUseCase()) {
                    is Result.Success -> {
                        _categoriesState.value = result.data
                        Log.d(TAG, "📂 ${result.data.size} catégories chargées")
                    }
                    is Result.Error -> {
                        Log.e(TAG, "❌ Erreur chargement catégories: ${result.error.message}")
                    }
                    is Result.Loading -> { /* Ignored */ }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception chargement catégories: ${e.message}")
            }
        }
    }

    /**
     * Applique des filtres et recharge.
     * 
     * Le Pager est automatiquement recréé grâce au flatMapLatest.
     */
    fun applyFilters(filters: ProductFilters) {
        Log.d(TAG, "🔍 Application des filtres: $filters")
        currentFilters = filters
        _currentFilters.value = filters
    }

    /**
     * Recherche par texte.
     */
    fun searchProducts(query: String) {
        Log.d(TAG, "🔎 Recherche: $query")
        val newFilters = currentFilters.copy(searchQuery = query)
        currentFilters = newFilters
        _currentFilters.value = newFilters
    }

    /**
     * Filtre par catégorie.
     */
    fun filterByCategory(categoryId: String?) {
        Log.d(TAG, "📂 Filtre catégorie: $categoryId")
        val newFilters = currentFilters.copy(categoryId = categoryId)
        currentFilters = newFilters
        _currentFilters.value = newFilters
    }

    /**
     * Change le tri.
     */
    fun changeSortBy(sortBy: String) {
        Log.d(TAG, "⬆️ Tri: $sortBy")
        val newFilters = currentFilters.copy(sortBy = sortBy)
        currentFilters = newFilters
        _currentFilters.value = newFilters
    }

    /**
     * Réinitialise les filtres.
     */
    fun clearFilters() {
        Log.d(TAG, "🧹 Réinitialisation des filtres")
        val newFilters = ProductFilters()
        currentFilters = newFilters
        _currentFilters.value = newFilters
    }

    /**
     * Rafraîchit la liste.
     * 
     * Note: Le rafraîchissement se fait via PagingDataAdapter.refresh()
     * côté UI, pas ici. Cette méthode force une nouvelle émission.
     */
    fun refresh() {
        Log.d(TAG, "🔄 Rafraîchissement")
        // Force une nouvelle émission pour recréer le Pager
        _currentFilters.value = currentFilters.copy()
    }
}

/**
 * Filtres de produits pour le Marketplace.
 * 
 * @property categoryId ID de la catégorie (null = toutes)
 * @property minPrice Prix minimum
 * @property maxPrice Prix maximum
 * @property minCommission Commission minimum
 * @property searchQuery Texte de recherche
 * @property sortBy Tri (commission, price_asc, price_desc, newest)
 */
data class ProductFilters(
    val categoryId: String? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val minCommission: Double? = null,
    val searchQuery: String = "",
    val sortBy: String = "commission"
)
