package com.project.e_commerce.android.presentation.ui.screens.reelsScreen

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

// ORIGINAL GLIDE IMPORTS (COMMENTED OUT):
// import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
// import com.bumptech.glide.integration.compose.GlideImage
// import com.project.e_commerce.android.presentation.viewModel.MainUiStateViewModel

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.google.accompanist.pager.*
import com.project.e_commerce.android.R
import com.project.e_commerce.android.presentation.ui.composable.composableScreen.public.VideoPlayer
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.ui.screens.HeaderStyle
import com.project.e_commerce.android.presentation.ui.screens.HeartAnimation
import com.project.e_commerce.android.presentation.ui.screens.RequireLoginPrompt
import com.project.e_commerce.android.presentation.ui.utail.noRippleClickable
import com.project.e_commerce.android.presentation.viewModel.CartViewModel
import com.project.e_commerce.android.presentation.viewModel.CartItemUi
import com.project.e_commerce.android.presentation.viewModel.followingViewModel.FollowingViewModel
import com.project.e_commerce.android.presentation.viewModel.reelsScreenViewModel.ReelsScreenViewModel
import com.project.e_commerce.android.presentation.viewModel.RecentlyViewedViewModel
import com.project.e_commerce.android.presentation.ui.screens.marketplace.components.MarketplaceProductBadge
import com.project.e_commerce.android.data.repository.TrackingRepository

