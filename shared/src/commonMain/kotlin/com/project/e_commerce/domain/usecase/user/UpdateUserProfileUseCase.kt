package com.project.e_commerce.domain.usecase.user

import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserProfile
import com.project.e_commerce.domain.repository.UserRepository

/**
 * Use Case pour mettre à jour le profil utilisateur.
 * 
 * Valide les données avant de les envoyer au repository.
 * 
 * @property userRepository Repository des utilisateurs
 */
class UpdateUserProfileUseCase(
    private val userRepository: UserRepository
) {
    /**
     * Met à jour le profil utilisateur.
     * 
     * @param userProfile Nouveau profil utilisateur
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend operator fun invoke(userProfile: UserProfile): Result<Unit> {
        // Validation de base
        if (userProfile.uid.isBlank()) {
            return Result.Error(ApiError.ValidationError("User ID cannot be empty"))
        }
        
        if (userProfile.username.isBlank()) {
            return Result.Error(ApiError.ValidationError("Username cannot be empty"))
        }
        
        if (userProfile.username.length < 3) {
            return Result.Error(ApiError.ValidationError("Username must be at least 3 characters"))
        }
        
        if (userProfile.username.length > 50) {
            return Result.Error(ApiError.ValidationError("Username cannot exceed 50 characters"))
        }
        
        // Validation username (alphanumeric + underscore)
        if (!userProfile.username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            return Result.Error(
                ApiError.ValidationError("Username can only contain letters, numbers, and underscores")
            )
        }
        
        // Validation bio (optionnelle mais limitée)
        if (userProfile.bio.length > 500) {
            return Result.Error(ApiError.ValidationError("Bio cannot exceed 500 characters"))
        }
        
        return userRepository.updateUserProfile(userProfile)
    }
}
