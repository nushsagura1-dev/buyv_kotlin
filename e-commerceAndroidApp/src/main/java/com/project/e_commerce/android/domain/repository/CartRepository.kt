package com.project.e_commerce.android.domain.repository

import com.project.e_commerce.android.presentation.viewModel.CartItemUi
import com.project.e_commerce.android.data.model.CartStats
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    suspend fun addToCart(userId: String, cartItem: CartItemUi): Result<Unit>
    suspend fun removeFromCart(userId: String, lineId: String): Result<Unit>
    suspend fun removeFromCartByProductId(userId: String, productId: String): Result<Unit>
    suspend fun updateCartItemQuantity(userId: String, lineId: String, quantity: Int): Result<Unit>
    fun getUserCart(userId: String): Flow<List<CartItemUi>>
    fun getProductCartStats(productId: String): Flow<CartStats>
    fun isProductInUserCart(userId: String, productId: String): Flow<Boolean>
    suspend fun clearUserCart(userId: String): Result<Unit>
    suspend fun incrementProductCartCount(productId: String): Result<Unit>
    suspend fun decrementProductCartCount(productId: String): Result<Unit>
}
