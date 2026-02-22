package com.project.e_commerce.android.presentation.utils

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.project.e_commerce.android.R
import androidx.compose.runtime.mutableStateMapOf
import com.project.e_commerce.domain.repository.UserRepository
import com.project.e_commerce.domain.model.Result
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Utility class for handling Cloudinary URLs and operations.
 */
object CloudinaryUtils {
    
    /**
     * Check if a URL is a Cloudinary URL.
     */
    fun isCloudinaryUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.contains("res.cloudinary.com") || url.contains("cloudinary.com")
    }

    /**
     * Reusable composable to load profile images with cache and fallback.
     *
     * @param userId The user ID whose profile image to load
     * @param modifier Compose Modifier for custom styling
     * @param size The diameter of the profile image (defaults to 48.dp)
     * @param placeholderRes Resource ID for the placeholder image, defaulting to a built-in image
     */
    @Composable
    fun ProfileImage(
        userId: String,
        modifier: Modifier = Modifier,
        size: Dp = 48.dp,
        placeholderRes: Int = R.drawable.profile
    ) {
        var imageUrl by remember(userId) { mutableStateOf<String?>(null) }
        var loading by remember(userId) { mutableStateOf(true) }

        // Only fetch if userId is not blank
        LaunchedEffect(userId) {
            loading = true
            imageUrl = ProfileImageCache.getProfileImageUrl(userId)
            loading = false
        }

        val finalModifier = modifier
            .size(size)
            .clip(CircleShape)

        if (loading) {
            // Loading state: show placeholder
            Image(
                painter = painterResource(id = placeholderRes),
                contentDescription = "Loading profile image",
                modifier = finalModifier,
                contentScale = ContentScale.Crop
            )
        } else if (!imageUrl.isNullOrEmpty()) {
            // Loaded/Success state: show image from URL
            AsyncImage(
                model = CloudinaryUtils.normalizeCloudinaryUrl(imageUrl!!),
                contentDescription = "Profile image",
                modifier = finalModifier,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = placeholderRes),
                error = painterResource(id = placeholderRes)
            )
        } else {
            // No image: show placeholder
            Image(
                painter = painterResource(id = placeholderRes),
                contentDescription = "Default profile image",
                modifier = finalModifier,
                contentScale = ContentScale.Crop
            )
        }
    }
    
    /**
     * Check if a URL is a Cloudinary image URL.
     */
    fun isCloudinaryImageUrl(url: String?): Boolean {
        if (!isCloudinaryUrl(url)) return false
        return url?.contains("/image/upload/") == true
    }
    
    /**
     * Check if a URL is a Cloudinary video URL.
     */
    fun isCloudinaryVideoUrl(url: String?): Boolean {
        if (!isCloudinaryUrl(url)) return false
        return url?.contains("/video/upload/") == true
    }
    
    /**
     * Transform a Cloudinary URL to ensure it's properly formatted for Coil.
     * This adds the https:// scheme if missing.
     */
    fun normalizeCloudinaryUrl(url: String): String {
        if (url.startsWith("//")) {
            return "https:$url"
        }
        if (!url.startsWith("http")) {
            return "https://$url"
        }
        return url
    }
}

/**
 * Profile Image Cache using backend API instead of Firebase.
 * Migrated from Firebase Firestore to Backend API (Phase 3-4).
 */
object ProfileImageCache : KoinComponent {
    private val cache = mutableStateMapOf<String, String?>()
    private val userRepository: UserRepository by inject()

    suspend fun getProfileImageUrl(userId: String): String? {
        if (userId.isBlank()) return null

        // Return cached value if available
        if (cache.containsKey(userId)) {
            Log.d("ProfileImageCache", "✅ Cache hit for user $userId: ${cache[userId]}")
            return cache[userId]
        }

        // Fetch from Backend API and cache
        return try {
            when (val result = userRepository.getUserProfile(userId)) {
                is Result.Success -> {
                    val profileImageUrl = result.data.profileImageUrl
                    // Cache the result (even if null)
                    cache[userId] = profileImageUrl
                    Log.d(
                        "ProfileImageCache",
                        "✅ Fetched and cached profile image for $userId: $profileImageUrl"
                    )
                    profileImageUrl
                }
                is Result.Error -> {
                    Log.e("ProfileImageCache", "❌ Failed to fetch profile image for $userId: ${result.error}")
                    cache[userId] = null
                    null
                }
                is Result.Loading -> null
            }
        } catch (e: Exception) {
            Log.e("ProfileImageCache", "❌ Exception fetching profile image for $userId: ${e.message}")
            // Cache null to avoid repeated failed requests
            cache[userId] = null
            null
        }
    }

    /**
     * Clear cache for a specific user (useful when profile is updated)
     */
    fun clearUserCache(userId: String) {
        cache.remove(userId)
        Log.d("ProfileImageCache", "🗑️ Cleared cache for user $userId")
    }

