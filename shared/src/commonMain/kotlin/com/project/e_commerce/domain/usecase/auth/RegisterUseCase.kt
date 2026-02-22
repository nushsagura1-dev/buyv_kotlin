package com.project.e_commerce.domain.usecase.auth

import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserProfile
import com.project.e_commerce.domain.repository.AuthRepository

/**
 * Use case pour l'inscription d'un nouvel utilisateur.
 * 
 * Crée un compte utilisateur et son profil associé.
 */
class RegisterUseCase(
    private val authRepository: AuthRepository
) {
    /**
     * Exécute l'inscription.
     * 
     * @param email Email de l'utilisateur
     * @param password Mot de passe
     * @param displayName Nom d'affichage
     * @return Result contenant le profil utilisateur créé ou une erreur
     */
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String
    ): Result<UserProfile> {
        return authRepository.signUpWithEmail(email, password, displayName)
    }
}
