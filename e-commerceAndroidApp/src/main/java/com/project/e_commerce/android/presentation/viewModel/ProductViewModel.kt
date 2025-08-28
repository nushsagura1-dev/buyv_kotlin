package com.project.e_commerce.android.presentation.viewModel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Comment
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.LoveItem
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.NewComment
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Ratings
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Reels
import com.project.e_commerce.android.domain.model.Product
import com.project.e_commerce.android.domain.model.Category
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.project.e_commerce.android.R
import com.project.e_commerce.android.data.model.CartStats

class ProductViewModel : ViewModel() {
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

    init {
        try {
            Log.d("PRODUCT_DEBUG", "🔍 PRODUCT VIEWMODEL INITIALIZED - DEBUG IS WORKING!")
            Log.d("PRODUCT_DEBUG", "🚀 Starting to load products from Firebase...")
            Log.d("PRODUCT_DEBUG", "📱 ProductViewModel instance: $this")
            
            viewModelScope.launch {
                try {
                    Log.d("PRODUCT_DEBUG", "🔄 Starting Firebase operations in viewModelScope")
                    checkFirebaseCollections()
                    getCategoriesFromFirebase()
                    getFeaturedProductsFromFirebase()
                    getBestSellerProductsFromFirebase()
                    getAllProductsFromFirebase()
                    fetchReelsFromFirestore() // NEW: actually query a 'reels' collection
                    Log.d("PRODUCT_DEBUG", "✅ All Firebase operations completed")
                } catch (e: Exception) {
                    Log.e("PRODUCT_DEBUG", "Error in init LaunchedEffect", e)
                }
            }
        } catch (e: Exception) {
            Log.e("PRODUCT_DEBUG", "Error in init block", e)
        }
    }

