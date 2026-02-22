package com.project.e_commerce.android.presentation.viewModel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.android.R
import com.project.e_commerce.android.data.model.CartStats
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Comment
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.LoveItem
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.NewComment
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Ratings
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Reels
import com.project.e_commerce.domain.model.Category
import com.project.e_commerce.domain.model.Product
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.usecase.product.GetCategoriesUseCase
import com.project.e_commerce.domain.usecase.product.GetProductDetailsUseCase
import com.project.e_commerce.domain.usecase.product.GetProductsUseCase
import kotlinx.coroutines.launch
import android.util.Log

class ProductViewModel(
    private val getProductsUseCase: GetProductsUseCase,
    private val getProductDetailsUseCase: GetProductDetailsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    var categories by mutableStateOf<List<Category>>(emptyList())
        private set

    var featuredProducts by mutableStateOf<List<Product>>(emptyList())
        private set

    var bestSellerProducts by mutableStateOf<List<Product>>(emptyList())
        private set

    var allProducts by mutableStateOf<List<Product>>(emptyList())
        private set

    var categoryProducts by mutableStateOf<List<Product>>(emptyList())
        private set

    var selectedProduct by mutableStateOf<Product?>(null)
    var productReels by mutableStateOf<List<Reels>>(emptyList())
        private set
        
    var isLoading by mutableStateOf(false)
        private set
        
    var error by mutableStateOf<String?>(null)
        private set

    init {
        // Charge les données automatiquement au démarrage
        Log.d("PRODUCT_VM", "🚀 ProductViewModel initialized, loading initial data...")
        loadInitialData()
    }
    
    fun loadInitialData() {
        viewModelScope.launch {
            Log.d("PRODUCT_VM", "🚀 Loading initial data with Use Cases")
            isLoading = true
            
            // Load Categories
            loadCategories()
            
            // Load Products
            loadAllProducts()
            
            // Load Featured & Best Sellers
            loadFeaturedProducts()
            loadBestSellerProducts()
            
            isLoading = false
        }
    }

    private suspend fun loadCategories() {
        when (val result = getCategoriesUseCase()) {
            is Result.Success -> {
                categories = result.data
                Log.d("PRODUCT_VM", "✅ Loaded ${categories.size} categories")
            }
            is Result.Error -> {
                Log.e("PRODUCT_VM", "❌ Error loading categories: ${result.error.message}")
            }
            else -> {}
        }
    }

    private suspend fun loadAllProducts() {
        when (val result = getProductsUseCase.getAllProducts()) {
            is Result.Success -> {
                allProducts = result.data
                Log.d("PRODUCT_VM", "✅ Loaded ${allProducts.size} products")
                
                // Generate Reels from products
                if (allProducts.isNotEmpty()) {
                    productReels = generateReelsFromProducts(allProducts)
                }
            }
            is Result.Error -> {
                error = result.error.message
                Log.e("PRODUCT_VM", "❌ Error loading products: ${result.error.message}")
            }
            else -> {}
        }
    }

    private suspend fun loadFeaturedProducts() {
        when (val result = getProductsUseCase.getRecentProducts(10)) {
            is Result.Success -> featuredProducts = result.data
            is Result.Error -> Log.e("PRODUCT_VM", "❌ Error loading featured: ${result.error.message}")
            else -> {}
        }
    }

    private suspend fun loadBestSellerProducts() {
        when (val result = getProductsUseCase.getBestSellers()) {
            is Result.Success -> bestSellerProducts = result.data
            is Result.Error -> Log.e("PRODUCT_VM", "❌ Error loading best sellers: ${result.error.message}")
            else -> {}
        }
    }

    fun getProductById(productId: String) {
        viewModelScope.launch {
            isLoading = true
            selectedProduct = null  // Clear stale data
            error = null            // Clear stale error
            when (val result = getProductDetailsUseCase(productId)) {
                is Result.Success -> {
                    selectedProduct = result.data
                    Log.d("PRODUCT_VM", "✅ Loaded product: ${result.data.name}")
                }
                is Result.Error -> {
                    Log.e("PRODUCT_VM", "❌ Error loading product $productId: ${result.error.message}")
                    error = result.error.message
                }
                else -> {}
            }
            isLoading = false
        }
    }

    // Legacy method kept for UI compatibility, but delegates to UseCase
    fun getAllProductsFromFirebase() {
        viewModelScope.launch { loadAllProducts() }
    }
    
    fun getBestSellerProductsFromFirebase() {
        viewModelScope.launch { loadBestSellerProducts() }
    }
    
    fun getFeaturedProductsFromFirebase() {
        viewModelScope.launch { loadFeaturedProducts() }
    }
    
    fun getCategoriesFromFirebase() {
        viewModelScope.launch { loadCategories() }
    }

    fun getProductsByCategory(categoryId: String) { // Changed param name from 'category' (string desc) to 'categoryId' for clarity, but usecase might expect ID
        viewModelScope.launch {
            isLoading = true
            // If the UI passes the category Name, we might have an issue if UseCase expects ID.
            // Assuming UseCase expects what the old variable 'category' contained (legacy code passed doc.getString("category") which is likely ID).
            
            when (val result = getProductsUseCase.getProductsByCategory(categoryId)) {
                is Result.Success -> {
                    categoryProducts = result.data
                    Log.d("PRODUCT_VM", "✅ Loaded ${categoryProducts.size} products for category $categoryId")
                }
                is Result.Error -> {
                    Log.e("PRODUCT_VM", "❌ Category load error: ${result.error.message}")
                    categoryProducts = emptyList()
                }
                else -> {}
            }
            isLoading = false
        }
    }

    // --- Legacy / Unmigrated Features (Comments, Rate, Custom Reel Generation) ---

    // Kept from original file
    private suspend fun getCommentsAndRatesForProduct(productId: String): Pair<List<Comment>, List<Ratings>> {
        // ⚠️ MIGRATION: Firestore removed - Comments/Rates now via backend API (CommentsUseCase)
        Log.d("PRODUCT_VM", "📝 getCommentsAndRates: Using backend API for comments")
        return Pair(emptyList(), emptyList())
    }

    private fun generateReelsFromProducts(products: List<Product>): List<Reels> {
        return try {
            products.mapNotNull { product ->
                try {
                    // Use real user info from the PostDto data carried through Product
                    val realUserName = product.displayName.ifEmpty { product.username }.ifEmpty { "User_${product.userId.take(8)}" }
                    Reels(
                        id = product.id,
                        userId = product.userId,
                        userName = realUserName,
                        userImage = R.drawable.profile,
                        video = try {
                            if (product.reelVideoUrl.isNotEmpty() && product.reelVideoUrl.startsWith("http")) {
                                Uri.parse(product.reelVideoUrl)
                            } else null
                        } catch (e: Exception) { null },
                        images = product.productImages.mapNotNull { url ->
                            try {
                                if (url.isNotEmpty() && url.startsWith("http")) Uri.parse(url) else null
                            } catch (e: Exception) { null }
                        },
                        contentDescription = product.description,
                        love = LoveItem(product.likesCount, product.isLiked),
                        ratings = emptyList(),
                        comments = emptyList(),
                        isLoading = false,
                        isError = false,
                        numberOfCart = 0,
                        numberOfComments = product.commentsCount,
                        newComment = NewComment(),
                        cartStats = CartStats(productId = product.id),
                        productName = product.name,
                        productPrice = product.price.toString(),
                        productImage = product.productImages.firstOrNull() ?: product.image,
                        sizes = product.sizeColorData.mapNotNull { it["size"] }.distinct(),
                        colors = product.sizeColorData.mapNotNull { it["color"] }.distinct(),
                        rating = product.rating,
                        // Link marketplace product fields for badge and Buy flow
                        marketplaceProductId = product.id,
                        marketplaceProductName = product.name,
                        marketplaceProductPrice = product.originalPrice,
                        marketplaceCommissionRate = product.commissionRate,
                        // Use the real Firebase post_id from the linked promotion (for comments API)
                        // Falls back to null if no promotion — ViewModel handles gracefully
                        postUid = product.postUid.ifEmpty { null },
                        isBookmarked = product.isBookmarked
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun refreshProducts() {
        viewModelScope.launch {
            allProducts = emptyList()
            productReels = emptyList()
            loadAllProducts()
        }
    }
    
    fun refreshReels() {
        viewModelScope.launch {
            if (allProducts.isNotEmpty()) {
                productReels = generateReelsFromProducts(allProducts)
            } else {
                loadAllProducts()
            }
        }
    }

    fun fetchReelsFromFirestore() {
        // ⚠️ MIGRATION: Firestore dependency removed, using product-based reels only
        Log.d("PRODUCT_VM", "📹 fetchReelsFromFirestore: Using product-based reels (backend migration)")
        refreshReels()
    }
}
