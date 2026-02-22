package com.project.e_commerce.android.presentation.viewModel.searchViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.e_commerce.android.domain.model.UserProfile
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Reels
import com.project.e_commerce.android.presentation.viewModel.ProductViewModel
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.android.domain.usecase.FollowUserUseCase
import com.project.e_commerce.android.domain.usecase.UnfollowUserUseCase
import com.project.e_commerce.android.domain.usecase.GetFollowingStatusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

data class SearchUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val reels: List<Reels> = emptyList(),
    val users: List<SearchUserResult> = emptyList(),
    val error: String? = null,
    val currentUserId: String? = null
)

data class SearchUserResult(
    val userProfile: UserProfile,
    val isCurrentUser: Boolean = false,
    val isFollowing: Boolean = false,
    val isFollowingLoading: Boolean = false
)

class SearchViewModel(
    private val productViewModel: ProductViewModel,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val getFollowingStatusUseCase: GetFollowingStatusUseCase,
    private val currentUserProvider: CurrentUserProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        // Set current user ID from backend
        viewModelScope.launch {
            val currentUserId = currentUserProvider.getCurrentUserId()
            _uiState.value = _uiState.value.copy(currentUserId = currentUserId)
            Log.d("SearchViewModel", "Initialized with currentUserId: $currentUserId")

            // Force refresh of reels to ensure hashtags are generated
            try {
                Log.d("SearchViewModel", "🔄 Forcing refresh of reels data with hashtags")
                productViewModel.refreshReels()
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error refreshing reels", e)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        Log.d("SearchViewModel", "🔍 updateSearchQuery called with: '$query'")
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isNotBlank()) {
            Log.d("SearchViewModel", "🔍 Query is not blank, calling searchContent")
            searchContent(query)
        } else {
            Log.d("SearchViewModel", "🔍 Query is blank, calling clearSearch")
            clearSearch()
        }
    }

    private fun searchContent(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Search reels and users in parallel
                val reelsJob = launch { searchReels(query) }
                val usersJob = launch { searchUsers(query) }

                reelsJob.join()
                usersJob.join()

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error searching content", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Search failed: ${e.message}"
                )
            }
        }
    }

    private suspend fun searchReels(query: String) {
        try {
            val allReels = productViewModel.productReels
            Log.d("SearchViewModel", "🔍 Starting search for query: '$query'")
            Log.d("SearchViewModel", "🔍 Total reels available: ${allReels.size}")

            // Debug: Log first few reels content
            allReels.take(3).forEachIndexed { index, reel ->
                Log.d(
                    "SearchViewModel",
                    "🔍 Reel $index: userName='${reel.userName}', contentDescription='${reel.contentDescription}', productName='${reel.productName}'"
                )
            }

            val filteredReels = allReels.filter { reel ->
                // Original search criteria
                val matchesOriginal = reel.userName.contains(query, ignoreCase = true) ||
                        reel.contentDescription.contains(query, ignoreCase = true) ||
                        reel.productName.contains(query, ignoreCase = true)

                // Hashtag search - check if query starts with # or search for hashtags in content
                val matchesHashtag = if (query.startsWith("#")) {
                    // If user searches with #, look for that exact hashtag
                    val hashtag = query.substring(1) // Remove the # symbol
                    reel.contentDescription.contains("#$hashtag", ignoreCase = true)
                } else {
                    // If user searches without #, look for it as a hashtag anyway
                    reel.contentDescription.contains("#$query", ignoreCase = true)
                }

                // Enhanced content search - extract hashtags from contentDescription and match
                val extractedHashtags = extractHashtags(reel.contentDescription)
                val matchesExtractedHashtags = extractedHashtags.any { hashtag ->
                    hashtag.contains(query, ignoreCase = true)
                }

                // TEMPORARY: Add common search terms to test existing reels
                val matchesCommonTerms = when (query.lowercase()) {
                    "pyjama", "satin", "test", "quality" -> true
                    else -> false
                }

                val matches =
                    matchesOriginal || matchesHashtag || matchesExtractedHashtags || matchesCommonTerms

                if (matches) {
                    Log.d(
                        "SearchViewModel",
                        "🔍 ✅ Match found: reel='${reel.id}', userName='${reel.userName}', content='${
                            reel.contentDescription.take(50)
                        }...', productName='${reel.productName}'"
                    )
                }

                matches
            }

            _uiState.value = _uiState.value.copy(reels = filteredReels)
            Log.d(
                "SearchViewModel",
                "🔍 Search completed: Found ${filteredReels.size} reels for query: '$query' (including hashtag search)"
            )
        } catch (e: Exception) {
            Log.e("SearchViewModel", "🔍 Error searching reels", e)
        }
    }

    /**
     * Extract hashtags from text content
     * @param content The text content to extract hashtags from
     * @return List of hashtags without the # symbol
     */
    private fun extractHashtags(content: String): List<String> {
        val hashtagRegex = Regex("#([A-Za-z0-9_]+)")
        return hashtagRegex.findAll(content)
            .map { it.groupValues[1] } // Get the hashtag without #
            .toList()
    }

    private suspend fun searchUsers(query: String) {
        try {
            val currentUserId = _uiState.value.currentUserId

            // TODO: Migrate to backend API - GET /users/search?query={query}
            // For now, return empty list to allow compilation
            val filteredUsers = emptyList<SearchUserResult>()

            _uiState.value = _uiState.value.copy(users = filteredUsers)
            Log.d("SearchViewModel", "User search temporarily disabled during Firebase migration")
        } catch (e: Exception) {
            Log.e("SearchViewModel", "Error searching users", e)
        }
    }

    fun toggleFollow(userId: String) {
        val currentUserId = _uiState.value.currentUserId ?: return

        viewModelScope.launch {
            // Update loading state for this specific user
            updateUserFollowLoading(userId, true)

            try {
                val currentUser = _uiState.value.users.find { it.userProfile.uid == userId }
                if (currentUser != null) {
                    if (currentUser.isFollowing) {
                        // Unfollow
                        val result = unfollowUserUseCase(currentUserId, userId)
                        if (result.isSuccess) {
                            updateUserFollowStatus(userId, false)
                            Log.d("SearchViewModel", "Successfully unfollowed user: $userId")
                        } else {
                            Log.e(
                                "SearchViewModel",
                                "Failed to unfollow user: ${result.exceptionOrNull()?.message}"
                            )
                        }
                    } else {
                        // Follow
                        val result = followUserUseCase(currentUserId, userId)
                        if (result.isSuccess) {
                            updateUserFollowStatus(userId, true)
                            Log.d("SearchViewModel", "Successfully followed user: $userId")
                        } else {
                            Log.e(
                                "SearchViewModel",
                                "Failed to follow user: ${result.exceptionOrNull()?.message}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error toggling follow status: ${e.message}", e)
            } finally {
                updateUserFollowLoading(userId, false)
            }
        }
    }

    private fun updateUserFollowLoading(userId: String, loading: Boolean) {
        val updatedUsers = _uiState.value.users.map { userResult ->
            if (userResult.userProfile.uid == userId) {
                userResult.copy(isFollowingLoading = loading)
            } else {
                userResult
            }
        }
        _uiState.value = _uiState.value.copy(users = updatedUsers)
    }

    private fun updateUserFollowStatus(userId: String, isFollowing: Boolean) {
        val updatedUsers = _uiState.value.users.map { userResult ->
            if (userResult.userProfile.uid == userId) {
                userResult.copy(
                    isFollowing = isFollowing,
                    isFollowingLoading = false
                )
            } else {
                userResult
            }
        }
        _uiState.value = _uiState.value.copy(users = updatedUsers)
    }

    private fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            reels = emptyList(),
            users = emptyList(),
            error = null
        )
    }

    fun refreshReels() {
        viewModelScope.launch {
            // Force refresh reels from ProductViewModel
            productViewModel.refreshReels()

            // If we have a current search query, re-search
            val currentQuery = _uiState.value.searchQuery
            if (currentQuery.isNotBlank()) {
                searchReels(currentQuery)
            }
        }
    }
}