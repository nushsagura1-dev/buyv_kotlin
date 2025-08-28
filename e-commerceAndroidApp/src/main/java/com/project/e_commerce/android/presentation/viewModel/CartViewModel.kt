package com.project.e_commerce.android.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.project.e_commerce.android.domain.repository.CartRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.delay

data class CartItem(
    val productId: String,
    val name: String,
    val price: Double,
    val imageUrl: String,
    val quantity: Int = 1,

    // NEW:
    val lineId: String = java.util.UUID.randomUUID().toString(),
    val variantId: String? = null,
    val size: String? = null,
    val color: String? = null,
    val attributes: Map<String, String> = emptyMap() // لأي مواصفات إضافية (مثلاً خامة، طول..)
)

data class CartState(
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val shipping: Double = 8.0,
    val tax: Double = 0.0,
    val total: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class DummyCartRepository : CartRepository {
    override suspend fun addToCart(userId: String, cartItem: CartItem) = Result.success(Unit)
    override suspend fun removeFromCart(userId: String, lineId: String) = Result.success(Unit)
    override suspend fun removeFromCartByProductId(userId: String, productId: String) =
        Result.success(Unit)

    override suspend fun updateCartItemQuantity(userId: String, lineId: String, quantity: Int) =
        Result.success(Unit)

    override fun getUserCart(userId: String) = kotlinx.coroutines.flow.flowOf(emptyList<CartItem>())
    override fun getProductCartStats(productId: String) =
        kotlinx.coroutines.flow.flowOf(com.project.e_commerce.android.data.model.CartStats(productId = productId))

    override fun isProductInUserCart(userId: String, productId: String) =
        kotlinx.coroutines.flow.flowOf(false)

    override suspend fun clearUserCart(userId: String) = Result.success(Unit)
    override suspend fun incrementProductCartCount(productId: String) = Result.success(Unit)
    override suspend fun decrementProductCartCount(productId: String) = Result.success(Unit)
}

class CartViewModel(
    private val cartRepository: CartRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    // ✅ بدل ما يكون عندك List فقط، هنخلي الحالة كاملة
    private val _state = MutableStateFlow(CartState())
    val state: StateFlow<CartState> = _state

    init {
        try {
            // Listen to Firebase cart changes with safety check
            val userId = try {
                auth.currentUser?.uid
            } catch (e: Exception) {
                Log.e("CartViewModel", "Error getting current user: ", e)
                null
            }
            Log.d(
                "CartViewModel",
                "CartViewModel INIT: Resolved userId=$userId from FirebaseAuth currentUser"
            )

            if (userId != null && userId.isNotBlank()) {
                viewModelScope.launch {
                    try {
                        cartRepository.getUserCart(userId).collect { cartItems ->
                            _state.update { current ->
                                current.copy(
                                    items = cartItems,
                                    isLoading = false,
                                    error = null
                                ).let { newState ->
                                    calculateTotals(newState.items, newState.shipping, newState.tax)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CartViewModel", "Error in getUserCart flow", e)
                        _state.update { it.copy(error = "Failed to load cart: ${e.message}") }
                    }
                }
            } else {
                Log.w(
                    "CartViewModel",
                    "CartViewModel: No authenticated user found, not calling getUserCart"
                )
            }
        } catch (e: Exception) {
            Log.e("CartViewModel", "Error in CartViewModel init block", e)
        }
    }

    private fun isSameLine(a: CartItem, b: CartItem): Boolean {
        return a.productId == b.productId &&
                a.variantId == b.variantId &&
                a.size == b.size &&
                a.color == b.color &&
                a.attributes == b.attributes
    }

    fun addToCart(item: CartItem) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _state.update { it.copy(error = "User not authenticated") }
            return
        }

        // Clear any previous errors and set loading
        _state.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                cartRepository.addToCart(userId, item)
                    .onSuccess {
                        // Success - Firebase listener will update the state
                        Log.d("CartViewModel", "Successfully added item to cart")
                        // Clear loading state after a short delay to allow Firebase listener to update
                        delay(500)
                        _state.update { it.copy(isLoading = false) }
                    }
                    .onFailure { error ->
                        Log.e("CartViewModel", "Failed to add item to cart: ${error.message}")
                        val userFriendlyError = when {
                            error.message?.contains("Not_Found") == true -> "Cart update failed. Please try again."
                            error.message?.contains("Permission denied") == true -> "Access denied. Please check your login."
                            else -> "Failed to add item to cart. Please try again."
                        }
                        _state.update { 
                            it.copy(
                                isLoading = false, 
                                error = userFriendlyError
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Exception in addToCart: ${e.message}")
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        error = "Failed to add item: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateQuantity(lineId: String, quantity: Int) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _state.update { it.copy(error = "User not authenticated") }
            return
        }

        if (quantity <= 0) {
            removeItem(lineId)
            return
        }

        // Clear any previous errors and set loading
        _state.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                cartRepository.updateCartItemQuantity(userId, lineId, quantity)
                    .onSuccess {
                        // Success - Firebase listener will update the state
                        Log.d("CartViewModel", "Successfully updated quantity")
                        // Clear loading state after a short delay to allow Firebase listener to update
                        delay(500)
                        _state.update { it.copy(isLoading = false) }
                    }
                    .onFailure { error ->
                        Log.e("CartViewModel", "Failed to update quantity: ${error.message}")
                        val userFriendlyError = when {
                            error.message?.contains("Not_Found") == true -> "Quantity update failed. Please try again."
                            error.message?.contains("Permission denied") == true -> "Access denied. Please check your login."
                            else -> "Failed to update quantity. Please try again."
                        }
                        _state.update { 
                            it.copy(
                                isLoading = false, 
                                error = userFriendlyError
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Exception in updateQuantity: ${e.message}")
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        error = "Failed to update quantity: ${e.message}"
                    )
                }
            }
        }
    }

    fun removeItem(lineId: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _state.update { it.copy(error = "User not authenticated") }
            return
        }

        // Clear any previous errors and set loading
        _state.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                cartRepository.removeFromCart(userId, lineId)
                    .onSuccess {
                        // Success - Firebase listener will update the state
                        Log.d("CartViewModel", "Successfully removed item")
                        // Clear loading state after a short delay to allow Firebase listener to update
                        delay(500)
                        _state.update { it.copy(isLoading = false) }
                    }
                    .onFailure { error ->
                        Log.e("CartViewModel", "Failed to remove item: ${error.message}")
                        val userFriendlyError = when {
                            error.message?.contains("Not_Found") == true -> "Item removal failed. Please try again."
                            error.message?.contains("Permission denied") == true -> "Access denied. Please check your login."
                            else -> "Failed to remove item. Please try again."
                        }
                        _state.update { 
                            it.copy(
                                isLoading = false, 
                                error = userFriendlyError
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Exception in removeItem: ${e.message}")
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        error = "Failed to remove item: ${e.message}"
                    )
                }
            }
        }
    }

    fun removeFromCartByProductId(productId: String) {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _state.update { it.copy(error = "User not authenticated") }
                return
            }

            if (productId.isBlank()) {
                _state.update { it.copy(error = "Invalid product ID") }
                return
            }

            // Clear any previous errors and set loading
            _state.update { it.copy(isLoading = true, error = null) }
            
            viewModelScope.launch {
                try {
                    cartRepository.removeFromCartByProductId(userId, productId)
                        .onSuccess {
                            // Success - Firebase listener will update the state
                            Log.d("CartViewModel", "Successfully removed product from cart")
                            // Clear loading state after a short delay to allow Firebase listener to update
                            delay(500)
                            _state.update { it.copy(isLoading = false) }
                        }
                        .onFailure { error ->
                            Log.e("CartViewModel", "Failed to remove product from cart: ${error.message}")
                            val userFriendlyError = when {
                                error.message?.contains("Not_Found") == true -> "Product removal failed. Please try again."
                                error.message?.contains("Permission denied") == true -> "Access denied. Please check your login."
                                else -> "Failed to remove product. Please try again."
                            }
                            _state.update { 
                                it.copy(
                                    isLoading = false, 
                                    error = userFriendlyError
                                ) 
                            }
                        }
                } catch (e: Exception) {
                    Log.e("CartViewModel", "Exception in removeFromCartByProductId: ${e.message}")
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            error = "Unexpected error: ${e.message}"
                        ) 
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CartViewModel", "Exception in removeFromCartByProductId: ${e.message}")
            _state.update { it.copy(error = "Unexpected error: ${e.message}") }
        }
    }

    fun clearCart() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _state.update { it.copy(error = "User not authenticated") }
            return
        }

        // Clear any previous errors and set loading
        _state.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                cartRepository.clearUserCart(userId)
                    .onSuccess {
                        // Success - Firebase listener will update the state
                        Log.d("CartViewModel", "Successfully cleared cart")
                        // Clear loading state after a short delay to allow Firebase listener to update
                        delay(500)
                        _state.update { it.copy(isLoading = false) }
                    }
                    .onFailure { error ->
                        Log.e("CartViewModel", "Failed to clear cart: ${error.message}")
                        val userFriendlyError = when {
                            error.message?.contains("Not_Found") == true -> "Cart clearing failed. Please try again."
                            error.message?.contains("Permission denied") == true -> "Access denied. Please check your login."
                            else -> "Failed to clear cart. Please try again."
                        }
                        _state.update { 
                            it.copy(
                                isLoading = false, 
                                error = userFriendlyError
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Exception in clearCart: ${e.message}")
                _state.update { 
                    it.copy(
                        isLoading = false, 
                        error = "Failed to clear cart: ${e.message}"
                    )
                }
            }
        }
    }

    // Check if a specific product is in the current user's cart
    fun isProductInCart(productId: String): Flow<Boolean> {
        val userId = auth.currentUser?.uid
        return if (userId != null) {
            cartRepository.isProductInUserCart(userId, productId)
        } else {
            flowOf(false)
        }
    }

    // Clear any error messages
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    // Manually refresh cart state
    fun refreshCart() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            _state.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                try {
                    // The Firebase listener will automatically update the state
                    delay(1000) // Give Firebase listener time to update
                    _state.update { it.copy(isLoading = false) }
                } catch (e: Exception) {
                    Log.e("CartViewModel", "Exception in refreshCart: ${e.message}")
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    // Get cart statistics for a specific product
    fun getProductCartStats(productId: String): Flow<com.project.e_commerce.android.data.model.CartStats> {
        return cartRepository.getProductCartStats(productId)
    }

    private fun calculateTotals(items: List<CartItem>, shipping: Double, tax: Double): CartState {
        val subtotal = items.sumOf { it.price * it.quantity }
        val total = subtotal + shipping + tax
        return CartState(
            items = items,
            subtotal = subtotal,
            shipping = shipping,
            tax = tax,
            total = total,
            isLoading = false,
            error = null
        )
    }
}

//class DummyCartViewModel : CartViewModel(DummyCartRepository(), FirebaseAuth.getInstance())
