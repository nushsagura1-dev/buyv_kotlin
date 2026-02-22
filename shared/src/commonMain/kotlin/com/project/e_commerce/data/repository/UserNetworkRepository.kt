package com.project.e_commerce.data.repository

import com.project.e_commerce.data.remote.api.UserApiService
import com.project.e_commerce.data.remote.dto.DeleteAccountResponseDto
import com.project.e_commerce.data.remote.mapper.toDomain
import com.project.e_commerce.data.remote.mapper.toStatsMap
import com.project.e_commerce.data.remote.mapper.toUpdateDto
import com.project.e_commerce.domain.model.*
import com.project.e_commerce.domain.repository.UserRepository
import io.ktor.client.plugins.*
import io.ktor.utils.io.errors.*

/**
 * Network implementation of UserRepository using FastAPI backend
 */
class UserNetworkRepository(
    private val userApi: UserApiService
) : UserRepository {
    
    override suspend fun getUserProfile(userId: String): Result<UserProfile> {
        return try {
            val userDto = userApi.getUserById(userId)
            Result.Success(userDto.toDomain())
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                Result.Error(ApiError.NotFound)
            } else {
                Result.Error(ApiError.fromException(e))
            }
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit> {
        return try {
            val updateDto = userProfile.toUpdateDto()
            userApi.updateUserProfile(userProfile.uid, updateDto)
            Result.Success(Unit)
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                Result.Error(ApiError.NotFound)
            } else {
                Result.Error(ApiError.fromException(e))
            }
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    
    override suspend fun searchUsers(query: String): Result<List<UserProfile>> {
        return try {
            if (query.isBlank()) {
                return Result.Success(emptyList())
            }
            
            val usersDto = userApi.searchUsers(query)
            val users = usersDto.map { it.toDomain() }
            Result.Success(users)
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun followUser(followerId: String, followedId: String): Result<Unit> {
        return try {
            userApi.followUser(followerId, followedId)
            Result.Success(Unit)
        } catch (e: ClientRequestException) {
            when (e.response.status.value) {
                403 -> Result.Error(ApiError.Forbidden)
                404 -> Result.Error(ApiError.NotFound)
                400 -> Result.Error(ApiError.ValidationError("Cannot follow yourself"))
                else -> Result.Error(ApiError.fromException(e))
            }
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun unfollowUser(followerId: String, followedId: String): Result<Unit> {
        return try {
            userApi.unfollowUser(followerId, followedId)
            Result.Success(Unit)
        } catch (e: ClientRequestException) {
            when (e.response.status.value) {
                403 -> Result.Error(ApiError.Forbidden)
                404 -> Result.Error(ApiError.NotFound)
                400 -> Result.Error(ApiError.ValidationError("Cannot unfollow yourself"))
                else -> Result.Error(ApiError.fromException(e))
            }
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun getFollowers(userId: String): Result<List<UserFollowModel>> {
        return try {
            val followersDto = userApi.getFollowers(userId)
            val followers = followersDto.map { it.toDomain() }
            Result.Success(followers)
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                Result.Error(ApiError.NotFound)
            } else {
                Result.Error(ApiError.fromException(e))
            }
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun getFollowing(userId: String): Result<List<UserFollowModel>> {
        return try {
            val followingDto = userApi.getFollowing(userId)
            val following = followingDto.map { it.toDomain() }
            Result.Success(following)
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                Result.Error(ApiError.NotFound)
            } else {
                Result.Error(ApiError.fromException(e))
            }
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun getFollowingStatus(currentUserId: String, targetUserId: String): Result<FollowingStatus> {
        return try {
            val statusDto = userApi.getFollowingStatus(currentUserId, targetUserId)
            Result.Success(statusDto.toDomain())
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                Result.Error(ApiError.NotFound)
            } else {
                Result.Error(ApiError.fromException(e))
            }
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun getUserPosts(userId: String): Result<List<UserPost>> {
        return try {
            val postsDto = userApi.getUserPosts(userId)
            val posts = postsDto.map { it.toDomain() }
            Result.Success(posts)
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                Result.Error(ApiError.NotFound)
            } else {
                Result.Error(ApiError.fromException(e))
            }
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun incrementLikesCount(userId: String): Result<Unit> {
        // Backend computes likes_count from the PostLike table automatically.
        // The count is updated server-side when liking a post via POST /posts/{id}/like.
        return Result.Success(Unit)
    }
    
    override suspend fun decrementLikesCount(userId: String): Result<Unit> {
        // Backend computes likes_count from the PostLike table automatically.
        // The count is updated server-side when unliking a post via DELETE /posts/{id}/like.
        return Result.Success(Unit)
    }
    
    /**
     * Get user statistics (not in interface but useful utility)
     */
    suspend fun getUserStats(userId: String): Result<Map<String, Int>> {
        return try {
            val statsDto = userApi.getUserStats(userId)
            Result.Success(statsDto.toStatsMap())
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 404) {
                Result.Error(ApiError.NotFound)
            } else {
                Result.Error(ApiError.fromException(e))
            }
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    suspend fun updateFcmToken(token: String): Result<Unit> {
        return try {
            userApi.updateFcmToken(token)
            Result.Success(Unit)
        } catch (e: ClientRequestException) {
            Result.Error(ApiError.fromException(e))
        } catch (e: IOException) {
            Result.Error(ApiError.NetworkError)
        } catch (e: Exception) {
            Result.Error(ApiError.Unknown(e.message ?: "Unknown error occurred"))
        }
    }
    
    override suspend fun deleteAccount(): DeleteAccountResponseDto {
        return userApi.deleteAccount()
    }
}
