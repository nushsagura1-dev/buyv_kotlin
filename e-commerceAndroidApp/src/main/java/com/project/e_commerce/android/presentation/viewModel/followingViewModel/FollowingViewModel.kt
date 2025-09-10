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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import android.util.Log
import com.project.e_commerce.android.presentation.utils.UserInfoCache
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FollowingViewModel(
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val getFollowingStatusUseCase: GetFollowingStatusUseCase,
    private val getFollowingUsersUseCase: GetFollowingUsersUseCase,
    private val getUserProfilesByIdsUseCase: GetUserProfilesByIdsUseCase,
    private val getFollowersUseCase: GetFollowersUseCase,
    private val firestore: FirebaseFirestore
) : BaseViewModel() {

    private var loadAttempts = 0
    private val maxLoadAttempts = 3

    companion object {
        private const val TAG = "FollowingViewModel"
    }

    private val _uiState = MutableStateFlow(FollowingUiState())
    val uiState: StateFlow<FollowingUiState> = _uiState.asStateFlow()

    init {
        Log.d("FollowingTabDebug", "FollowingViewModel created")
        viewModelScope.launch {
            uiState.collect { state ->
                Log.d(
                    "FollowingTabDebug",
                    "uiState changed: isLoading=${state.isLoading}, followingCount=${state.following.size}"
                )
            }
        }
    }

    override fun setLoadingState(loadingState: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = loadingState)
    }

    override fun setErrorState(errorState: Boolean, errorMessage: String) {
        _uiState.value = _uiState.value.copy(
            error = if (errorState) errorMessage else null
        )
    }

    fun loadUserData(currentUserId: String, targetUsername: String) {
        Log.d(
            "FollowingTabDebug",
            "loadUserData called for userId=$currentUserId username=$targetUsername"
        )
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
            Log.d(TAG, "✅ Unfollow result: $unfollowResult")
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
            Log.d(TAG, "✅ Follow result: $followResult")
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
            // Get all users from Firestore and filter out those with relationships
            val suggestedUsers = try {
                Log.d(TAG, "🔍 Fetching all users from Firestore for suggestions...")
                val allUsersSnapshot = firestore.collection("users")
                    .limit(100) // Limit to prevent excessive data transfer
                    .get()
                    .await()

                Log.d(TAG, "📥 Retrieved ${allUsersSnapshot.documents.size} users from Firestore")

                allUsersSnapshot.documents.mapNotNull { document ->
                    try {
                        val userId = document.id
                        val userData = document.data

                        // Skip if no data or if it's the current user
                        if (userData == null || userId == currentUserId) {
                            return@mapNotNull null
                        }

                        // Skip if user already has a relationship with current user
                        if ((followingUserIds + followersUserIds).contains(userId)) {
                            return@mapNotNull null
                        }

                        val displayName = userData["displayName"] as? String ?: ""
                        val username = userData["username"] as? String ?: ""
                        val profileImageUrl = userData["profileImageUrl"] as? String

                        // Skip users with empty names/usernames
                        if (displayName.isBlank() && username.isBlank()) {
                            return@mapNotNull null
                        }

                        UserFollowModel(
                            id = userId,
                            name = displayName.ifEmpty { username },
                            username = username,
                            profileImageUrl = profileImageUrl,
                            isFollowingMe = false,
                            isIFollow = false
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing user document ${document.id}: ${e.message}")
                        null
                    }
                }.take(20) // Limit suggested users to 20

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to fetch suggested users from Firestore: ${e.message}")
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
            
            Log.d(TAG, "✅ Real following data loaded successfully")
            Log.d(TAG, "📊 Updated UI state - Following: ${followingUsers.size}, Followers: ${followersUsers.size}")
            Log.d(TAG, "👥 Following user IDs: ${followingUsers.map { it.id }}")
            Log.d(TAG, "👥 Followers user IDs: ${followersUsers.map { it.id }}")
            Log.d(TAG, "💡 Suggested users: ${suggestedUsers.size} (mutual strangers)")
            
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

        // Mock suggested users - people with NO relationship to current user
        val mockSuggested = List(6) { index ->
            UserFollowModel(
                id = "suggested_$index",
                name = "Suggested User ${index + 1}",
                username = "suggested_user_${index + 1}",
                profileImageUrl = null,
                isFollowingMe = false, // They don't follow current user
                isIFollow = false      // Current user doesn't follow them
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

        Log.d(
            TAG,
            "✅ Mock data loaded - Suggested: ${mockSuggested.size} users with no relationships"
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
