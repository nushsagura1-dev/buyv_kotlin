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
import com.google.firebase.auth.FirebaseAuth
import com.project.e_commerce.android.domain.usecase.GetUserProfileUseCase
import com.project.e_commerce.android.domain.usecase.LikeReelUseCase
import com.project.e_commerce.android.domain.usecase.GetReelLikeStatusUseCase
import com.project.e_commerce.android.domain.usecase.GetReelLikeCountUseCase
import com.project.e_commerce.android.domain.usecase.UpdateUserLikeCountUseCase
import com.project.e_commerce.android.domain.usecase.AddCommentUseCase
import com.project.e_commerce.android.domain.usecase.GetReelCommentsUseCase
import com.project.e_commerce.android.domain.usecase.LikeCommentUseCase
import com.project.e_commerce.android.domain.usecase.GetCommentLikeStatusUseCase
import com.project.e_commerce.android.domain.usecase.GetCommentLikeCountUseCase
import com.project.e_commerce.android.domain.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.project.e_commerce.android.domain.repository.CartRepository
import com.project.e_commerce.android.data.model.CartStats

class ReelsScreenViewModel(
    private val productViewModel: ProductViewModel,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val likeReelUseCase: LikeReelUseCase,
    private val getReelLikeStatusUseCase: GetReelLikeStatusUseCase,
    private val getReelLikeCountUseCase: GetReelLikeCountUseCase,
    private val updateUserLikeCountUseCase: UpdateUserLikeCountUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val getReelCommentsUseCase: GetReelCommentsUseCase,
    private val likeCommentUseCase: LikeCommentUseCase,
    private val getCommentLikeStatusUseCase: GetCommentLikeStatusUseCase,
    private val getCommentLikeCountUseCase: GetCommentLikeCountUseCase,
    private val firestore: FirebaseFirestore,
    private val cartRepository: CartRepository
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
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                Log.d("ReelsScreenViewModel", "✅ Authentication ready: ${currentUser.uid}")
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

        _errorMessage.emit("Failed to load reels after $maxAttempts attempts")
        _isLoading.emit(false)
        _state.emit(emptyList())
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
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w("ReelsScreenViewModel", "No current user, cannot load like states and comments")
            return reels
        }

        return try {
            Log.d(
                "ReelsScreenViewModel",
                "🔄 Loading like states and comments for ${reels.size} reels"
            )

            // Load like states and comments - simplified approach for better reliability
            val updatedReels = reels.map { reel ->
                try {
                    Log.d("ReelsScreenViewModel", "🔍 Loading data for reel: ${reel.id}")
                    
                    // Get like status and count for each reel
                    val isLikedResult = getReelLikeStatusUseCase(reel.id, currentUser.uid)
                    val likeCountResult = getReelLikeCountUseCase(reel.id)

                    // Get real comments from Firebase with detailed logging
                    Log.d("ReelsScreenViewModel", "🔍 Loading comments for reel: ${reel.id}")
                    val commentsResult = getReelCommentsUseCase(reel.id)

                    val isLiked = isLikedResult.getOrNull() ?: false
                    val likeCount = likeCountResult.getOrNull() ?: 0
                    val comments = commentsResult.getOrNull() ?: emptyList()

                    Log.d("ReelsScreenViewModel", "✅ Loaded ${comments.size} comments for reel ${reel.id}")
                    if (comments.isNotEmpty()) {
                        Log.d("ReelsScreenViewModel", "📝 Comments for reel ${reel.id}: ${comments.map { "${it.userName}: ${it.comment}" }}")
                    }

                    reel.copy(
                        love = reel.love.copy(
                            number = likeCount,
                            isLoved = isLiked
                        ),
                        comments = comments,
                        numberOfComments = comments.size
                    )
                } catch (e: Exception) {
                    Log.e(
                        "ReelsScreenViewModel",
                        "❌ Error loading data for reel ${reel.id}: ${e.message}"
                    )
                    reel
                }
            }

            Log.d(
                "ReelsScreenViewModel",
                "✅ Loaded like states and comments for ${updatedReels.size} reels"
            )
            
            // Log summary of comments loaded
            val totalComments = updatedReels.sumOf { it.comments.size }
            Log.d("ReelsScreenViewModel", "📊 Total comments loaded across all reels: $totalComments")
            updatedReels.forEach { reel ->
                if (reel.comments.isNotEmpty()) {
                    Log.d("ReelsScreenViewModel", "📝 Reel ${reel.id} has ${reel.comments.size} comments")
                }
            }
            
            updatedReels
        } catch (e: Exception) {
            Log.e("ReelsScreenViewModel", "❌ Error loading reel data: ${e.message}")
            reels
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
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e("ReelsScreenViewModel", "User must be logged in to like a reel")
                return@launch
            }

            try {
                val userId = currentUser.uid

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

                    Log.d(
                        "ReelsScreenViewModel",
                        "🚀 Optimistic update: reel $reelId liked=$newLikeState, count=$newCount"
                    )
                }

                // 📡 BACKGROUND SYNC: Now sync with Firebase (don't wait for it)
                launch {
                    try {
                        val likeResult = likeReelUseCase(reelId, userId)

                        if (likeResult.isSuccess) {
                            // Fetch real data from Firebase to confirm/correct the optimistic update
                            val isLikedResult = getReelLikeStatusUseCase(reelId, userId)
                            val likeCountResult = getReelLikeCountUseCase(reelId)

                            val actualIsLiked = isLikedResult.getOrNull() ?: false
                            val actualLikeCount = likeCountResult.getOrNull() ?: 0

                            // Update with real data (this will fix any inconsistencies)
                            val correctedState = _state.value.map { reel ->
                                if (reel.id == reelId) {
                                    reel.copy(
                                        love = reel.love.copy(
                                            number = actualLikeCount,
                                            isLoved = actualIsLiked
                                        )
                                    )
                                } else reel
                            }
                            _state.emit(correctedState)

                            Log.d(
                                "ReelsScreenViewModel",
                                "✅ Firebase sync complete: reel $reelId liked=$actualIsLiked, count=$actualLikeCount"
                            )
                        } else {
                            Log.e(
                                "ReelsScreenViewModel",
                                "❌ Firebase like operation failed, reverting optimistic update"
                            )

                            // Revert optimistic update on failure
                            if (currentReel != null) {
                                val revertedState = _state.value.map { reel ->
                                    if (reel.id == reelId) {
                                        reel.copy(
                                            love = currentReel.love // Revert to original state
                                        )
                                    } else reel
                                }
                                _state.emit(revertedState)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ReelsScreenViewModel", "❌ Error during Firebase sync: ${e.message}")
                        // Could revert optimistic update here if needed
                    }
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
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e("ReelsScreenViewModel", "User must be logged in to comment")
                return@launch
            }

            if (comment.isBlank()) {
                Log.w("ReelsScreenViewModel", "Comment cannot be empty")
                return@launch
            }

            try {
                val userName =
                    currentUser.displayName ?: currentUser.email?.split("@")?.firstOrNull()
                    ?: "User"

                Log.d(
                    "ReelsScreenViewModel",
                    "💬 Adding comment to reel $videoId: '$comment' by $userName"
                )

                // Add comment to Firebase
                val addResult =
                    addCommentUseCase(videoId, currentUser.uid, userName, comment.trim())

                if (addResult.isSuccess) {
                    val newComment = addResult.getOrNull()!!

                    // Update local state immediately
                    val updatedState = _state.value.map { reel ->
                        if (reel.id == videoId) {
                            val updatedComments = reel.comments.toMutableList().apply {
                                add(0, newComment) // Add to beginning for newest first
                            }
                            reel.copy(
                                comments = updatedComments,
                                numberOfComments = updatedComments.size,
                                newComment = NewComment("") // Clear input
                            )
                        } else {
                            reel
                        }
                    }
                    _state.emit(updatedState)

                    Log.d("ReelsScreenViewModel", "✅ Comment added successfully to reel $videoId")

                    // Refresh comments from Firebase to ensure all users see updates
                    launch {
                        delay(1000) // Give Firebase a moment to sync
                        refreshCommentsForReel(videoId)
                    }
                } else {
                    Log.e(
                        "ReelsScreenViewModel",
                        "❌ Failed to add comment: ${addResult.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e("ReelsScreenViewModel", "❌ Error adding comment: ${e.message}")
            }
        }
    }

    private suspend fun refreshCommentsForReel(reelId: String) {
        try {
            val commentsResult = getReelCommentsUseCase(reelId)
            if (commentsResult.isSuccess) {
                val freshComments = commentsResult.getOrNull() ?: emptyList()

                val updatedState = _state.value.map { reel ->
                    if (reel.id == reelId) {
                        reel.copy(
                            comments = freshComments,
                            numberOfComments = freshComments.size
                        )
                    } else reel
                }
                _state.emit(updatedState)

                Log.d(
                    "ReelsScreenViewModel",
                    "🔄 Refreshed ${freshComments.size} comments for reel $reelId"
                )
            }
        } catch (e: Exception) {
            Log.e("ReelsScreenViewModel", "❌ Error refreshing comments: ${e.message}")
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
                val userId = FirebaseAuth.getInstance().currentUser?.uid
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
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.w("ReelsScreenViewModel", "❌ No current user, cannot refresh comments")
                return@launch
            }

            Log.d("ReelsScreenViewModel", "🔄 Manually refreshing all comments...")

            val updatedState = _state.value.map { reel ->
                try {
                    // Get fresh comments from Firebase
                    Log.d("ReelsScreenViewModel", "🔄 Refreshing comments for reel: ${reel.id}")
                    val commentsResult = getReelCommentsUseCase(reel.id)
                    val freshComments = commentsResult.getOrNull() ?: emptyList()

                    Log.d(
                        "ReelsScreenViewModel",
                        "📱 Refreshed ${freshComments.size} comments for reel ${reel.id}"
                    )
                    if (freshComments.isNotEmpty()) {
                        Log.d(
                            "ReelsScreenViewModel",
                            "📝 Fresh comments: ${freshComments.map { "${it.userName}: ${it.comment}" }}"
                        )
                    }

                    reel.copy(
                        comments = freshComments,
                        numberOfComments = freshComments.size
                    )
                } catch (e: Exception) {
                    Log.e(
                        "ReelsScreenViewModel",
                        "❌ Error refreshing comments for reel ${reel.id}: ${e.message}"
                    )
                    reel
                }
            }

            _state.emit(updatedState)

            val totalComments = updatedState.sumOf { it.comments.size }
            Log.d(
                "ReelsScreenViewModel",
                "✅ Manual refresh complete. Total comments: $totalComments"
            )
        }
    }

}