package com.project.e_commerce.domain.usecase.user

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserFollowModel
import com.project.e_commerce.domain.repository.UserRepository

/**
 * Use Case pour récupérer la liste des utilisateurs suivis.
 * 
 * @property userRepository Repository des utilisateurs
 */
class GetFollowingUseCase(
    private val userRepository: UserRepository
) {
    /**
     * Récupère la liste des utilisateurs suivis.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result contenant la liste des utilisateurs suivis
     */
    suspend operator fun invoke(userId: String): Result<List<UserFollowModel>> {
        // Validation
        if (userId.isBlank()) {
            return Result.Error(ApiError.ValidationError("User ID cannot be empty"))
        }
        
        return userRepository.getFollowing(userId)
    }
}
