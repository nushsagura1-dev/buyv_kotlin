package com.project.e_commerce.data.local

import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserProfile
import com.project.e_commerce.domain.repository.AuthRepository

/**
 * Provider pour obtenir les informations de l'utilisateur actuellement connecté.
 * 
 * Remplace Firebase Auth currentUser par un appel backend.
 * Cache le profil utilisateur pour éviter des appels répétés.
 */
interface CurrentUserProvider {
    /**
     * Récupère l'UID de l'utilisateur actuellement connecté
     * @return UID ou null si non connecté
     */
    suspend fun getCurrentUserId(): String?
    
    /**
     * Récupère le profil complet de l'utilisateur actuellement connecté
     * @return UserProfile ou null si non connecté
     */
    suspend fun getCurrentUser(): UserProfile?
    
    /**
     * Vérifie si un utilisateur est connecté
     * @return true si connecté, false sinon
     */
    suspend fun isAuthenticated(): Boolean
    
    /**
     * Efface le cache du profil utilisateur
     * À appeler après logout ou mise à jour du profil
     */
    fun clearCache()
}

/**
 * Implémentation du CurrentUserProvider utilisant AuthRepository (backend)
 */
class CurrentUserProviderImpl(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : CurrentUserProvider {
    
    private var cachedUser: UserProfile? = null
    private var cacheTimestamp: Long = 0
    private val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    
    override suspend fun getCurrentUserId(): String? {
        val user = getCurrentUser()
        return user?.uid
    }
    
    override suspend fun getCurrentUser(): UserProfile? {
        // Check if token exists first (quick check)
        if (tokenManager.getAccessToken() == null) {
            cachedUser = null
            return null
        }
        
        // Check cache validity
        val currentTime = System.currentTimeMillis()
        if (cachedUser != null && (currentTime - cacheTimestamp) < CACHE_DURATION_MS) {
            return cachedUser
        }
        
        // Fetch from backend
        return when (val result = authRepository.getCurrentUser()) {
            is Result.Success -> {
                cachedUser = result.data
                cacheTimestamp = currentTime
                cachedUser
            }
            is Result.Error -> {
                // Token might be expired or invalid
                cachedUser = null
                null
            }
            is Result.Loading -> {
                // Should not happen in getCurrentUser since it's suspend
                // Return cached value if available
                cachedUser
            }
        }
    }
    
    override suspend fun isAuthenticated(): Boolean {
        // Quick check: token exists
        if (tokenManager.getAccessToken() == null) {
            return false
        }
        
        // Verify with backend
        return getCurrentUser() != null
    }
    
    override fun clearCache() {
        cachedUser = null
        cacheTimestamp = 0
    }
}
