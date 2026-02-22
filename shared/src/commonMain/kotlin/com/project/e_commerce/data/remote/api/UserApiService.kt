package com.project.e_commerce.data.remote.api

import com.project.e_commerce.data.remote.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * User API service
 * Handles user-related endpoints
 */
class UserApiService(private val httpClient: HttpClient) {
    
    /**
     * Search users by query
     * GET /users/search?q={query}
     */
    suspend fun searchUsers(query: String): List<UserDto> {
        return httpClient.get("users/search") {
            parameter("q", query)
        }.body()
    }
    
    /**
     * Get user by UID
     * GET /users/{uid}
     */
    suspend fun getUserById(userId: String): UserDto {
        return httpClient.get("users/$userId").body()
    }
    
    /**
     * Get user statistics
     * GET /users/{uid}/stats
     */
    suspend fun getUserStats(userId: String): UserStatsDto {
        return httpClient.get("users/$userId/stats").body()
    }
    
    /**
     * Update user profile
     * PUT /users/{uid}
     */
    suspend fun updateUserProfile(userId: String, updateDto: UserUpdateDto): UserDto {
        return httpClient.put("users/$userId") {
            contentType(ContentType.Application.Json)
            setBody(updateDto)
        }.body()
    }
    
    /**
     * Follow a user
     * POST /users/{follower_id}/follow/{followed_id}
     */
    suspend fun followUser(followerId: String, followedId: String): MessageResponseDto {
        return httpClient.post("users/$followerId/follow/$followedId") {
            contentType(ContentType.Application.Json)
        }.body()
    }
    
    /**
     * Unfollow a user
     * DELETE /users/{follower_id}/unfollow/{followed_id}
     */
    suspend fun unfollowUser(followerId: String, followedId: String): MessageResponseDto {
        return httpClient.delete("users/$followerId/unfollow/$followedId").body()
    }
    
    /**
     * Get list of followers for a user
     * GET /users/{user_id}/followers
     */
    suspend fun getFollowers(userId: String): List<UserFollowInfoDto> {
        return httpClient.get("users/$userId/followers").body()
    }
    
    /**
     * Get list of users that a user is following
     * GET /users/{user_id}/following
     */
    suspend fun getFollowing(userId: String): List<UserFollowInfoDto> {
        return httpClient.get("users/$userId/following").body()
    }
    
    /**
     * Get following status between two users
     * GET /users/{current_user_id}/follow-status/{target_user_id}
     */
    suspend fun getFollowingStatus(currentUserId: String, targetUserId: String): FollowingStatusDto {
        return httpClient.get("users/$currentUserId/follow-status/$targetUserId").body()
    }
    
    /**
     * Get posts by a specific user
     * GET /posts/user/{uid}
     */
    suspend fun getUserPosts(userId: String): List<UserPostDto> {
        return httpClient.get("posts/user/$userId").body()
    }
    
    /**
     * Update FCM token for push notifications
     * POST /users/me/fcm-token
     */
    suspend fun updateFcmToken(token: String) {
        httpClient.post("users/me/fcm-token") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("fcmToken" to token))
        }
    }

    /**
     * Delete current user's account permanently
     * DELETE /users/me
     * 
     * This will permanently delete the user account and all associated data:
     * - Posts, reels, and photos
     * - Comments and likes
     * - Follows and followers
     * - Orders and commissions
     * - Notifications
     * 
     * Required for App Store and Play Store compliance (GDPR/CCPA).
     * 
     * @return MessageResponseDto with confirmation message and deleted user ID
     */
    suspend fun deleteAccount(): DeleteAccountResponseDto {
        return httpClient.delete("users/me").body()
    }
}
