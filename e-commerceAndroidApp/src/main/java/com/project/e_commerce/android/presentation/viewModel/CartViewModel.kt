package com.project.e_commerce.android.presentation.viewModel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update


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
    val total: Double = 0.0
)

class CartViewModel : ViewModel() {

    // ✅ بدل ما يكون عندك List فقط، هنخلي الحالة كاملة
    private val _state = MutableStateFlow(CartState())
    val state: StateFlow<CartState> = _state

    private fun isSameLine(a: CartItem, b: CartItem): Boolean {
        return a.productId == b.productId &&
                a.variantId == b.variantId &&
                a.size == b.size &&
                a.color == b.color &&
                a.attributes == b.attributes
    }

    fun addToCart(item: CartItem) {
        _state.update { current ->
            val newList = current.items.toMutableList()
            val idx = newList.indexOfFirst { isSameLine(it, item) }
            if (idx >= 0) {
                val existing = newList[idx]
                newList[idx] = existing.copy(quantity = existing.quantity + item.quantity)
            } else {
                newList.add(item)
            }
            calculateTotals(newList, current.shipping, current.tax)
        }
    }


    fun updateQuantity(lineId: String, quantity: Int) {
        _state.update { current ->
            val newList = current.items
                .map { if (it.lineId == lineId) it.copy(quantity = quantity) else it }
                .filter { it.quantity > 0 }
            calculateTotals(newList, current.shipping, current.tax)
        }
    }

    fun removeItem(lineId: String) {
        _state.update { current ->
            val newList = current.items.filter { it.lineId != lineId }
            calculateTotals(newList, current.shipping, current.tax)
        }
    }

    fun clearCart() {
        _state.value = CartState()
    }

    private fun calculateTotals(items: List<CartItem>, shipping: Double, tax: Double): CartState {
        val subtotal = items.sumOf { it.price * it.quantity }
        val total = subtotal + shipping + tax
        return CartState(
            items = items,
            subtotal = subtotal,
            shipping = shipping,
            tax = tax,
            total = total
        )
    }
}
