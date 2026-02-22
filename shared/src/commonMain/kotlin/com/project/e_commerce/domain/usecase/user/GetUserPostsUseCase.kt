package com.project.e_commerce.domain.usecase.user

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserPost
import com.project.e_commerce.domain.repository.UserRepository

/**
 * Use Case pour récupérer les posts d'un utilisateur.
 * 
 * Utilisé pour afficher le feed de posts sur le profil utilisateur.
 * 
 * @property userRepository Repository des utilisateurs
 */
class GetUserPostsUseCase(
    private val userRepository: UserRepository
) {
    /**
     * Récupère les posts d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result contenant la liste de posts
     */
    suspend operator fun invoke(userId: String): Result<List<UserPost>> {
        // Validation
        if (userId.isBlank()) {
            return Result.Error(ApiError.ValidationError("User ID cannot be empty"))
        }
        
        return userRepository.getUserPosts(userId)
    }
}
