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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.project.e_commerce.android.presentation.viewModel.ProductViewModel
import kotlinx.coroutines.delay
import com.google.firebase.auth.FirebaseAuth
import com.project.e_commerce.android.domain.usecase.GetUserProfileUseCase
import com.project.e_commerce.android.domain.model.UserProfile

class ReelsScreenViewModel(
    private val productViewModel: ProductViewModel,
    private val getUserProfileUseCase: GetUserProfileUseCase
) : BaseViewModel(), ReelsScreenInteraction {

    private val _state: MutableStateFlow<List<Reels>> = MutableStateFlow(listOf())
    val state: StateFlow<List<Reels>> get() = _state

    private val _showAddToCart = MutableStateFlow(false)
    val showAddToCart: StateFlow<Boolean> get() = _showAddToCart.asStateFlow()

    init {
        Log.d("ReelsScreenViewModel", "🚀 ReelsScreenViewModel initialized")
        viewModelScope.launch {
            // Load real reels from ProductViewModel instead of hardcoded data
            loadReelsFromDatabase()
            
            // Monitor ProductViewModel changes and refresh reels
            observeProductViewModelChanges()
        }
    }

    private suspend fun loadReelsFromDatabase() {
        try {
            Log.d("ReelsScreenViewModel", "🔄 loadReelsFromDatabase called")
            Log.d("ReelsScreenViewModel", "🔍 ProductViewModel instance: $productViewModel")
            
            // Load reels from ProductViewModel which generates them from Firebase products
            val reels = productViewModel.productReels
            Log.d("ReelsScreenViewModel", "📺 Got ${reels.size} reels from ProductViewModel")
            Log.d("ReelsScreenViewModel", "🔍 Reels content: ${reels.map { "${it.id}:${it.productName}" }}")
            
            // Update reels with real user profile data
            val updatedReels = updateReelsWithRealUserData(reels)
            
            _state.emit(updatedReels)
            
            // If no reels yet, wait a bit and try again (products might still be loading)
            if (reels.isEmpty()) {
                Log.d("ReelsScreenViewModel", "⚠️ No reels found, waiting 1 second and retrying...")
                delay(1000)
                val retryReels = productViewModel.productReels
                Log.d("ReelsScreenViewModel", "🔄 Retry got ${retryReels.size} reels")
                if (retryReels.isNotEmpty()) {
                    // Update retry reels with real user profile data
                    val updatedRetryReels = updateReelsWithRealUserData(retryReels)
                    _state.emit(updatedRetryReels)
                    Log.d("ReelsScreenViewModel", "✅ Successfully loaded ${updatedRetryReels.size} reels on retry")
                } else {
                    Log.d("ReelsScreenViewModel", "❌ Still no reels after retry")
                }
            } else {
                Log.d("ReelsScreenViewModel", "✅ Successfully loaded ${reels.size} reels")
            }
        } catch (e: Exception) {
            Log.e("ReelsViewModel", "❌ Error loading reels from database: ${e.message}")
            _state.emit(emptyList())
        }
    }

    private suspend fun observeProductViewModelChanges() {
        // Only check for changes a few times, not infinitely
        repeat(3) {
            delay(2000) // Check every 2 seconds, but only 3 times
            val currentReels = productViewModel.productReels
            val currentState = _state.value
            
            // If we have new reels, update the state
            if (currentReels.size != currentState.size || 
                currentReels.any { reel -> currentState.none { it.id == reel.id } }) {
                Log.d("ReelsScreenViewModel", "🔄 New reels detected, updating state")
                // Update new reels with real user profile data
                val updatedCurrentReels = updateReelsWithRealUserData(currentReels)
                _state.emit(updatedCurrentReels)
                return // Exit the function after first update
            }
        }
    }

    fun refreshReels() {
        viewModelScope.launch {
            Log.d("ReelsScreenViewModel", "🔄 refreshReels called manually")
            loadReelsFromDatabase()
        }
    }
    
    fun forceRefreshFromProductViewModel() {
        viewModelScope.launch {
            Log.d("ReelsScreenViewModel", "🔄 forceRefreshFromProductViewModel called")
            // Debug current state before refresh
            debugCurrentState()
            // Force ProductViewModel to reload products and generate reels
            productViewModel.refreshProducts()
            delay(1000) // Wait a bit longer for products to load and process
            // Debug state after refresh
            debugCurrentState()
            loadReelsFromDatabase()
        }
    }

    fun ensureReelsLoaded() {
        viewModelScope.launch {
            Log.d("ReelsScreenViewModel", "🔍 ensureReelsLoaded called - current reels in state: ${_state.value.size}")
            if (_state.value.isEmpty()) {
                Log.d("ReelsScreenViewModel", "🔄 No reels in state, loading from database...")
                loadReelsFromDatabase()
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
    
    /**
     * Fetch real user profile data and update reels with actual usernames
     */
    private suspend fun updateReelsWithRealUserData(reels: List<Reels>): List<Reels> {
        return try {
            Log.d("ReelsScreenViewModel", "🔄 Starting user data update for ${reels.size} reels")
            val updatedReels = mutableListOf<Reels>()
            
            for (reel in reels) {
                Log.d("ReelsScreenViewModel", "🔍 Processing reel ${reel.id} - current userName: '${reel.userName}', userId: '${reel.userId}'")
                
                val updatedReel = if (reel.userId.isNotEmpty() && reel.userId != "unknown_user" && reel.userName.startsWith("User_")) {
                    Log.d("ReelsScreenViewModel", "🔄 Fetching user profile for userId: ${reel.userId}")
                    // Try to fetch real user profile data
                    val userProfileResult = getUserProfileUseCase(reel.userId)
                    if (userProfileResult.isSuccess) {
                        val userProfile = userProfileResult.getOrNull()
                        if (userProfile != null) {
                            // Try to get a meaningful username from the profile
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
                        // If failed, keep the placeholder but make it more readable
                        reel.copy(userName = "User_${reel.userId.take(6)}")
                    }
                } else {
                    Log.d("ReelsScreenViewModel", "ℹ️ Keeping reel as is - userName: '${reel.userName}', userId: '${reel.userId}'")
                    reel // Keep as is if already has real data
                }
                updatedReels.add(updatedReel)
            }
            
            Log.d("ReelsScreenViewModel", "✅ Updated ${updatedReels.size} reels with real user data")
            // Log the final usernames for debugging
            updatedReels.forEach { reel ->
                Log.d("ReelsScreenViewModel", "📱 Final reel ${reel.id}: userName='${reel.userName}', userId='${reel.userId}'")
            }
            updatedReels
        } catch (e: Exception) {
            Log.e("ReelsScreenViewModel", "❌ Error updating reels with user data: ${e.message}")
            e.printStackTrace()
            reels // Return original reels if update fails
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
        val copyState = _state.value.mapIndexed { index, value ->
            if (index == 0) {
                value.copy(
                    newComment = NewComment(comment)

                )
            } else value
        }
        viewModelScope.launch {
            _state.emit(copyState)
        }
    }

    override fun onClickSearchButton(navController: NavController) {
        navController.navigate(Screens.ReelsScreen.SearchReelsAndUsersScreen.route)
    }

    override fun onClackLoveReelsButton(reelId: String) {

        val copyState = _state.value.mapIndexed { index, it ->
            if (it.id == reelId) {
                val data = it.copy(
                    love = _state.value[index].love.copy(
                        number = if (_state.value[index].love.isLoved) _state.value[index].love.number - 1
                        else _state.value[index].love.number + 1,
                        isLoved = !_state.value[index].love.isLoved
                    )
                )
                Log.i("LOVED", it.love.number.toString() + "  : " + it.love.isLoved.toString())
                data
            } else it
        }
        viewModelScope.launch {
            _state.emit(copyState)
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
//        val copyState = _state.value.mapIndexed { _, reel ->
//            if (reel.id == reelsId) {
//                val data = reel.comments.map { comment ->
//                    if (comment.id == commentId) {
//                        val newData = reel.copy(
//                            isLoved = !comment.isLoved,
//                        )
//                        newData
//                        reel
//                    } else reel
//                }
//            }
//            else reel
//        }
//

    override fun onClickAddComment(videoId: String, comment: String) {
        // Get current user's real username from Firebase Auth
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userName = currentUser?.displayName ?: currentUser?.email?.split("@")?.firstOrNull() ?: "User"
        
        val newValue =  Comment(
            id = java.util.UUID.randomUUID().toString(),
            userName = userName,
            comment = comment,
            time = "now",
        )
        val updatedRequests = _state.value.map {
            if(it.id == videoId) {
                it.comments.toMutableList().apply { add(newValue) }
                it
            }
            else it
        }
        val copyState = _state.value.map {
            if (it.id == videoId)
                it.copy(
                    comments = updatedRequests.filter { it.id == videoId }[0].comments
                )
            else it
        }
        viewModelScope.launch {
            _state.emit(copyState)
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

    /**
     * Get reels from specific users (for following functionality)
     * This method filters reels from the current state based on user IDs
     */
    fun getReelsFromUsers(userIds: List<String>): List<Reels> {
        return try {
            val currentReels = _state.value
            Log.d("ReelsScreenViewModel", "🔍 getReelsFromUsers called with ${userIds.size} user IDs")
            Log.d("ReelsScreenViewModel", "🔍 Current reels in state: ${currentReels.size}")
            
            if (currentReels.isNotEmpty() && userIds.isNotEmpty()) {
                // Filter reels by user IDs
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

}