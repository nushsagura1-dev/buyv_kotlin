package com.project.e_commerce.android.data.repository

import android.util.Log
import com.project.e_commerce.android.domain.model.FollowingStatus
import com.project.e_commerce.android.domain.repository.FollowingRepository
import com.project.e_commerce.domain.usecase.user.FollowUserUseCase
import com.project.e_commerce.domain.usecase.user.GetFollowersUseCase
import com.project.e_commerce.domain.usecase.user.GetFollowingStatusUseCase
import com.project.e_commerce.domain.usecase.user.GetFollowingUseCase
import com.project.e_commerce.domain.usecase.user.UnfollowUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import com.project.e_commerce.domain.model.Result as KmpResult

/**
 * Backend-based implementation of FollowingRepository.
 * Uses KMP UseCases to communicate with the FastAPI backend.
 */
class BackendFollowingRepository(
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val getFollowersUseCase: GetFollowersUseCase,
    private val getFollowingUseCase: GetFollowingUseCase,
    private val getFollowingStatusUseCase: GetFollowingStatusUseCase
) : FollowingRepository {

    companion object {
        private const val TAG = "BackendFollowingRepo"
    }

    // Cache for following status
    private val followingStatusCache = mutableMapOf<String, FollowingStatus>()
    
    override suspend fun followUser(followerId: String, followedId: String): Result<Unit> {
        Log.d(TAG, "🔄 Following user via backend: $followerId -> $followedId")
        return when (val result = followUserUseCase(followerId, followedId)) {
            is KmpResult.Success -> {
                // Update cache
                val cacheKey = "${followerId}_${followedId}"
                followingStatusCache[cacheKey] = FollowingStatus(
                    isFollowing = true,
                    isFollowedBy = followingStatusCache[cacheKey]?.isFollowedBy ?: false
                )
                Log.d(TAG, "✅ Successfully followed user via backend")
                Result.success(Unit)
            }
            is KmpResult.Error -> {
                Log.e(TAG, "❌ Failed to follow via backend: ${result.error}")
                Result.failure(Exception(result.error.toString()))
            }
            is KmpResult.Loading -> {
                Log.d(TAG, "⏳ Following request in progress...")
                Result.success(Unit) // Will complete eventually
            }
        }
    }

    override suspend fun unfollowUser(followerId: String, followedId: String): Result<Unit> {
        Log.d(TAG, "🔄 Unfollowing user via backend: $followerId -> $followedId")
        return when (val result = unfollowUserUseCase(followerId, followedId)) {
            is KmpResult.Success -> {
                // Update cache
                val cacheKey = "${followerId}_${followedId}"
                followingStatusCache[cacheKey] = FollowingStatus(
                    isFollowing = false,
                    isFollowedBy = followingStatusCache[cacheKey]?.isFollowedBy ?: false
                )
                Log.d(TAG, "✅ Successfully unfollowed user via backend")
                Result.success(Unit)
            }
            is KmpResult.Error -> {
                Log.e(TAG, "❌ Failed to unfollow via backend: ${result.error}")
                Result.failure(Exception(result.error.toString()))
            }
            is KmpResult.Loading -> {
                Log.d(TAG, "⏳ Unfollowing request in progress...")
                Result.success(Unit)
            }
        }
    }

    override suspend fun getFollowingStatus(followerId: String, followedId: String): FollowingStatus {
        Log.d(TAG, "🔍 Getting following status via backend: $followerId -> $followedId")
        return when (val result = getFollowingStatusUseCase(followerId, followedId)) {
            is KmpResult.Success -> {
                val status = FollowingStatus(
                    isFollowing = result.data.isFollowing,
                    isFollowedBy = result.data.isFollowedBy
                )
                // Cache the result
                followingStatusCache["${followerId}_${followedId}"] = status
                Log.d(TAG, "✅ Got status: isFollowing=${status.isFollowing}, isFollowedBy=${status.isFollowedBy}")
                status
            }
            is KmpResult.Error -> {
                Log.e(TAG, "❌ Failed to get status: ${result.error}")
                FollowingStatus(isFollowing = false, isFollowedBy = false)
            }
            is KmpResult.Loading -> {
                Log.d(TAG, "⏳ Loading following status...")
                followingStatusCache["${followerId}_${followedId}"] ?: FollowingStatus(isFollowing = false, isFollowedBy = false)
            }
        }
    }

    override suspend fun isFollowing(followerId: String, followedId: String): Boolean {
        return getFollowingStatus(followerId, followedId).isFollowing
    }

    override suspend fun isFollowedBy(followerId: String, followedId: String): Boolean {
        return getFollowingStatus(followerId, followedId).isFollowedBy
    }

    override suspend fun getFollowers(userId: String): List<String> {
        Log.d(TAG, "🔍 Getting followers via backend for user: $userId")
        return when (val result = getFollowersUseCase(userId)) {
            is KmpResult.Success -> {
                val followerIds = result.data.map { it.id }
                Log.d(TAG, "✅ Got ${followerIds.size} followers")
                followerIds
            }
            is KmpResult.Error -> {
                Log.e(TAG, "❌ Failed to get followers: ${result.error}")
                emptyList()
            }
            is KmpResult.Loading -> {
                Log.d(TAG, "⏳ Loading followers...")
                emptyList()
            }
        }
    }

    override suspend fun getFollowing(userId: String): List<String> {
        Log.d(TAG, "🔍 Getting following via backend for user: $userId")
        return when (val result = getFollowingUseCase(userId)) {
            is KmpResult.Success -> {
                val followingIds = result.data.map { it.id }
                Log.d(TAG, "✅ Got ${followingIds.size} following")
                followingIds
            }
            is KmpResult.Error -> {
                Log.e(TAG, "❌ Failed to get following: ${result.error}")
                emptyList()
            }
            is KmpResult.Loading -> {
                Log.d(TAG, "⏳ Loading following...")
                emptyList()
            }
        }
    }

    override suspend fun getFollowersCount(userId: String): Int {
        return getFollowers(userId).size
    }

    override suspend fun getFollowingCount(userId: String): Int {
        return getFollowing(userId).size
    }

    override fun observeFollowingStatus(followerId: String, followedId: String): Flow<FollowingStatus> = flow {
        // Emit cached value first if available
        val cacheKey = "${followerId}_${followedId}"
        followingStatusCache[cacheKey]?.let { emit(it) }
        
        // Then fetch fresh data
        val status = getFollowingStatus(followerId, followedId)
        emit(status)
    }

    override fun observeFollowersCount(userId: String): Flow<Int> = flow {
        emit(getFollowersCount(userId))
    }

    override fun observeFollowingCount(userId: String): Flow<Int> = flow {
        emit(getFollowingCount(userId))
    }
}
