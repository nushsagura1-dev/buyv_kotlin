package com.project.e_commerce.domain.usecase.user

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.UserRepository

/**
 * Use Case pour ne plus suivre un utilisateur.
 * 
 * Implémente la logique métier pour la fonctionnalité de unfollow.
 * 
 * @property userRepository Repository des utilisateurs
 */
class UnfollowUserUseCase(
    private val userRepository: UserRepository
) {
    /**
     * Arrête de suivre un utilisateur.
     * 
     * @param followerId Identifiant de l'utilisateur qui arrête de suivre
     * @param followedId Identifiant de l'utilisateur à ne plus suivre
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend operator fun invoke(followerId: String, followedId: String): Result<Unit> {
        // Validation
        if (followerId.isBlank()) {
            return Result.Error(ApiError.ValidationError("Follower ID cannot be empty"))
        }
        
        if (followedId.isBlank()) {
            return Result.Error(ApiError.ValidationError("Followed ID cannot be empty"))
        }
        
        return userRepository.unfollowUser(followerId, followedId)
    }
}