import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.catch
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.tasks.await
import com.project.e_commerce.android.presentation.utils.UserInfoCache
import com.project.e_commerce.android.presentation.utils.UserDisplayName
import com.project.e_commerce.android.presentation.utils.UserDisplayType
import com.project.e_commerce.android.presentation.utils.VideoPreloader
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.CommentsSheet
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.components.ReelContent

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ReelsView(
    navController: NavController,
    viewModel: ReelsScreenViewModel,
    cartViewModel: CartViewModel,
    isLoggedIn: Boolean = true,
    targetReelId: String? = null,
    onShowSheet: (SheetType, Reels?) -> Unit,
    mainUiStateViewModel: com.project.e_commerce.android.presentation.viewModel.MainUiStateViewModel? = null
) {
    val showLoginPrompt = remember { mutableStateOf(false) }
    val recentlyViewedViewModel: RecentlyViewedViewModel = koinViewModel()
    val trackingRepository: TrackingRepository = koinInject()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current  // For video preloading
    val scope = rememberCoroutineScope()  // For tracking calls
    Log.d("ReelsView", "ReelsView: composable entry, viewModel=$viewModel")

    // targetReelId is now passed as a direct parameter (was previously read from
    // savedStateHandle, which had a race condition with LaunchedEffect timing)
    val navBackStackEntry = navController.currentBackStackEntry

    Log.d("ReelsView", "Target reel ID from navigation: $targetReelId")

    // Share launcher for sharing reels
    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle share result if needed
        Log.d("ReelsView", "Share completed with result: ${result.resultCode}")
    }

    // Helper function to create share content
    fun createShareContent(reel: Reels): String {
        return buildString {
            append("Check out this amazing product: ${reel.productName}")
            if (reel.productPrice.isNotBlank()) {
                append(" for only ${reel.productPrice}")
            }
            if (reel.contentDescription.isNotBlank()) {
                append("\n\n${reel.contentDescription}")
            }
            if (reel.video != null && reel.video.toString().isNotBlank()) {
                append("\n\nWatch the video: ${reel.video}")
            }
            append("\n\nDownload our app to see more amazing products!")
        }
    }

    // Helper function to share reel
    fun shareReel(reel: Reels) {
        val shareContent = createShareContent(reel)
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, shareContent)
            type = "text/plain"
        }
        shareLauncher.launch(android.content.Intent.createChooser(shareIntent, "Share Reel"))
    }

    // NEW: Use improved state management
    val reelsList by viewModel.state.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Log.d(
        "ReelsView",
        "collected reels state, size=${reelsList.size}, isLoading=$isLoading, error=$errorMessage"
    )

    // NEW: Global video state management for lifecycle handling
    val globalVideoPlayStates = remember { mutableStateMapOf<String, Boolean>() }
    var wasAppInBackground by remember { mutableStateOf(false) }

    // NEW: Lifecycle observer for the entire ReelsView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("ReelsView", "🎥 App going to background - pausing all videos")
                    wasAppInBackground = true
                    // Pause all currently playing videos
                    globalVideoPlayStates.keys.forEach { reelId ->
                        if (globalVideoPlayStates[reelId] == true) {
                            Log.d("ReelsView", "🎥 Pausing reel $reelId due to app background")
                            globalVideoPlayStates[reelId] = false
                        }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("ReelsView", "🎥 App coming to foreground")
                    if (wasAppInBackground) {
                        // Only resume the current page video if it was playing
                        // This will be handled by individual VideoPlayer components
                        wasAppInBackground = false
                        Log.d("ReelsView", "🎥 App resumed from background")
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    Log.d("ReelsView", "🎥 App stopped - ensuring all videos are paused")
                    globalVideoPlayStates.clear() // Clear all play states
                }
                else -> { /* Handle other lifecycle events if needed */ }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var currentUserId by remember { mutableStateOf("") }

    // --- BEGIN USER INFO CACHE (FOR DISPLAY NAME SUPPORT) ---
    // UserInfoCache is now a singleton object, no need to instantiate
    // --- END USER INFO CACHE ---

    Log.d("ReelsView", "🎬 Current user ID: $currentUserId")
    Log.d("ReelsView", "🎬 Reels list size: ${reelsList.size}")

    // NEW: Improved loading/error/empty handling
    when {
        isLoading -> {
            Log.d("ReelsView", "isLoading=true, showing spinner")
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading reels...", color = Color.White, fontSize = 16.sp)
                }
            }
            return
        }

        errorMessage != null -> {
            Log.e("ReelsView", "Error loading reels: $errorMessage")
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error loading reels", color = Color.Red, fontSize = 18.sp)
                    Text("$errorMessage", color = Color.LightGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        viewModel.forceRefreshFromProductViewModel()
                    }) {
                        Text("Retry", color = Color.White)
                    }
                }
            }
            return
        }

        reelsList.isEmpty() -> {
            Log.w("ReelsView", "Empty data loaded, showing empty message")
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No reels found", color = Color.White, fontSize = 18.sp)
                    Text(
                        "Create or follow users to see reels",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        viewModel.forceRefreshFromProductViewModel()
                    }) {
                        Text("Refresh", color = Color.White)
                    }
                }
            }
            return
        }
    }

    // Additional safety check for reels list
    if (reelsList.any { it.id.isBlank() }) {
        Log.w("ReelsView", "🎬 Some reels have blank IDs, filtering them out")
        // Filter out reels with blank IDs to prevent crashes
        val validReels = reelsList.filter { it.id.isNotBlank() }
        if (validReels.isEmpty()) {
            Log.w("ReelsView", "🎬 No valid reels after filtering, showing empty state")
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No valid reels available", color = Color.White, fontSize = 18.sp)
            }
            return
        }
    }

    // Initialize pager state with safety bounds - only when reels are available
    // If a targetReelId is provided, find its index in the list
    val initialPage = if (!targetReelId.isNullOrBlank() && reelsList.isNotEmpty()) {
        reelsList.indexOfFirst { it.id == targetReelId }.takeIf { it >= 0 } ?: 0
    } else if (reelsList.isNotEmpty()) {
        minOf(0, maxOf(0, reelsList.size - 1))
    } else {
        0
    }
    Log.d("ReelsView", "🎬 Initializing pager state with initialPage: $initialPage, total pages: ${reelsList.size}")

    val pagerState = rememberPagerState(
        initialPage = initialPage
    )

    // If a targetReelId is provided, reset the pager to show that reel
    LaunchedEffect(targetReelId, reelsList) {
        if (!targetReelId.isNullOrBlank() && reelsList.isNotEmpty()) {
            val idx = reelsList.indexOfFirst { it.id == targetReelId }
            if (idx >= 0) {
                Log.d(
                    "ReelsView",
                    "Navigating to target reel index: $idx for reelId: $targetReelId"
                )
                pagerState.scrollToPage(idx)
            }
        }
    }

    Log.d("ReelsView", "🎬 Pager state initialized successfully")

    var currentPage by remember { mutableStateOf(initialPage) }
    Log.d("ReelsView", "🎬 Current page state initialized: $currentPage")

    var isSelectedComments by remember { mutableStateOf(true) }
    
    // NEW: State for modern comments bottom sheet
    var showCommentsSheet by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    
    // Heart animation: per-reel state (fix double tap bug)
    // Instead of using just one showHeart/heartPosition for all pages,
    // use per-reel state keyed by reel.id!

    // NEW: Use only local selectedTab state for tab logic (never use savedStateHandle)
    val tabList = listOf("For you", "Following", "Explore")
    var selectedTab by remember { mutableStateOf("For you") }

    // Force refresh from ProductViewModel when needed
    LaunchedEffect(Unit) {
        viewModel.forceRefreshFromProductViewModel()
    }

    var isVisible by remember { mutableStateOf(false) }

    // Refresh following data when reels view becomes active
    val followingViewModel: FollowingViewModel = koinViewModel()
    // ✅ REACTIVATED: Get current user provider from Koin in Composable context
    val currentUserProvider: com.project.e_commerce.data.local.CurrentUserProvider = org.koin.compose.koinInject()
    
    // TODO: Get currentUserId from CurrentUserProvider during Firebase migration
    val currentUserIdForFollowing: String? = currentUserId.ifBlank { null }

    LaunchedEffect(Unit) {
        // Load current user ID from backend
        currentUserId = currentUserProvider.getCurrentUserId() ?: ""

        // Force refresh reels when screen becomes active
        Log.d("ReelsView", "🔄 ReelsView became active, forcing reels refresh")
        viewModel.forceRefreshFromProductViewModel()

        // ✅ REACTIVATED: Following data loading now uses CurrentUserProvider (backend auth)
        try {
            val currentUser = currentUserProvider.getCurrentUser()
            if (currentUser != null) {
                Log.d("ReelsView", "🔄 Loading following data for correct Follow button states")
                val username = currentUser.displayName ?: currentUser.email?.split("@")?.firstOrNull() ?: "user"

                // Reset any previous load attempts to ensure fresh load
                followingViewModel.resetLoadAttempts()

                // Only load if not already loading to avoid conflicts
                val currentState = followingViewModel.uiState.value
                if (!currentState.isLoading && currentState.following.isEmpty()) {
                    Log.d("ReelsView", "🔄 Following data is empty, loading fresh data")
                    followingViewModel.loadUserData(currentUser.uid, username)

                    // Add a fallback mechanism - retry after 3 seconds if still empty
                    kotlinx.coroutines.delay(3000)
                    val stateAfterDelay = followingViewModel.uiState.value
                    if (!stateAfterDelay.isLoading && stateAfterDelay.following.isEmpty() && stateAfterDelay.error != null) {
                        Log.d("ReelsView", "🔄 Retrying following data load after initial failure")
                        followingViewModel.clearError()
                        followingViewModel.resetLoadAttempts()
                        followingViewModel.loadUserData(currentUser.uid, username)
                    }
                } else if (!currentState.isLoading) {
                    Log.d(
                        "ReelsView",
                        "✅ Following data already available (${currentState.following.size} users)"
                    )
                } else {
                    Log.d("ReelsView", "⏳ Following data is already loading, waiting...")
                }
            }
        } catch (e: Exception) {
            Log.e("ReelsView", "❌ Failed to load current user for following data: ${e.message}")
        }
    }

    var isSelectedRatings by remember { mutableStateOf(false) }
    val modalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )

    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    var currentSheet by remember { mutableStateOf<SheetType?>(null) }

    val onCommentTabClick = {
        isSelectedComments = true
        isSelectedRatings = false
    }
    val onRatingTabClick = {
        isSelectedComments = false
        isSelectedRatings = true
    }

    val showSheet = remember { mutableStateOf(false) }
    val sheetTab = remember { mutableStateOf(SheetTab.Comments) }
    var currentReel by remember { mutableStateOf<Reels?>(null) }

    // Tab change handler - only navigate to Explore, otherwise just change local selectedTab
    val onTabChange: (String) -> Unit = { newTab ->
        if (newTab == "Explore") {
            navController.navigate(Screens.ReelsScreen.ExploreScreen.route)
        } else {
            selectedTab = newTab
        }
    }

    // When returning from Explore, always default to "For you"
    LaunchedEffect(Unit) {
        // Could further refine if you want to auto-detect, but here we just resync on screen appear
        selectedTab = "For you"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Add black background
            .imePadding()
    ) {
        // Always use the local selectedTab state for top tab bar
        when (selectedTab) {
            "Following" -> {
                FollowingReelsContent(
                    navController = navController,
                    followingViewModel = followingViewModel,
                    reelsViewModel = viewModel,
                    cartViewModel = cartViewModel,
                    onShowSheet = { sheetType, reel ->
                        showSheet.value = true
                        currentSheet = sheetType
                        currentReel = reel
                        // mainUiStateViewModel.setBottomSheetVisible(true)
                    },
                    onShareReel = ::shareReel,
                    recentlyViewedViewModel = recentlyViewedViewModel
                )
            }
            "For you" -> {
                ReelsList(
                    navController = navController,
                    onClickCommentButton = { reel ->
                        try {
                            val postIdToUse = reel.postUid?.takeIf { it.isNotBlank() } ?: reel.id
                            Log.d("CommentCallbackDebug", "🎬 onClickCommentButton: Opening comments sheet for reel ${reel.id}, using postId=$postIdToUse (postUid=${reel.postUid})")
                            selectedPostId = postIdToUse
                            showCommentsSheet = true
                            // Hide bottom bar
                            mainUiStateViewModel?.hideBottomBar()
                        } catch (e: Exception) {
                            Log.e(
                                "CommentCallbackDebug",
                                "🎬 Exception in onClickCommentButton callback: ${e.message}",
                                e
                            )
                        }
                    },
                    viewModel = viewModel,
                    cartViewModel = cartViewModel,
                    onClickCartButton = { reel ->
                        if (!isLoggedIn) showLoginPrompt.value = true
                        showSheet.value = true
                        sheetTab.value = SheetTab.Ratings
                        currentReel = reel
                    },
                    onClickMoreButton = { reel ->
                        // Improved sharing implementation with product info
                        if (reel != null) {
                            shareReel(reel)
                        }
                    },
                    reelsList = reelsList,
                    isLoggedIn = isLoggedIn,
                    showLoginPrompt = showLoginPrompt,
                    initialPage = initialPage,
                    onShareReel = ::shareReel,
                    recentlyViewedViewModel = recentlyViewedViewModel,
                    globalVideoPlayStates = globalVideoPlayStates, // NEW: Pass global state
                    mainUiStateViewModel = mainUiStateViewModel // NEW: Pass mainUiStateViewModel
                )
            }
            else -> {
                // Defensive fallback: should not show UI for "Explore" here (it navigates away)
                // Display nothing or do nothing here; navigation to Explore already happens on tab click
            }
        }

        // Overlay Custom BottomSheet
        if (showSheet.value) {
            Log.d("ReelsViewDebug", "🎬 showSheet is true, attempting to render bottom sheet")
            Log.d("ReelsViewDebug", "🎬 currentReel is null: ${currentReel == null}")
            Log.d("ReelsViewDebug", "🎬 currentReel id: ${currentReel?.id}")

            // Hide bottom bar when comment sheet is open
            mainUiStateViewModel?.hideBottomBar()

            if (currentReel == null) {
                Log.e(
                    "ReelsViewDebug",
                    "🎬 ERROR: currentReel is null when trying to show bottom sheet!"
                )
                showSheet.value = false
                // Show bottom bar when sheet is closed
                mainUiStateViewModel?.showBottomBar()
            } else {
                Log.d("ReelsViewDebug", "🎬 About to render ModernBottomSheetContent")
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable {
                            showSheet.value = false
                            // Show bottom bar when sheet is closed
                            mainUiStateViewModel?.showBottomBar()
                        }
                )
                ModernBottomSheetContent(
                    reel = currentReel,
                    isSelectedComments = isSelectedComments,
                    isSelectedRatings = isSelectedRatings,
                    onCommentTabClick = onCommentTabClick,
                    onRatingTabClick = onRatingTabClick,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onCloseClick = {
                        showSheet.value = false
                        // Show bottom bar when sheet is closed
                        mainUiStateViewModel?.showBottomBar()
                    },
                    viewModel = viewModel
                )
            }
        } else {
            // Show bottom bar when sheet is not visible
            mainUiStateViewModel?.showBottomBar()
        }

        if (showLoginPrompt.value) {
            RequireLoginPrompt(
                onLogin = { showLoginPrompt.value = false },
                onSignUp = { showLoginPrompt.value = false },
                onDismiss = { showLoginPrompt.value = false }
            )
        }

        // TikTok-like transparent top bar with white text (unselected = grayish/alpha)
        ReelsTopHeader(
            onClickSearch = { navController.navigate(Screens.ReelsScreen.SearchReelsAndUsersScreen.route) },
            selectedTab = selectedTab,
            onTabChange = onTabChange,
            onClickExplore = { selectedTab = "Explore" },
            headerStyle = HeaderStyle.TRANSPARENT_WHITE_TEXT,
            modifier = Modifier
        )
    }

    if (showLoginPrompt.value) {
        RequireLoginPrompt(
            onLogin = { showLoginPrompt.value = false },
            onSignUp = { showLoginPrompt.value = false },
            onDismiss = { showLoginPrompt.value = false }
        )
    }

    // NEW: Modern Comments Bottom Sheet
    if (showCommentsSheet && selectedPostId != null) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable {
                    showCommentsSheet = false
                    selectedPostId = null
                    mainUiStateViewModel?.showBottomBar()
                }
        )
        
        CommentsSheet(
            postId = selectedPostId!!,
            onDismiss = {
                showCommentsSheet = false
                selectedPostId = null
                mainUiStateViewModel?.showBottomBar()
            }
        )
    }

    // Monitor page changes with safety bounds checking
    LaunchedEffect(pagerState.currentPage) {
        Log.d("ReelsView", "🎬 Page changed to: ${pagerState.currentPage}")

        // Safety check: ensure reelsList is not empty and page index is valid
        if (reelsList.isEmpty()) {
            Log.w("ReelsView", "🎬 Reels list is empty, skipping page update")
            mainUiStateViewModel?.setCurrentReel(null)
            return@LaunchedEffect
        }

        val pageIndex = pagerState.currentPage
        if (pageIndex < 0 || pageIndex >= reelsList.size) {
            Log.w("ReelsView", "🎬 Invalid page index: $pageIndex, reelsList size: ${reelsList.size}")
            mainUiStateViewModel?.setCurrentReel(null)
            return@LaunchedEffect
        }

        currentPage = pageIndex

        // Update current reel for cart status with safety check
        val currentReel = reelsList[pageIndex]
        if (currentReel != null && currentReel.id.isNotBlank()) {
            Log.d("ReelsView", "🎬 Current reel updated: ${currentReel.id}")
            viewModel.checkCartStatus(currentReel.id)
            
            // Set current reel in MainUiStateViewModel for Buy FAB
            mainUiStateViewModel?.setCurrentReel(currentReel)
            
            // Wait a bit to make sure user is actually viewing this reel, then track it
            kotlinx.coroutines.delay(1000) // Wait 1 second before tracking as viewed
            
            // Double check the page hasn't changed during the delay
            if (pagerState.currentPage == pageIndex) {
                Log.d("ReelsView", "🎬 Tracking reel as viewed: ${currentReel.id}")
                recentlyViewedViewModel.addReelToRecentlyViewed(currentReel)
            }
        } else {
            Log.w("ReelsView", "🎬 No valid current reel available")
            mainUiStateViewModel?.setCurrentReel(null)
        }
    }
}


