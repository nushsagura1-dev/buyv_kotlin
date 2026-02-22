package com.project.e_commerce.android.presentation.viewModel.followingViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.android.domain.model.FollowingStatus
import com.project.e_commerce.android.domain.model.UserFollowModel
import com.project.e_commerce.android.domain.model.UserProfile
import com.project.e_commerce.android.domain.usecase.*
import com.project.e_commerce.android.presentation.viewModel.baseViewModel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.project.e_commerce.android.presentation.utils.UserInfoCache

class FollowingViewModel(
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val getFollowingStatusUseCase: GetFollowingStatusUseCase,
    private val getFollowingUsersUseCase: GetFollowingUsersUseCase,
    private val getUserProfilesByIdsUseCase: GetUserProfilesByIdsUseCase,
    private val getFollowersUseCase: GetFollowersUseCase
    // ⚠️ MIGRATION: Firestore removed - suggested users feature now uses backend API
) : BaseViewModel() {

    private var loadAttempts = 0
    private val maxLoadAttempts = 3

    companion object {
        private const val TAG = "FollowingViewModel"
    }

    private val _uiState = MutableStateFlow(FollowingUiState())
    val uiState: StateFlow<FollowingUiState> = _uiState.asStateFlow()



    override fun setLoadingState(loadingState: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = loadingState)
    }

    override fun setErrorState(errorState: Boolean, errorMessage: String) {
        _uiState.value = _uiState.value.copy(
            error = if (errorState) errorMessage else null
        )
    }

    fun loadUserData(currentUserId: String, targetUsername: String) {
        viewModelScope.launch {
            try {
                // Prevent multiple simultaneous loads
                if (_uiState.value.isLoading) return@launch
                
                // Check retry attempts
                if (loadAttempts >= maxLoadAttempts) {
                    Log.w(TAG, "⚠️ Max load attempts reached, showing error")
                    setErrorState(true, "Failed to load data after multiple attempts")
                    return@launch
                }
                
                loadAttempts++
                setLoadingState(true)

                // Load real following data
                loadRealFollowingData(currentUserId)
                
                setLoadingState(false)
                loadAttempts = 0
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load user data: ${e.message}")
                setErrorState(true, e.message ?: "Failed to load user data")
                setLoadingState(false) // Ensure loading state is reset on error
            }
        }
    }
    
    fun resetLoadAttempts() {
        loadAttempts = 0
    }

    suspend fun toggleFollow(currentUserId: String, targetUserId: String) {
        if (isFollowing(currentUserId, targetUserId)) {
            unfollowUser(currentUserId, targetUserId)
        } else {
            followUser(currentUserId, targetUserId)
        }
        refreshFollowing(currentUserId)
    }

    private suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        return getFollowingStatusUseCase(currentUserId, targetUserId).isFollowing
    }

    private suspend fun unfollowUser(currentUserId: String, targetUserId: String) {
        val unfollowResult = unfollowUserUseCase(currentUserId, targetUserId)
        if (unfollowResult.isSuccess) {
            // Clear user info cache for both users so follower counts update
            UserInfoCache.clearUserCache(currentUserId)
            UserInfoCache.clearUserCache(targetUserId)
        } else {
            Log.e(TAG, "❌ Unfollow failed: ${unfollowResult.exceptionOrNull()?.message}")
            setErrorState(
                true,
                "Failed to unfollow user: ${unfollowResult.exceptionOrNull()?.message}"
            )
        }
    }

    private suspend fun followUser(currentUserId: String, targetUserId: String) {
        val followResult = followUserUseCase(currentUserId, targetUserId)
        if (followResult.isSuccess) {
            // Clear user info cache for both users so follower counts update
            UserInfoCache.clearUserCache(currentUserId)
            UserInfoCache.clearUserCache(targetUserId)
        } else {
            Log.e(TAG, "❌ Follow failed: ${followResult.exceptionOrNull()?.message}")
            setErrorState(true, "Failed to follow user: ${followResult.exceptionOrNull()?.message}")
        }
    }

    private suspend fun refreshFollowing(userId: String) {
        loadRealFollowingData(userId)
    }

    private suspend fun loadRealFollowingData(currentUserId: String) {
        try {
            // Add a timeout to prevent infinite loading
            val timeout = 10000L // 10 seconds
            val startTime = System.currentTimeMillis()
            
            // Get following and followers lists
            val followingUserIds = getFollowingUsersUseCase(currentUserId)
            val followersUserIds = getFollowersUseCase(currentUserId) // Get actual followers
            
            // Check timeout
            if (System.currentTimeMillis() - startTime > timeout) {
                Log.w(TAG, "⚠️ Loading took too long, aborting")
                return
            }
            
            // Get user profiles for following users
            val followingProfiles = getUserProfilesByIdsUseCase(followingUserIds)
            val followersProfiles = getUserProfilesByIdsUseCase(followersUserIds)
            
            // Convert to UserFollowModel
            val followingUsers = followingProfiles.map { profile ->
                UserFollowModel(
                    id = profile.uid,
                    name = profile.displayName.ifEmpty { profile.username },
                    username = profile.username,
                    profileImageUrl = profile.profileImageUrl,
                    isFollowingMe = followersUserIds.contains(profile.uid), // Check if they follow current user
                    isIFollow = true
                )
            }
            
            val followersUsers = followersProfiles.map { profile ->
                UserFollowModel(
                    id = profile.uid,
                    name = profile.displayName.ifEmpty { profile.username },
                    username = profile.username,
                    profileImageUrl = profile.profileImageUrl,
                    isFollowingMe = true,
                    isIFollow = followingUserIds.contains(profile.uid) // Check if current user follows them
                )
            }
            
            // Find mutual friends (users who follow each other)
            val mutualFriends = followingUsers.filter { following ->
                followersUserIds.contains(following.id)
            }.map { user ->
                user.copy(isFollowingMe = true, isIFollow = true)
            }

            // Generate suggested users (users who don't follow current user and current user doesn't follow)
            // ⚠️ MIGRATION: Firestore removed - suggested users feature disabled until backend API available
            val suggestedUsers = try {
                Log.w(TAG, "⏸️ Suggested users feature disabled (requires backend API)")
                emptyList<UserFollowModel>()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to fetch suggested users: ${e.message}")
                // Fallback to empty list if Firestore fails
                emptyList()
            }
            
            _uiState.value = _uiState.value.copy(
                followers = followersUsers,
                following = followingUsers,
                friends = mutualFriends,
                suggestedUsers = suggestedUsers,
                followersCount = followersUsers.size,
                followingCount = followingUsers.size
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load real following data: ${e.message}")
            // Show error state with empty lists instead of mock data
            _uiState.value = _uiState.value.copy(
                followers = emptyList(),
                following = emptyList(),
                friends = emptyList(),
                suggestedUsers = emptyList(),
                followersCount = 0,
                followingCount = 0
            )
            setErrorState(true, e.message ?: "Failed to load following data")
        }
    }

    fun clearError() {
        setErrorState(false)
    }
}

data class FollowingUiState(
    val followers: List<UserFollowModel> = emptyList(),
    val following: List<UserFollowModel> = emptyList(),
    val friends: List<UserFollowModel> = emptyList(),
    val suggestedUsers: List<UserFollowModel> = emptyList(),
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)
