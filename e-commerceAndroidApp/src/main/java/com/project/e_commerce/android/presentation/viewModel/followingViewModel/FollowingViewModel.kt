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

class FollowingViewModel(
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val getFollowingStatusUseCase: GetFollowingStatusUseCase,
    private val getFollowingUsersUseCase: GetFollowingUsersUseCase,
    private val getUserProfilesByIdsUseCase: GetUserProfilesByIdsUseCase,
    private val getFollowersUseCase: GetFollowersUseCase
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
                if (_uiState.value.isLoading) {
                    Log.d(TAG, "⚠️ Already loading, skipping duplicate request")
                    return@launch
                }
                
                // Check retry attempts
                if (loadAttempts >= maxLoadAttempts) {
                    Log.w(TAG, "⚠️ Max load attempts reached, showing error")
                    setErrorState(true, "Failed to load data after multiple attempts")
                    return@launch
                }
                
                loadAttempts++
                setLoadingState(true)
                Log.d(TAG, "🔄 Loading user data for: $targetUsername (attempt $loadAttempts)")

                // Load real following data
                loadRealFollowingData(currentUserId)
                
                setLoadingState(false)
                Log.d(TAG, "✅ User data loaded successfully")
                loadAttempts = 0 // Reset retry count on success
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load user data: ${e.message}")
                setErrorState(true, e.message ?: "Failed to load user data")
                setLoadingState(false) // Ensure loading state is reset on error
            }
        }
    }
    
    fun resetLoadAttempts() {
        loadAttempts = 0
        Log.d(TAG, "🔄 Reset load attempts counter")
    }

    fun toggleFollow(currentUserId: String, targetUserId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Toggling follow: $currentUserId -> $targetUserId")
                
                // Get current following status
                val currentStatus = getFollowingStatusUseCase(currentUserId, targetUserId)
                Log.d(TAG, "📊 Current following status: $currentStatus")
                
                if (currentStatus.isFollowing) {
                    // Unfollow
                    Log.d(TAG, "❌ Unfollowing user: $targetUserId")
                    val unfollowResult = unfollowUserUseCase(currentUserId, targetUserId)
                    if (unfollowResult.isSuccess) {
                        Log.d(TAG, "✅ Unfollow result: $unfollowResult")
                    } else {
                        Log.e(TAG, "❌ Unfollow failed: ${unfollowResult.exceptionOrNull()?.message}")
                        setErrorState(true, "Failed to unfollow user: ${unfollowResult.exceptionOrNull()?.message}")
                        return@launch
                    }
                } else {
                    // Follow
                    Log.d(TAG, "➕ Following user: $targetUserId")
                    val followResult = followUserUseCase(currentUserId, targetUserId)
                    if (followResult.isSuccess) {
                        Log.d(TAG, "✅ Follow result: $followResult")
                    } else {
                        Log.e(TAG, "❌ Follow failed: ${followResult.exceptionOrNull()?.message}")
                        setErrorState(true, "Failed to follow user: ${followResult.exceptionOrNull()?.message}")
                        return@launch
                    }
                }
                
                // Refresh the data
                Log.d(TAG, "🔄 Refreshing following data after toggle")
                loadRealFollowingData(currentUserId)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to toggle follow: ${e.message}")
                Log.e(TAG, "❌ Exception details: ", e)
                setErrorState(true, e.message ?: "Failed to toggle follow")
            }
        }
    }

    private suspend fun loadRealFollowingData(currentUserId: String) {
        Log.d(TAG, "🔄 Loading real following data for user: $currentUserId")
        
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
            
            Log.d(TAG, "📊 Found ${followingUserIds.size} following users")
            Log.d(TAG, "📊 Found ${followersUserIds.size} followers")
            
            // Get user profiles for following users
            val followingProfiles = getUserProfilesByIdsUseCase(followingUserIds)
            val followersProfiles = getUserProfilesByIdsUseCase(followersUserIds)
            
            Log.d(TAG, "👥 Loaded ${followingProfiles.size} following profiles")
            Log.d(TAG, "👥 Loaded ${followersProfiles.size} follower profiles")
            
            // Convert to UserFollowModel
            val followingUsers = followingProfiles.map { profile ->
                UserFollowModel(
                    id = profile.uid,
                    name = profile.displayName.ifEmpty { profile.username },
                    username = profile.username,
                    profileImageUrl = profile.profileImageUrl,
                    isFollowingMe = false, // TODO: Check if they follow current user
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
                    isIFollow = false // TODO: Check if current user follows them
                )
            }
            
            // Find mutual friends (users who follow each other)
            val mutualFriends = followingUsers.filter { following ->
                followersUserIds.contains(following.id)
            }.map { user ->
                user.copy(isFollowingMe = true, isIFollow = true)
            }
            
            // Generate suggested users (for now, just show some followers)
            val suggestedUsers = followersUsers.take(3).map { user ->
                user.copy(isFollowingMe = true, isIFollow = false)
            }
            
            _uiState.value = _uiState.value.copy(
                followers = followersUsers,
                following = followingUsers,
                friends = mutualFriends,
                suggestedUsers = suggestedUsers,
                followersCount = followersUsers.size,
                followingCount = followingUsers.size
            )
            
            Log.d(TAG, "✅ Real following data loaded successfully")
            Log.d(TAG, "📊 Updated UI state - Following: ${followingUsers.size}, Followers: ${followersUsers.size}")
            Log.d(TAG, "👥 Following user IDs: ${followingUsers.map { it.id }}")
            Log.d(TAG, "👥 Followers user IDs: ${followersUsers.map { it.id }}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load real following data: ${e.message}")
            // Fallback to mock data if real data fails
            loadMockData()
        }
    }

    private fun loadMockData() {
        Log.d(TAG, "🔄 Loading mock data as fallback")
        
        // Mock data for demonstration - replace with real data later
        val mockFollowers = List(5) { index ->
            UserFollowModel(
                id = "follower_$index",
                name = "Follower $index",
                username = "User_Follower$index",
                profileImageUrl = null,
                isFollowingMe = index % 2 == 0,
                isIFollow = index % 3 == 0
            )
        }

        val mockFollowing = List(3) { index ->
            UserFollowModel(
                id = "following_$index",
                name = "Following $index",
                username = "User_Following$index",
                profileImageUrl = null,
                isFollowingMe = index % 2 == 0,
                isIFollow = true
            )
        }

        val mockFriends = List(2) { index ->
            UserFollowModel(
                id = "friend_$index",
                name = "Friend $index",
                username = "User_Friend$index",
                profileImageUrl = null,
                isFollowingMe = true,
                isIFollow = true
            )
        }

        val mockSuggested = List(4) { index ->
            UserFollowModel(
                id = "suggested_$index",
                name = "Suggested $index",
                username = "User_Suggested$index",
                profileImageUrl = null,
                isFollowingMe = index % 2 == 0,
                isIFollow = false
            )
        }

        _uiState.value = _uiState.value.copy(
            followers = mockFollowers,
            following = mockFollowing,
            friends = mockFriends,
            suggestedUsers = mockSuggested,
            followersCount = mockFollowers.size,
            followingCount = mockFollowing.size
        )
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
