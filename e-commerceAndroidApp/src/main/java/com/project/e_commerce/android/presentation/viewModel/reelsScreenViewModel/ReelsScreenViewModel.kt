package com.project.e_commerce.android.presentation.viewModel.reelsScreenViewModel

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.viewModel.baseViewModel.BaseViewModel
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Comment
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.LoveItem
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.NewComment
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Ratings
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Reels
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import com.project.e_commerce.android.presentation.viewModel.ProductViewModel
import kotlinx.coroutines.delay
import com.project.e_commerce.data.local.CurrentUserProvider
import com.project.e_commerce.android.domain.usecase.GetUserProfileUseCase
import com.project.e_commerce.domain.usecase.post.LikePostUseCase
import com.project.e_commerce.domain.usecase.post.UnlikePostUseCase
import com.project.e_commerce.domain.usecase.post.CheckPostLikeStatusUseCase
import com.project.e_commerce.domain.usecase.comment.AddCommentUseCase
import com.project.e_commerce.domain.usecase.comment.GetCommentsUseCase
import com.project.e_commerce.domain.usecase.comment.LikeCommentUseCase
import com.project.e_commerce.domain.model.UserProfile
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.android.domain.repository.CartRepository
import com.project.e_commerce.android.data.model.CartStats
import com.project.e_commerce.domain.repository.PostRepository

