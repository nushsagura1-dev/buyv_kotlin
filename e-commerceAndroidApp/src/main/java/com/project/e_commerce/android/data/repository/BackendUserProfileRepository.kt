package com.project.e_commerce.android.data.repository

import android.util.Log
import com.project.e_commerce.android.domain.model.UserPost
import com.project.e_commerce.android.domain.model.UserProduct
import com.project.e_commerce.android.domain.model.UserProfile
import com.project.e_commerce.android.domain.repository.UserProfileRepository
import com.project.e_commerce.domain.usecase.user.FollowUserUseCase
import com.project.e_commerce.domain.usecase.user.GetFollowersUseCase
import com.project.e_commerce.domain.usecase.user.GetFollowingUseCase
import com.project.e_commerce.domain.usecase.user.GetUserPostsUseCase
import com.project.e_commerce.domain.usecase.user.GetUserProfileUseCase
import com.project.e_commerce.domain.usecase.user.UnfollowUserUseCase
import com.project.e_commerce.domain.usecase.user.UpdateUserProfileUseCase
import com.project.e_commerce.domain.repository.PostRepository
import com.project.e_commerce.data.remote.api.MarketplaceApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.project.e_commerce.domain.model.Result as KmpResult
import com.project.e_commerce.domain.model.UserProfile as KmpUserProfile

/**
 * Backend-based implementation of UserProfileRepository.
 * Uses KMP UseCases to communicate with the FastAPI backend.
 */