enum class SheetType {
    Comments,
    AddToCart
}

@Composable
fun ReelsTopHeader(
    onClickSearch: () -> Unit,
    selectedTab: String,
    onTabChange: (String) -> Unit,
    onClickExplore: () -> Unit,
    headerStyle: HeaderStyle,
    modifier: Modifier = Modifier
) {
    val underlineColor = if (selectedTab == "Explore") Color(0xFF0066CC) else Color.White

    // Completely transparent header with only text buttons visible
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            HeaderTab(
                text = "Explore",
                isSelected = selectedTab == "Explore",
                onClick = {
                    onTabChange("Explore")
                    onClickExplore()
                },
                textColor = Color.White, // Force white text for visibility
                underlineColor = if (selectedTab == "Explore") Color(0xFF0066CC) else Color.White
            )
            HeaderTab(
                text = "Following",
                isSelected = selectedTab == "Following",
                onClick = { onTabChange("Following") },
                textColor = Color.White, // Force white text for visibility
                underlineColor = underlineColor
            )
            HeaderTab(
                text = "For you",
                isSelected = selectedTab == "For you",
                onClick = { onTabChange("For you") },
                textColor = Color.White, // Force white text for visibility
                underlineColor = underlineColor
            )
        }
        IconButton(
            onClick = onClickSearch,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = if (selectedTab == "Explore") Color(0xFF0066CC) else Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}




@Composable
fun HeaderTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    textColor: Color,
    underlineColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.7f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                    blurRadius = 4f
                )
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(3.dp)
                    .shadow(3.dp, RoundedCornerShape(2.dp))
                    .background(underlineColor, RoundedCornerShape(2.dp))
            )
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}


