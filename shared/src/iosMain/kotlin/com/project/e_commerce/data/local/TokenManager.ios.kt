package com.project.e_commerce.data.local

import platform.Foundation.*
import platform.Security.*

/**
 * iOS implementation of TokenManager using Keychain for secure storage
 * Provides secure storage for JWT tokens using iOS Keychain Services
 */
actual class TokenManager {
    
    private val serviceName = "com.project.e_commerce.tokens"
    
    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRATION = "token_expiration"
    }
    
    actual fun saveAccessToken(token: String) {
        saveToKeychain(KEY_ACCESS_TOKEN, token)
    }
    
    actual fun getAccessToken(): String? {
        return getFromKeychain(KEY_ACCESS_TOKEN)
    }
    
    actual fun saveRefreshToken(token: String) {
        saveToKeychain(KEY_REFRESH_TOKEN, token)
    }
    
    actual fun getRefreshToken(): String? {
        return getFromKeychain(KEY_REFRESH_TOKEN)
    }
    
    actual fun clearTokens() {
        deleteFromKeychain(KEY_ACCESS_TOKEN)
        deleteFromKeychain(KEY_REFRESH_TOKEN)
        deleteFromKeychain(KEY_TOKEN_EXPIRATION)
    }
    
    actual fun saveTokenExpiration(expirationTime: Long) {
        saveToKeychain(KEY_TOKEN_EXPIRATION, expirationTime.toString())
    }
    
    actual fun getTokenExpiration(): Long? {
        return getFromKeychain(KEY_TOKEN_EXPIRATION)?.toLongOrNull()
    }
    
    actual fun isTokenExpired(): Boolean {
        val expiration = getTokenExpiration() ?: return true
        val currentTime = NSDate().timeIntervalSince1970.toLong()
        return currentTime >= expiration
    }
    
    /**
     * Save string value to iOS Keychain
     */
    private fun saveToKeychain(key: String, value: String) {
        // First, delete any existing value
        deleteFromKeychain(key)
        
        // Prepare query dictionary
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceName,
            kSecAttrAccount to key,
            kSecValueData to (value as NSString).dataUsingEncoding(NSUTF8StringEncoding),
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        )
        
        // Add to keychain
        val status = SecItemAdd(query as CFDictionaryRef, null)
        
        if (status != errSecSuccess) {
            println("TokenManager: Failed to save $key to Keychain. Status: $status")
        }
    }
    
    /**
     * Retrieve string value from iOS Keychain
     */
    private fun getFromKeychain(key: String): String? {
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceName,
            kSecAttrAccount to key,
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne
        )
        
        memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
            
            if (status == errSecSuccess) {
                val data = result.value as? NSData
                return data?.let {
                    NSString.create(it, NSUTF8StringEncoding) as String
                }
            }
        }
        
        return null
    }
    
    /**
     * Delete value from iOS Keychain
     */
    private fun deleteFromKeychain(key: String) {
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceName,
            kSecAttrAccount to key
        )
        
        SecItemDelete(query as CFDictionaryRef)
    }
}
