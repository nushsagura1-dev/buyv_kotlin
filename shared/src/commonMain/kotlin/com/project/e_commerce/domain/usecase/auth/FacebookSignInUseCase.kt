package com.project.e_commerce.domain.usecase.auth

import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserProfile
import com.project.e_commerce.domain.repository.AuthRepository

/**
 * AUTH-002 — Use case pour la connexion via Facebook.
 *
 * Vérifie l'access token Facebook auprès du backend et retourne
 * le profil utilisateur BuyV. Si le compte n'existe pas encore,
 * il est créé automatiquement côté backend.
 */
class FacebookSignInUseCase(
    private val authRepository: AuthRepository
) {
    /**
     * Exécute la connexion Facebook.
     *
     * @param accessToken L'access token obtenu du SDK Facebook côté client
     * @return Result contenant le profil utilisateur ou une erreur
     */
    suspend operator fun invoke(accessToken: String): Result<UserProfile> {
        return authRepository.signInWithFacebook(accessToken)
    }
}