enum class SheetTab { Comments, Ratings }


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelsList(
    navController: NavController,
    onClickCommentButton: (Reels) -> Unit,
    viewModel: ReelsScreenViewModel,
    cartViewModel: CartViewModel,
    onClickCartButton: (Reels) -> Unit,
    onClickMoreButton: (Reels) -> Unit,
    reelsList: List<Reels>,
    isLoggedIn: Boolean,
    showLoginPrompt: androidx.compose.runtime.MutableState<Boolean>,
    initialPage: Int = 0,
    onShareReel: (Reels) -> Unit = {},
    recentlyViewedViewModel: com.project.e_commerce.android.presentation.viewModel.RecentlyViewedViewModel,
    globalVideoPlayStates: SnapshotStateMap<String, Boolean> = mutableStateMapOf(), // NEW: Accept global state
    mainUiStateViewModel: com.project.e_commerce.android.presentation.viewModel.MainUiStateViewModel? = null // NEW: Add mainUiStateViewModel parameter
) {
    // Inject TrackingRepository for view tracking
    val trackingRepository: TrackingRepository = koinInject()
    val pagerState = rememberPagerState(
        initialPage = if (reelsList.isNotEmpty()) {
            initialPage.coerceIn(0, reelsList.size - 1)
        } else {
            0
        }
    )
    val currentPage = pagerState.currentPage

    // Safety check: ensure we have valid reels to display
    if (reelsList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No reels available")
        }
        return
    }

    // NEW: Track current reel changes and update MainUiStateViewModel
    LaunchedEffect(pagerState.currentPage, reelsList) {
        Log.d("ReelsListPager", "🎬 ReelsList page changed to: ${pagerState.currentPage}")

        if (reelsList.isNotEmpty() && pagerState.currentPage < reelsList.size) {
            val currentReel = reelsList[pagerState.currentPage]
            Log.d(
                "ReelsListPager",
                "🎬 Current reel in ReelsList: ${currentReel.id} - ${currentReel.productName}"
            )

            // Update MainUiStateViewModel with current reel for Buy FAB
            mainUiStateViewModel?.setCurrentReel(currentReel)

            // Check cart status for the current reel
            viewModel.checkCartStatus(currentReel.id)

            // Track as recently viewed after a short delay
            delay(1000)
            if (pagerState.currentPage < reelsList.size) { // Double-check page hasn't changed
                recentlyViewedViewModel.addReelToRecentlyViewed(currentReel)
            }
        } else {
            Log.w("ReelsListPager", "🎬 Invalid page or empty reels list")
            mainUiStateViewModel?.setCurrentReel(null)
        }
    }

    // NEW: Use the global state for tracking which videos are playing/paused
    val videoPlayStates = globalVideoPlayStates

    // Initialize play states - only current page should be playing
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(currentPage, reelsList) {
        if (reelsList.isNotEmpty() && currentPage < reelsList.size) {
            val currentReelId = reelsList[currentPage].id
            // Pause all other videos
            videoPlayStates.keys.forEach { key ->
                videoPlayStates[key] = false
            }
            // Play only the current video
            videoPlayStates[currentReelId] = true
            Log.d("ReelsList", "🎥 Set playing state for reel $currentReelId (GLOBAL)")
            
            // NEW: Preload next videos when page changes
            val videoUris = reelsList.mapNotNull { it.video }
            VideoPreloader.preloadNextVideos(
                context = context,
                currentIndex = currentPage,
                videoUris = videoUris
            )
            Log.d("ReelsList", "🔄 Preloading triggered for page $currentPage (${VideoPreloader.getPreloadedCount()} cached, ${VideoPreloader.getActivePreloadJobsCount()} loading)")
        }
    }
    
    // NEW: Cleanup preloader when leaving ReelsView
    DisposableEffect(Unit) {
        onDispose {
            Log.d("ReelsList", "🧹 Cleaning up video preloader")
            VideoPreloader.clearAll()
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        count = reelsList.size
    ) { page ->
        val reel = reelsList[page]

        Log.d(
            "ReelsCrash",
            "Rendering reel at page=$page: id=${reel.id}, video=${reel.video}, userName=${reel.userName}, productName=${reel.productName}, images=${reel.images}, price=${reel.productPrice}, image placeholder?=${reel.productImage}"
        )

        // New guards and data logging as requested:
        if ((reel.video == null || "${reel.video}".isBlank() || !(reel.video.toString()
                .startsWith("http"))) && (reel.images == null || reel.images.isEmpty())
        ) {
            Log.e("ReelsCrash", "No valid video or image for reel ${reel.id}. Showing fallback.")
            Image(
                painter = painterResource(id = R.drawable.profile),
                contentDescription = "Fallback Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            return@VerticalPager
        }
        if (page < 0 || page >= reelsList.size) {
            Log.w("ReelsView", "🎬 Invalid page index: $page, bounds: 0-${reelsList.size-1}")
            return@VerticalPager
        }

        Log.d("ReelsView", "🎬 Rendering reel ${reel.id} at page $page")
        Log.d("ReelsView", "🎬 Reel video: ${reel.video}, isError: ${reel.isError}")
        Log.d("ReelsView", "🎬 Reel images count: ${reel.images?.size}")
        Log.d("ReelsView", "🎬 Reel fallback image: ${reel.fallbackImageRes}")

        // NEW: Get play state for this reel
        val isCurrentlyPlaying = videoPlayStates[reel.id] ?: (currentPage == page)

        // NEW: Per-reel heart animation state
        var showHeartForThisReel by remember(reel.id) { mutableStateOf(false) }
        var heartPositionForThisReel by remember(reel.id) { mutableStateOf(Offset.Zero) }
        
        // Phase 6: Track Reel view when visible
        val scope = rememberCoroutineScope()
        LaunchedEffect(page, currentPage, reel.id) {
            if (page == currentPage) {
                // Delay 1 second to ensure user actually views the Reel
                delay(1000)
                
                // Only track if still on this page
                if (page == currentPage) {
                    scope.launch {
                        try {
                            val promoterUid = reel.userId ?: ""
                            val productId = reel.marketplaceProductId
                            trackingRepository.trackReelView(
                                reelId = reel.id,
                                promoterUid = promoterUid,
                                productId = productId,
                                watchDuration = null,
                                completionRate = null
                            )
                            Log.d("ReelsTracking", "✅ View tracked for Reel ${reel.id}")
                        } catch (e: Exception) {
                            Log.e("ReelsTracking", "❌ Failed to track view: ${e.message}")
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {

            val videoUriToUse = reel.video
            Log.d("ReelsCrash", "[DEBUG] Using videoUri: $videoUriToUse for reel.id=${reel.id}")
            if (videoUriToUse != null && !reel.isError && videoUriToUse.toString().isNotEmpty() && videoUriToUse.toString().startsWith("http")) {
                VideoPlayer(
                    uri = videoUriToUse,
                    isPlaying = isCurrentlyPlaying, // Use the state-managed play status
                    onPlaybackStarted = {
                        Log.d("ReelsView", "🎬 Video playback started for reel ${reel.id}")
                    },
                    // NEW: Handle play/pause toggle
                    onPlaybackToggle = { isPlaying ->
                        Log.d(
                            "ReelsView",
                            "🎥 Video playback toggled for reel ${reel.id}: $isPlaying"
                        )
                        videoPlayStates[reel.id] = isPlaying

                        // If this video starts playing, pause all others
                        if (isPlaying) {
                            videoPlayStates.keys.forEach { reelId ->
                                if (reelId != reel.id) {
                                    videoPlayStates[reelId] = false
                                }
                            }
                        }
                    },
                    // NEW: Handle double tap to like with exact position
                    onDoubleTap = { tapOffset ->
                        if (isLoggedIn) {
                            // Heart animation fix: Record position & show trigger for this reel only!
                            heartPositionForThisReel = tapOffset
                            showHeartForThisReel = true

                            // Only like if not already liked, but always animate
                            if (!reel.love.isLoved) {
                                viewModel.onClackLoveReelsButton(reel.id)
                            }
                        } else {
                            showLoginPrompt.value = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (reel.images != null && reel.images.isNotEmpty() && reel.images.all {
                    it != null && it.toString().isNotBlank()
                }) {
                Log.d("ReelsView", "🎬 Showing images for reel ${reel.id}, count: ${reel.images.size}")
                val imagesPagerState = rememberPagerState(initialPage = 0)
                val aspect = 1f
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .offset(y = (-16f).dp)
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                    ) {
                        HorizontalPager(
                            state = imagesPagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(aspect),
                            count = reel.images.size
                        ) { imgIndex ->
                            val imageUri = reel.images[imgIndex]
                            if (imageUri != null && imageUri.toString().startsWith("http")) {
                                Image(
                                    painter = rememberAsyncImagePainter(imageUri),
                                    contentDescription = "Reel Image $imgIndex",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Log.e(
                                    "ReelsCrash",
                                    "Missing or invalid image for reel ${reel.id} at imgIndex=$imgIndex. Using placeholder."
                                )
                                Image(
                                    painter = painterResource(id = R.drawable.profile),
                                    contentDescription = "Fallback Image (Invalid URI)",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                        ) {
                            repeat(reel.images.size) { idx ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(if (imagesPagerState.currentPage == idx) 10.dp else 7.dp)
                                        .background(
                                            if (imagesPagerState.currentPage == idx) Color.White else Color.White.copy(
                                                alpha = 0.5f
                                            ),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            } else {
                Log.e(
                    "ReelsCrash",
                    "Neither video nor images are valid for reel ${reel.id}. Using fallback image."
                )
                Image(
                    painter = painterResource(id = R.drawable.profile),
                    contentDescription = "Fallback Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom content aligned like provided: left info + right interaction buttons in one row
            ReelContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart),
                navController = navController,
                reel = reel,
                viewModel = viewModel,
                cartViewModel = cartViewModel,
                onClickCommentButton = { onClickCommentButton(reel) },
                onClickCartButton = { onClickCartButton(reel) },
                onClickMoreButton = { onClickMoreButton(reel) },
                showLoginPrompt = showLoginPrompt,
                isLoggedIn = isLoggedIn,
            )

            // (Removed separate bottom-left column in favor of unified ReelContent row)

            // Heart animation overlay for like -- FIXED per-reel state!
            if (showHeartForThisReel) {
                HeartAnimation(
                    isVisible = true,
                    position = heartPositionForThisReel,
                    iconPainter = painterResource(id = R.drawable.ic_love_checked),
                    onAnimationEnd = { showHeartForThisReel = false },
                    iconSize = 100.dp
                )
            }
            // RequireLoginPrompt overlay
            if (showLoginPrompt.value) {
                RequireLoginPrompt(
                    onLogin = { showLoginPrompt.value = false },
                    onSignUp = { showLoginPrompt.value = false },
                    onDismiss = { showLoginPrompt.value = false }
                )
            }
        }
    }
}

// NOTE: ReelContent, UserInfo, OfferCard, ReelDescription, ReelHashtags, InteractionButtons
// are now in the components package:
// com.project.e_commerce.android.presentation.ui.screens.reelsScreen.components.ReelContent
// com.project.e_commerce.android.presentation.ui.screens.reelsScreen.components.ReelsInteractionButtons

// ReelContent, UserInfo, OfferCard, ReelDescription, ReelHashtags, InteractionButtons 
// are now in: com.project.e_commerce.android.presentation.ui.screens.reelsScreen.components

// Modern Bottom Sheet Content
@Composable
fun ModernBottomSheetContent(
    reel: Reels?,
    isSelectedComments: Boolean,
    isSelectedRatings: Boolean,
    onCommentTabClick: () -> Unit,
    onRatingTabClick: () -> Unit,
    onCloseClick: () -> Unit,
    viewModel: ReelsScreenViewModel,
    modifier: Modifier = Modifier
) {
    Log.d("BottomSheetDebug", "🎬 ModernBottomSheetContent called")
    Log.d("BottomSheetDebug", "🎬 Reel is null: ${reel == null}")

    if (reel != null) {
        Log.d("BottomSheetDebug", "🎬 Reel id: ${reel.id}")
        Log.d("BottomSheetDebug", "🎬 Reel newComment: ${reel.newComment}")
        Log.d("BottomSheetDebug", "🎬 Reel newComment.comment: ${reel.newComment.comment}")
        Log.d("BottomSheetDebug", "🎬 Reel comments: ${reel.comments}")
        Log.d("BottomSheetDebug", "🎬 Reel ratings: ${reel.ratings}")
    }

    Log.d("BottomSheetDebug", "🎬 isSelectedComments: $isSelectedComments")
    Log.d("BottomSheetDebug", "🎬 isSelectedRatings: $isSelectedRatings")

    // ✅ FIX: Observe the ViewModel state to get reactive comment text
    val reelsState by viewModel.state.collectAsState()
    val currentReel = reelsState.find { it.id == reel?.id }

    val commentsList = try {
        currentReel?.comments ?: emptyList()
    } catch (e: Exception) {
        Log.e("BottomSheetDebug", "🎬 Error accessing comments list: ${e.message}")
        emptyList()
    }
    Log.d("BottomSheetDebug", "🎬 Comments list size: ${commentsList.size}")

    val ratesList = try {
        currentReel?.ratings ?: emptyList()
    } catch (e: Exception) {
        Log.e("BottomSheetDebug", "🎬 Error accessing ratings list: ${e.message}")
        emptyList()
    }
    Log.d("BottomSheetDebug", "🎬 Ratings list size: ${ratesList.size}")

    // ✅ FIX: Use the reactive state for the comment text
    val newComment = try {
        val comment = currentReel?.newComment?.comment ?: ""
        Log.d("BottomSheetDebug", "🎬 Extracted newComment from reactive state: '$comment' from reel id: ${currentReel?.id}")
        comment
    } catch (e: Exception) {
        Log.e("BottomSheetDebug", "🎬 Error accessing newComment: ${e.message}")
        ""
    }
    Log.d("BottomSheetDebug", "🎬 New comment from reactive state: '$newComment'")

    val onCommentChange: (String) -> Unit = { newText ->
        val reelId = try {
            currentReel?.id ?: ""
        } catch (e: Exception) {
            Log.e("BottomSheetDebug", "🎬 Error accessing reel id for comment change: ${e.message}")
            ""
        }
        Log.d("BottomSheetDebug", "🎬 onCommentChange called with text: '$newText', reelId: '$reelId'")
        if (reelId.isNotBlank()) {
            Log.d("BottomSheetDebug", "🎬 Calling viewModel.onWriteNewCommentForReel...")
            viewModel.onWriteNewCommentForReel(reelId, newText)
            Log.d("BottomSheetDebug", "🎬 viewModel.onWriteNewCommentForReel completed")
        } else {
            Log.e("BottomSheetDebug", "🎬 ReelId is blank, cannot update comment")
        }
    }
    val onClickSend: () -> Unit = {
        val reelId = try {
            currentReel?.id ?: ""
        } catch (e: Exception) {
            Log.e("BottomSheetDebug", "🎬 Error accessing reel id: ${e.message}")
            ""
        }
        viewModel.onClickAddComment(reelId, newComment)
    }

    Log.d("BottomSheetDebug", "🎬 About to render bottom sheet UI")

    val minHeightDp = 300.dp  // أقل ارتفاع (400dp)
    val maxHeightFraction = 0.92f   // أقصى نسبة للشاشة

    Log.d("BottomSheetDebug", "🎬 Getting screen configuration...")
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val density = LocalDensity.current
    Log.d("BottomSheetDebug", "🎬 Screen height: $screenHeight, density: $density")

    // احسب نسبة الارتفاع الأدنى (400dp / ارتفاع الشاشة)
    val minHeightFraction = with(density) {
        minHeightDp.toPx() / screenHeight.toPx()
    }
    Log.d("BottomSheetDebug", "🎬 Min height fraction calculated: $minHeightFraction")

    var sheetHeightFraction by remember { mutableStateOf(0.5f) }
    var lastDragPosition by remember { mutableStateOf(0f) }
    Log.d("BottomSheetDebug", "🎬 State variables initialized")

    Log.d("BottomSheetDebug", "🎬 About to render Column composable...")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(sheetHeightFraction)
            .background(Color.White, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset -> lastDragPosition = offset.y },
                    onVerticalDrag = { change, dragAmount ->
                        val pxDelta = -dragAmount
                        val dpDelta = with(density) { pxDelta.toDp().value }
                        val screenHeightValue = with(density) { screenHeight.toPx() }
                        val fractionDelta = dpDelta / (screenHeightValue / density.density)
                        sheetHeightFraction = (sheetHeightFraction + fractionDelta)
                            .coerceIn(minHeightFraction, maxHeightFraction)
                    }
                )
            }
            .imePadding()
    ) {
        Log.d("BottomSheetDebug", "🎬 Inside Column, about to render handle bar...")
        // Handle bar for dragging
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(5.dp)
                .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Log.d("BottomSheetDebug", "🎬 Handle bar rendered, about to render header row...")

        // Header with tabs and close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Log.d("BottomSheetDebug", "🎬 Inside header Row, about to render tab buttons...")
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ModernTabButton(
                    text = "Comments",
                    icon = painterResource(id = R.drawable.comment_outlined),
                    isSelected = isSelectedComments,
                    iconSize = 24,
                    onClick = onCommentTabClick
                )

                ModernTabButton(
                    text = "Rates",
                    icon = painterResource(id = R.drawable.ic_star),
                    isSelected = isSelectedRatings,
                    onClick = onRatingTabClick
                )
            }
            Log.d("BottomSheetDebug", "🎬 Tab buttons rendered, about to render close button...")

            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.offset(x = 8.dp, y = (-8f).dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Black,
                )
            }
        }
        Log.d("BottomSheetDebug", "🎬 Header row rendered, about to render spacer...")

        Spacer(modifier = Modifier.height(12.dp))
        Log.d("BottomSheetDebug", "🎬 About to render content section...")

        // المحتوى المتغير حسب التبويب المختار
        if (isSelectedComments) {
            Log.d("BottomSheetDebug", "🎬 About to render CommentsSection...")
            CommentsSection(
                comments = commentsList,
                newComment = newComment,
                onCommentChange = onCommentChange,
                onClickSend = onClickSend,
                modifier = Modifier.weight(1f)
            )
        } else {
            Log.d("BottomSheetDebug", "🎬 About to render RatingsSection...")
            RatingsSection(
                ratings = ratesList,
                newComment = newComment,
                onCommentChange = onCommentChange,
                onClickSend = onClickSend,
                modifier = Modifier.weight(1f)
            )
        }
        Log.d("BottomSheetDebug", "🎬 Content section rendered successfully!")
    }
}


@Composable
fun ModernTabButton(
    text: String,
    icon: Painter,
    isSelected: Boolean,
    iconSize :Int = 18,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(iconSize.dp)
            )
            Text(
                text = text,
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(3.dp)
                    .background(Color(0xFF808080), RoundedCornerShape(2.dp))
            )
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}


@Composable
fun CommentsSection(
    comments: List<Comment>,
    newComment: String,
    onCommentChange: (String) -> Unit,
    onClickSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // المحتوى القابل للتمرير
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(comments) { comment ->
                ModernCommentItem(comment = comment)
            }

            // إضافة رسالة عند عدم وجود تعليقات
            if (comments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = Color(0xFFCCCCCC),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No comments yet",
                                color = Color(0xFF888888),
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Be the first to comment!",
                                color = Color(0xFFCCCCCC),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // حقل الإدخال الثابت في الأسفل
        ModernInputField(
            value = newComment,
            onValueChange = onCommentChange,
            onSendClick = onClickSend,
            placeholder = "Write Comment",
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}


// Helper function to calculate average rating from ratings list
private fun calculateAverageRating(ratings: List<Ratings>): Double {
    return if (ratings.isEmpty()) {
        0.0
    } else {
        val totalStars = ratings.sumOf { it.rate }
        val averageRating = totalStars.toDouble() / ratings.size
        // Round to 1 decimal place
        (averageRating * 10).toInt() / 10.0
    }
}

// Helper function to format rating display
private fun formatRating(rating: Double): String {
    return if (rating == 0.0) {
        "0.0"
    } else {
        String.format("%.1f", rating)
    }
}

@Composable
fun RatingsSection(
    ratings: List<Ratings>,
    newComment: String,
    onCommentChange: (String) -> Unit,
    onClickSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var videoThumbnail by remember { mutableStateOf<Bitmap?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedMediaUri = uri
        if (uri != null && uri.toString().contains("video")) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val bitmap = retriever.getFrameAtTime(0)
                videoThumbnail = bitmap
            } catch (_: Exception) {
                videoThumbnail = null
            } finally {
                retriever.release()
            }
        } else {
            videoThumbnail = null
        }
    }

    val averageRating = calculateAverageRating(ratings)
    val formattedRating = formatRating(averageRating)

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // عرض التقييم الإجمالي
        // الجزء العلوي في RatingsSection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // الصندوق الرمادي فيه التقييم والنجمة
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = formattedRating,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "(${ratings.size} Rates)",
                    fontSize = 14.sp,
                    color = Color(0xFF888888)
                )
            }
        }


        // قائمة التقييمات
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ratings) { rating ->
                ModernRatingItem(rating = rating)
            }

            if (ratings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFCCCCCC),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No ratings yet",
                                color = Color(0xFF888888),
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Be the first to rate!",
                                color = Color(0xFFCCCCCC),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // حقل الإدخال الثابت في الأسفل
        /*if (selectedMediaUri != null) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .padding(top = 12.dp, bottom = 6.dp) // هامش فوق وتحت للمعاينة الجديدة
            ) {
                // صورة أو thumbnail الفيديو
                if (videoThumbnail != null) {
                    Image(
                        bitmap = videoThumbnail!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(14.dp))
                    )
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(selectedMediaUri),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(14.dp))
                    )
                }
                // زر X (جزء منه فوق الصورة وجزء خارجها)
                IconButton(
                    onClick = {
                        selectedMediaUri = null
                        videoThumbnail = null
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-10f).dp) // جزء من الزر يخرج خارج الصورة
                        .background(Color(0xAA222222), CircleShape) // شفافية أعلى
                        .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                        .zIndex(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close, // أيقونة X
                        contentDescription = "Remove Media",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }*/


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Color(0x33000000),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
        )

        // Input field with photo icon and submit button


//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 8.dp, vertical = 8.dp)
//                .background(Color.White, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
//        ) {
//            TextField(
//                value = newComment,
//                onValueChange = onCommentChange,
//                placeholder = {
//                    Text(
//                        text = "Add Rating",
//                        color = Color.Gray,
//                        fontSize = 16.sp
//                    )
//                },
//                modifier = Modifier.weight(1f),
//                colors = TextFieldDefaults.textFieldColors(
//                    backgroundColor = Color.Transparent,
//                    focusedIndicatorColor = Color.Transparent,
//                    unfocusedIndicatorColor = Color.Transparent,
//                    cursorColor = Color(0xFF176DBA),
//                    textColor = Color.Black
//                ),
//                singleLine = true
//            )
//
//            IconButton(
//                onClick = { launcher.launch("*/*") },
//                modifier = Modifier.size(36.dp)
//            ) {
//                Icon(
//                    painter = painterResource(id = R.drawable.ic_photo),
//                    contentDescription = "Pick Media",
//                    tint = Color(0xFF176DBA),
//                    modifier = Modifier.size(24.dp)
//                )
//            }
//            Spacer(modifier = Modifier.width(8.dp))
//
//            IconButton(
//                onClick = onClickSend,
//                modifier = Modifier.size(36.dp).offset(y= (-4).dp)
//            ) {
//                Icon(
//                    painter = painterResource(id = R.drawable.ic_send),
//                    contentDescription = "Send",
//                    tint = Color(0xFF176DBA),
//                    modifier = Modifier.size(32.dp)
//                )
//            }
//        }

    }
}


@Composable
fun ModernInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    placeholder: String = "Add Rate",
    modifier: Modifier = Modifier
) {
    Log.d(
        "ModernInputFieldDebug",
        "🎬 ModernInputField called with value: '$value', placeholder: '$placeholder'"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(
                Color(0x33000000),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(Color.White, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))

    ) {
        TextField(
            value = value,
            onValueChange = { newValue ->
                Log.d("ModernInputFieldDebug", "🎬 TextField onValueChange called with: '$newValue'")
                onValueChange(newValue)
                Log.d("ModernInputFieldDebug", "🎬 TextField onValueChange callback completed")
            },
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            },
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color(0xFF176DBA),
                textColor = Color.Black
            ),
            singleLine = true
        )

        IconButton(
            onClick = onSendClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_send),
                contentDescription = "Send",
                tint = Color(0xFF176DBA),
                modifier = Modifier.size(32.dp)
            )
        }
    }

}



