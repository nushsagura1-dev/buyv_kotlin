package com.project.e_commerce.android.presentation.viewModel.otherUserProfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.project.e_commerce.android.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

class OtherUserProfileViewModel(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getFollowingStatusUseCase: GetFollowingStatusUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val getUserReelsUseCase: GetUserReelsUseCase,
    private val getUserProductsUseCase: GetUserProductsUseCase,
    private val getFollowersUseCase: GetFollowersUseCase,
    private val getFollowingUsersUseCase: GetFollowingUsersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtherUserProfileUiState())
    val uiState: StateFlow<OtherUserProfileUiState> = _uiState.asStateFlow()

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("OtherUserProfile", "🔄 Loading profile for user: $userId")
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Load user profile
                val profileResult = getUserProfileUseCase(userId)
                if (profileResult.isSuccess) {
                    val profile = profileResult.getOrNull()
                    if (profile != null) {
                        Log.d("OtherUserProfile", "✅ Profile loaded: ${profile.displayName}")
                        _uiState.value = _uiState.value.copy(
                            displayName = profile.displayName,
                            username = profile.username,
                            bio = profile.bio ?: "",
                            profileImageUrl = profile.profileImageUrl,
                            isLoading = false
                        )
                        
                        // Load additional data
                        loadUserStats(userId)
                        checkFollowingStatus(userId)
                    } else {
                        Log.w("OtherUserProfile", "⚠️ Profile is null for user: $userId")
                        _uiState.value = _uiState.value.copy(
                            error = "User profile not found",
                            isLoading = false
                        )
                    }
                } else {
                    Log.e("OtherUserProfile", "❌ Failed to load profile: ${profileResult.exceptionOrNull()?.message}")
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load profile: ${profileResult.exceptionOrNull()?.message}",
                        isLoading = false
                    )
                }
                
            } catch (e: Exception) {
                Log.e("OtherUserProfile", "❌ Error loading profile: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load profile",
                    isLoading = false
                )
            }
        }
    }
    
    private suspend fun loadUserStats(userId: String) {
        try {
            Log.d("OtherUserProfile", "📊 Loading stats for user: $userId")
            
            // Load reels data and count
            val reels = getUserReelsUseCase(userId)
            Log.d("OtherUserProfile", "🎬 Reels result: $reels")
            if (reels.isSuccess) {
                val reelsData = reels.getOrNull() ?: emptyList()
                val reelsCount = reelsData.size
                Log.d("OtherUserProfile", "🎬 Reels count: $reelsCount")
                Log.d("OtherUserProfile", "🎬 Reels data: $reelsData")
                _uiState.value = _uiState.value.copy(
                    postsCount = reelsCount,
                    userReels = reelsData
                )
            } else {
                Log.e("OtherUserProfile", "❌ Failed to load reels: ${reels.exceptionOrNull()?.message}")
            }
            
            // Load products data and count
            val products = getUserProductsUseCase(userId)
            Log.d("OtherUserProfile", "📦 Products result: $products")
            if (products.isSuccess) {
                val productsData = products.getOrNull() ?: emptyList()
                val productsCount = productsData.size
                Log.d("OtherUserProfile", "📦 Products count: $productsCount")
                Log.d("OtherUserProfile", "📦 Products data: $productsData")
                _uiState.value = _uiState.value.copy(
                    productsCount = productsCount,
                    userProducts = productsData
                )
            } else {
                Log.e("OtherUserProfile", "❌ Failed to load products: ${products.exceptionOrNull()?.message}")
            }
            
            // Load followers count - this returns List<String> directly
            val followers = getFollowersUseCase(userId)
            val followersCount = followers.size
            Log.d("OtherUserProfile", "👥 Followers count: $followersCount")
            _uiState.value = _uiState.value.copy(followersCount = followersCount)
            
            // Load following count - this returns List<String> directly
            val following = getFollowingUsersUseCase(userId)
            val followingCount = following.size
            Log.d("OtherUserProfile", "👥 Following count: $followingCount")
            _uiState.value = _uiState.value.copy(followingCount = followingCount)
            
        } catch (e: Exception) {
            Log.e("OtherUserProfile", "❌ Error loading stats: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private suspend fun checkFollowingStatus(targetUserId: String) {
        try {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId != null) {
                val followingStatus = getFollowingStatusUseCase(currentUserId, targetUserId)
                val isFollowing = followingStatus.isFollowing
                Log.d("OtherUserProfile", "🔍 Following status: $isFollowing")
                _uiState.value = _uiState.value.copy(isFollowing = isFollowing)
            }
        } catch (e: Exception) {
            Log.e("OtherUserProfile", "❌ Error checking following status: ${e.message}")
        }
    }
    
    fun followUser(userId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserId != null) {
                    Log.d("OtherUserProfile", "➕ Following user: $userId")
                    val result = followUserUseCase(currentUserId, userId)
                    if (result.isSuccess) {
                        Log.d("OtherUserProfile", "✅ Successfully followed user: $userId")
                        _uiState.value = _uiState.value.copy(isFollowing = true)
                        
                        // Refresh followers count for the target user
                        val followers = getFollowersUseCase(userId)
                        val followersCount = followers.size
                        _uiState.value = _uiState.value.copy(followersCount = followersCount)
                    } else {
                        Log.e("OtherUserProfile", "❌ Failed to follow user: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("OtherUserProfile", "❌ Error following user: ${e.message}")
            }
        }
    }
    
    fun unfollowUser(userId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserId != null) {
                    Log.d("OtherUserProfile", "➖ Unfollowing user: $userId")
                    val result = unfollowUserUseCase(currentUserId, userId)
                    if (result.isSuccess) {
                        Log.d("OtherUserProfile", "✅ Successfully unfollowed user: $userId")
                        _uiState.value = _uiState.value.copy(isFollowing = false)
                        
                        // Refresh followers count for the target user
                        val followers = getFollowersUseCase(userId)
                        val followersCount = followers.size
                        _uiState.value = _uiState.value.copy(followersCount = followersCount)
                    } else {
                        Log.e("OtherUserProfile", "❌ Failed to unfollow user: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("OtherUserProfile", "❌ Error unfollowing user: ${e.message}")
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class OtherUserProfileUiState(
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val profileImageUrl: String? = null,
    val postsCount: Int = 0,
    val productsCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val userReels: List<com.project.e_commerce.android.domain.model.UserPost> = emptyList(),
    val userProducts: List<com.project.e_commerce.android.domain.model.UserProduct> = emptyList()
)
