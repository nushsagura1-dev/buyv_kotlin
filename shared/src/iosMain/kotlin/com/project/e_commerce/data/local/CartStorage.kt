package com.project.e_commerce.data.local

import platform.Foundation.NSUserDefaults

/**
 * Implémentation iOS de CartStorage utilisant NSUserDefaults.
 */
actual class CartStorage {
    
    private val defaults = NSUserDefaults.standardUserDefaults
    
    actual fun saveCart(userId: String, cartJson: String) {
        defaults.setObject(cartJson, "cart_$userId")
    }
    
    actual fun getCart(userId: String): String? {
        return defaults.stringForKey("cart_$userId")
    }
    
    actual fun clearCart(userId: String) {
        defaults.removeObjectForKey("cart_$userId")
    }
}
