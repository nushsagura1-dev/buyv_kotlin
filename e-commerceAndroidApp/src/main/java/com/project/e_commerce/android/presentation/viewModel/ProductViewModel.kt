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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.project.e_commerce.android.R

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
        Log.d("PRODUCT_DEBUG", "🔍 PRODUCT VIEWMODEL INITIALIZED - DEBUG IS WORKING!")
        Log.d("PRODUCT_DEBUG", "🚀 Starting to load products from Firebase...")
        Log.d("PRODUCT_DEBUG", "📱 ProductViewModel instance: $this")
        
        viewModelScope.launch {
            Log.d("PRODUCT_DEBUG", "🔄 Starting Firebase operations in viewModelScope")
            checkFirebaseCollections()
            getCategoriesFromFirebase()
            getFeaturedProductsFromFirebase()
            getBestSellerProductsFromFirebase()
            getAllProductsFromFirebase()
            Log.d("PRODUCT_DEBUG", "✅ All Firebase operations completed")
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
                Log.d("PRODUCT_DEBUG", "🎬 Calling generateReelsFromProducts with ${products.size} products")
                allProducts = products
                generateReelsFromProducts(products)
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
                generateReelsFromProducts(products)
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
                generateReelsFromProducts(products)
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

    private fun generateReelsFromProducts(products: List<Product>) {
        viewModelScope.launch {
            Log.d("PRODUCT_DEBUG", "🎬 generateReelsFromProducts called with ${products.size} products")
            if (products.isEmpty()) {
                Log.w("PRODUCT_DEBUG", "⚠️ No products to convert to reels!")
                return@launch
            }
            
            val reels = products.map { product ->
                Log.d("PRODUCT_DEBUG", "DEBUG: Converting product ${product.id} to reel")
                Log.d("PRODUCT_DEBUG", "DEBUG: Product ${product.id} - name: ${product.name}")
                Log.d("PRODUCT_DEBUG", "DEBUG: Product ${product.id} - productImages: ${product.productImages}")
                Log.d("PRODUCT_DEBUG", "DEBUG: Product ${product.id} - reelVideoUrl: ${product.reelVideoUrl}")
                
                val (comments, ratings) = getCommentsAndRatesForProduct(product.id)

                val sizes = product.sizeColorData
                    .mapNotNull { it["size"] as? String }
                    .filter { it.isNotBlank() }
                    .distinct()

                val colors = product.sizeColorData
                    .mapNotNull { it["color"] as? String }
                    .filter { it.isNotBlank() }
                    .distinct()

                val reel = Reels(
                    id = product.id,
                    userId = product.userId.ifEmpty { "unknown_user" }, // Use a more descriptive placeholder
                    userName = if (product.userId.isNotEmpty()) "User_${product.userId.take(8)}" else "Unknown_User", // Placeholder that will be replaced with real data
                    userImage = R.drawable.profile, // Add missing userImage field
                    video = if (product.reelVideoUrl.isNotEmpty()) Uri.parse(product.reelVideoUrl) else null,
                    images = if (product.productImages.isNotEmpty()) product.productImages.map { Uri.parse(it) } else null,
                    contentDescription = if (product.reelTitle.isNotEmpty()) product.reelTitle else product.description,
                    love = LoveItem(0, false),
                    ratings = ratings,
                    comments = comments,
                    isLoading = false,
                    isError = false,
                    numberOfCart = 0,
                    numberOfComments = comments.size,
                    newComment = NewComment(),
                    productName = product.name,
                    productPrice = product.price,
                    productImage = product.productImages.firstOrNull() ?: "",
                    sizes = sizes,
                    colors = colors,
                    rating = product.rating,
                )
                
                Log.d("PRODUCT_DEBUG", "DEBUG: Created reel ${reel.id} - video: ${reel.video}, images: ${reel.images}, productImage: ${reel.productImage}")
                reel
            }
            Log.d("PRODUCT_DEBUG", "🎬 Generated ${reels.size} reels")
            Log.d("PRODUCT_DEBUG", "📺 Setting productReels to ${reels.size} reels")
            Log.d("PRODUCT_DEBUG", "📱 Reel IDs: ${reels.map { it.id }}")
            productReels = reels
            Log.d("PRODUCT_DEBUG", "✅ productReels now contains ${productReels.size} reels")
        }
    }
    
    fun refreshProducts() {
        Log.d("PRODUCT_DEBUG", "🔄 refreshProducts called manually")
        viewModelScope.launch {
            getAllProductsFromFirebase()
        }
    }

}

data class Category(
    val id: String,
    val name: String,
    val image: String
)

data class Product(
    val id: String = "",
    val userId: String = "", // Add userId to identify product owner
    val name: String = "",
    val description: String = "",
    val price: String = "0",
    val categoryId: String = "",
    val categoryName: String = "",
    val quantity: String = "0",
    val rating: Double = 0.0,
    val productImages: List<String> = emptyList(),
    val image: String = "",
    val reelTitle: String = "",
    val reelVideoUrl: String = "",
    val searchQuery: String = "",
    val tags: String = "",
    val sizeColorData: List<Map<String, Any>> = emptyList(),
    val createdAt: Timestamp? = null
)
