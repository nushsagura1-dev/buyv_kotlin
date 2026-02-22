package com.project.e_commerce.domain.repository

import com.project.e_commerce.domain.model.FollowRelationship
import com.project.e_commerce.domain.model.FollowingStatus
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserFollowModel
import com.project.e_commerce.domain.model.UserPost
import com.project.e_commerce.domain.model.UserProfile

/**
 * Interface du repository utilisateur.
 * 
 * Gère les profils utilisateur, les follows, et les interactions sociales.
 */
interface UserRepository {
    
    /**
     * Récupère le profil d'un utilisateur par son ID.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result contenant le profil utilisateur
     */
    suspend fun getUserProfile(userId: String): Result<UserProfile>
    
    /**
     * Met à jour le profil utilisateur.
     * 
     * @param userProfile Nouveau profil (les champs doivent être validés)
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit>
    
    /**
     * Recherche des utilisateurs par username.
     * 
     * @param query Requête de recherche (sera sanitisée)
     * @return Result contenant la liste d'utilisateurs correspondants
     */
    suspend fun searchUsers(query: String): Result<List<UserProfile>>
    
    /**
     * Suit un utilisateur.
     * 
     * @param followerId Identifiant de l'utilisateur qui suit
     * @param followedId Identifiant de l'utilisateur suivi
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun followUser(followerId: String, followedId: String): Result<Unit>
    
    /**
     * Ne plus suivre un utilisateur.
     * 
     * @param followerId Identifiant de l'utilisateur qui arrête de suivre
     * @param followedId Identifiant de l'utilisateur qui n'est plus suivi
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun unfollowUser(followerId: String, followedId: String): Result<Unit>
    
    /**
     * Récupère la liste des followers d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result contenant la liste de followers
     */
    suspend fun getFollowers(userId: String): Result<List<UserFollowModel>>
    
    /**
     * Récupère la liste des utilisateurs suivis.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result contenant la liste des utilisateurs suivis
     */
    suspend fun getFollowing(userId: String): Result<List<UserFollowModel>>
    
    /**
     * Récupère le statut de suivi entre deux utilisateurs.
     * 
     * @param currentUserId Identifiant de l'utilisateur actuel
     * @param targetUserId Identifiant de l'utilisateur cible
     * @return Result contenant le statut de suivi
     */
    suspend fun getFollowingStatus(currentUserId: String, targetUserId: String): Result<FollowingStatus>
    
    /**
     * Récupère les posts d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result contenant la liste de posts
     */
    suspend fun getUserPosts(userId: String): Result<List<UserPost>>
    
    /**
     * Incrémente le compteur de likes d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun incrementLikesCount(userId: String): Result<Unit>
    
    /**
     * Décrémente le compteur de likes d'un utilisateur.
     * 
     * @param userId Identifiant de l'utilisateur
     * @return Result<Unit> indiquant le succès ou l'échec
     */
    suspend fun decrementLikesCount(userId: String): Result<Unit>
    
    /**
     * Delete the current user's account permanently.
     * This will delete all associated data (posts, comments, likes, follows, orders, etc.).
     * Required for App Store and Play Store compliance (GDPR/CCPA).
     * 
     * @return Result containing DeleteAccountResponseDto with confirmation
     */
    suspend fun deleteAccount(): com.project.e_commerce.data.remote.dto.DeleteAccountResponseDto
}
