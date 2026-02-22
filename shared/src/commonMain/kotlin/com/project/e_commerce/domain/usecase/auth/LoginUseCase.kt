package com.project.e_commerce.domain.usecase.auth

import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserProfile
import com.project.e_commerce.domain.repository.AuthRepository

/**
 * Use case pour la connexion utilisateur avec email/password.
 * 
 * Encapsule la logique d'authentification et gère les erreurs.
 */
class LoginUseCase(
    private val authRepository: AuthRepository
) {
    /**
     * Exécute la connexion.
     * 
     * @param email Email de l'utilisateur
     * @param password Mot de passe
     * @return Result contenant le profil utilisateur ou une erreur
     */
    suspend operator fun invoke(email: String, password: String): Result<UserProfile> {
        return authRepository.signInWithEmail(email, password)
    }
}