// ModernCommentItem
// ORIGINAL GLIDE ANNOTATION (COMMENTED OUT):
// @OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ModernCommentItem(
    comment: Comment,
    modifier: Modifier = Modifier
) {
    var isLoved by remember { mutableStateOf(comment.isLoved) }
    var likesCount by remember { mutableStateOf(800) }
    var dislikesCount by remember { mutableStateOf(2) }
    
    // Fetch commenter's profile image
    val commenterProfileImageUrl = remember { mutableStateOf<String?>(null) }
    
    // TODO: Migrate to backend API - profile image fetch temporarily disabled
    /*
    LaunchedEffect(comment.userId) {
        if (comment.userId.isNotBlank()) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val doc = db.collection("users").document(comment.userId).get().await()
                val profileImageUrl = doc.getString("profileImageUrl")
                commenterProfileImageUrl.value = profileImageUrl
                Log.d("ModernCommentItem", "✅ Fetched profile image for commenter ${comment.userId}: $profileImageUrl")
            } catch (e: Exception) {
                Log.e("ModernCommentItem", "❌ Failed to fetch profile image for commenter ${comment.userId}: ${e.message}")
            }
        }
    }
    */

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // User avatar with real profile image
        if (commenterProfileImageUrl.value != null && commenterProfileImageUrl.value!!.isNotBlank()) {
            AsyncImage(
                model = commenterProfileImageUrl.value,
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        } else {
            AsyncImage(
                model = R.drawable.profile,
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Main content area
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Username
            Text(
                text = comment.userName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Comment text
            Text(
                text = comment.comment,
                fontSize = 14.sp,
                color = Color(0xFF333333),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Time and Reply
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = comment.time,
                    fontSize = 13.sp,
                    color = Color(0xFF888888)
                )
                Text(
                    text = "Reply",
                    fontSize = 13.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier.clickable { /* Handle reply */ }
                )
            }
        }

        // Right side - Icons and counts
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Icons row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isLoved) R.drawable.ic_love_checked else R.drawable.ic_heart_outlined
                    ),
                    contentDescription = "Like",
                    tint = if (isLoved) Color.Red else Color(0xFF999999),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            isLoved = !isLoved
                            likesCount += if (isLoved) 1 else -1
                        }
                )
                Icon(
                    painter = painterResource(id = R.drawable.dis_like),
                    contentDescription = "Dislike",
                    tint = Color(0xFF999999),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            dislikesCount++
                        }
                )
            }

            // Counts row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = likesCount.toString(),
                    fontSize = 12.sp,
                    color = Color(0xFF888888),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = dislikesCount.toString(),
                    fontSize = 12.sp,
                    color = Color(0xFF888888),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}



