package com.project.e_commerce.domain.usecase.user

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserFollowModel
import com.project.e_commerce.domain.repository.UserRepository

/**
 * Use Case pour récupérer la liste des followers d'un utilisateur.
 * 
 * @property userRepository Repository des utilisateurs
 */
class GetFollowersUseCase(
    private val userRepository: UserRepository
) {
    /**
     * Récupère la liste des followers.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result contenant la liste de followers
     */
    suspend operator fun invoke(userId: String): Result<List<UserFollowModel>> {
        // Validation
        if (userId.isBlank()) {
            return Result.Error(ApiError.ValidationError("User ID cannot be empty"))
        }
        
        return userRepository.getFollowers(userId)
    }
}
