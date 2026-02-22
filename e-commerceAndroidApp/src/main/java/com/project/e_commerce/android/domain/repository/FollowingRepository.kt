package com.project.e_commerce.android.domain.repository

import com.project.e_commerce.android.domain.model.FollowRelationship
import com.project.e_commerce.android.domain.model.FollowingStatus
import kotlinx.coroutines.flow.Flow

interface FollowingRepository {
    // Follow/Unfollow operations
    suspend fun followUser(followerId: String, followedId: String): Result<Unit>
    suspend fun unfollowUser(followerId: String, followedId: String): Result<Unit>
    
    // Following status queries
    suspend fun getFollowingStatus(followerId: String, followedId: String): FollowingStatus
    suspend fun isFollowing(followerId: String, followedId: String): Boolean
    suspend fun isFollowedBy(followerId: String, followedId: String): Boolean
    
    // Following lists
    suspend fun getFollowers(userId: String): List<String> // List of user IDs who follow this user
    suspend fun getFollowing(userId: String): List<String> // List of user IDs this user follows
    
    // Following counts
    suspend fun getFollowersCount(userId: String): Int
    suspend fun getFollowingCount(userId: String): Int
    
    // Real-time following status
    fun observeFollowingStatus(followerId: String, followedId: String): Flow<FollowingStatus>
    fun observeFollowersCount(userId: String): Flow<Int>
    fun observeFollowingCount(userId: String): Flow<Int>
}
