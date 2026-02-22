package com.project.e_commerce.data.local

/**
 * Gestionnaire de token JWT.
 * 
 * Stocke et récupère le token d'authentification de manière sécurisée.
 * Les implémentations platform-specific (Android/iOS) doivent utiliser
 * le stockage sécurisé approprié :
 * - Android : EncryptedSharedPreferences
 * - iOS : Keychain
 */
expect class TokenManager {
    /**
     * Sauvegarde le token d'accès.
     * 
     * @param token Token JWT
     */
    fun saveAccessToken(token: String)
    
    /**
     * Récupère le token d'accès.
     * 
     * @return Token JWT ou null si non disponible
     */
    fun getAccessToken(): String?
    
    /**
     * Sauvegarde le refresh token.
     * 
     * @param token Refresh token
     */
    fun saveRefreshToken(token: String)
    
    /**
     * Récupère le refresh token.
     * 
     * @return Refresh token ou null si non disponible
     */
    fun getRefreshToken(): String?
    
    /**
     * Supprime tous les tokens stockés.
     * Utilisé lors de la déconnexion.
     */
    fun clearTokens()
    
    /**
     * Sauvegarde l'expiration du token (timestamp).
     * 
     * @param expiresAt Timestamp d'expiration en secondes
     */
    fun saveTokenExpiration(expiresAt: Long)
    
    /**
     * Récupère le timestamp d'expiration.
     * 
     * @return Timestamp ou null
     */
    fun getTokenExpiration(): Long?
    
    /**
     * Vérifie si le token est expiré.
     * 
     * @return true si expiré ou non disponible
     */
    fun isTokenExpired(): Boolean
}
