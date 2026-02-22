package com.project.e_commerce.android.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.domain.model.Post
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserFollowModel
import com.project.e_commerce.domain.model.UserPost
import com.project.e_commerce.domain.model.UserProfile
import com.project.e_commerce.domain.usecase.post.CreatePostUseCase
import com.project.e_commerce.domain.usecase.user.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SocialUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchResults: List<UserProfile> = emptyList(),
    val followers: List<UserFollowModel> = emptyList(),
    val following: List<UserFollowModel> = emptyList(),
    val userPosts: List<UserPost> = emptyList(),
    val selectedUser: UserProfile? = null,
    val isFollowing: Boolean = false,
    val isFollowedBy: Boolean = false,
    val createdPost: Post? = null,
    val isCreatingPost: Boolean = false,
    val createPostError: String? = null
)

class SocialViewModel(
    private val createPostUseCase: CreatePostUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val getFollowersUseCase: GetFollowersUseCase,
    private val getFollowingUseCase: GetFollowingUseCase,
    private val getFollowingStatusUseCase: GetFollowingStatusUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getUserPostsUseCase: GetUserPostsUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val searchUsersUseCase: SearchUsersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = searchUsersUseCase(query)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, searchResults = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
                is Result.Loading -> { /* Already showing loading state */ }
            }
        }
    }

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getUserProfileUseCase(userId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, selectedUser = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
                is Result.Loading -> { /* Already showing loading state */ }
            }
        }
    }

    fun loadUserPosts(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getUserPostsUseCase(userId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, userPosts = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
                is Result.Loading -> { /* Already showing loading state */ }
            }
        }
    }

    fun checkFollowingStatus(currentUserId: String, targetUserId: String) {
        viewModelScope.launch {
            when (val result = getFollowingStatusUseCase(currentUserId, targetUserId)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isFollowing = result.data.isFollowing,
                            isFollowedBy = result.data.isFollowedBy
                        ) 
                    }
                }
                is Result.Error -> {
                    // Handle error silently or log
                }
                is Result.Loading -> { /* Loading state not needed here */ }
            }
        }
    }

    fun followUser(followerId: String, followedId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = followUserUseCase(followerId, followedId)) {
                is Result.Success -> {
                     _uiState.update { it.copy(isLoading = false, isFollowing = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
                is Result.Loading -> { /* Already showing loading state */ }
            }
        }
    }

    fun unfollowUser(followerId: String, followedId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
             when (val result = unfollowUserUseCase(followerId, followedId)) {
                is Result.Success -> {
                     _uiState.update { it.copy(isLoading = false, isFollowing = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
                is Result.Loading -> { /* Already showing loading state */ }
            }
        }
    }

    fun loadFollowers(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getFollowersUseCase(userId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, followers = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
                is Result.Loading -> { /* Already showing loading state */ }
            }
        }
    }

    fun loadFollowing(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getFollowingUseCase(userId)) {
                 is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, following = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
                is Result.Loading -> { /* Already showing loading state */ }
            }
        }
    }

    fun updateUserProfile(userId: String, username: String, bio: String, photoUrl: String?) {
        viewModelScope.launch {
             _uiState.update { it.copy(isLoading = true) }
            val updatedProfile = UserProfile(
                uid = userId,
                username = username,
                bio = bio,
                profileImageUrl = photoUrl
            )
            when (val result = updateUserProfileUseCase(updatedProfile)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
                is Result.Loading -> { /* Already showing loading state */ }
            }
        }
    }
    
    fun selectUser(user: UserProfile) {
        _uiState.update { it.copy(selectedUser = user) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Create a new post/reel
     * @param type "reel", "product", or "photo"
     * @param mediaUrl URL of uploaded media
     * @param caption Optional description
     */
    fun createPost(type: String, mediaUrl: String, caption: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingPost = true, createPostError = null, createdPost = null) }
            when (val result = createPostUseCase(type, mediaUrl, caption)) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            isCreatingPost = false, 
                            createdPost = result.data,
                            createPostError = null
                        ) 
                    }
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(
                            isCreatingPost = false, 
                            createPostError = result.error.message
                        ) 
                    }
                }
                is Result.Loading -> { /* Already showing loading state */ }
            }
        }
    }
    
    /**
     * Clear created post state (after navigation or display)
     */
    fun clearCreatedPost() {
        _uiState.update { it.copy(createdPost = null, createPostError = null) }
    }
}