class ReelsScreenViewModel(
    private val productViewModel: ProductViewModel,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val unlikePostUseCase: UnlikePostUseCase,
    private val checkPostLikeStatusUseCase: CheckPostLikeStatusUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val likeCommentUseCase: LikeCommentUseCase,
    private val postRepository: PostRepository,
    private val cartRepository: CartRepository? = null, // ⚠️ MIGRATION: Made optional
    private val currentUserProvider: CurrentUserProvider
) : BaseViewModel(), ReelsScreenInteraction {

    private val _state: MutableStateFlow<List<Reels>> = MutableStateFlow(listOf())
    val state: StateFlow<List<Reels>> get() = _state

    private val _showAddToCart = MutableStateFlow(false)
    val showAddToCart: StateFlow<Boolean> get() = _showAddToCart.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> get() = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage.asStateFlow()

    init {
        try {
            Log.d("ReelsScreenViewModel", "🚀 ReelsScreenViewModel initialized")
            viewModelScope.launch {
                try {
                    _isLoading.emit(true)
                    _errorMessage.emit(null)

                    waitForAuthentication()

                    loadReelsWithRetry()

                } catch (e: Exception) {
                    Log.e("ReelsScreenViewModel", "Error in init LaunchedEffect", e)
                    _errorMessage.emit("Failed to initialize: ${e.message}")
                    _isLoading.emit(false)
                }
            }
        } catch (e: Exception) {
            Log.e("ReelsScreenViewModel", "Error in init block", e)
        }
    }

    private suspend fun waitForAuthentication() {
        var attempts = 0
        while (attempts < 10) {
            val userId = currentUserProvider.getCurrentUserId()
            if (userId != null) {
                Log.d("ReelsScreenViewModel", "✅ Authentication ready: $userId")
                return
            }
            delay(500)
            attempts++
        }
        Log.w("ReelsScreenViewModel", "⚠️ Authentication timeout, proceeding anyway")
    }

    private suspend fun loadReelsWithRetry() {
        var attempts = 0
        val maxAttempts = 3

        while (attempts < maxAttempts) {
            try {
                Log.d("ReelsScreenViewModel", "🔄 Loading reels attempt ${attempts + 1}")

                ensureProductsLoaded()

                val reels = loadReelsFromDatabase()

                if (reels.isNotEmpty()) {
                    Log.d("ReelsScreenViewModel", "✅ Successfully loaded ${reels.size} reels")
                    _state.emit(reels)
                    _isLoading.emit(false)
                    _errorMessage.emit(null)
                    return
                } else {
                    Log.w("ReelsScreenViewModel", "⚠️ No reels loaded on attempt ${attempts + 1}")
                }
                
            } catch (e: Exception) {
                Log.e("ReelsScreenViewModel", "❌ Error loading reels on attempt ${attempts + 1}", e)
            }

            attempts++
            if (attempts < maxAttempts) {
                delay(1000L * attempts)
            }
        }

        // No reels found is a valid empty state (e.g. no marketplace products created yet), not an error
        Log.w("ReelsScreenViewModel", "⚠️ No reels found after $maxAttempts attempts — showing empty state")
        _isLoading.emit(false)
        _state.emit(emptyList())
        _errorMessage.emit(null)
    }

    private suspend fun ensureProductsLoaded() {
        var attempts = 0
        while (attempts < 20) {
            if (productViewModel.allProducts.isNotEmpty()) {
                Log.d(
                    "ReelsScreenViewModel",
                    "✅ Products loaded: ${productViewModel.allProducts.size}"
                )
                return
            }
            delay(500)
            attempts++
        }
        Log.w("ReelsScreenViewModel", "⚠️ ProductViewModel products still empty after timeout")
    }

    private suspend fun loadReelsFromDatabase(): List<Reels> {
        Log.d("ReelsScreenViewModel", "🚀 Loading reels from database")
        return try {
            delay(1000)

            val reels = productViewModel.productReels
            Log.d("ReelsScreenViewModel", "🚀 Received ${reels.size} reels from ProductViewModel")

            if (reels.isEmpty()) {
                val products = productViewModel.allProducts
                if (products.isNotEmpty()) {
                    Log.d(
                        "ReelsScreenViewModel",
                        "🔄 Generating reels from ${products.size} products"
                    )
                    productViewModel.refreshReels()
                    delay(500)
                    val freshReels = productViewModel.productReels
                    loadLikeStatesForReels(freshReels)
                } else {
                    Log.w("ReelsScreenViewModel", "⚠️ No products available either")
                    emptyList()
                }
            } else {
                loadLikeStatesForReels(reels)
            }
        } catch (e: Exception) {
            Log.e("ReelsScreenViewModel", "❌ Error loading reels from database", e)
            throw e
        }
    }

    private suspend fun loadLikeStatesForReels(reels: List<Reels>): List<Reels> {
        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null) {
            Log.w("ReelsScreenViewModel", "No current user, cannot load like states and comments")
            return reels
        }

        Log.d("ReelsScreenViewModel", "🔄 Loading like/bookmark/comment states for ${reels.size} reels")

        return coroutineScope {
            reels.map { reel ->
                async {
                    try {
                        // For actual reels (postUid already set from feed), use the post UID directly
                        // No need to call the bridge endpoint — it would create incorrect shadow posts
                        // Only use actual postUid from backend — reel.id is a marketplace product UUID,
                        // NOT a valid post UID, so don't fall back to it
                        val postUid = reel.postUid?.takeIf { it.isNotBlank() }

                        Log.d("ReelsScreenViewModel", "✅ Using postUid=$postUid for reel ${reel.id} (postUid was ${reel.postUid})")

                        // Load comments for this post (only if we have a valid postUid)
                        val comments = if (postUid.isNullOrBlank()) {
                            reel.comments
                        } else try {
                            when (val result = getCommentsUseCase(postUid)) {
                                is Result.Success -> result.data.map { comment ->
                                    Comment(
                                        id = comment.id.toString(),
                                        userId = comment.userId,
                                        userName = comment.displayName.ifEmpty { comment.username },
                                        comment = comment.content,
                                        time = "",
                                        isLoved = comment.isLiked
                                    )
                                }
                                is Result.Error -> {
                                    Log.w("ReelsScreenViewModel", "⚠️ Failed to load comments for post $postUid: ${result.error.message}")
                                    reel.comments
                                }
                                is Result.Loading -> reel.comments
                            }
                        } catch (e: Exception) {
                            Log.w("ReelsScreenViewModel", "⚠️ Error loading comments: ${e.message}")
                            reel.comments
                        }

                        reel.copy(
                            postUid = postUid,
                            comments = comments,
                            numberOfComments = comments.size
                        )
                    } catch (e: Exception) {
                        Log.w("ReelsScreenViewModel", "⚠️ Error loading state for reel ${reel.id}: ${e.message}")
                        reel
                    }
                }
            }.awaitAll()
        }
    }

    fun refreshReels() {
        Log.d("ReelsScreenViewModel", "🚀 Refreshing reels")
        viewModelScope.launch {
            loadReelsWithRetry()
        }
    }
    
    fun forceRefreshFromProductViewModel() {
        viewModelScope.launch {
            Log.d("ReelsScreenViewModel", "🔄 forceRefreshFromProductViewModel called")
            productViewModel.refreshProducts()
            productViewModel.refreshReels()
            delay(1000)
            loadReelsWithRetry()
        }
    }

    fun ensureReelsLoaded() {
        viewModelScope.launch {
            Log.d("ReelsScreenViewModel", "🔍 ensureReelsLoaded called - current reels in state: ${_state.value.size}")
            if (_state.value.isEmpty()) {
                Log.d("ReelsScreenViewModel", "🔄 No reels in state, loading from database...")
                loadReelsWithRetry()
            } else {
                Log.d("ReelsScreenViewModel", "✅ Reels already loaded: ${_state.value.size}")
            }
        }
    }

    fun getCurrentReelsCount(): Int {
        return _state.value.size
    }
    
    fun debugCurrentState() {
        Log.d("ReelsScreenViewModel", "🔍 DEBUG: Current state analysis")
        Log.d("ReelsScreenViewModel", "📱 Reels in state: ${_state.value.size}")
        Log.d("ReelsScreenViewModel", "📱 Reels IDs: ${_state.value.map { it.id }}")
        Log.d("ReelsScreenViewModel", "📱 ProductViewModel reels: ${productViewModel.productReels.size}")
        Log.d("ReelsScreenViewModel", "📱 ProductViewModel reels IDs: ${productViewModel.productReels.map { it.id }}")
        Log.d("ReelsScreenViewModel", "📱 ProductViewModel allProducts: ${productViewModel.allProducts.size}")
        Log.d("ReelsScreenViewModel", "📱 ProductViewModel allProducts IDs: ${productViewModel.allProducts.map { it.id }}")
    }
    
    private suspend fun updateReelsWithRealUserData(reels: List<Reels>): List<Reels> {
        return try {
            Log.d("ReelsScreenViewModel", "🔄 Starting user data update for ${reels.size} reels")
            val updatedReels = mutableListOf<Reels>()
            
            for (reel in reels) {
                Log.d("ReelsScreenViewModel", "🔍 Processing reel ${reel.id} - current userName: '${reel.userName}', userId: '${reel.userId}'")
                
                val updatedReel = if (reel.userId.isNotEmpty() && reel.userId != "unknown_user" && reel.userName.startsWith("User_")) {
                    Log.d("ReelsScreenViewModel", "🔄 Fetching user profile for userId: ${reel.userId}")
                    val userProfileResult = getUserProfileUseCase(reel.userId)
                    if (userProfileResult.isSuccess) {
                        val userProfile = userProfileResult.getOrNull()
                        if (userProfile != null) {
                            val newUserName = when {
                                userProfile.displayName.isNotBlank() -> userProfile.displayName
                                userProfile.username.isNotBlank() -> userProfile.username
                                else -> "User_${reel.userId.take(6)}"
                            }
                            Log.d("ReelsScreenViewModel", "✅ Got real user data: '${newUserName}' for userId: ${reel.userId}")
                            Log.d("ReelsScreenViewModel", "🔍 Profile details - displayName: '${userProfile.displayName}', username: '${userProfile.username}'")
                            reel.copy(userName = newUserName)
                        } else {
                            Log.w("ReelsScreenViewModel", "⚠️ User profile is null for userId: ${reel.userId}")
                            reel.copy(userName = "User_${reel.userId.take(6)}")
                        }
                    } else {
                        Log.w("ReelsScreenViewModel", "⚠️ Failed to fetch user profile for userId: ${reel.userId}")
                        reel.copy(userName = "User_${reel.userId.take(6)}")
                    }
                } else {
                    Log.d("ReelsScreenViewModel", "ℹ️ Keeping reel as is - userName: '${reel.userName}', userId: '${reel.userId}'")
                    reel
                }
                updatedReels.add(updatedReel)
            }
            
            Log.d("ReelsScreenViewModel", "✅ Updated ${updatedReels.size} reels with real user data")
            updatedReels
        } catch (e: Exception) {
            Log.e("ReelsScreenViewModel", "❌ Error updating reels with user data: ${e.message}")
            e.printStackTrace()
            reels
        }
    }

    override fun setLoadingState(loadingState: Boolean) {

    }

    override fun setErrorState(errorState: Boolean, errorMessage: String) {

    }


    fun forceLoveReels(reelsId: String) {
        viewModelScope.launch {
            _state.value = _state.value.map { reel ->
                if (reel.id == reelsId) {
                    reel.copy(love = reel.love.copy(isLoved = true))
                } else reel
            }
        }
    }

    fun onWriteNewComment(comment: String) {
        // This function updates the comment text for all reels (which is what the UI expects)
        // The UI will handle which reel is currently being viewed
        val copyState = _state.value.map { reel ->
            reel.copy(newComment = NewComment(comment))
        }
        viewModelScope.launch {
            _state.emit(copyState)
        }
    }

    fun onWriteNewCommentForReel(reelId: String, comment: String) {
        Log.d(
            "CommentTextFieldDebug",
            "🔄 onWriteNewCommentForReel called with reelId='$reelId', comment='$comment'"
        )
        Log.d("CommentTextFieldDebug", "🔄 Current state size: ${_state.value.size}")

        // Log all reel IDs in current state
        _state.value.forEachIndexed { index, reel ->
            Log.d(
                "CommentTextFieldDebug",
                "🔄 State[$index]: id='${reel.id}', newComment='${reel.newComment.comment}'"
            )
        }

        val copyState = _state.value.map { reel ->
            if (reel.id == reelId) {
                Log.d(
                    "CommentTextFieldDebug",
                    "✅ Found matching reel! Updating comment from '${reel.newComment.comment}' to '$comment'"
                )
                reel.copy(newComment = NewComment(comment))
            } else {
                Log.d(
                    "CommentTextFieldDebug",
                    "⏭️ Skipping reel with id='${reel.id}' (doesn't match '$reelId')"
                )
                reel
            }
        }

        // Log the updated state
        copyState.forEachIndexed { index, reel ->
            Log.d(
                "CommentTextFieldDebug",
                "🔄 UpdatedState[$index]: id='${reel.id}', newComment='${reel.newComment.comment}'"
            )
        }

        viewModelScope.launch {
            Log.d("CommentTextFieldDebug", "🚀 Emitting updated state...")
            _state.emit(copyState)
            Log.d("CommentTextFieldDebug", "✅ State emitted successfully")
        }
    }

    override fun onClickSearchButton(navController: NavController) {
        navController.navigate(Screens.ReelsScreen.SearchReelsAndUsersScreen.route)
    }

    override fun onClackLoveReelsButton(reelId: String) {
        viewModelScope.launch {
            val userId = currentUserProvider.getCurrentUserId()
            if (userId == null) {
                Log.e("ReelsScreenViewModel", "User must be logged in to like a reel")
                return@launch
            }

            try {
                // Show immediate visual feedback with a simple state toggle
                val currentReel = _state.value.find { it.id == reelId }
                if (currentReel != null) {
                    val isCurrentlyLiked = currentReel.love.isLoved
                    val newLikeState = !isCurrentlyLiked
                    val newCount =
                        if (newLikeState) currentReel.love.number + 1 else currentReel.love.number - 1

                    // Immediate UI feedback
                    val immediateState = _state.value.map { reel ->
                        if (reel.id == reelId) {
                            reel.copy(
                                love = reel.love.copy(
                                    number = newCount,
                                    isLoved = newLikeState
                                )
                            )
                        } else reel
                    }
                    _state.emit(immediateState)

                    // Sync with backend using the post UID
                    val postUid = currentReel.postUid
                    if (postUid != null) {
                        try {
                            val result = if (newLikeState) {
                                likePostUseCase(postUid, userId)
                            } else {
                                unlikePostUseCase(postUid, userId)
                            }
                            when (result) {
                                is Result.Success -> {
                                    Log.d("ReelsScreenViewModel", "❤️ Backend like sync success: reel $reelId, post $postUid, liked=$newLikeState")
                                }
                                is Result.Error -> {
                                    Log.e("ReelsScreenViewModel", "❌ Backend like sync failed: ${result.error.message}")
                                    // Revert on failure
                                    val revertState = _state.value.map { reel ->
                                        if (reel.id == reelId) {
                                            reel.copy(love = reel.love.copy(number = currentReel.love.number, isLoved = isCurrentlyLiked))
                                        } else reel
                                    }
                                    _state.emit(revertState)
                                }
                                is Result.Loading -> { /* ignore */ }
                            }
                        } catch (e: Exception) {
                            Log.e("ReelsScreenViewModel", "❌ Backend like sync error: ${e.message}")
                        }
                    } else {
                        Log.w("ReelsScreenViewModel", "⚠️ No postUid for reel $reelId — like is local-only")
                    }

                    Log.d(
                        "ReelsScreenViewModel",
                        "❤️ Like toggle: reel $reelId liked=$newLikeState, count=$newCount"
                    )
                }

            } catch (e: Exception) {
                Log.e("ReelsScreenViewModel", "❌ Error during like operation: ${e.message}")
            }
        }
    }

    override fun onClickCartButton() {
        _showAddToCart.value = true
    }


    override fun onClickMoreButton(shareLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>) {
        val sendIntent: Intent = Intent().apply{
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "www.google.com")
            type = "text/plain"
        }
        shareLauncher.launch(Intent.createChooser(sendIntent, null))
    }

    override fun onPauseVideo() {
        TODO("Not yet implemented")
    }

    override fun onResumeVideo() {
        TODO("Not yet implemented")
    }

    override fun onTwoClickToVideo() {
        TODO("Not yet implemented")
    }

    override fun onLoveComment(reelsId: String, commentId: String) {

    }

    override fun onClickAddComment(videoId: String, comment: String) {
        viewModelScope.launch {
            val currentUser = currentUserProvider.getCurrentUser()
            if (currentUser == null) {
                Log.e("ReelsScreenViewModel", "User must be logged in to comment")
                return@launch
            }

            if (comment.isBlank()) {
                Log.w("ReelsScreenViewModel", "Comment cannot be empty")
                return@launch
            }

            try {
                val userName = currentUser.displayName ?: currentUser.email?.split("@")?.firstOrNull() ?: "User"

                Log.d(
                    "ReelsScreenViewModel",
                    "💬 Adding comment to reel $videoId: '$comment' by $userName"
                )

                // Find the reel to get its postUid
                val currentReel = _state.value.find { it.id == videoId }
                val postUid = currentReel?.postUid

                if (postUid != null) {
                    // Send comment to backend
                    try {
                        when (val result = addCommentUseCase(postUid, comment.trim(), currentUser.uid)) {
                            is Result.Success -> {
                                val backendComment = result.data
                                val newAndroidComment = Comment(
                                    id = backendComment.id.toString(),
                                    userId = currentUser.uid,
                                    userName = userName,
                                    comment = backendComment.content,
                                    time = "",
                                    isLoved = false,
                                    reply = emptyList()
                                )
                                val updatedState = _state.value.map { reel ->
                                    if (reel.id == videoId) {
                                        val updatedComments = reel.comments.toMutableList().apply { add(0, newAndroidComment) }
                                        reel.copy(
                                            comments = updatedComments,
                                            numberOfComments = updatedComments.size,
                                            newComment = NewComment("")
                                        )
                                    } else reel
                                }
                                _state.emit(updatedState)
                                Log.d("ReelsScreenViewModel", "✅ Backend comment added to reel $videoId (post $postUid)")
                            }
                            is Result.Error -> {
                                Log.e("ReelsScreenViewModel", "❌ Backend comment failed: ${result.error.message}")
                            }
                            is Result.Loading -> { /* ignore */ }
                        }
                    } catch (e: Exception) {
                        Log.e("ReelsScreenViewModel", "❌ Error adding comment via backend: ${e.message}")
                    }
                } else {
                    // Fallback: add comment locally only
                    Log.w("ReelsScreenViewModel", "⚠️ No postUid for reel $videoId — adding local-only comment")
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    val timeString = dateFormat.format(java.util.Date())

                    val newAndroidComment = Comment(
                        id = java.util.UUID.randomUUID().toString(),
                        userId = currentUser.uid,
                        userName = userName,
                        comment = comment.trim(),
                        time = timeString,
                        isLoved = false,
                        reply = emptyList()
                    )
                    val updatedState = _state.value.map { reel ->
                        if (reel.id == videoId) {
                            val updatedComments = reel.comments.toMutableList().apply { add(0, newAndroidComment) }
                            reel.copy(
                                comments = updatedComments,
                                numberOfComments = updatedComments.size,
                                newComment = NewComment("")
                            )
                        } else reel
                    }
                    _state.emit(updatedState)
                }
            } catch (e: Exception) {
                Log.e("ReelsScreenViewModel", "❌ Error adding comment: ${e.message}")
            }
        }
    }

    private suspend fun refreshCommentsForReel(reelId: String) {
        val reel = _state.value.find { it.id == reelId }
        val postUid = reel?.postUid
        if (postUid == null) {
            Log.d("ReelsScreenViewModel", "ℹ️ Skipping comment refresh for reel $reelId (no postUid)")
            return
        }
        try {
            when (val result = getCommentsUseCase(postUid)) {
                is Result.Success -> {
                    val comments = result.data.map { comment ->
                        Comment(
                            id = comment.id.toString(),
                            userId = comment.userId,
                            userName = comment.displayName.ifEmpty { comment.username },
                            comment = comment.content,
                            time = "",
                            isLoved = comment.isLiked
                        )
                    }
                    val updatedState = _state.value.map { r ->
                        if (r.id == reelId) r.copy(comments = comments, numberOfComments = comments.size) else r
                    }
                    _state.emit(updatedState)
                    Log.d("ReelsScreenViewModel", "✅ Refreshed ${comments.size} comments for reel $reelId")
                }
                is Result.Error -> Log.w("ReelsScreenViewModel", "⚠️ Comment refresh error: ${result.error.message}")
                is Result.Loading -> {}
            }
        } catch (e: Exception) {
            Log.w("ReelsScreenViewModel", "⚠️ Comment refresh failed: ${e.message}")
        }
    }

    override fun onClickShownAllComment() {
        val copyState = _state.value.map {
            it.copy(
                comments = emptyList<Comment>()
            )
        }
        viewModelScope.launch {
            _state.emit(copyState)
        }
    }

    override fun onClickShownAllRates() {
        TODO("Not yet implemented")
    }

    fun getReelsFromUsers(userIds: List<String>): List<Reels> {
        Log.d(
            "FollowingTabDebug",
            "ReelsScreenViewModel.getReelsFromUsers called with userIds=$userIds"
        )
        return try {
            val currentReels = _state.value
            Log.d("ReelsScreenViewModel", "🔍 getReelsFromUsers called with ${userIds.size} user IDs")
            Log.d("ReelsScreenViewModel", "🔍 Current reels in state: ${currentReels.size}")
            
            if (currentReels.isNotEmpty() && userIds.isNotEmpty()) {
                val filteredReels = currentReels.filter { reel ->
                    reel.userId in userIds
                }
                Log.d("ReelsScreenViewModel", "✅ Filtered ${filteredReels.size} reels from ${userIds.size} users")
                filteredReels
            } else {
                Log.d("ReelsScreenViewModel", "⚠️ No reels or user IDs available - reels: ${currentReels.size}, userIds: ${userIds.size}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ReelsScreenViewModel", "Error getting reels from users: ${e.message}")
            emptyList()
        }
    }

    fun checkCartStatus(reelId: String) {
        viewModelScope.launch {
            try {
                // ⚠️ MIGRATION: CartRepository optional - skip if not available
                if (cartRepository == null) {
                    Log.w("ReelsScreenViewModel", "⏸️ checkCartStatus: CartRepository not available")
                    return@launch
                }
                
                val userId = currentUserProvider.getCurrentUserId()
                if (userId != null && reelId.isNotBlank()) {
                    cartRepository.isProductInUserCart(userId, reelId)
                        .collect { isInCart ->
                            val updatedState = _state.value.map { reel ->
                                if (reel.id == reelId) {
                                    reel.copy(isInCurrentUserCart = isInCart)
                                } else reel
                            }
                            _state.emit(updatedState)
                        }
                } else {
                    Log.w("ReelsScreenViewModel", "Cannot check cart status: userId=$userId, reelId=$reelId")
                }
            } catch (e: Exception) {
                Log.e("ReelsScreenViewModel", "Error checking cart status: ${e.message}")
            }
        }
    }

    fun getProductCartStats(productId: String): Flow<CartStats> {
        // ⚠️ MIGRATION: CartRepository optional - return empty flow if not available
        if (cartRepository == null) {
            Log.w("ReelsScreenViewModel", "⏸️ getProductCartStats: CartRepository not available")
            return kotlinx.coroutines.flow.flowOf(CartStats(productId = productId))
        }
        return cartRepository.getProductCartStats(productId)
    }

    fun updateReelCartStats(reelId: String, cartStats: CartStats) {
        viewModelScope.launch {
            val updatedState = _state.value.map { reel ->
                if (reel.id == reelId) {
                    reel.copy(cartStats = cartStats)
                } else reel
            }
            _state.emit(updatedState)
        }
    }

    // 🔧 DEBUG: Manual function to refresh all comments for all reels
    fun refreshAllComments() {
        viewModelScope.launch {
            _state.value.forEach { reel ->
                if (reel.postUid != null) {
                    refreshCommentsForReel(reel.id)
                }
            }
        }
    }

    fun onToggleBookmark(reelId: String) {
        viewModelScope.launch {
            val userId = currentUserProvider.getCurrentUserId() ?: return@launch
            val currentReel = _state.value.find { it.id == reelId } ?: return@launch
            val postUid = currentReel.postUid
            if (postUid == null) {
                Log.w("ReelsScreenViewModel", "⚠️ No postUid for reel $reelId — cannot bookmark")
                return@launch
            }

            val newBookmarkState = !currentReel.isBookmarked
            // Immediate UI feedback
            val immediateState = _state.value.map { reel ->
                if (reel.id == reelId) reel.copy(isBookmarked = newBookmarkState) else reel
            }
            _state.emit(immediateState)

            try {
                val result = if (newBookmarkState) {
                    postRepository.bookmarkPost(postUid, userId)
                    Result.Success(Unit)
                } else {
                    postRepository.unbookmarkPost(postUid, userId)
                    Result.Success(Unit)
                }
                Log.d("ReelsScreenViewModel", "🔖 Bookmark toggled: reel $reelId, post $postUid, bookmarked=$newBookmarkState")
            } catch (e: Exception) {
                Log.e("ReelsScreenViewModel", "❌ Bookmark sync failed: ${e.message}")
                // Revert
                val revertState = _state.value.map { reel ->
                    if (reel.id == reelId) reel.copy(isBookmarked = currentReel.isBookmarked) else reel
                }
                _state.emit(revertState)
            }
        }
    }

}