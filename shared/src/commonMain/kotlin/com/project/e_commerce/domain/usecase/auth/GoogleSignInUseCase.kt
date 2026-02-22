package com.project.e_commerce.domain.usecase.auth

import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserProfile
import com.project.e_commerce.domain.repository.AuthRepository

/**
 * Use case pour la connexion via Google Sign-In.
 * 
 * Vérifie le token Google auprès du backend et retourne le profil utilisateur.
 * Les comptes admin ne peuvent PAS se connecter via Google (protection backend).
 */
class GoogleSignInUseCase(
    private val authRepository: AuthRepository
) {
    /**
     * Exécute la connexion Google.
     * 
     * @param idToken Le token ID obtenu du SDK Google côté client
     * @return Result contenant le profil utilisateur ou une erreur
     */
    suspend operator fun invoke(idToken: String): Result<UserProfile> {
        return authRepository.signInWithGoogle(idToken)
    }
}
