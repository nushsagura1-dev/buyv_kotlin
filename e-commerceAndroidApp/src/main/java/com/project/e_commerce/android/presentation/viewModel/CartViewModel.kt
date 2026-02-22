package com.project.e_commerce.android.presentation.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.domain.model.Cart
import com.project.e_commerce.domain.model.CartItem
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.usecase.auth.GetCurrentUserUseCase
import com.project.e_commerce.domain.usecase.cart.AddToCartUseCase
import com.project.e_commerce.domain.usecase.cart.ClearCartUseCase
import com.project.e_commerce.domain.usecase.cart.GetCartUseCase
import com.project.e_commerce.domain.usecase.cart.RemoveFromCartUseCase
import com.project.e_commerce.domain.usecase.cart.UpdateCartItemUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

// UI Model for CartItem (kept for compatibility or convenience, or use Shared directly if possible)
// However, the UI uses this definition. To minimize UI breakage, I will map it.
// Or I can replace it with Shared CartItem if references match.
// Android CartItem has 'name', Shared has 'productName'.
// Android CartItem has 'imageUrl', Shared has 'productImage'.
// To avoid breaking ALL UI files, I will keep this Data Class and map it.
data class CartItemUi(
    val productId: String,
    val name: String,
    val price: Double,
    val imageUrl: String,
    val quantity: Int = 1,
    val lineId: String = java.util.UUID.randomUUID().toString(),
    val variantId: String? = null,
    val size: String? = null,
    val color: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    // Affiliate tracking fields
    val promoterUid: String? = null,
    val reelId: String? = null,
    val sessionId: String? = null
)

data class CartState(
    val items: List<CartItemUi> = emptyList(),
    val subtotal: Double = 0.0,
    val shipping: Double = 8.0,
    val tax: Double = 0.0,
    val total: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CartViewModel(
    private val getCartUseCase: GetCartUseCase,
    private val addToCartUseCase: AddToCartUseCase,
    private val updateCartItemUseCase: UpdateCartItemUseCase,
    private val removeFromCartUseCase: RemoveFromCartUseCase,
    private val clearCartUseCase: ClearCartUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CartState())
    val state: StateFlow<CartState> = _state.asStateFlow()
    
    // Cache userId to avoid repeated calls
    private var currentUserId: String? = null

    init {
        // Ne charge pas le panier automatiquement
        // Les écrans appelleront initializeCart() manuellement quand l'utilisateur est connecté
    }

    fun initializeCart() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            // Get Current User
            when (val userResult = getCurrentUserUseCase()) {
                is Result.Success -> {
                    val user = userResult.data
                    if (user != null) {
                        currentUserId = user.uid
                        Log.d("CartViewModel", "✅ User authenticated: ${user.uid}")
                        observeCart(user.uid)
                    } else {
                        Log.w("CartViewModel", "⚠️ No authenticated user found")
                        _state.update { it.copy(isLoading = false, error = "User not authenticated") }
                    }
                }
                is Result.Error -> {
                    Log.e("CartViewModel", "❌ Error getting user: ${userResult.error.message}")
                    _state.update { it.copy(isLoading = false, error = userResult.error.message) }
                }
                else -> {
                     _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }
    
    private suspend fun observeCart(userId: String) {
        try {
            getCartUseCase(userId, observe = true).collectLatest { cart ->
                if (cart != null) {
                    val uiItems = cart.items.map { it.toUiModel() }
                    _state.update { current ->
                        current.copy(
                            items = uiItems,
                            subtotal = cart.subtotal,
                            // Ensure calculation consistency
                            total = cart.subtotal + current.shipping + current.tax, 
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false, items = emptyList()) }
                }
            }
        } catch (e: Exception) {
            Log.e("CartViewModel", "❌ Error observing cart", e)
             _state.update { it.copy(isLoading = false, error = e.message) }
        }
    }

    // Mapping Extension
    private fun CartItem.toUiModel(): CartItemUi {
        return CartItemUi(
            productId = this.productId,
            name = this.productName,
            price = this.price,
            imageUrl = this.productImage,
            quantity = this.quantity,
            lineId = this.id, // ID from Shared is the Line ID
            size = this.size,
            color = this.color,
            attributes = this.attributes,
            promoterUid = this.promoterUid,
            reelId = this.reelId
        )
    }
    
    private fun CartItemUi.toDomainModel(): CartItem {
        return CartItem(
            id = this.lineId,
            productId = this.productId,
            productName = this.name,
            productImage = this.imageUrl,
            price = this.price,
            quantity = this.quantity,
            size = this.size,
            color = this.color,
            attributes = this.attributes,
            promoterUid = this.promoterUid,
            reelId = this.reelId
        )
    }

    fun addToCart(item: CartItemUi) { // Accepts UI model, converts to Domain
        val userId = currentUserId
        if (userId == null) {
            _state.update { it.copy(error = "User not authenticated") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val domainItem = item.toDomainModel()
            
            when (val result = addToCartUseCase(userId, domainItem)) {
                is Result.Success -> {
                    Log.d("CartViewModel", "✅ Item added to cart")
                    // Flow will update UI automatically
                     delay(300)
                     _state.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    Log.e("CartViewModel", "❌ Failed to add item: ${result.error.message}")
                    _state.update { it.copy(isLoading = false, error = result.error.message) }
                }
                else -> {}
            }
        }
    }

    fun updateQuantity(lineId: String, quantity: Int) {
        val userId = currentUserId
        if (userId == null) return

        if (quantity <= 0) {
            removeItem(lineId)
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = updateCartItemUseCase(userId, lineId, quantity)) {
                is Result.Success -> {
                    Log.d("CartViewModel", "✅ Quantity updated")
                     delay(300)
                     _state.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.error.message) }
                }
                else -> {}
            }
        }
    }

    fun removeItem(lineId: String) {
        val userId = currentUserId
        if (userId == null) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = removeFromCartUseCase(userId, lineId)) {
                is Result.Success -> {
                     delay(300)
                     _state.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.error.message) }
                }
                else -> {}
            }
        }
    }

    fun clearCart() {
        val userId = currentUserId
        if (userId == null) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = clearCartUseCase(userId)) {
                is Result.Success -> {
                     delay(300)
                     _state.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.error.message) }
                }
                else -> {}
            }
        }
    }
    
    // Helper to allow checking if product is in cart (Reactive)
    // This functionality usually requires a specific flow from repository or computing it from state
    // For now, we can compute it from the state flow if needed, but original VM returned a Flow<Boolean>
    fun isProductInCart(productId: String): kotlinx.coroutines.flow.Flow<Boolean> {
        return state.map { cartState ->
            cartState.items.any { it.productId == productId }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    fun refreshCart() {
        val userId = currentUserId
        if (userId != null) {
            viewModelScope.launch { observeCart(userId) }
        }
    }
}