    private suspend fun checkFirebaseCollections() {
        try {
            Log.d("PRODUCT_DEBUG", "🔍 Checking Firebase products collection...")
            val db = FirebaseFirestore.getInstance()
            
            // Try to access the products collection directly
            val productsSnapshot = db.collection("products").get().await()
            Log.d("PRODUCT_DEBUG", "✅ 'products' collection exists and is accessible")
            Log.d("PRODUCT_DEBUG", "📊 'products' collection has ${productsSnapshot.documents.size} documents")
            
            if (productsSnapshot.documents.isNotEmpty()) {
                Log.d("PRODUCT_DEBUG", "📄 First document ID: ${productsSnapshot.documents.first().id}")
                Log.d("PRODUCT_DEBUG", "🔍 First document data: ${productsSnapshot.documents.first().data}")
            } else {
                Log.w("PRODUCT_DEBUG", "⚠️ 'products' collection exists but is empty")
            }
        } catch (e: Exception) {
            Log.e("PRODUCT_DEBUG", "❌ Error accessing 'products' collection: ${e.message}")
            Log.e("PRODUCT_DEBUG", "❌ Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
        }
    }

    fun getProductById(productId: String) {
        viewModelScope.launch {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("products")
                    .document(productId)
                    .get()
                    .await()

                selectedProduct = Product(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "", // Add userId from Firebase
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    price = doc.getString("price") ?: "0",
                    categoryId = doc.getString("category") ?: "",
                    categoryName = doc.getString("categoryName") ?: "",
                    quantity = doc.getString("quantity") ?: "0",
                    rating = doc.getDouble("rating") ?: 0.0,
                    productImages = (doc.get("productImages") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    image = ((doc.get("productImages") as? List<*>)?.firstOrNull() as? String) ?: "",
                    reelTitle = doc.getString("reelTitle") ?: "",
                    reelVideoUrl = doc.getString("reelVideoUrl") ?: "",
                    searchQuery = doc.getString("search_query") ?: "",
                    tags = doc.getString("tags") ?: "",
                    sizeColorData = (doc.get("sizeColorData") as? List<Map<String, Any>>) ?: emptyList(),
                    createdAt = doc.getTimestamp("createdAt")
                )
            } catch (e: Exception) {
                e.printStackTrace()
                selectedProduct = null
            }
        }
    }

    private suspend fun getCommentsAndRatesForProduct(productId: String): Pair<List<Comment>, List<Ratings>> {
        val db = FirebaseFirestore.getInstance()

        val commentsSnapshot = db.collection("products")
            .document(productId)
            .collection("comments")
            .get()
            .await()

        val comments = commentsSnapshot.documents.map { doc ->
            Comment(
                id = doc.id,
                userName = doc.getString("userName") ?: "Anonymous",
                comment = doc.getString("comment") ?: "",
                time = (doc.get("time") as? Long)?.toString() ?: "0",
                isLoved = false
            )
        }

        val ratesSnapshot = db.collection("products")
            .document(productId)
            .collection("rates")
            .get()
            .await()

        val ratings = ratesSnapshot.documents.map { doc ->
            Ratings(
                userName = doc.getString("userName") ?: "Anonymous",
                review = doc.getString("review") ?: "",
                rate = (doc.getLong("rate") ?: 0).toInt()
            )
        }

        return Pair(comments, ratings)
    }

    fun getAllProductsFromFirebase() {
        viewModelScope.launch {
            try {
                Log.d("PRODUCT_DEBUG", "🔄 getAllProductsFromFirebase called")
                val db = FirebaseFirestore.getInstance()
                Log.d("PRODUCT_DEBUG", "📁 Querying Firebase collection: products")
                val snapshot = db.collection("products").get().await()
                Log.d("PRODUCT_DEBUG", "📊 Firebase query result: ${snapshot.documents.size} documents")
                Log.d("PRODUCT_DEBUG", "📄 Document IDs: ${snapshot.documents.map { it.id }}")
                if (snapshot.documents.isNotEmpty()) {
                    Log.d("PRODUCT_DEBUG", "🔍 First document data: ${snapshot.documents.first().data}")
                }

                val products = snapshot.documents.mapNotNull { doc ->
                    Log.d("PRODUCT_DEBUG", "DEBUG: Processing document: ${doc.id}")
                    Log.d("PRODUCT_DEBUG", "DEBUG: Document data: ${doc.data}")

                    val productImages = doc.get("productImages") as? List<*> ?: emptyList<Any>()
                    val images = productImages.filterIsInstance<String>()
                    val mainImage = images.firstOrNull() ?: ""
                    val sizeColorData = (doc.get("sizeColorData") as? List<Map<String, Any>>) ?: emptyList()

                    Log.d("PRODUCT_DEBUG", "DEBUG: Product ${doc.id} - productImages: $productImages")
                    Log.d("PRODUCT_DEBUG", "DEBUG: Product ${doc.id} - filtered images: $images")
                    Log.d("PRODUCT_DEBUG", "DEBUG: Product ${doc.id} - mainImage: $mainImage")
                    Log.d("PRODUCT_DEBUG", "DEBUG: Product ${doc.id} - reelVideoUrl: ${doc.getString("reelVideoUrl")}")

                    Product(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "", // Add userId from Firebase
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        price = doc.getString("price") ?: "0",
                        categoryId = doc.getString("category") ?: "",
                        categoryName = doc.getString("categoryName") ?: "",
                        quantity = doc.getString("quantity") ?: "0",
                        rating = doc.getDouble("rating") ?: 0.0,
                        productImages = images,
                        image = mainImage,
                        reelTitle = doc.getString("reelTitle") ?: "",
                        reelVideoUrl = doc.getString("reelVideoUrl") ?: "",
                        searchQuery = doc.getString("search_query") ?: "",
                        tags = doc.getString("tags") ?: "",
                        sizeColorData = sizeColorData,
                        createdAt = doc.getTimestamp("createdAt")
                    )
                }
                Log.d("PRODUCT_DEBUG", "✅ Successfully processed ${products.size} products")
                Log.d("PRODUCT_DEBUG", "📱 Product IDs: ${products.map { it.id }}")
                // Commented out since we now load reels from posts collection
                // Log.d("PRODUCT_DEBUG", "🎬 Calling generateReelsFromProducts with ${products.size} products")
                allProducts = products
                // generateReelsFromProducts(products)
                
                // Now load reels (will fall back to products if posts collection is empty)
                Log.d("PRODUCT_DEBUG", "🔄 Products loaded, now loading reels...")
                loadReelsFromPostsCollection()
                Log.d("PRODUCT_DEBUG", "✅ Reels loading completed. Final productReels count: ${productReels.size}")

                // Always populate productReels from products as fallback if none are loaded
                if (productReels.isEmpty() && products.isNotEmpty()) {
                    productReels = generateReelsFromProducts(products)
                    Log.d(
                        "PRODUCT_DEBUG",
                        "Used products-to-reels fallback: now ${productReels.size} reels"
                    )
                    // Notify possible listeners/state flows of the new data (if needed)
                    refreshReels() // This will notify/emit if needed in ReelsScreenViewModel
                }
            } catch (e: Exception) {
                Log.d("PRODUCT_DEBUG", "DEBUG: Error in getAllProductsFromFirebase: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun getBestSellerProductsFromFirebase() {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("products")
                    .orderBy("soldCount", Query.Direction.DESCENDING)
                    .limit(10)
                    .get().await()
                val products = snapshot.documents.mapNotNull { doc ->
                    val productImages = doc.get("productImages") as? List<*> ?: emptyList<Any>()
                    val images = productImages.filterIsInstance<String>()
                    val mainImage = images.firstOrNull() ?: ""
                    val sizeColorData = (doc.get("sizeColorData") as? List<Map<String, Any>>) ?: emptyList()

                    Product(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "", // Add userId from Firebase
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        price = doc.getString("price") ?: "0",
                        categoryId = doc.getString("category") ?: "",
                        categoryName = doc.getString("categoryName") ?: "",
                        quantity = doc.getString("quantity") ?: "0",
                        rating = doc.getDouble("rating") ?: 0.0,
                        productImages = images,
                        image = mainImage,
                        reelTitle = doc.getString("reelTitle") ?: "",
                        reelVideoUrl = doc.getString("reelVideoUrl") ?: "",
                        searchQuery = doc.getString("search_query") ?: "",
                        tags = doc.getString("tags") ?: "",
                        sizeColorData = sizeColorData,
                        createdAt = doc.getTimestamp("createdAt")
                    )
                }
                bestSellerProducts = products
                // Commented out since we now load reels from posts collection
                // generateReelsFromProducts(products)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getFeaturedProductsFromFirebase() {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("products")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .await()
                val products = snapshot.documents.mapNotNull { doc ->
                    val productImages = doc.get("productImages") as? List<*> ?: emptyList<Any>()
                    val images = productImages.filterIsInstance<String>()
                    val mainImage = images.firstOrNull() ?: ""
                    val sizeColorData = (doc.get("sizeColorData") as? List<Map<String, Any>>) ?: emptyList()

                    Product(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "", // Add userId from Firebase
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        price = doc.getString("price") ?: "0",
                        categoryId = doc.getString("category") ?: "",
                        categoryName = doc.getString("categoryName") ?: "",
                        quantity = doc.getString("quantity") ?: "0",
                        rating = doc.getDouble("rating") ?: 0.0,
                        productImages = images,
                        image = mainImage,
                        reelTitle = doc.getString("reelTitle") ?: "",
                        reelVideoUrl = doc.getString("reelVideoUrl") ?: "",
                        searchQuery = doc.getString("search_query") ?: "",
                        tags = doc.getString("tags") ?: "",
                        sizeColorData = sizeColorData,
                        createdAt = doc.getTimestamp("createdAt")
                    )
                }
                featuredProducts = products
                // Commented out since we now load reels from posts collection
                // generateReelsFromProducts(products)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getCategoriesFromFirebase() {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("categories").get().await()
                categories = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name")
                    val image = doc.getString("image")
                    if (name != null && image != null) {
                        Category(doc.id, name, image)
                    } else null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getProductsByCategory(category: String) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("products")
                    .whereEqualTo("category", category)
                    .get()
                    .await()
                categoryProducts = snapshot.documents.mapNotNull { doc ->
                    val productImages = doc.get("productImages") as? List<*> ?: emptyList<Any>()
                    val images = productImages.filterIsInstance<String>()
                    val mainImage = images.firstOrNull() ?: ""
                    val sizeColorData = (doc.get("sizeColorData") as? List<Map<String, Any>>) ?: emptyList()

                    Product(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "", // Add userId from Firebase
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        price = doc.getString("price") ?: "0",
                        categoryId = doc.getString("category") ?: "",
                        categoryName = doc.getString("categoryName") ?: "",
                        quantity = doc.getString("quantity") ?: "0",
                        rating = doc.getDouble("rating") ?: 0.0,
                        productImages = images,
                        image = mainImage,
                        reelTitle = doc.getString("reelTitle") ?: "",
                        reelVideoUrl = doc.getString("reelVideoUrl") ?: "",
                        searchQuery = doc.getString("search_query") ?: "",
                        tags = doc.getString("tags") ?: "",
                        sizeColorData = sizeColorData,
                        createdAt = doc.getTimestamp("createdAt")
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun generateReelsFromProducts(products: List<Product>): List<Reels> {
        Log.d("ProductViewModel", "🎬 generateReelsFromProducts called with ${products.size} products")
        return try {
            val reels = products.mapNotNull { product ->
                try {
                    Log.d("ProductViewModel", "🎬 Processing product: ${product.id}, name: ${product.name}")
                    
                    val reel = Reels(
                        id = product.id,
                        userId = product.userId,
                        userName = "User_${product.userId.take(8)}",
                        userImage = R.drawable.profile,
                        video = try {
                            if (product.reelVideoUrl.isNotEmpty() && product.reelVideoUrl.startsWith("http")) {
                                Log.d("ProductViewModel", "🎬 Creating video URI for product ${product.id}: ${product.reelVideoUrl}")
                                Uri.parse(product.reelVideoUrl)
                            } else {
                                Log.w("ProductViewModel", "🎬 Invalid video URL for product ${product.id}: ${product.reelVideoUrl}")
                                null
                            }
                        } catch (e: Exception) {
                            Log.e("ProductViewModel", "🎬 CRASH: Failed to parse video URL for product ${product.id}: ${product.reelVideoUrl}", e)
                            null
                        },
                        images = try {
                            if (product.productImages.isNotEmpty()) {
                                Log.d("ProductViewModel", "🎬 Processing ${product.productImages.size} images for product ${product.id}")
                                product.productImages.mapNotNull { url ->
                                    try {
                                        if (url.isNotEmpty() && url.startsWith("http")) {
                                            Uri.parse(url)
                                        } else {
                                            Log.w("ProductViewModel", "🎬 Invalid image URL: $url")
                                            null
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ProductViewModel", "🎬 CRASH: Failed to parse image URL: $url", e)
                                        null
                                    }
                                }
                            } else {
                                Log.d("ProductViewModel", "🎬 No images for product ${product.id}")
                                emptyList()
                            }
                        } catch (e: Exception) {
                            Log.e("ProductViewModel", "🎬 CRASH: Failed to parse product images for ${product.id}", e)
                            emptyList()
                        },
                        contentDescription = product.description,
                        love = LoveItem(0, false),
                        ratings = emptyList(),
                        comments = emptyList(),
                        isLoading = false,
                        isError = false,
                        numberOfCart = 0,
                        numberOfComments = 0,
                        newComment = NewComment(),
                        cartStats = CartStats(productId = product.id),
                        productName = product.name,
                        productPrice = product.price.toString(),
                        productImage = product.productImages.firstOrNull() ?: "",
                        sizes = product.sizeColorData
                            .mapNotNull { it["size"] as? String }
                            .filter { it.isNotBlank() }
                            .distinct(),
                        colors = product.sizeColorData
                            .mapNotNull { it["color"] as? String }
                            .filter { it.isNotBlank() }
                            .distinct(),
                        rating = product.rating
                    )
                    
                    Log.d("ProductViewModel", "🎬 Successfully created reel for product ${product.id}")
                    reel
                } catch (e: Exception) {
                    Log.e("ProductViewModel", "🎬 CRASH: Error creating reel for product ${product.id}", e)
                    null
                }
            }
            
            Log.d("ProductViewModel", "🎬 Successfully generated ${reels.size} reels from ${products.size} products")
            reels
        } catch (e: Exception) {
            Log.e("ProductViewModel", "🎬 CRASH: Error in generateReelsFromProducts", e)
            emptyList()
        }
    }

    private suspend fun loadReelsFromPostsCollection(): List<Reels> {
        Log.d("ProductViewModel", "🎬 loadReelsFromPostsCollection called")
        return try {
            val reels = generateReelsFromProducts(allProducts)
            Log.d("ProductViewModel", "🎬 Generated ${reels.size} reels from ${allProducts.size} products")
            
            reels.forEachIndexed { index, reel ->
                Log.d("ProductViewModel", "🎬 Reel $index: id=${reels[index].id}, video=${reels[index].video}, images=${reels[index].images?.size}")
            }
            
            reels
        } catch (e: Exception) {
            Log.e("ProductViewModel", "🎬 CRASH: Error generating reels from products", e)
            emptyList()
        }
    }
    
    fun refreshProducts() {
        Log.d("PRODUCT_DEBUG", "🔄 refreshProducts called manually")
        viewModelScope.launch {
            getAllProductsFromFirebase()
            // Reload reels from posts collection to ensure consistency
            loadReelsFromPostsCollection()
        }
    }
    
    fun refreshReels() {
        Log.d("PRODUCT_DEBUG", "🎬 refreshReels called manually")
        viewModelScope.launch {
            loadReelsFromPostsCollection()
        }
    }

    fun fetchReelsFromFirestore() {
        viewModelScope.launch {
            try {
                Log.d("PRODUCT_DEBUG", "🔄 fetchReelsFromFirestore called")
                val db = FirebaseFirestore.getInstance()
                val snapshot =
                    db.collection("reels").get().await() // or use "posts" depending on your schema
                Log.d("PRODUCT_DEBUG", "📊 Fetched ${snapshot.documents.size} reels")
                val reelsList = snapshot.documents.mapNotNull { doc ->
                    try {
                        // Parse the Firestore doc into a Reels object (edit mapping to your schema!)
                        Reels(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "User",
                            userImage = R.drawable.profile, // or parse Cloudinary URL if needed
                            video = doc.getString("videoUrl")
                                ?.let { if (it.isNotBlank()) Uri.parse(it) else null },
                            images = (doc.get("images") as? List<*>)?.mapNotNull { url ->
                                url?.let { u ->
                                    Uri.parse(
                                        u.toString()
                                    )
                                }
                            } ?: emptyList(),
                            contentDescription = doc.getString("description") ?: "",
                            love = LoveItem(0, false), // Map as needed
                            ratings = emptyList(),
                            comments = emptyList(),
                            isLoading = false,
                            isError = false,
                            numberOfCart = 0,
                            numberOfComments = 0,
                            newComment = NewComment(),
                            cartStats = CartStats(productId = doc.id),
                            productName = doc.getString("productName") ?: "",
                            productPrice = doc.getString("productPrice") ?: "0",
                            productImage = (doc.get("images") as? List<*>)?.firstOrNull()
                                ?.toString() ?: "",
                            sizes = emptyList(),
                            colors = emptyList(),
                            rating = doc.getDouble("rating") ?: 0.0
                        )
                    } catch (e: Exception) {
                        Log.e("PRODUCT_DEBUG", "Error parsing reel doc: ", e)
                        null
                    }
                }
                productReels = reelsList
                Log.d("PRODUCT_DEBUG", "✅ Loaded ${reelsList.size} reels from Firestore")
            } catch (e: Exception) {
                Log.e("PRODUCT_DEBUG", "❌ Error fetching reels from Firestore: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
