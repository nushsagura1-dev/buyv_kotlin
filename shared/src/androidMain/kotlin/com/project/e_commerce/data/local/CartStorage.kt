package com.project.e_commerce.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Implémentation Android de CartStorage utilisant SharedPreferences.
 */
actual class CartStorage(private val context: Context) {
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("cart_storage", Context.MODE_PRIVATE)
    }
    
    actual fun saveCart(userId: String, cartJson: String) {
        prefs.edit().putString("cart_$userId", cartJson).apply()
    }
    
    actual fun getCart(userId: String): String? {
        return prefs.getString("cart_$userId", null)
    }
    
    actual fun clearCart(userId: String) {
        prefs.edit().remove("cart_$userId").apply()
    }
}
