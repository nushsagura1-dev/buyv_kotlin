package com.project.e_commerce.domain.usecase.auth

import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.repository.AuthRepository

/**
 * Use case pour la déconnexion utilisateur.
 * 
 * Déconnecte l'utilisateur actuel et nettoie la session.
 */
class LogoutUseCase(
    private val authRepository: AuthRepository
) {
    /**
     * Exécute la déconnexion.
     * 
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.signOut()
    }
}