// ModernRatingItem
// ORIGINAL GLIDE ANNOTATION (COMMENTED OUT):
// @OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ModernRatingItem(
    rating: Ratings,
    modifier: Modifier = Modifier
) {
    var isFavorite by remember { mutableStateOf(false) }

    // Fetch rater's profile image
    val raterProfileImageUrl = remember { mutableStateOf<String?>(null) }

    // TODO: Migrate to backend API - profile image fetch temporarily disabled
    /*
    LaunchedEffect(rating.userId) {
        if (rating.userId.isNotBlank()) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val doc = db.collection("users").document(rating.userId).get().await()
                val profileImageUrl = doc.getString("profileImageUrl")
                raterProfileImageUrl.value = profileImageUrl
                Log.d(
                    "ModernRatingItem",
                    "✅ Fetched profile image for rater ${rating.userId}: $profileImageUrl"
                )
            } catch (e: Exception) {
                Log.e(
                    "ModernRatingItem",
                    "❌ Failed to fetch profile image for rater ${rating.userId}: ${e.message}"
                )
            }
        }
    }
    */

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User avatar with real profile image
        if (raterProfileImageUrl.value != null && raterProfileImageUrl.value!!.isNotBlank()) {
            AsyncImage(
                model = raterProfileImageUrl.value,
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        } else {
            AsyncImage(
                model = R.drawable.profile,
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // النصوص
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = rating.userName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = rating.review,
                fontSize = 14.sp,
                color = Color(0xFF333333),
                lineHeight = 18.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = if (index < rating.rate) Color(0xFFFFC107) else Color(0xFFE0E0E0),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // القلب والوقت
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isFavorite) R.drawable.ic_love_checked else R.drawable.ic_love
                ),
                contentDescription = "Favorite",
                tint = if (isFavorite) Color.Red else Color.Gray,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        isFavorite = !isFavorite
                    }
            )
            Text(
                text = "12h",
                fontSize = 12.sp,
                color = Color(0xFF888888)
            )
        }
    }
}




