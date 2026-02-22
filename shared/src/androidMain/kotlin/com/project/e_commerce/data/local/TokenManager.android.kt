package com.project.e_commerce.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Implémentation Android du TokenManager.
 * 
 * Utilise EncryptedSharedPreferences pour stocker les tokens de manière sécurisée.
 * Les données sont encryptées à l'aide du MasterKey Android.
 */
actual class TokenManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "auth_tokens"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRATION = "token_expiration"
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences if encryption fails
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    actual fun saveAccessToken(token: String) {
        sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }
    
    actual fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }
    
    actual fun saveRefreshToken(token: String) {
        sharedPreferences.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }
    
    actual fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }
    
    actual fun clearTokens() {
        sharedPreferences.edit().clear().apply()
    }
    
    actual fun saveTokenExpiration(expiresAt: Long) {
        sharedPreferences.edit().putLong(KEY_TOKEN_EXPIRATION, expiresAt).apply()
    }
    
    actual fun getTokenExpiration(): Long? {
        val expiration = sharedPreferences.getLong(KEY_TOKEN_EXPIRATION, -1L)
        return if (expiration == -1L) null else expiration
    }
    
    actual fun isTokenExpired(): Boolean {
        val expiration = getTokenExpiration() ?: return true
        val currentTime = System.currentTimeMillis() / 1000 // Convert to seconds
        return currentTime >= expiration
    }
}
