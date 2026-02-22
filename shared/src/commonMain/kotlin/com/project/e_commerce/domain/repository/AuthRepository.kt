package com.project.e_commerce.domain.repository

import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserProfile

/**
 * Interface du repository d'authentification.
 * 
 * Définit les opérations d'authentification et de gestion de session utilisateur.
 * Les implémentations platform-specific (Android/iOS) doivent être fournies via expect/actual.
 */
interface AuthRepository {
    
    /**
     * Inscrit un nouvel utilisateur avec email et mot de passe.
     * 
     * @param email Email de l'utilisateur (doit être validé avant)
     * @param password Mot de passe (doit être validé avant)
     * @param displayName Nom d'affichage
     * @return Result contenant le UserProfile ou une erreur
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<UserProfile>
    
    /**
     * Connecte un utilisateur avec email et mot de passe.
     * 
     * @param email Email de l'utilisateur
     * @param password Mot de passe
     * @return Result contenant le UserProfile ou une erreur
     */
    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<UserProfile>
    
    /**
     * Connecte un utilisateur avec Google.
     * 
     * @param idToken Le token ID Google obtenu côté client
     * @return Result contenant le UserProfile ou une erreur
     */
    suspend fun signInWithGoogle(idToken: String = ""): Result<UserProfile>
    
    /**
     * Déconnecte l'utilisateur actuel.
     * 
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun signOut(): Result<Unit>
    
    /**
     * Récupère l'utilisateur actuellement connecté.
     * 
     * @return Result contenant le UserProfile ou null si non connecté
     */
    suspend fun getCurrentUser(): Result<UserProfile?>
    
    /**
     * Vérifie si un utilisateur est connecté.
     * 
     * @return true si un utilisateur est connecté, false sinon
     */
    suspend fun isUserSignedIn(): Boolean
    
    /**
     * Envoie un email de réinitialisation de mot de passe.
     * 
     * @param email Email de l'utilisateur
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    
    /**
     * Confirme la réinitialisation du mot de passe avec un token et un nouveau mot de passe.
     *
     * @param token Token de réinitialisation reçu par email
     * @param newPassword Nouveau mot de passe
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun confirmPasswordReset(token: String, newPassword: String): Result<Unit>

    /**
     * 
     * @param userProfile Nouveau profil utilisateur
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit>
    
    /**
     * Supprime le compte utilisateur actuel.
     * 
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun deleteAccount(): Result<Unit>
}
