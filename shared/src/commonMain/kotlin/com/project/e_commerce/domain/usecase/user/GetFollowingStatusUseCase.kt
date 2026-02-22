package com.project.e_commerce.domain.usecase.user

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.FollowingStatus
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.UserRepository

/**
 * Use Case pour vérifier le statut de suivi entre deux utilisateurs.
 * 
 * Utile pour afficher le bouton "Follow" ou "Following" sur un profil.
 * 
 * @property userRepository Repository des utilisateurs
 */
class GetFollowingStatusUseCase(
    private val userRepository: UserRepository
) {
    /**
     * Récupère le statut de suivi.
     * 
     * @param currentUserId Identifiant de l'utilisateur actuel
     * @param targetUserId Identifiant de l'utilisateur cible
     * @return Result contenant le statut de suivi
     */
    suspend operator fun invoke(currentUserId: String, targetUserId: String): Result<FollowingStatus> {
        // Validation
        if (currentUserId.isBlank()) {
            return Result.Error(ApiError.ValidationError("Current user ID cannot be empty"))
        }
        
        if (targetUserId.isBlank()) {
            return Result.Error(ApiError.ValidationError("Target user ID cannot be empty"))
        }
        
        // Si c'est le même utilisateur, retourner un statut spécial
        if (currentUserId == targetUserId) {
            return Result.Success(
                FollowingStatus(
                    isFollowing = false,
                    isFollowedBy = false
                )
            )
        }
        
        return userRepository.getFollowingStatus(currentUserId, targetUserId)
    }
}