class BackendUserProfileRepository(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getUserPostsUseCase: GetUserPostsUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val getFollowersUseCase: GetFollowersUseCase,
    private val getFollowingUseCase: GetFollowingUseCase,
    private val postRepository: PostRepository,
    private val marketplaceApi: MarketplaceApiService
) : UserProfileRepository {

    companion object {
        private const val TAG = "BackendUserProfileRepo"
    }

    override suspend fun getUserProfile(uid: String): Result<UserProfile> {
        Log.d(TAG, "🔍 Getting user profile for: $uid")
        return when (val result = getUserProfileUseCase(uid)) {
            is KmpResult.Success -> {
                val kmpProfile = result.data
                val androidProfile = UserProfile(
                    uid = kmpProfile.uid,
                    email = kmpProfile.email,
                    username = kmpProfile.username,
                    displayName = kmpProfile.displayName,
                    bio = kmpProfile.bio,
                    profileImageUrl = kmpProfile.profileImageUrl,
                    phone = kmpProfile.phone,
                    role = kmpProfile.role,
                    followersCount = kmpProfile.followersCount,
                    followingCount = kmpProfile.followingCount,
                    likesCount = kmpProfile.likesCount,
                    createdAt = kmpProfile.createdAt,
                    lastUpdated = kmpProfile.lastUpdated
                )
                Log.d(TAG, "✅ Got profile: ${androidProfile.displayName}")
                Result.success(androidProfile)
            }
            is KmpResult.Error -> {
                Log.e(TAG, "❌ Failed to get profile: ${result.error}")
                Result.failure(Exception(result.error.toString()))
            }
            is KmpResult.Loading -> {
                Log.d(TAG, "⏳ Loading profile...")
                Result.failure(Exception("Loading"))
            }
        }
    }

    override suspend fun updateUserProfile(profile: UserProfile): Result<UserProfile> {
        Log.d(TAG, "🔄 Updating user profile for: ${profile.uid}")
        val kmpProfile = KmpUserProfile(
            uid = profile.uid,
            email = profile.email,
            displayName = profile.displayName,
            username = profile.username,
            profileImageUrl = profile.profileImageUrl,
            bio = profile.bio,
            phone = profile.phone,
            role = profile.role,
            followersCount = profile.followersCount,
            followingCount = profile.followingCount,
            likesCount = profile.likesCount,
            createdAt = profile.createdAt,
            lastUpdated = System.currentTimeMillis()
        )
        return when (val result = updateUserProfileUseCase(kmpProfile)) {
            is KmpResult.Success -> {
                Log.d(TAG, "✅ Profile updated")
                Result.success(profile)
            }
            is KmpResult.Error -> {
                Log.e(TAG, "❌ Failed to update profile: ${result.error}")
                Result.failure(Exception(result.error.toString()))
            }
            is KmpResult.Loading -> {
                Result.failure(Exception("Loading"))
            }
        }
    }

    override suspend fun createUserProfile(profile: UserProfile): Result<UserProfile> {
        // Profile creation is handled by register endpoint
        Log.d(TAG, "createUserProfile called - delegating to update")
        return updateUserProfile(profile)
    }

    override fun getUserProfileFlow(uid: String): Flow<UserProfile?> = flow {
        when (val result = getUserProfile(uid)) {
            else -> emit(result.getOrNull())
        }
    }

    override suspend fun getUserPosts(uid: String): Result<List<UserPost>> {
        Log.d(TAG, "🔍 Getting user posts for: $uid")
        return when (val result = getUserPostsUseCase(uid)) {
            is KmpResult.Success -> {
                val posts = result.data.map { kmpPost ->
                    UserPost(
                        id = kmpPost.id,
                        userId = uid,
                        type = kmpPost.type,
                        title = kmpPost.title,
                        description = kmpPost.description,
                        mediaUrl = kmpPost.mediaUrl,
                        thumbnailUrl = kmpPost.thumbnailUrl,
                        images = kmpPost.images,
                        likesCount = kmpPost.likesCount,
                        commentsCount = kmpPost.commentsCount,
                        viewsCount = kmpPost.viewsCount,
                        isPublished = kmpPost.isPublished,
                        createdAt = kmpPost.createdAt,
                        updatedAt = kmpPost.updatedAt
                    )
                }
                Log.d(TAG, "✅ Got ${posts.size} posts")
                Result.success(posts)
            }
            is KmpResult.Error -> {
                Log.e(TAG, "❌ Failed to get posts: ${result.error}")
                Result.failure(Exception(result.error.toString()))
            }
            is KmpResult.Loading -> {
                Result.failure(Exception("Loading"))
            }
        }
    }

    override suspend fun getUserReels(uid: String): Result<List<UserPost>> {
        Log.d(TAG, "🔍 Getting user reels for: $uid")
        return when (val postsResult = getUserPosts(uid)) {
            else -> {
                val reels = postsResult.getOrNull()?.filter { it.type.lowercase() == "reel" } ?: emptyList()
                if (postsResult.isSuccess) {
                    Log.d(TAG, "✅ Got ${reels.size} reels")
                    Result.success(reels)
                } else {
                    Result.failure(postsResult.exceptionOrNull() ?: Exception("Unknown error"))
                }
            }
        }
    }

    override suspend fun getUserProducts(uid: String): Result<List<UserProduct>> {
        Log.d(TAG, "🔍 Getting promoted products for: $uid")
        return try {
            // Fetch user's promotions from marketplace API
            val promotions = marketplaceApi.getMyPromotions(uid)
            Log.d(TAG, "✅ Got ${promotions.size} promotions for user")
            
            // For each promotion, fetch the product details and map to UserProduct
            val products = promotions.mapNotNull { promotion ->
                try {
                    val product = marketplaceApi.getProduct(promotion.productId)
                    UserProduct(
                        id = product.id,
                        userId = uid,
                        name = product.name,
                        description = product.description ?: product.shortDescription ?: "",
                        price = product.sellingPrice,
                        images = listOfNotNull(product.mainImageUrl) + product.images,
                        category = product.categoryName ?: "",
                        stockQuantity = 0,
                        likesCount = 0,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to fetch product ${promotion.productId}: ${e.message}")
                    null
                }
            }.distinctBy { it.id }
            
            Log.d(TAG, "✅ Got ${products.size} promoted products")
            Result.success(products)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load promoted products: ${e.message}")
            Result.success(emptyList()) // Return empty list on error, don't crash
        }
    }

    override fun getUserPostsFlow(uid: String): Flow<List<UserPost>> = flow {
        when (val result = getUserPosts(uid)) {
            else -> emit(result.getOrNull() ?: emptyList())
        }
    }

    override suspend fun getUserLikedPosts(uid: String): Result<List<UserPost>> {
        Log.d(TAG, "🔍 Getting liked posts for: $uid")
        return try {
            val posts = postRepository.getLikedPosts(uid)
            val userPosts = posts.map { post ->
                UserPost(
                    id = post.id,
                    userId = post.userId,
                    type = if (post.mediaType.name == "VIDEO") "REEL" else "IMAGE",
                    title = post.caption,
                    description = post.caption,
                    mediaUrl = post.mediaUrl,
                    thumbnailUrl = post.thumbnailUrl,
                    images = listOfNotNull(post.thumbnailUrl ?: post.mediaUrl),
                    likesCount = post.likesCount,
                    commentsCount = post.commentsCount,
                    viewsCount = 0,
                    isPublished = true,
                    createdAt = post.createdAt,
                    updatedAt = post.updatedAt
                )
            }
            Log.d(TAG, "✅ Got ${userPosts.size} liked posts")
            Result.success(userPosts)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get liked posts: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getUserLikedProducts(uid: String): Result<List<UserProduct>> {
        return Result.success(emptyList())
    }

    override suspend fun getUserBookmarkedPosts(uid: String): Result<List<UserPost>> {
        Log.d(TAG, "🔍 Getting bookmarked posts for: $uid")
        return try {
            val posts = postRepository.getBookmarkedPosts(uid)
            val userPosts = posts.map { post ->
                UserPost(
                    id = post.id,
                    userId = post.userId,
                    type = if (post.mediaType.name == "VIDEO") "REEL" else "IMAGE",
                    title = post.caption,
                    description = post.caption,
                    mediaUrl = post.mediaUrl,
                    thumbnailUrl = post.thumbnailUrl,
                    images = listOfNotNull(post.thumbnailUrl ?: post.mediaUrl),
                    likesCount = post.likesCount,
                    commentsCount = post.commentsCount,
                    viewsCount = 0,
                    isPublished = true,
                    createdAt = post.createdAt,
                    updatedAt = post.updatedAt
                )
            }
            Log.d(TAG, "✅ Got ${userPosts.size} bookmarked posts")
            Result.success(userPosts)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get bookmarked posts: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getUserBookmarkedProducts(uid: String): Result<List<UserProduct>> {
        return Result.success(emptyList())
    }

    override fun getUserLikedContentFlow(uid: String): Flow<List<UserPost>> = flow {
        emit(getUserLikedPosts(uid).getOrNull() ?: emptyList())
    }

    override fun getUserBookmarkedContentFlow(uid: String): Flow<List<UserPost>> = flow {
        emit(getUserBookmarkedPosts(uid).getOrNull() ?: emptyList())
    }

    override suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit> {
        Log.d(TAG, "🔄 Following: $currentUserId -> $targetUserId")
        return when (val result = followUserUseCase(currentUserId, targetUserId)) {
            is KmpResult.Success -> {
                Log.d(TAG, "✅ Followed successfully")
                Result.success(Unit)
            }
            is KmpResult.Error -> {
                Log.e(TAG, "❌ Failed to follow: ${result.error}")
                Result.failure(Exception(result.error.toString()))
            }
            is KmpResult.Loading -> Result.success(Unit)
        }
    }

    override suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit> {
        Log.d(TAG, "🔄 Unfollowing: $currentUserId -> $targetUserId")
        return when (val result = unfollowUserUseCase(currentUserId, targetUserId)) {
            is KmpResult.Success -> {
                Log.d(TAG, "✅ Unfollowed successfully")
                Result.success(Unit)
            }
            is KmpResult.Error -> {
                Log.e(TAG, "❌ Failed to unfollow: ${result.error}")
                Result.failure(Exception(result.error.toString()))
            }
            is KmpResult.Loading -> Result.success(Unit)
        }
    }

    override suspend fun getFollowers(userId: String): Result<List<String>> {
        Log.d(TAG, "🔍 Getting followers for: $userId")
        return when (val result = getFollowersUseCase(userId)) {
            is KmpResult.Success -> {
                val followerIds = result.data.map { it.id }
                Log.d(TAG, "✅ Got ${followerIds.size} followers")
                Result.success(followerIds)
            }
            is KmpResult.Error -> {
                Log.e(TAG, "❌ Failed to get followers: ${result.error}")
                Result.failure(Exception(result.error.toString()))
            }
            is KmpResult.Loading -> Result.success(emptyList())
        }
    }

    override suspend fun getFollowing(userId: String): Result<List<String>> {
        Log.d(TAG, "🔍 Getting following for: $userId")
        return when (val result = getFollowingUseCase(userId)) {
            is KmpResult.Success -> {
                val followingIds = result.data.map { it.id }
                Log.d(TAG, "✅ Got ${followingIds.size} following")
                Result.success(followingIds)
            }
            is KmpResult.Error -> {
                Log.e(TAG, "❌ Failed to get following: ${result.error}")
                Result.failure(Exception(result.error.toString()))
            }
            is KmpResult.Loading -> Result.success(emptyList())
        }
    }

    override suspend fun uploadProfileImage(uid: String, imageUri: String): Result<String> {
        Log.d(TAG, "🔄 Upload profile image - TODO: implement with backend")
        // TODO: Implement file upload to backend
        return Result.success(imageUri)
    }

    override suspend fun deleteProfileImage(uid: String): Result<Unit> {
        Log.d(TAG, "🔄 Delete profile image - TODO: implement with backend")
        return Result.success(Unit)
    }
}