/*
@Composable
fun CommentsContent(
    comments: List<Comment>,
    newComment: String,
    onCommentChange: (String) -> Unit,
    onLoveComment: (String, String) -> Unit,
    onClickAddComment : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(comments) { comment ->
                CommentItem(
                    userName = comment.userName,
                    comment = comment.comment,
                    time = comment.time,
                    isLoved = comment.isLoved,
                    numberOfLoved = 5,
                    isReplyShown = comment.isReplyShown,
                    onClick = {
                        onLoveComment(comment.id, comment.id)
                    }
                )
            }
        }

        // حقل الكتابة يظهر دائما أسفل الشاشة مباشرة
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(18.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            TextField(
                value = newComment,
                onValueChange = onCommentChange,
                placeholder = { Text("Add Comment") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(onClick = onClickAddComment) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = Color(0xFF176DBA)
                )
            }
        }
    }
}



@Composable
fun RatingsContent(
    ratings: List<Ratings>,
    newRating: String,
    onRatingChange: (String) -> Unit,
    onClickAddRating: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "4.9 ⭐",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryColor
            )
            Text(
                text = "(32 Rates)",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        Spacer(Modifier.height(8.dp))

        // Ratings list
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(ratings) { rating ->
                    RatingCard(
                        userName = rating.userName,
                        rateContent = rating.review,
                        rateNumber = rating.rate,
                        time = "12 h"
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        // Add Rate input fixed at bottom
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            TextField(
                value = newRating,
                onValueChange = onRatingChange,
                placeholder = { Text("Add Rate") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(onClick = onClickAddRating) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_send),
                    contentDescription = null,
                    tint = PrimaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}


@Composable
fun TabWithIconAndIndicator(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color = Color.Black
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(bottom = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) selectedColor else Color(0xFF888888),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                color = if (selected) selectedColor else Color(0xFF888888),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 16.sp
            )
        }
        if (selected) {
            Box(
                Modifier
                    .height(3.dp)
                    .width(32.dp)
                    .background(selectedColor, RoundedCornerShape(2.dp))
            )
        } else {
            Spacer(Modifier.height(3.dp))
        }
    }
}*/



