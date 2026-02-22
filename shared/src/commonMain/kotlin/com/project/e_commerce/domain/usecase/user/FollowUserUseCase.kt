package com.project.e_commerce.domain.usecase.user

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.UserRepository

/**
 * Use Case pour suivre un utilisateur.
 * 
 * Implémente la logique métier pour la fonctionnalité de suivi (follow).
 * 
 * @property userRepository Repository des utilisateurs
 */
class FollowUserUseCase(
    private val userRepository: UserRepository
) {
    /**
     * Suit un utilisateur.
     * 
     * @param followerId Identifiant de l'utilisateur qui suit
     * @param followedId Identifiant de l'utilisateur à suivre
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
        
        // Ne peut pas se suivre soi-même
        if (followerId == followedId) {
            return Result.Error(ApiError.ValidationError("Cannot follow yourself"))
        }
        
        return userRepository.followUser(followerId, followedId)
    }
}
