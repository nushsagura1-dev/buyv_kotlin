package com.project.e_commerce.data.repository

import com.project.e_commerce.data.local.CartStorage
import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Cart
import com.project.e_commerce.domain.model.CartItem
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implémentation du CartRepository utilisant le stockage local.
 * 
 * Remplace l'implémentation Firestore pour une approche offline-first.
 * Utilise CartStorage pour la persistance locale.
 */
class CartRepositoryImpl(
    private val cartStorage: CartStorage
) : CartRepository {
    
    // Cache en mémoire pour les flux réactifs
    private val _cartFlow = MutableStateFlow<Cart?>(null)
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }
    
    override suspend fun getUserCart(userId: String): Result<Cart> {
        return try {
            val cartJson = cartStorage.getCart(userId)
            val cart = if (cartJson != null) {
                try {
                    json.decodeFromString<Cart>(cartJson)
                } catch (e: Exception) {
                    // En cas d'erreur de parsing, on repart sur un panier vide
                    Cart(id = userId, userId = userId)
                }
            } else {
                Cart(id = userId, userId = userId)
            }
            
            // Mettre à jour le flux
            _cartFlow.value = cart
            
            Result.Success(cart)
        } catch (e: Exception) {
            Result.Error(ApiError.fromException(e))
        }
    }

    override fun getUserCartFlow(userId: String): Flow<Cart?> {
        // Initial load
        try {
            val cartJson = cartStorage.getCart(userId)
            if (cartJson != null) {
                _cartFlow.value = json.decodeFromString<Cart>(cartJson)
            } else {
                _cartFlow.value = Cart(id = userId, userId = userId)
            }
        } catch (e: Exception) {
            _cartFlow.value = Cart(id = userId, userId = userId)
        }
        
        return _cartFlow
    }
    
    override suspend fun addToCart(userId: String, item: CartItem): Result<Unit> {
        return try {
            // Récupérer le panier actuel (via flux ou storage)
            var currentCart = _cartFlow.value ?: run {
                val result = getUserCart(userId)
                if (result is Result.Success) result.data else Cart(id = userId, userId = userId)
            }
            
            val existingItems = currentCart.items.toMutableList()
            
            // Vérifier si l'article existe déjà
            val existingItemIndex = existingItems.indexOfFirst { 
                it.productId == item.productId && 
                it.size == item.size && 
                it.color == item.color 
            }
            
            if (existingItemIndex != -1) {
                val existingItem = existingItems[existingItemIndex]
                existingItems[existingItemIndex] = existingItem.copy(
                    quantity = existingItem.quantity + item.quantity
                )
            } else {
                val newItem = item.copy(
                    id = "${item.productId}_${Clock.System.now().toEpochMilliseconds()}",
                    addedAt = Clock.System.now().toEpochMilliseconds()
                )
                existingItems.add(newItem)
            }
            
            // Calculer sous-total
            val subtotal = existingItems.sumOf { it.totalPrice }
            
            val updatedCart = currentCart.copy(
                items = existingItems,
                subtotal = subtotal,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
            
            saveCart(updatedCart)
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ApiError.fromException(e))
        }
    }
    
    override suspend fun removeFromCart(userId: String, lineId: String): Result<Unit> {
        return try {
            val currentCart = _cartFlow.value ?: run {
                val result = getUserCart(userId)
                if (result is Result.Success) result.data else return Result.Error(ApiError.Unknown("Cart not loaded"))
            }
            
            val updatedItems = currentCart.items.filter { it.id != lineId }
            val subtotal = updatedItems.sumOf { it.totalPrice }
            
            val updatedCart = currentCart.copy(
                items = updatedItems,
                subtotal = subtotal,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
            
            saveCart(updatedCart)
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ApiError.fromException(e))
        }
    }
    
    override suspend fun updateQuantity(userId: String, lineId: String, quantity: Int): Result<Unit> {
        if (quantity <= 0) return removeFromCart(userId, lineId)
        
        return try {
            val currentCart = _cartFlow.value ?: run {
                 val result = getUserCart(userId)
                 if (result is Result.Success) result.data else return Result.Error(ApiError.Unknown("Cart not loaded"))
            }
            
            val updatedItems = currentCart.items.map { item ->
                if (item.id == lineId) item.copy(quantity = quantity) else item
            }
            
            val subtotal = updatedItems.sumOf { it.totalPrice }
            
            val updatedCart = currentCart.copy(
                items = updatedItems,
                subtotal = subtotal,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
            
            saveCart(updatedCart)
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ApiError.fromException(e))
        }
    }
    
    override suspend fun clearCart(userId: String): Result<Unit> {
        return try {
            cartStorage.clearCart(userId)
            
            val emptyCart = Cart(id = userId, userId = userId)
            _cartFlow.value = emptyCart
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(ApiError.fromException(e))
        }
    }
    
    override suspend fun syncCart(userId: String): Result<Cart> {
        return getUserCart(userId)
    }
    
    private fun saveCart(cart: Cart) {
        val jsonStr = json.encodeToString(cart)
        cartStorage.saveCart(cart.userId, jsonStr)
        _cartFlow.value = cart
    }
}