@Composable
fun BuyBottomSheet(
    onClose: () -> Unit = {},
    productPrice: Double = 0.0,
    productImage: String = "",
    reel: Reels? = null,
    cartViewModel: CartViewModel? = null,
    navController: NavController? = null,
    modifier: Modifier = Modifier
) {
    Log.d("BuyBottomSheetDebug", "🎬 BuyBottomSheet opened for reel: ${reel?.id}")

    var quantity by remember { mutableStateOf(1) }
    val totalPrice = productPrice * quantity
    var addedToCart by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // --- TOP CARD (Figma spec, fully refactored) ---
    val thumbnailUrl = remember(reel) {
        Log.d("BuyBottomSheetDebug", "🎬 Generating thumbnail for reel: ${reel?.id}")

        if (reel != null) {
            val result =
                com.project.e_commerce.android.presentation.utils.VideoThumbnailUtils.getBestThumbnail(
                images = reel.images?.map { it.toString() },
                videoUrl = reel.video?.toString(),
                fallbackUrl = reel.productImage
            )
            Log.d("BuyBottomSheetDebug", "🎬 Generated thumbnail: $result")
            result
        } else {
            Log.d("BuyBottomSheetDebug", "🎬 No reel provided, using productImage: $productImage")
            productImage
        }
    }
    val realDescription = reel?.contentDescription.orEmpty()
    val realName = reel?.productName.orEmpty()

    // ✅ FIX: Use the same rating calculation as RatingsSection instead of hardcoded value
    val averageRating = if (reel != null && reel.ratings.isNotEmpty()) {
        calculateAverageRating(reel.ratings)
    } else if (reel != null && reel.rating > 0.1) {
        reel.rating
    } else {
        0.0
    }
    val formattedRating = formatRating(averageRating)
    val ratingsCount = reel?.ratings?.size ?: 0

    // UI matches AddToCartBottomSheet layout (placement and styling), logic preserved
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .heightIn(min = 390.dp, max = 600.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .imePadding()
    ) {
        // Close button row (top-right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Product image
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = realName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Title
        Text(
            text = realName,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Rating chip (amber on light-amber)
        if (averageRating > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .background(Color(0xFFFFF8E1), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$formattedRating", color = Color(0xFFFFC107), fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Description
        if (realDescription.isNotBlank()) {
            Text(
                text = realDescription,
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Price and quantity row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${totalPrice.toInt()}$",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6F00)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(48.dp)
                    .border(1.dp, Color(0xFF176DBA), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp)
            ) {
                IconButton(onClick = { if (quantity > 1) quantity-- }) {
                    Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Text(
                    "$quantity",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    color = Color(0xFF0B74DA)
                )
                IconButton(onClick = { quantity++ }) {
                    Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add to Cart button
        Button(
            onClick = {
                if (reel != null && cartViewModel != null) {
                    val cartItem = CartItemUi(
                        productId = reel.marketplaceProductId ?: reel.id,
                        name = realName.ifBlank { "Product" },
                        price = productPrice,
                        imageUrl = thumbnailUrl.orEmpty(),
                        quantity = quantity,
                        reelId = reel.id,
                        promoterUid = reel.userId.ifBlank { null } // Promoteur pour split de commission
                    )
                    cartViewModel.addToCart(cartItem)
                    addedToCart = true
                    scope.launch {
                        delay(1500)
                        onClose()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (addedToCart) Color(0xFF4CAF50) else Color(0xFFFF6F00)
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !addedToCart
        ) {
            if (addedToCart) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Added to Cart!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to Cart", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        // View Product Details link
        if (navController != null && reel?.marketplaceProductId != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    onClose()
                    navController.navigate(Screens.Marketplace.ProductDetail.createRoute(reel.marketplaceProductId!!))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "View Full Product Details",
                    color = Color(0xFF176DBA),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}


@Composable
fun FollowingReelsContent(
    navController: NavController,
    followingViewModel: FollowingViewModel,
    reelsViewModel: ReelsScreenViewModel,
    cartViewModel: CartViewModel,
    onShowSheet: (SheetType, Reels?) -> Unit,
    onShareReel: (Reels) -> Unit,
    recentlyViewedViewModel: com.project.e_commerce.android.presentation.viewModel.RecentlyViewedViewModel
) {
    Log.d("FollowingTabDebug", "Entered FollowingReelsContent")
    val followingState by followingViewModel.uiState.collectAsState()
    val currentUserProvider: com.project.e_commerce.data.local.CurrentUserProvider = org.koin.compose.koinInject()
    var currentUserId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        currentUserId = currentUserProvider.getCurrentUserId()
    }
    val hasLoadedFollowing =
        androidx.compose.runtime.saveable.rememberSaveable(currentUserId) { mutableStateOf(false) }

    // NEW: Create dedicated state for following reels videos
    val followingVideoPlayStates = remember { mutableStateMapOf<String, Boolean>() }

    Log.d(
        "FollowingTabDebug",
        "currentUserId=$currentUserId hasLoadedFollowing.value=${hasLoadedFollowing.value}"
    )
    LaunchedEffect(currentUserId) {
        Log.d(
            "FollowingTabDebug",
            "LaunchedEffect(currentUserId=$currentUserId) running, hasLoadedFollowing.value=${hasLoadedFollowing.value}"
        )
        if (!hasLoadedFollowing.value && currentUserId != null) {
            val uid = currentUserId!!
            Log.d(
                "FollowingTabDebug",
                "Calling loadUserData for userId=$uid in FollowingReelsContent"
            )
            followingViewModel.resetLoadAttempts()
            followingViewModel.loadUserData(uid, "current_user")
            hasLoadedFollowing.value = true
        }
    }

    // Debug logging for state changes - only log when state actually changes
    // (Removed LaunchedEffect to prevent recomposition loop)
    Log.d(
        "FollowingReelsContent",
        "🔍 State changed - isLoading: ${followingState.isLoading}, followingCount: ${followingState.following.size}"
    )

    // Log when component recomposes to track unnecessary recompositions
    Log.d("FollowingReelsContent", "🔄 Component recomposed - isLoading: ${followingState.isLoading}, followingCount: ${followingState.following.size}")

    when {
        followingState.isLoading -> {
            Log.d("FollowingReelsContent", "⏳ Showing loading state")
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        followingState.error?.isNotEmpty() == true -> {
            Log.d("FollowingReelsContent", "❌ Showing error state: ${followingState.error}")
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error: ${followingState.error}",
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            }
        }
        followingState.following.isEmpty() -> {
            Log.d("FollowingReelsContent", "👥 No following users")
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Text(
                        text = "You're not following anyone yet",
                        style = MaterialTheme.typography.h6,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    Text(
                        text = "Follow some users to see their reels here",
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
        }
        else -> {
            Log.d("FollowingReelsContent", "✅ Showing following reels for ${followingState.following.size} users")
            val followingUserIds = followingState.following.map { it.id }
            FollowingReelsList(
                navController = navController,
                followingUserIds = followingUserIds,
                reelsViewModel = reelsViewModel,
                cartViewModel = cartViewModel,
                onShowSheet = onShowSheet,
                onShareReel = onShareReel,
                recentlyViewedViewModel = recentlyViewedViewModel,
                globalVideoPlayStates = followingVideoPlayStates // NEW: Pass the remembered mutableStateMapOf
            )
        }
    }
}

@Composable
fun FollowingReelsList(
    navController: NavController,
    followingUserIds: List<String>,
    reelsViewModel: ReelsScreenViewModel,
    cartViewModel: CartViewModel,
    onShowSheet: (SheetType, Reels?) -> Unit,
    onShareReel: (Reels) -> Unit,
    recentlyViewedViewModel: com.project.e_commerce.android.presentation.viewModel.RecentlyViewedViewModel,
    globalVideoPlayStates: SnapshotStateMap<String, Boolean> = mutableStateMapOf() // NEW: Accept global state
) {
    val reelsState by reelsViewModel.state.collectAsState()

    Log.d("FollowingReelsList", "🚀 COMPONENT CREATED - Users: ${followingUserIds.size}")

    // Get following reels in real-time (updates when reels state changes)
    val followingReels = reelsViewModel.getReelsFromUsers(followingUserIds)

    Log.d("FollowingReelsList", "📱 Displaying ${followingReels.size} reels")

    if (followingReels.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Text(
                    text = "No reels from people you follow",
                    style = MaterialTheme.typography.h6,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
                Text(
                    text = "The people you follow haven't posted any reels yet",
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        }
    } else {
        // Use the existing ReelsList composable for proper reel display
        ReelsList(
            navController = navController,
            onClickCommentButton = { onShowSheet(SheetType.Comments, reelsState.firstOrNull()) },
            viewModel = reelsViewModel,
            cartViewModel = cartViewModel,
            onClickCartButton = { onShowSheet(SheetType.AddToCart, reelsState.firstOrNull()) },
            onClickMoreButton = { reel ->
                if (reel != null) onShareReel(reel)
            },
            reelsList = followingReels,
            isLoggedIn = true, // Since we're in the following tab, user is logged in
            showLoginPrompt = remember { mutableStateOf(false) },
            initialPage = 0,
            onShareReel = onShareReel,
            recentlyViewedViewModel = recentlyViewedViewModel,
            globalVideoPlayStates = globalVideoPlayStates, // Pass through
            mainUiStateViewModel = null // Following tab doesn't need Buy FAB, so pass null
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun BuyBottomSheetPreview() {
    BuyBottomSheet()
}