    /**
     * Clear entire cache
     */
    fun clearAllCache() {
        cache.clear()
        Log.d("ProfileImageCache", "🗑️ Cleared entire profile image cache")
    }
}

/**
 * User Information data class for caching
 */
data class CachedUserInfo(
    val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val profileImageUrl: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val bio: String = "",
    val isVerified: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Comprehensive User Info Cache using backend API instead of Firebase.
 * Migrated from Firebase Firestore to Backend API (Phase 3-4).
 * Handles usernames, display names, follower counts, etc.
 */
object UserInfoCache : KoinComponent {
    private val cache = mutableStateMapOf<String, CachedUserInfo>()
    private val CACHE_EXPIRY_TIME = 5 * 60 * 1000L // 5 minutes
    private val userRepository: UserRepository by inject()

    suspend fun getUserInfo(userId: String): CachedUserInfo? {
        if (userId.isBlank()) return null

        // Check if we have cached data that's still fresh
        val cachedInfo = cache[userId]
        if (cachedInfo != null) {
            val age = System.currentTimeMillis() - cachedInfo.lastUpdated
            if (age < CACHE_EXPIRY_TIME) {
                Log.d("UserInfoCache", "✅ Cache hit for user $userId: ${cachedInfo.displayName}")
                return cachedInfo
            } else {
                Log.d("UserInfoCache", "🕐 Cache expired for user $userId, refreshing...")
            }
        }

        // Fetch from Backend API and cache
        return try {
            when (val result = userRepository.getUserProfile(userId)) {
                is Result.Success -> {
                    val profile = result.data
                    val userInfo = CachedUserInfo(
                        userId = userId,
                        displayName = profile.displayName,
                        username = profile.username,
                        profileImageUrl = profile.profileImageUrl,
                        followersCount = profile.followersCount,
                        followingCount = profile.followingCount,
                        bio = profile.bio,
                        isVerified = false, // TODO: Add isVerified to UserProfile if needed
                        lastUpdated = System.currentTimeMillis()
                    )

                    // Cache the result
                    cache[userId] = userInfo
                    Log.d(
                        "UserInfoCache",
                        "✅ Fetched and cached user info for $userId: ${userInfo.displayName} (@${userInfo.username})"
                    )

                    userInfo
                }
                is Result.Error -> {
                    Log.e("UserInfoCache", "❌ Failed to fetch user info for $userId: ${result.error}")
                    null
                }
                is Result.Loading -> null
            }
        } catch (e: Exception) {
            Log.e("UserInfoCache", "❌ Exception fetching user info for $userId: ${e.message}")
            null
        }
    }

    /**
     * Get just the display name (fallback to username)
     */
    suspend fun getDisplayName(userId: String): String {
        val userInfo = getUserInfo(userId)
        return when {
            !userInfo?.displayName.isNullOrBlank() -> userInfo!!.displayName
            !userInfo?.username.isNullOrBlank() -> userInfo!!.username
            else -> "User"
        }
    }

    /**
     * Get just the username (fallback to display name)
     */
    suspend fun getUsername(userId: String): String {
        val userInfo = getUserInfo(userId)
        return when {
            !userInfo?.username.isNullOrBlank() -> userInfo!!.username
            !userInfo?.displayName.isNullOrBlank() -> userInfo!!.displayName
            else -> "user"
        }
    }

    /**
     * Get follower count
     */
    suspend fun getFollowerCount(userId: String): Int {
        return getUserInfo(userId)?.followersCount ?: 0
    }

    /**
     * Get following count
     */
    suspend fun getFollowingCount(userId: String): Int {
        return getUserInfo(userId)?.followingCount ?: 0
    }

    /**
     * Clear cache for a specific user (useful when profile is updated)
     */
    fun clearUserCache(userId: String) {
        cache.remove(userId)
        ProfileImageCache.clearUserCache(userId) // Also clear profile image cache
        Log.d("UserInfoCache", "🗑️ Cleared cache for user $userId")
    }

    /**
     * Update cached user info (useful after follow/unfollow actions)
     */
    fun updateUserInfo(userId: String, updatedInfo: CachedUserInfo) {
        cache[userId] = updatedInfo.copy(lastUpdated = System.currentTimeMillis())
        Log.d("UserInfoCache", "🔄 Updated cache for user $userId: ${updatedInfo.displayName}")
    }

    /**
     * Update follower counts in cache
     */
    fun updateFollowerCounts(userId: String, followersCount: Int, followingCount: Int) {
        val existingInfo = cache[userId]
        if (existingInfo != null) {
            cache[userId] = existingInfo.copy(
                followersCount = followersCount,
                followingCount = followingCount,
                lastUpdated = System.currentTimeMillis()
            )
            Log.d(
                "UserInfoCache",
                "📊 Updated follower counts for $userId: $followersCount followers, $followingCount following"
            )
        }
    }

    /**
     * Clear entire cache
     */
    fun clearAllCache() {
        cache.clear()
        Log.d("UserInfoCache", "🗑️ Cleared entire user info cache")
    }
}

/**
 * Display types for user information
 */
enum class UserDisplayType {
    FULL_NAME_AND_USERNAME, // "Ahmed Azizi (@a.azizi)"
    DISPLAY_NAME_ONLY,      // "Ahmed Azizi"
    USERNAME_ONLY,          // "@a.azizi" or "a.azizi"
    DISPLAY_NAME_WITH_STATS, // "Ahmed Azizi • 1.2K followers"
    FULL_NAME_AND_USERNAME_COMPACT // "Ahmed Azizi" on first line, "@a.azizi" on second line
}

/**
 * Reusable composable for displaying user names consistently throughout the app
 */
@Composable
fun UserDisplayName(
    userId: String,
    displayType: UserDisplayType = UserDisplayType.DISPLAY_NAME_ONLY,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = androidx.compose.material.LocalTextStyle.current,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    maxLines: Int = 1,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
) {
    var displayText by remember(userId, displayType) { mutableStateOf("Loading...") }

    LaunchedEffect(userId, displayType) {
        if (userId.isNotBlank()) {
            try {
                val userInfo = UserInfoCache.getUserInfo(userId)
                displayText = when (displayType) {
                    UserDisplayType.FULL_NAME_AND_USERNAME -> {
                        val name = userInfo?.displayName?.takeIf { it.isNotBlank() } ?: "User"
                        val username = userInfo?.username?.takeIf { it.isNotBlank() }
                        if (username != null) "$name (@$username)" else name
                    }

                    UserDisplayType.DISPLAY_NAME_ONLY -> {
                        userInfo?.displayName?.takeIf { it.isNotBlank() }
                            ?: userInfo?.username?.takeIf { it.isNotBlank() }
                            ?: "User"
                    }

                    UserDisplayType.USERNAME_ONLY -> {
                        val username = userInfo?.username?.takeIf { it.isNotBlank() }
                            ?: userInfo?.displayName?.takeIf { it.isNotBlank() }
                            ?: "user"
                        "@$username"
                    }

                    UserDisplayType.DISPLAY_NAME_WITH_STATS -> {
                        val name = userInfo?.displayName?.takeIf { it.isNotBlank() }
                            ?: userInfo?.username?.takeIf { it.isNotBlank() }
                            ?: "User"
                        val followers = userInfo?.followersCount ?: 0
                        val followersText = when {
                            followers >= 1000000 -> "${
                                String.format(
                                    "%.1f",
                                    followers / 1000000.0
                                )
                            }M"

                            followers >= 1000 -> "${String.format("%.1f", followers / 1000.0)}K"
                            else -> followers.toString()
                        }
                        "$name • $followersText followers"
                    }
                    UserDisplayType.FULL_NAME_AND_USERNAME_COMPACT -> {
                        val name = userInfo?.displayName?.takeIf { it.isNotBlank() } ?: "User"
                        val username = userInfo?.username?.takeIf { it.isNotBlank() }
                        if (username != null) "$name\n@$username" else name
                    }
                }

                Log.d("UserDisplayName", "✅ Loaded display name for $userId: $displayText")
            } catch (e: Exception) {
                Log.e("UserDisplayName", "❌ Failed to load display name for $userId: ${e.message}")
                displayText = "User"
            }
        } else {
            displayText = "User"
        }
    }

    androidx.compose.material.Text(
        text = displayText,
        modifier = modifier,
        style = textStyle,
        color = color,
        maxLines = maxLines,
        overflow = overflow
    )
}

