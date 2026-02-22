package com.project.e_commerce.android.presentation.viewModel.profileViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import android.util.Log
import com.project.e_commerce.android.domain.model.*
import com.project.e_commerce.android.domain.repository.UserProfileRepository
import com.project.e_commerce.android.domain.usecase.*
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.data.local.TokenManager
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.domain.usecase.post.DeletePostUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getUserReelsUseCase: GetUserReelsUseCase,
    private val getUserProductsUseCase: GetUserProductsUseCase,
    private val getUserLikedPostsUseCase: GetUserLikedPostsUseCase,
    private val getUserBookmarkedPostsUseCase: GetUserBookmarkedPostsUseCase,
    private val getFollowingUsersUseCase: GetFollowingUsersUseCase,
    private val getFollowersUseCase: GetFollowersUseCase,
    private val currentUserProvider: CurrentUserProvider,
    private val tokenManager: TokenManager,
    private val deletePostUseCase: DeletePostUseCase
) : ViewModel(), ProfileScreenInteraction {
    
    // UI State
    private val _uiState = MutableStateFlow(ProfileUIState())
    val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

    // User Profile Data
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // User Content Data
    private val _userReels = MutableStateFlow<List<UserPost>>(emptyList())
    val userReels: StateFlow<List<UserPost>> = _userReels.asStateFlow()

    private val _userProducts = MutableStateFlow<List<UserProduct>>(emptyList())
    val userProducts: StateFlow<List<UserProduct>> = _userProducts.asStateFlow()

    private val _userLikedContent = MutableStateFlow<List<UserPost>>(emptyList())
    val userLikedContent: StateFlow<List<UserPost>> = _userLikedContent.asStateFlow()

    private val _userBookmarkedContent = MutableStateFlow<List<UserPost>>(emptyList())
    val userBookmarkedContent: StateFlow<List<UserPost>> = _userBookmarkedContent.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val currentUserId: String? = currentUserProvider.getCurrentUserId()
            if (currentUserId == null) {
                Log.e("ProfileViewModel", "Cannot load profile: user not authenticated")
                return@launch
            }
            
            // At this point, currentUserId is guaranteed non-null
            val userId: String = currentUserId
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load user profile
                getUserProfileUseCase(userId).onSuccess { profile ->
                    _userProfile.value = profile

                    // Debug profile data loading
                    Log.d("PROFILE_DEBUG", "🖼️ ===== PROFILE IMAGE DEBUG =====")
                    Log.d("PROFILE_DEBUG", "🖼️ Profile loaded for user: $userId")
                    Log.d("PROFILE_DEBUG", "🖼️ Profile displayName: '${profile.displayName}'")
                    Log.d("PROFILE_DEBUG", "🖼️ Profile username: '${profile.username}'")
                    Log.d("PROFILE_DEBUG", "🖼️ Profile email: '${profile.email}'")
                    Log.d(
                        "PROFILE_DEBUG",
                        "🖼️ Profile profileImageUrl: '${profile.profileImageUrl}'"
                    )
                    Log.d(
                        "PROFILE_DEBUG",
                        "🖼️ Profile profileImageUrl is null: ${profile.profileImageUrl == null}"
                    )
                    Log.d(
                        "PROFILE_DEBUG",
                        "🖼️ Profile profileImageUrl is blank: ${profile.profileImageUrl.isNullOrBlank()}"
                    )
                    Log.d("PROFILE_DEBUG", "🖼️ =================================")

                    _uiState.value = _uiState.value.copy(
                        displayName = profile.displayName,
                        username = profile.username,
                        profileImageUrl = profile.profileImageUrl,
                        role = profile.role,
                            likesCount = profile.likesCount
                        )

                        // Debug UI state after update
                        Log.d("PROFILE_DEBUG", "🖼️ UI State updated:")
                        Log.d(
                            "PROFILE_DEBUG",
                            "🖼️ uiState.profileImageUrl: '${_uiState.value.profileImageUrl}'"
                        )
                        Log.d(
                            "PROFILE_DEBUG",
                            "🖼️ uiState.profileImageUrl is null: ${_uiState.value.profileImageUrl == null}"
                        )

                        // Debug logging
                        Log.d("PROFILE_DEBUG", "🔥 Profile loaded: displayName='${profile.displayName}', username='${profile.username}'")
                    }.onFailure { error ->
                        _uiState.value = _uiState.value.copy(error = error.message)
                        Log.e("PROFILE_DEBUG", "❌ Failed to load profile: ${error.message}")
                        Log.e(
                            "PROFILE_DEBUG",
                            "🖼️ Profile loading failed - profileImageUrl will remain null"
                        )
                    }

                    // Load REAL following/followers data from following system
                    try {
                        val followingUserIds = getFollowingUsersUseCase(userId)
                        val followersUserIds = getFollowersUseCase(userId)
                        
                        Log.d("PROFILE_DEBUG", "📊 Real following data: ${followingUserIds.size} following, ${followersUserIds.size} followers")
                        
                        _uiState.value = _uiState.value.copy(
                            followingCount = followingUserIds.size,
                            followersCount = followersUserIds.size
                        )
                        
                        Log.d("PROFILE_DEBUG", "✅ Updated UI state with real following data: Following=${followingUserIds.size}, Followers=${followersUserIds.size}")
                        
                    } catch (e: Exception) {
                        Log.e("PROFILE_DEBUG", "❌ Failed to load following data: ${e.message}")
                        // Fallback to profile data if following system fails
                        _uiState.value = _uiState.value.copy(
                            followingCount = _userProfile.value?.followingCount ?: 0,
                            followersCount = _userProfile.value?.followersCount ?: 0
                        )
                    }

                    // Load user reels
                    getUserReelsUseCase(userId).onSuccess { reels ->
                        _userReels.value = reels
                        Log.d(
                            "PROFILE_DEBUG",
                            "🔥 Loaded ${reels.size} reels for user $userId"
                        )
                        reels.forEach { reel ->
                            Log.d("PROFILE_DEBUG", "🔥 Reel: ${reel.title} - ID: ${reel.id}")
                            Log.d(
                                "PROFILE_DEBUG",
                                "🔥 Reel userId: ${reel.userId} (current user: $userId)"
                            )
                            Log.d("PROFILE_DEBUG", "🔥 Reel type: ${reel.type}")
                            Log.d("PROFILE_DEBUG", "🔥 Reel isPublished: ${reel.isPublished}")
                            Log.d("PROFILE_DEBUG", "🔥 Reel mediaUrl: ${reel.mediaUrl}")
                            Log.d("PROFILE_DEBUG", "🔥 Reel images: ${reel.images}")
                            Log.d("PROFILE_DEBUG", "🔥 Reel thumbnail: ${reel.thumbnailUrl}")
                            Log.d("PROFILE_DEBUG", "🔥 =====================================")
                        }

                        // Additional validation - filter out any reels that don't belong to current user
                        val validUserReels = reels.filter { reel ->
                            val belongsToUser = reel.userId == userId
                            if (!belongsToUser) {
                                Log.w(
                                    "PROFILE_DEBUG",
                                    "⚠️ Found reel that doesn't belong to current user: ${reel.id} belongs to ${reel.userId}"
                                )
                            }
                            belongsToUser
                        }

                        // Deduplicate reels by ID to prevent showing the same reel twice
                        val uniqueReels = validUserReels.distinctBy { it.id }

                        if (uniqueReels.size != validUserReels.size) {
                            Log.w(
                                "PROFILE_DEBUG",
                                "⚠️ DUPLICATES REMOVED: Had ${validUserReels.size} reels, removed ${validUserReels.size - uniqueReels.size} duplicates"
                            )
                            val duplicateIds =
                                validUserReels.groupBy { it.id }.filter { it.value.size > 1 }.keys
                            Log.w("PROFILE_DEBUG", "⚠️ Duplicate reel IDs: $duplicateIds")
                        }

                        if (validUserReels.size != reels.size) {
                            Log.w(
                                "PROFILE_DEBUG",
                                "⚠️ Filtered out ${reels.size - validUserReels.size} reels that didn't belong to current user"
                            )
                        }

                        _userReels.value = uniqueReels
                        Log.d("PROFILE_DEBUG", "✅ Final user reels count: ${uniqueReels.size}")
                    }.onFailure { error ->
                        Log.e("PROFILE_DEBUG", "❌ Failed to load reels: ${error.message}")
                    }

                    // Load user products
                    getUserProductsUseCase(userId).onSuccess { products ->
                        _userProducts.value = products
                    }

                    // Load liked content
                    getUserLikedPostsUseCase(userId).onSuccess { likedPosts ->
                        _userLikedContent.value = likedPosts
                        Log.d("PROFILE_DEBUG", "❤️ Loaded ${likedPosts.size} liked posts for user")
                        likedPosts.forEach { post ->
                            Log.d("PROFILE_DEBUG", "❤️ Liked Post: ${post.title} - ${post.mediaUrl}")
                        }
                    }.onFailure { error ->
                        Log.e("PROFILE_DEBUG", "❌ Failed to load liked posts: ${error.message}")
                    }

                    // Load bookmarked content
                    getUserBookmarkedPostsUseCase(userId).onSuccess { bookmarkedPosts ->
                        _userBookmarkedContent.value = bookmarkedPosts
                        Log.d("PROFILE_DEBUG", "🔖 Loaded ${bookmarkedPosts.size} bookmarked posts for user")
                        bookmarkedPosts.forEach { post ->
                            Log.d("PROFILE_DEBUG", "🔖 Bookmarked Post: ${post.title} - ${post.mediaUrl}")
                        }
                    }.onFailure { error ->
                        Log.e("PROFILE_DEBUG", "❌ Failed to load bookmarked posts: ${error.message}")
                    }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
                Log.e("PROFILE_DEBUG", "❌ Error loading profile data: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun refreshProfile() {
        loadUserProfile()
    }

    fun refreshFollowingData() {
        viewModelScope.launch {
            val currentUserId: String? = currentUserProvider.getCurrentUserId()
            if (currentUserId == null) {
                Log.e("ProfileViewModel", "Cannot refresh following data: user not authenticated")
                return@launch
            }
            
            val userId: String = currentUserId
            
            try {
                // Load REAL following/followers data from following system
                val followingUserIds = getFollowingUsersUseCase(userId)
                val followersUserIds = getFollowersUseCase(userId)
                
                Log.d("PROFILE_DEBUG", "🔄 Refreshed following data: ${followingUserIds.size} following, ${followersUserIds.size} followers")
                
                _uiState.value = _uiState.value.copy(
                    followingCount = followingUserIds.size,
                    followersCount = followersUserIds.size
                )
                
                Log.d("PROFILE_DEBUG", "✅ Updated UI state with refreshed following data: Following=${followingUserIds.size}, Followers=${followersUserIds.size}")
                
            } catch (e: Exception) {
                Log.e("PROFILE_DEBUG", "❌ Failed to refresh following data: ${e.message}")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // Navigation methods
    override fun onClickProfileOption(navController: NavController) {
        navController.navigate(Screens.ProfileScreen.EditProfileScreen.route)
    }

    override fun onClickSettingsOption(navController: NavController) {
        navController.navigate(Screens.ProfileScreen.SettingsScreen.route)
    }

    override fun onClickRequestHelpOption(navController: NavController) {
        navController.navigate(Screens.ProfileScreen.RequestHelpScreen.route)
    }

    override fun onClickLogoutOption(navController: NavController) {
        // Clear tokens and cache (Backend logout)
        tokenManager.clearTokens()
        currentUserProvider.clearCache()
        navController.navigate(Screens.LoginScreen.route) {
            popUpTo(Screens.ProfileScreen.route) {
                inclusive = true
            }
        }
    }
    
    // Delete post state
    private val _deletePostState = MutableStateFlow<DeletePostState>(DeletePostState.Idle)
    val deletePostState: StateFlow<DeletePostState> = _deletePostState.asStateFlow()
    
    /**
     * Delete a user's post (reel)
     * @param postId The ID of the post to delete
     */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            val userId = currentUserProvider.getCurrentUserId()
            if (userId == null) {
                _deletePostState.value = DeletePostState.Error("User not authenticated")
                return@launch
            }
            
            _deletePostState.value = DeletePostState.Loading
            
            try {
                deletePostUseCase(postId, userId)
                
                // Remove the deleted post from the local list
                _userReels.value = _userReels.value.filter { it.id != postId }
                
                Log.d("ProfileViewModel", "✅ Post deleted successfully: $postId")
                _deletePostState.value = DeletePostState.Success
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "❌ Failed to delete post: ${e.message}")
                _deletePostState.value = DeletePostState.Error(e.message ?: "Failed to delete post")
            }
        }
    }
    
    /**
     * Reset delete post state to idle
     */
    fun resetDeletePostState() {
        _deletePostState.value = DeletePostState.Idle
    }
}

sealed class DeletePostState {
    object Idle : DeletePostState()
    object Loading : DeletePostState()
    object Success : DeletePostState()
    data class Error(val message: String) : DeletePostState()
}

data class ProfileUIState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val displayName: String = "",
    val username: String = "",
    val profileImageUrl: String? = null,
    val role: String = "user", // user, promoter, admin
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val likesCount: Int = 0
)