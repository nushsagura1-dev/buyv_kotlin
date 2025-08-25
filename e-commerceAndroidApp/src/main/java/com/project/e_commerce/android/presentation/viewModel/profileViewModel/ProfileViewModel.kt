package com.project.e_commerce.android.presentation.viewModel.profileViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.project.e_commerce.android.domain.model.*
import com.project.e_commerce.android.domain.repository.UserProfileRepository
import com.project.e_commerce.android.domain.usecase.*
import com.project.e_commerce.android.presentation.ui.navigation.Screens
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
    private val getFollowersUseCase: GetFollowersUseCase
) : ViewModel(), ProfileScreenInteraction {

    private val auth = FirebaseAuth.getInstance()
    
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
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                try {
                    // Load user profile
                    getUserProfileUseCase(currentUser.uid).onSuccess { profile ->
                        _userProfile.value = profile
                        _uiState.value = _uiState.value.copy(
                            displayName = profile.displayName,
                            username = profile.username,
                            profileImageUrl = profile.profileImageUrl,
                            likesCount = profile.likesCount
                        )
                        
                        // Debug logging
                        Log.d("PROFILE_DEBUG", "🔥 Profile loaded: displayName='${profile.displayName}', username='${profile.username}'")
                    }.onFailure { error ->
                        _uiState.value = _uiState.value.copy(error = error.message)
                        Log.e("PROFILE_DEBUG", "❌ Failed to load profile: ${error.message}")
                    }

                    // Load REAL following/followers data from following system
                    try {
                        val followingUserIds = getFollowingUsersUseCase(currentUser.uid)
                        val followersUserIds = getFollowersUseCase(currentUser.uid)
                        
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
                    getUserReelsUseCase(currentUser.uid).onSuccess { reels ->
                        _userReels.value = reels
                        Log.d("PROFILE_DEBUG", "🔥 Loaded ${reels.size} reels for user")
                        reels.forEach { reel ->
                            Log.d("PROFILE_DEBUG", "🔥 Reel: ${reel.title} - ${reel.mediaUrl}")
                            Log.d("PROFILE_DEBUG", "🔥 Reel Images: ${reel.images}")
                            Log.d("PROFILE_DEBUG", "🔥 Reel Thumbnail: ${reel.thumbnailUrl}")
                        }
                    }.onFailure { error ->
                        Log.e("PROFILE_DEBUG", "❌ Failed to load reels: ${error.message}")
                    }

                    // Load user products
                    getUserProductsUseCase(currentUser.uid).onSuccess { products ->
                        _userProducts.value = products
                    }

                    // Load liked content
                    getUserLikedPostsUseCase(currentUser.uid).onSuccess { likedPosts ->
                        _userLikedContent.value = likedPosts
                        Log.d("PROFILE_DEBUG", "❤️ Loaded ${likedPosts.size} liked posts for user")
                        likedPosts.forEach { post ->
                            Log.d("PROFILE_DEBUG", "❤️ Liked Post: ${post.title} - ${post.mediaUrl}")
                        }
                    }.onFailure { error ->
                        Log.e("PROFILE_DEBUG", "❌ Failed to load liked posts: ${error.message}")
                    }

                    // Load bookmarked content
                    getUserBookmarkedPostsUseCase(currentUser.uid).onSuccess { bookmarkedPosts ->
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
    }

    fun refreshProfile() {
        loadUserProfile()
    }

    fun refreshFollowingData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    // Load REAL following/followers data from following system
                    val followingUserIds = getFollowingUsersUseCase(currentUser.uid)
                    val followersUserIds = getFollowersUseCase(currentUser.uid)
                    
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
        auth.signOut()
        navController.navigate(Screens.LoginScreen.route) {
            popUpTo(Screens.ProfileScreen.route) {
                inclusive = true
            }
        }
    }
}

data class ProfileUIState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val displayName: String = "",
    val username: String = "",
    val profileImageUrl: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val likesCount: Int = 0
)