package com.project.e_commerce.android.domain.repository

import com.project.e_commerce.android.domain.model.UserProfile
import com.project.e_commerce.android.domain.model.UserPost
import com.project.e_commerce.android.domain.model.UserProduct
import com.project.e_commerce.android.domain.model.UserInteraction
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    // User Profile Operations
    suspend fun getUserProfile(uid: String): Result<UserProfile>
    suspend fun updateUserProfile(profile: UserProfile): Result<UserProfile>
    suspend fun createUserProfile(profile: UserProfile): Result<UserProfile>
    fun getUserProfileFlow(uid: String): Flow<UserProfile?>
    
    // User Posts Operations
    suspend fun getUserPosts(uid: String): Result<List<UserPost>>
    suspend fun getUserReels(uid: String): Result<List<UserPost>>
    suspend fun getUserProducts(uid: String): Result<List<UserProduct>>
    fun getUserPostsFlow(uid: String): Flow<List<UserPost>>
    
    // User Interactions Operations
    suspend fun getUserLikedPosts(uid: String): Result<List<UserPost>>
    suspend fun getUserLikedProducts(uid: String): Result<List<UserProduct>>
    suspend fun getUserBookmarkedPosts(uid: String): Result<List<UserPost>>
    suspend fun getUserBookmarkedProducts(uid: String): Result<List<UserProduct>>
    fun getUserLikedContentFlow(uid: String): Flow<List<UserPost>>
    fun getUserBookmarkedContentFlow(uid: String): Flow<List<UserPost>>
    
    // Follow Operations
    suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit>
    suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit>
    suspend fun getFollowers(userId: String): Result<List<String>>
    suspend fun getFollowing(userId: String): Result<List<String>>
    
    // Profile Image Operations
    suspend fun uploadProfileImage(uid: String, imageUri: String): Result<String>
    suspend fun deleteProfileImage(uid: String): Result<Unit>
}
