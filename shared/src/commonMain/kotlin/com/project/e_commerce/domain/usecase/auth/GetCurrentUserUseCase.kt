package com.project.e_commerce.domain.usecase.auth

import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserProfile
import com.project.e_commerce.domain.repository.AuthRepository

/**
 * Use case pour récupérer l'utilisateur actuellement connecté.
 * 
 * Vérifie si un utilisateur est connecté et récupère son profil.
 */
class GetCurrentUserUseCase(
    private val authRepository: AuthRepository
) {
    /**
     * Exécute la récupération du profil utilisateur actuel.
     * 
     * @return Result contenant le profil utilisateur ou null si non connecté
     */
    suspend operator fun invoke(): Result<UserProfile?> {
        return authRepository.getCurrentUser()
    }
    
    /**
     * Vérifie rapidement si un utilisateur est connecté.
     * 
     * @return true si un utilisateur est connecté, false sinon
     */
    suspend fun isUserSignedIn(): Boolean {
        return authRepository.isUserSignedIn()
    }
}
