package com.project.e_commerce.android.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.project.e_commerce.android.domain.repository.CartRepository
import com.project.e_commerce.android.presentation.viewModel.CartItem
import com.project.e_commerce.android.data.model.CartStats
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseCartRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CartRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val CARTS_COLLECTION = "carts"
        private const val CART_STATS_COLLECTION = "cartStats"
        private const val TAG = "FirebaseCartRepository"
    }

    init {
        try {
            Log.d(
                TAG,
                "FirebaseCartRepository constructor ENTRY: auth=$auth class=${auth::class.java.simpleName}, firestore=$firestore class=${firestore::class.java.simpleName}"
            )
            if (auth == null) Log.e(TAG, "auth is NULL!")
            if (firestore == null) Log.e(TAG, "firestore is NULL!")
            Log.d(TAG, "FirebaseCartRepository constructor EXIT - SUCCESS")
        } catch (e: Exception) {
            Log.e(TAG, "EXCEPTION in FirebaseCartRepository init: ${e.stackTraceToString()}")
        }
    }

    override suspend fun addToCart(userId: String, cartItem: CartItem): Result<Unit> = runCatching {
        Log.d(TAG, "Adding item to cart for user: $userId, product: ${cartItem.productId}")
        
        try {
            // Validate inputs
            if (userId.isBlank() || cartItem.productId.isBlank()) {
                Log.e(TAG, "Invalid userId or productId: userId=$userId, productId=${cartItem.productId}")
                throw IllegalArgumentException("Invalid userId or productId")
            }
            
            val cartRef = firestore
                .collection(USERS_COLLECTION)
                .document(userId)
                .collection(CARTS_COLLECTION)
                .document(cartItem.lineId)
            
            val cartData = mapOf(
                "productId" to cartItem.productId,
                "name" to cartItem.name,
                "price" to cartItem.price,
                "imageUrl" to cartItem.imageUrl,
                "quantity" to cartItem.quantity,
                "lineId" to cartItem.lineId,
                "variantId" to cartItem.variantId,
                "size" to cartItem.size,
                "color" to cartItem.color,
                "attributes" to cartItem.attributes,
                "addedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            cartRef.set(cartData).await()
            
            // Update product cart stats
            incrementProductCartCount(cartItem.productId).getOrThrow()
            
            Log.d(TAG, "Successfully added item to cart")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding item to cart: ${e.message}")
            throw e
        }
    }

    override suspend fun removeFromCart(userId: String, lineId: String): Result<Unit> = runCatching {
        Log.d(TAG, "Removing item from cart: $lineId for user: $userId")
        
        val cartRef = firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(CARTS_COLLECTION)
            .document(lineId)
        
        // Get the item before deleting to update stats
        val itemDoc = cartRef.get().await()
        if (itemDoc.exists()) {
            val productId = itemDoc.getString("productId")
            if (productId != null) {
                decrementProductCartCount(productId).getOrThrow()
            }
        }
        
        cartRef.delete().await()
        Log.d(TAG, "Successfully removed item from cart")
    }

    override suspend fun removeFromCartByProductId(userId: String, productId: String): Result<Unit> = runCatching {
        Log.d(TAG, "Removing product from cart: $productId for user: $userId")
        
        try {
            // Validate inputs
            if (userId.isBlank() || productId.isBlank()) {
                Log.e(TAG, "Invalid userId or productId: userId=$userId, productId=$productId")
                throw IllegalArgumentException("Invalid userId or productId")
            }
            
            val cartQuery = firestore
                .collection(USERS_COLLECTION)
                .document(userId)
                .collection(CARTS_COLLECTION)
                .whereEqualTo("productId", productId)
            
            val querySnapshot = cartQuery.get().await()
            
            if (querySnapshot.documents.isNotEmpty()) {
                for (document in querySnapshot.documents) {
                    try {
                        document.reference.delete().await()
                        Log.d(TAG, "Deleted cart document: ${document.id}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting cart document ${document.id}: ${e.message}")
                        // Continue with other documents even if one fails
                    }
                }
                
                // Update product cart stats
                decrementProductCartCount(productId).getOrThrow()
                Log.d(TAG, "Successfully removed product from cart")
            } else {
                Log.d(TAG, "No cart items found for product: $productId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in removeFromCartByProductId: ${e.message}")
            throw e
        }
    }

    override suspend fun updateCartItemQuantity(userId: String, lineId: String, quantity: Int): Result<Unit> = runCatching {
        Log.d(TAG, "Updating cart item quantity: $lineId to $quantity for user: $userId")
        
        val cartRef = firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(CARTS_COLLECTION)
            .document(lineId)
        
        cartRef.update(
            mapOf(
                "quantity" to quantity,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        
        Log.d(TAG, "Successfully updated cart item quantity")
    }

    override fun getUserCart(userId: String): Flow<List<CartItem>> = callbackFlow {
        Log.d(TAG, "Getting cart for user: $userId")
        
        val cartQuery = firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(CARTS_COLLECTION)
            .orderBy("addedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        
        val listener = cartQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error getting cart: ${error.message}")
                close(error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                val cartItems = snapshot.documents.mapNotNull { doc ->
                    try {
                        CartItem(
                            productId = doc.getString("productId") ?: "",
                            name = doc.getString("name") ?: "",
                            price = doc.getDouble("price") ?: 0.0,
                            imageUrl = doc.getString("imageUrl") ?: "",
                            quantity = doc.getLong("quantity")?.toInt() ?: 1,
                            lineId = doc.getString("lineId") ?: doc.id,
                            variantId = doc.getString("variantId"),
                            size = doc.getString("size"),
                            color = doc.getString("color"),
                            attributes = (doc.get("attributes") as? Map<String, String>) ?: emptyMap()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing cart item: ${e.message}")
                        null
                    }
                }
                
                trySend(cartItems)
            }
        }
        
        awaitClose { listener.remove() }
    }

    override fun getProductCartStats(productId: String): Flow<CartStats> = callbackFlow {
        Log.d(TAG, "Getting cart stats for product: $productId")
        
        val statsRef = firestore
            .collection(CART_STATS_COLLECTION)
            .document(productId)
        
        val listener = statsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error getting cart stats: ${error.message}")
                close(error)
                return@addSnapshotListener
            }
            
            if (snapshot != null && snapshot.exists()) {
                val stats = CartStats(
                    productId = productId,
                    totalAdded = snapshot.getLong("totalAdded")?.toInt() ?: 0,
                    uniqueUsers = snapshot.getLong("uniqueUsers")?.toInt() ?: 0,
                    lastUpdated = snapshot.getLong("lastUpdated") ?: System.currentTimeMillis()
                )
                trySend(stats)
            } else {
                // Create default stats if document doesn't exist
                trySend(CartStats(productId = productId))
            }
        }
        
        awaitClose { listener.remove() }
    }

    override fun isProductInUserCart(userId: String, productId: String): Flow<Boolean> = callbackFlow {
        Log.d(TAG, "Checking if product $productId is in cart for user: $userId")
        
        val cartQuery = firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(CARTS_COLLECTION)
            .whereEqualTo("productId", productId)
            .limit(1)
        
        val listener = cartQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error checking cart status: ${error.message}")
                close(error)
                return@addSnapshotListener
            }
            
            val isInCart = snapshot != null && !snapshot.isEmpty
            trySend(isInCart)
        }
        
        awaitClose { listener.remove() }
    }

    override suspend fun clearUserCart(userId: String): Result<Unit> = runCatching {
        Log.d(TAG, "Clearing cart for user: $userId")
        
        val cartQuery = firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(CARTS_COLLECTION)
        
        val querySnapshot = cartQuery.get().await()
        
        // Get all product IDs before clearing to update stats
        val productIds = querySnapshot.documents.mapNotNull { doc ->
            doc.getString("productId")
        }.distinct()
        
        // Clear cart
        for (document in querySnapshot.documents) {
            document.reference.delete().await()
        }
        
        // Update product cart stats
        productIds.forEach { productId ->
            decrementProductCartCount(productId).getOrThrow()
        }
        
        Log.d(TAG, "Successfully cleared user cart")
    }

    override suspend fun incrementProductCartCount(productId: String): Result<Unit> = runCatching {
        Log.d(TAG, "Incrementing cart count for product: $productId")
        
        val statsRef = firestore
            .collection(CART_STATS_COLLECTION)
            .document(productId)
        
        // Use set with merge to create document if it doesn't exist
        statsRef.set(
            mapOf(
                "productId" to productId,
                "totalAdded" to FieldValue.increment(1),
                "lastUpdated" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
        
        Log.d(TAG, "Successfully incremented product cart count")
    }

    override suspend fun decrementProductCartCount(productId: String): Result<Unit> = runCatching {
        Log.d(TAG, "Decrementing cart count for product: $productId")
        
        try {
            if (productId.isBlank()) {
                Log.e(TAG, "Invalid productId: $productId")
                throw IllegalArgumentException("Invalid productId")
            }
            
            val statsRef = firestore
                .collection(CART_STATS_COLLECTION)
                .document(productId)
            
            // Check if the document exists before trying to update
            val docSnapshot = statsRef.get().await()
            if (docSnapshot.exists()) {
                val currentTotal = docSnapshot.getLong("totalAdded") ?: 0
                if (currentTotal > 0) {
                    statsRef.set(
                        mapOf(
                            "productId" to productId,
                            "totalAdded" to FieldValue.increment(-1),
                            "lastUpdated" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()
                    Log.d(TAG, "Successfully decremented product cart count")
                } else {
                    Log.d(TAG, "Cart count already at 0 for product: $productId")
                }
            } else {
                Log.d(TAG, "Cart stats document doesn't exist for product: $productId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in decrementProductCartCount: ${e.message}")
            throw e
        }
    }
}