/**
 * Reusable composable for displaying follower/following counts
 */
@Composable
fun UserFollowStats(
    userId: String,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = androidx.compose.material.LocalTextStyle.current,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    showBoth: Boolean = true // Show both followers and following, or just followers
) {
    var statsText by remember(userId) { mutableStateOf("Loading...") }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            try {
                val userInfo = UserInfoCache.getUserInfo(userId)
                val followers = userInfo?.followersCount ?: 0
                val following = userInfo?.followingCount ?: 0

                val followersText = when {
                    followers >= 1000000 -> "${String.format("%.1f", followers / 1000000.0)}M"
                    followers >= 1000 -> "${String.format("%.1f", followers / 1000.0)}K"
                    else -> followers.toString()
                }

                val followingText = when {
                    following >= 1000000 -> "${String.format("%.1f", following / 1000000.0)}M"
                    following >= 1000 -> "${String.format("%.1f", following / 1000.0)}K"
                    else -> following.toString()
                }

                statsText = if (showBoth) {
                    "$followersText followers • $followingText following"
                } else {
                    "$followersText followers"
                }

                Log.d("UserFollowStats", "✅ Loaded stats for $userId: $statsText")
            } catch (e: Exception) {
                Log.e("UserFollowStats", "❌ Failed to load stats for $userId: ${e.message}")
                statsText = "0 followers"
            }
        } else {
            statsText = "0 followers"
        }
    }

    androidx.compose.material.Text(
        text = statsText,
        modifier = modifier,
        style = textStyle,
        color = color
    )
}
