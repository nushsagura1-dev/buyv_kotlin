package com.project.e_commerce.android.presentation.viewModel.marketplace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserPost
import com.project.e_commerce.domain.model.marketplace.MarketplaceProduct
import com.project.e_commerce.domain.model.marketplace.ProductPromotion
import com.project.e_commerce.domain.usecase.marketplace.CreatePromotionUseCase
import com.project.e_commerce.domain.usecase.marketplace.GetProductByIdUseCase
import com.project.e_commerce.domain.usecase.user.GetUserPostsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

/**
 * ViewModel pour l'écran de détails d'un produit
 * Gère l'affichage du produit et la création de promotions
 */
class ProductDetailViewModel(
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val createPromotionUseCase: CreatePromotionUseCase,
    private val getUserPostsUseCase: GetUserPostsUseCase,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    // État principal
    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    // Produit actuel
    private val _product = MutableStateFlow<MarketplaceProduct?>(null)
    val product: StateFlow<MarketplaceProduct?> = _product.asStateFlow()

    // État de la promotion en cours
    private val _promotionState = MutableStateFlow<PromotionState>(PromotionState.Idle)
    val promotionState: StateFlow<PromotionState> = _promotionState.asStateFlow()
    
    // Posts de l'utilisateur pour le picker
    private val _userPosts = MutableStateFlow<List<UserPost>>(emptyList())
    val userPosts: StateFlow<List<UserPost>> = _userPosts.asStateFlow()
    
    // État de chargement des posts
    private val _postsLoading = MutableStateFlow(false)
    val postsLoading: StateFlow<Boolean> = _postsLoading.asStateFlow()

    // ID du produit courant
    var currentProductId by mutableStateOf<String?>(null)
        private set

    /**
     * Charge les détails d'un produit
     */
    fun loadProduct(productId: String) {
        if (currentProductId == productId && _product.value != null) {
            Log.d("PRODUCT_DETAIL_VM", "📦 Produit déjà chargé: $productId")
            return
        }

        viewModelScope.launch {
            Log.d("PRODUCT_DETAIL_VM", "📦 Chargement du produit: $productId")
            currentProductId = productId
            _uiState.value = ProductDetailUiState.Loading

            getProductByIdUseCase(productId).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _uiState.value = ProductDetailUiState.Loading
                        Log.d("PRODUCT_DETAIL_VM", "⏳ Chargement...")
                    }
                    
                    is Result.Success -> {
                        _product.value = result.data
                        _uiState.value = ProductDetailUiState.Success(result.data)
                        Log.d("PRODUCT_DETAIL_VM", "✅ Produit chargé: ${result.data.name}")
                    }
                    
                    is Result.Error -> {
                        _uiState.value = ProductDetailUiState.Error(
                            result.error.message
                        )
                        Log.e("PRODUCT_DETAIL_VM", "❌ Erreur: ${result.error.message}")
                    }
                }
            }
        }
    }

    /**
     * Charge les posts de l'utilisateur connecté pour le picker
     */
    fun loadUserPosts() {
        viewModelScope.launch {
            val userId = currentUserProvider.getCurrentUserId()
            if (userId == null) {
                Log.w("PRODUCT_DETAIL_VM", "⚠️ Utilisateur non connecté")
                return@launch
            }
            
            _postsLoading.value = true
            Log.d("PRODUCT_DETAIL_VM", "📝 Chargement des posts de l'utilisateur: $userId")
            
            when (val result = getUserPostsUseCase(userId)) {
                is Result.Success -> {
                    _userPosts.value = result.data
                    Log.d("PRODUCT_DETAIL_VM", "✅ ${result.data.size} posts chargés")
                }
                is Result.Error -> {
                    Log.e("PRODUCT_DETAIL_VM", "❌ Erreur chargement posts: ${result.error.message}")
                }
                is Result.Loading -> { /* Handled */ }
            }
            _postsLoading.value = false
        }
    }

    /**
     * Crée une promotion (lie un post/reel à ce produit)
     */
    fun createPromotion(postId: String, onSuccess: () -> Unit = {}) {
        val productId = currentProductId ?: run {
            _promotionState.value = PromotionState.Error("Product not loaded")
            return
        }

        viewModelScope.launch {
            Log.d("PRODUCT_DETAIL_VM", "🎯 Création promotion: post=$postId, product=$productId")
            _promotionState.value = PromotionState.Loading

            createPromotionUseCase(
                postId = postId,
                productId = productId
            ).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _promotionState.value = PromotionState.Loading
                        Log.d("PRODUCT_DETAIL_VM", "⏳ Création en cours...")
                    }
                    
                    is Result.Success -> {
                        _promotionState.value = PromotionState.Success(result.data)
                        Log.d("PRODUCT_DETAIL_VM", "✅ Promotion créée: ${result.data.id}")
                        onSuccess()
                        
                        // Reset après 2 secondes
                        kotlinx.coroutines.delay(2000)
                        _promotionState.value = PromotionState.Idle
                    }
                    
                    is Result.Error -> {
                        _promotionState.value = PromotionState.Error(
                            result.error.message
                        )
                        Log.e("PRODUCT_DETAIL_VM", "❌ Erreur: ${result.error.message}")
                    }
                }
            }
        }
    }

    /**
     * Réinitialise l'état de la promotion
     */
    fun resetPromotionState() {
        _promotionState.value = PromotionState.Idle
    }

    /**
     * Calcule la commission estimée pour un montant donné
     */
    fun calculateCommission(amount: Double): Double {
        return _product.value?.getEstimatedCommission() ?: 0.0
    }

    /**
     * Vérifie si le produit est disponible
     */
    fun isProductAvailable(): Boolean {
        return _product.value?.isAvailable() ?: false
    }

    /**
     * Rafraîchit les détails du produit
     */
    fun refresh() {
        currentProductId?.let { loadProduct(it) }
    }
}

/**
 * État UI de ProductDetail
 */
sealed class ProductDetailUiState {
    object Loading : ProductDetailUiState()
    data class Success(val product: MarketplaceProduct) : ProductDetailUiState()
    data class Error(val message: String) : ProductDetailUiState()
}

/**
 * État de la création de promotion
 */
sealed class PromotionState {
    object Idle : PromotionState()
    object Loading : PromotionState()
    data class Success(val promotion: ProductPromotion) : PromotionState()
    data class Error(val message: String) : PromotionState()
}
