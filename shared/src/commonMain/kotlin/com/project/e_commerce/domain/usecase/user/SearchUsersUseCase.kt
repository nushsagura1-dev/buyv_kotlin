package com.project.e_commerce.domain.usecase.user

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserProfile
import com.project.e_commerce.domain.repository.UserRepository

/**
 * Use Case pour rechercher des utilisateurs.
 * 
 * Implémente la validation et la sanitisation de la requête de recherche.
 * 
 * @property userRepository Repository des utilisateurs
 */
class SearchUsersUseCase(
    private val userRepository: UserRepository
) {
    /**
     * Recherche des utilisateurs par username.
     * 
     * @param query Requête de recherche
     * @return Result contenant la liste d'utilisateurs trouvés
     */
    suspend operator fun invoke(query: String): Result<List<UserProfile>> {
        // Si query vide, retourner liste vide
        if (query.isBlank()) {
            return Result.Success(emptyList())
        }
        
        // Validation longueur minimum
        if (query.length < 2) {
            return Result.Error(
                ApiError.ValidationError("Search query must be at least 2 characters")
            )
        }
        
        // Limitation de la longueur pour éviter les abus
        if (query.length > 100) {
            return Result.Error(
                ApiError.ValidationError("Search query cannot exceed 100 characters")
            )
        }
        
        // Sanitisation basique (le repository fera aussi sa validation)
        val sanitizedQuery = query.trim()
        
        return userRepository.searchUsers(sanitizedQuery)
    }
}
