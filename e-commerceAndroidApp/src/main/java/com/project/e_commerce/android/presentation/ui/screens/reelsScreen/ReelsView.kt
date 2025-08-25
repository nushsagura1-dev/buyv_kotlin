package com.project.e_commerce.android.presentation.ui.screens.reelsScreen

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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.google.accompanist.pager.*
import com.google.firebase.auth.FirebaseAuth
import com.project.e_commerce.android.R
import com.project.e_commerce.android.presentation.ui.composable.composableScreen.public.VideoPlayer
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.ui.screens.HeaderStyle
import com.project.e_commerce.android.presentation.ui.screens.HeartAnimation
import com.project.e_commerce.android.presentation.ui.screens.RequireLoginPrompt
import com.project.e_commerce.android.presentation.ui.utail.noRippleClickable
import com.project.e_commerce.android.presentation.viewModel.CartViewModel
import com.project.e_commerce.android.presentation.viewModel.followingViewModel.FollowingViewModel
import com.project.e_commerce.android.presentation.viewModel.reelsScreenViewModel.ReelsScreenViewModel

import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import android.util.Log
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ReelsView(
    navController: NavHostController,
    viewModel: ReelsScreenViewModel,
    cartViewModel: CartViewModel,
    isLoggedIn: Boolean,
    setShowLoginPrompt: (Boolean) -> Unit,
    onShowSheet: (SheetType) -> Unit
) {
    // Get the saved tab from navigation
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    var selectedTab by rememberSaveable { 
        mutableStateOf(savedStateHandle?.get("selectedTab") ?: "For you") 
    }
    
    var showLoginPrompt by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    
    // Refresh following data when reels view becomes active
    val followingViewModel: FollowingViewModel = koinViewModel()
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid
    
    LaunchedEffect(Unit) {
        currentUserId?.let { userId ->
            val currentUser = auth.currentUser
            val username = currentUser?.displayName ?: currentUser?.email?.split("@")?.firstOrNull() ?: "user"
            followingViewModel.loadUserData(userId, username)
        }
        
        // Force refresh reels when screen becomes active
        Log.d("ReelsView", "🔄 ReelsView became active, forcing reels refresh")
        viewModel.forceRefreshFromProductViewModel()
    }

    val state = viewModel.state.collectAsState().value
    
    // Debug logging for reels state
    LaunchedEffect(state) {
        Log.d("ReelsView", "📊 Reels state updated: ${state.size} reels")
        if (state.isNotEmpty()) {
            Log.d("ReelsView", "📺 First reel: ${state.first().id} - ${state.first().productName}")
        } else {
            Log.d("ReelsView", "⚠️ No reels in state")
        }
    }

    // Save tab state to savedStateHandle when it changes
    LaunchedEffect(selectedTab) {
        savedStateHandle?.set("selectedTab", selectedTab)
    }
    
    // Load tab state from savedStateHandle when component initializes
    LaunchedEffect(Unit) {
        val tab: String? = savedStateHandle?.get("selectedTab")
        if (tab != null && tab != selectedTab) {
            selectedTab = tab
        }
    }

    // Find the initial page index if a specific reel is requested
    val initialPage = try {
        val reelId = savedStateHandle?.get("reelId") as? String
        if (reelId != null) {
            val index = state.indexOfFirst { it.id == reelId }
            if (index >= 0) index else 0
        } else 0
    } catch (e: Exception) {
        0
    }

    var isSelectedComments by remember { mutableStateOf(true) }
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

    // Tab change + Explore navigation
    val onTabChange = { newTab: String ->
        if (newTab != selectedTab) {
            selectedTab = newTab
            savedStateHandle?.set("selectedTab", newTab)
        }
    }
    
    // Handle Explore tab click
    val currentTab = selectedTab
    if (currentTab == "Explore") {
        // Save current tab state before navigating
        savedStateHandle?.set("selectedTab", currentTab)
        navController.navigate(Screens.ReelsScreen.ExploreScreen.route)
    }

    LaunchedEffect(selectedTab) { Log.d("ReelsView", "📱 Tab state changed to: $selectedTab") }
    LaunchedEffect(Unit) { Log.d("ReelsView", "🚀 ReelsView initialized with tab: $selectedTab") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        val currentTab = selectedTab
        when (currentTab) {
            "Explore" -> {
                // Navigate to explore screen
                savedStateHandle?.set("selectedTab", currentTab)
                navController.navigate(Screens.ReelsScreen.ExploreScreen.route)
            }
            "Following" -> {
                FollowingReelsContent(
                    navController = navController,
                    followingViewModel = followingViewModel,
                    reelsViewModel = viewModel,
                    onShowSheet = { sheetType ->
                        showSheet.value = true
                        currentSheet = sheetType
                        // mainUiStateViewModel.setBottomSheetVisible(true)
                    }
                )
            }
            "For you" -> {
                ReelsList(
                    navController = navController,
                    onClickCommentButton = {
                        showSheet.value = true
                        // mainUiStateViewModel.setBottomSheetVisible(true)
                        sheetTab.value = SheetTab.Comments
                    },
                    viewModel = viewModel,
                    onClickCartButton = {
                        if (!isLoggedIn) showLoginPrompt = true
                        showSheet.value = true
                        sheetTab.value = SheetTab.Ratings
                    },
                    onClickMoreButton = { /* no-op */ },
                    reelsList = state,
                    isLoggedIn = isLoggedIn,
                    setShowLoginPrompt = { showLoginPrompt = it },
                    initialPage = initialPage
                )
            }
            else -> {
                ReelsList(
                    navController = navController,
                    onClickCommentButton = {
                        showSheet.value = true
                        // mainUiStateViewModel.setBottomSheetVisible(true)
                        sheetTab.value = SheetTab.Comments
                    },
                    viewModel = viewModel,
                    onClickCartButton = {
                        if (!isLoggedIn) showLoginPrompt = true
                        showSheet.value = true
                        sheetTab.value = SheetTab.Ratings
                    },
                    onClickMoreButton = { /* no-op */ },
                    reelsList = state,
                    isLoggedIn = isLoggedIn,
                    setShowLoginPrompt = { showLoginPrompt = it },
                    initialPage = initialPage
                )
            }
        }

        // Overlay Custom BottomSheet
        if (showSheet.value) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable {
                        showSheet.value = false
                        // mainUiStateViewModel.setBottomSheetVisible(false)
                    }
            )
            ModernBottomSheetContent(
                state = state,
                isSelectedComments = isSelectedComments,
                isSelectedRatings = isSelectedRatings,
                onCommentTabClick = onCommentTabClick,
                onRatingTabClick = onRatingTabClick,
                modifier = Modifier.align(Alignment.BottomCenter),
                onCloseClick = {
                    showSheet.value = false
                    // mainUiStateViewModel.setBottomSheetVisible(false)
                },
                viewModel = viewModel
            )
        }

        if (showLoginPrompt) {
            RequireLoginPrompt(
                onLogin = {
                    showLoginPrompt = false
                    navController.navigate(Screens.LoginScreen.route)
                },
                onSignUp = {
                    showLoginPrompt = false
                    navController.navigate(Screens.LoginScreen.CreateAccountScreen.route)
                },
                onDismiss = { showLoginPrompt = false }
            )
        }

        // TikTok-like transparent top bar with white text (unselected = grayish/alpha)
        ReelsTopHeader(
            onClickSearch = { navController.navigate(Screens.ReelsScreen.SearchReelsAndUsersScreen.route) },
            selectedTab = selectedTab,
            onTabChange = onTabChange,
            onClickExplore = { /* no-op */ },
            headerStyle = HeaderStyle.TRANSPARENT_WHITE_TEXT,
            modifier = Modifier
        )
        

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
    val bgColor = when (headerStyle) {
        HeaderStyle.TRANSPARENT_WHITE_TEXT -> Color.Transparent
        HeaderStyle.WHITE_BLACK_TEXT -> Color.White
    }
    val textColor = when (headerStyle) {
        HeaderStyle.TRANSPARENT_WHITE_TEXT -> Color.White
        HeaderStyle.WHITE_BLACK_TEXT -> Color.Black
    }
    val underlineColor = if (selectedTab == "Explore") Color(0xFF0066CC) else Color.White

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
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
                textColor = textColor,
                underlineColor = underlineColor
            )
            HeaderTab(
                text = "Following",
                isSelected = selectedTab == "Following",
                onClick = { onTabChange("Following") },
                textColor = textColor,
                underlineColor = underlineColor
            )
            HeaderTab(
                text = "For you",
                isSelected = selectedTab == "For you",
                onClick = { onTabChange("For you") },
                textColor = textColor,
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
                tint = if (selectedTab == "Explore") Color(0xFF0066CC) else textColor,
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
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (isSelected) textColor else textColor.copy(alpha = 0.6f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(3.dp)
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
    navController: NavHostController,
    onClickCommentButton: () -> Unit,
    viewModel: ReelsScreenViewModel,
    onClickCartButton: () -> Unit,
    onClickMoreButton: () -> Unit,
    reelsList: List<Reels>,
    isLoggedIn: Boolean,
    setShowLoginPrompt: (Boolean) -> Unit,
    initialPage: Int = 0
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage
    )
    val currentPage = pagerState.currentPage

    var showHeart by remember { mutableStateOf(false) }
    var heartPosition by remember { mutableStateOf(Offset.Zero) }

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
            .padding(bottom = 44.dp),
        count = reelsList.size
    ) { page ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            showHeart = true
                            heartPosition = offset
                            if (!isLoggedIn) {
                                setShowLoginPrompt(true)
                            } else {
                                viewModel.forceLoveReels(reelsList[page].id)
                            }
                        }
                    )
                }
        ) {

            val reel = reelsList[page]
            if (reel.video != null && !reel.isError) {
                VideoPlayer(
                    uri = reel.video,
                    isPlaying = (currentPage == page),
                    onPlaybackStarted = { }
                )
            } else if (reel.images != null && reel.images.isNotEmpty() ) {
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
                            Image(
                                painter = rememberAsyncImagePainter(reel.images[imgIndex]),
                                contentDescription = "Reel Image $imgIndex",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
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
                                            if (imagesPagerState.currentPage == idx) Color.White else Color.White.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            } else {
                Image(
                    painter = painterResource(id = reel.fallbackImageRes),
                    contentDescription = "Fallback Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            HeartAnimation(
                isVisible = showHeart,
                position = heartPosition,
                iconPainter = painterResource(id = R.drawable.ic_heart_checked),
                onAnimationEnd = {
                    showHeart = false
                },
                iconSize = 70.dp
            )


            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f)
                            )
                        )
                    )
            )

            ReelContent(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.BottomStart),
                navController = navController,
                reel = reelsList[page],
                viewModel = viewModel,
                onClickCommentButton = onClickCommentButton,
                onClickMoreButton = onClickMoreButton,
                onClickCartButton = onClickCartButton,
                setShowLoginPrompt = setShowLoginPrompt,
                isLoggedIn = isLoggedIn
            )
        }
    }
}


@Composable
fun ReelContent(
    modifier: Modifier,
    navController: NavHostController,
    reel: Reels,
    viewModel: ReelsScreenViewModel,
    onClickCommentButton: () -> Unit,
    onClickCartButton: () -> Unit,
    onClickMoreButton: () -> Unit,
    setShowLoginPrompt: (Boolean) -> Unit,
    isLoggedIn: Boolean,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            UserInfo(reel = reel, navController = navController, isLoggedIn = isLoggedIn, setShowLoginPrompt = setShowLoginPrompt)
            Spacer(modifier = Modifier.height(8.dp))
            ReelDescription(description = reel.contentDescription)
            Spacer(modifier = Modifier.height(4.dp))
            ReelHashtags(hashtags = listOf("satisfying", "roadmarking"))
            Spacer(modifier = Modifier.height(12.dp))
            OfferCard(
                productName = "Hanger Shirt",
                productType = "Shirt",
                productPrice = "100.00 \$",
                productImage = R.drawable.img4,
                onViewClick = { /* action */ }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        InteractionButtons(
            navController = navController,
            reel = reel,
            onClickLoveButton = {
                if (!isLoggedIn) {
                    setShowLoginPrompt(true)
                } else {
                    viewModel.onClackLoveReelsButton(reel.id)
                }
            },
            onClickCommentButton = onClickCommentButton,
            onClickCartButton = onClickCartButton,
            onClickMoreButton = onClickMoreButton
        )
    }
}


// ORIGINAL GLIDE ANNOTATION (COMMENTED OUT):
// @OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun UserInfo(
    reel: Reels,
    navController: NavHostController,
    isLoggedIn: Boolean,
    setShowLoginPrompt: (Boolean) -> Unit
) {
    // Get current user ID to check if they own this reel
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid
    val isOwner = currentUserId == reel.userId
    val isActuallyLoggedIn = currentUserId != null // Use Firebase Auth directly
    
    // Debug logging for follow button logic
    Log.d("UserInfo", "🔍 Follow button logic - currentUserId: '$currentUserId', reel.userId: '${reel.userId}', isOwner: $isOwner")
    Log.d("UserInfo", "🔍 UserId comparison details - currentUserId length: ${currentUserId?.length}, reel.userId length: ${reel.userId.length}")
    Log.d("UserInfo", "🔍 UserId comparison details - currentUserId blank: ${currentUserId.isNullOrBlank()}, reel.userId blank: ${reel.userId.isBlank()}")
    Log.d("UserInfo", "🔍 UserId comparison details - currentUserId == reel.userId: ${currentUserId == reel.userId}")
    Log.d("UserInfo", "🔍 UserId comparison details - currentUserId equals reel.userId: ${currentUserId?.equals(reel.userId)}")
    Log.d("UserInfo", "🔍 UserId comparison details - currentUserId contentEquals reel.userId: ${currentUserId?.contentEquals(reel.userId)}")
    
    // Get FollowingViewModel for actual follow/unfollow functionality
    val followingViewModel: FollowingViewModel = koinViewModel()
    
    // Get current following status for this user
    val uiState by followingViewModel.uiState.collectAsState()
    
    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()
    
    // Check if current user is following the reel owner
    val isFollowing = remember(reel.userId, uiState.following) { 
        uiState.following.any { it.id == reel.userId }
    }
    
    // Load current user's following data when component is created
    LaunchedEffect(currentUserId) {
        currentUserId?.let { userId ->
            // Get the current user's actual username from Firebase Auth
            val currentUser = auth.currentUser
            val username = currentUser?.displayName ?: currentUser?.email?.split("@")?.firstOrNull() ?: "user"
            followingViewModel.loadUserData(userId, username)
        }
    }
    
    // Monitor following state changes
    LaunchedEffect(uiState.following, isFollowing) {
        // State updated, no need to log everything
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = reel.userName,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable {
                navController.navigate(Screens.ReelsScreen.UserProfileScreen.route)
            }
        )
        
        // Debug logging for username
        Log.d("UserInfo", "🔍 Displaying username: '${reel.userName}' for reel ID: ${reel.id}")

        // Only show follow button if user is NOT the owner
        Log.d("UserInfo", "🔍 Follow button visibility check - isOwner: $isOwner, showing button: ${!isOwner}")
        if (!isOwner) {
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(8.dp),
                        clip = false
                    )
                    .background(
                        brush = if (!isFollowing)
                            Brush.horizontalGradient(listOf(Color(0xFFF05F57), Color(0xFF360940)))
                        else
                            Brush.horizontalGradient(listOf(Color(0xFFf8a714), Color(0xFFed380a))),

                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        if (!isActuallyLoggedIn) {
                            setShowLoginPrompt(true)
                        } else {
                            // Call actual follow/unfollow functionality
                            currentUserId?.let { userId ->
                                followingViewModel.toggleFollow(userId, reel.userId)
                                
                                // Refresh following data immediately after the operation
                                coroutineScope.launch {
                                    delay(500) // Small delay to allow Firebase operation to complete
                                    val currentUser = auth.currentUser
                                    val username = currentUser?.displayName ?: currentUser?.email?.split("@")?.firstOrNull() ?: "user"
                                    followingViewModel.loadUserData(userId, username)
                                }
                            }
                        }
                    }
                    .height(26.dp)
                    .padding(horizontal = if (!isFollowing) 12.dp else 16.dp)
                ,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (!isFollowing) "Follow +" else "Following",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}



@Composable
fun OfferCard(
    productName: String = "Hanger Shirt",
    productType: String = "Shirt",
    productPrice: String = "100.00 \$",
    productImage: Int = R.drawable.img4,
    onViewClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
                            .offset(x = (-12f).dp)
            .background(Color(0xCC222222), shape = RoundedCornerShape(16.dp))
            .padding(8.dp)
        ,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = productImage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))

        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = productName,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = productType,
                    color = Color.White,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(26.dp))
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(8.dp),
                            clip = false
                        )
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFf8a714), Color(0xFFed380a))
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onViewClick() }
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "View",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }

            }
            Text(
                text = productPrice,
                color = Color(0xFFFFEB3B),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(y = (4f).dp)
            )

        }
    }
}



@Composable
fun ReelDescription(description: String) {
    Text(
        text = description,
        color = Color.White,
        fontSize = 14.sp,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 20.sp
    )
}

@Composable
fun ReelHashtags(hashtags: List<String>) {
    Text(
        text = hashtags.joinToString(" ") { "#$it" },
        color = Color(0xFFFF6F00),
        fontSize = 14.sp
    )
}

// ORIGINAL GLIDE ANNOTATION (COMMENTED OUT):
// @OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun InteractionButtons(
    navController: NavHostController,
    reel: Reels,
    onClickLoveButton: () -> Unit,
    onClickCommentButton: () -> Unit,
    onClickCartButton: () -> Unit,
    onClickMoreButton: () -> Unit
) {
    var isCarted by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp) // مساحة أكبر لتضمين التوثيق
        ) {
            // ORIGINAL GLIDE CODE (COMMENTED OUT):
            // GlideImage(
            //     model = reel.userImage,
            //     contentScale = ContentScale.Crop,
            //     modifier = Modifier
            //         .size(40.dp)
            //         .align(Alignment.TopCenter)
            //         .clip(CircleShape)
            //         .clickable {
            //             navController.navigate(Screens.ReelsScreen.UserProfileScreen.route)
            //         },
            //     contentDescription = "User Avatar"
            // )
            
            // NEW COIL CODE:
            AsyncImage(
                model = reel.userImage,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.TopCenter)
                    .clip(CircleShape)
                    .clickable {
                        navController.navigate(Screens.ReelsScreen.UserProfileScreen.route)
                    },
                contentDescription = "User Avatar"
            )

            Image(
                painter = painterResource(id = R.drawable.verified_badge),
                contentDescription = "Verified Badge",
                modifier = Modifier
                    .size(18.dp)
                    .offset(y = (2f).dp)
                    .align(Alignment.BottomCenter)
            )
        }
        InteractionButton(
            painter = painterResource(
                id = if (reel.love.isLoved) R.drawable.ic_love_checked else R.drawable.ic_love
            ),
            count = (reel.love.number + 10).toString(),
            tint = if (reel.love.isLoved) Color.Red else Color.White,
            onClick = onClickLoveButton
        )
        InteractionButton(
            painter = painterResource(id = R.drawable.ic_comment),
            count = reel.numberOfComments.toString(),
            onClick = onClickCommentButton
        )
        InteractionButton(
            painter = painterResource(id = R.drawable.ic_cart),
            count = reel.numberOfCart.toString(),
            tint = if (isCarted) Color(0xFFFFC107) else Color.White, // لون أصفر ذهبي عند التفعيل
            onClick = { isCarted = !isCarted }
        )
        InteractionButton(
            painter = painterResource(id = R.drawable.ic_share),
            count = "Share",
            onClick = onClickMoreButton
        )
        InteractionButton(
            painter = painterResource(id = R.drawable.ic_music),
            count = "",
            iconSize = 32.dp,
            onClick = {navController.navigate(Screens.ReelsScreen.SoundPageScreen.route) }
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun InteractionButton(
    painter: Painter,
    count: String,
    tint: Color = Color.White,
    iconSize: Dp = 28.dp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.noRippleClickable { onClick() }
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

// Modern Bottom Sheet Content
@Composable
fun ModernBottomSheetContent(
    state: List<Reels>,
    isSelectedComments: Boolean,
    isSelectedRatings: Boolean,
    onCommentTabClick: () -> Unit,
    onRatingTabClick: () -> Unit,
    onCloseClick: () -> Unit,
    viewModel: ReelsScreenViewModel,
    modifier: Modifier = Modifier
) {
    val commentsList = state.firstOrNull()?.comments ?: emptyList()
    val ratesList = state.firstOrNull()?.ratings ?: emptyList()
    val newComment = state.firstOrNull()?.newComment?.comment ?: ""
    val onCommentChange: (String) -> Unit = { viewModel.onWriteNewComment(it) }
    val onClickSend: () -> Unit = {
        viewModel.onClickAddComment(state.firstOrNull()?.id ?: "", newComment)
    }

    val minHeightDp = 300.dp  // أقل ارتفاع (400dp)
    val maxHeightFraction = 0.92f   // أقصى نسبة للشاشة

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val density = LocalDensity.current

    // احسب نسبة الارتفاع الأدنى (400dp / ارتفاع الشاشة)
    val minHeightFraction = with(density) {
        minHeightDp.toPx() / screenHeight.toPx()
    }

    var sheetHeightFraction by remember { mutableStateOf(0.5f) }
    var lastDragPosition by remember { mutableStateOf(0f) }

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
    )   {
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

        // Header with tabs and close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ModernTabButton(
                    text = "Comments",
                    icon = painterResource(id = R.drawable.comment),
                    isSelected = isSelectedComments,
                    iconSize = 24,
                    onClick = onCommentTabClick
                )

                ModernTabButton(
                    text = "Rates",
                    icon = painterResource(id = R.drawable.rating),
                    isSelected = isSelectedRatings,
                    onClick = onRatingTabClick
                )
            }

            IconButton(onClick = onCloseClick,
                modifier = Modifier.offset(x = 8.dp, y = (-8f).dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Black,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // المحتوى المتغير حسب التبويب المختار
        if (isSelectedComments) {
            CommentsSection(
                comments = commentsList,
                newComment = newComment,
                onCommentChange = onCommentChange,
                onClickSend = onClickSend,
                modifier = Modifier.weight(1f)
            )
        } else {
            RatingsSection(
                ratings = ratesList,
                newComment = newComment,
                onCommentChange = onCommentChange,
                onClickSend = onClickSend,
                modifier = Modifier.weight(1f)
            )
        }
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
                        text = "4.9",
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
                    text = "(32 Rates)",
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
            onValueChange = onValueChange,
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // ORIGINAL GLIDE CODE (COMMENTED OUT):
        // GlideImage(
        //     model = R.drawable.profile,
        //     contentDescription = "User Avatar",
        //     contentScale = ContentScale.Crop,
        //     modifier = Modifier
        //         .size(40.dp)
        //         .clip(CircleShape)
        // )
        
        // NEW COIL CODE:
        AsyncImage(
            model = R.drawable.profile,
            contentDescription = "User Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = comment.userName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Text(
                text = comment.comment,
                fontSize = 14.sp,
                color = Color(0xFF333333),
                lineHeight = 18.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = comment.time,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Reply",
                    fontSize = 12.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier.clickable { /* Handle reply */ }
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isLoved) R.drawable.ic_love_checked else R.drawable.ic_love_un_checked
                    ),
                    contentDescription = "Like",
                    tint = if (isLoved) Color.Red else Color.Gray,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ){
                            isLoved = !isLoved
                            likesCount += if (isLoved) 1 else -1
                        }
                )
                Icon(
                    painter = painterResource(id = R.drawable.dis_like),
                    contentDescription = "Dislike",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable {
                            dislikesCount++
                        }
                )
            }

            // الصف السفلي: الأرقام
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = likesCount.toString(),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = dislikesCount.toString(),
                    fontSize = 12.sp,
                    color = Color.Gray
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // صورة المستخدم
        
        // ORIGINAL GLIDE CODE (COMMENTED OUT):
        // GlideImage(
        //     model = R.drawable.profile,
        //     contentDescription = "User Avatar",
        //     contentScale = ContentScale.Crop,
        //     modifier = Modifier
        //         .size(40.dp)
        //         .clip(CircleShape)
        // )
        
        // NEW COIL CODE:
        AsyncImage(
            model = R.drawable.profile,
            contentDescription = "User Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )

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
                    id = if (isFavorite) R.drawable.ic_love_checked else R.drawable.ic_love_un_checked
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
fun AddToCartBottomSheet(
    onClose: () -> Unit,
    productName: String,
    productPrice: String,
    productDescription: String = "Upgrade Your Wardrobe With This Premium Item — Combining Comfort, Style, And Durability.",
    rating: Double = 4.8,
    onAddToCart: (String, String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedColor by remember { mutableStateOf("Orange") }
    var selectedSize by remember { mutableStateOf("S") }
    var quantity by remember { mutableStateOf(0) }

    Column(
        modifier = modifier // هذا يجعل كل align أو خصائص تأتي من الخارج
            .then(
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .heightIn(min = 390.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .imePadding()
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = productName,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

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
                Text("$rating", color = Color(0xFFFFC107), fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = productDescription,
            color = Color.Gray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text("Select Size :", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("XS", "S", "L", "M", "XL").forEach { size ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selectedSize == size) Color(0xFF4CAF50) else Color(0xFFBDBDBD)
                        )
                        .clickable { selectedSize = size }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(size, color = Color.White, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Select Color :", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            listOf("Orange", "Blue", "Yellow", "Green", "Purple").forEach { color ->
                val isSelected = selectedColor == color
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 36.dp else 28.dp) // حجم أكبر للمختار
                        .shadow(
                            elevation = if (isSelected) 4.dp else 0.dp, // شادو واضح للمختار فقط
                            shape = CircleShape,
                            clip = false
                        )
                        .background(
                            color = when (color) {
                                "Orange" -> Color(0xFFFF9800)
                                "Blue" -> Color(0xFF1565C0)
                                "Yellow" -> Color(0xFFFFEB3B)
                                "Green" -> Color(0xFF37474F)
                                "Purple" -> Color(0xFF9C27B0)
                                else -> Color.Gray
                            },
                            shape = CircleShape
                        )
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                        .clickable { selectedColor = color }
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = productPrice,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6F00)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(48.dp)
                    .offset(y = (-10f).dp)
                    .border(1.dp, Color(0xFF176DBA), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp)
            ) {
                IconButton(onClick = { if (quantity > 0) quantity-- }) {
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
        Spacer(modifier = Modifier.height(4.dp))
    }
}



@Composable
fun FollowingReelsContent(
    navController: NavHostController,
    followingViewModel: FollowingViewModel,
    reelsViewModel: ReelsScreenViewModel,
    onShowSheet: (SheetType) -> Unit
) {
    val followingState by followingViewModel.uiState.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    Log.d("FollowingReelsContent", "🚀 COMPONENT CREATED/RENDERED - currentUserId: $currentUserId, timestamp: ${System.currentTimeMillis()}")

    // Load following data when needed (allows retries and updates)
    LaunchedEffect(currentUserId) {
        if (currentUserId != null && followingState.following.isEmpty() && !followingState.isLoading) {
            Log.d("FollowingReelsContent", "🔄 Loading following data for user: $currentUserId")
            followingViewModel.resetLoadAttempts() // Reset retry counter
            followingViewModel.loadUserData(currentUserId, "current_user")
        }
    }

    // Debug logging for state changes - only log when state actually changes
    LaunchedEffect(followingState.isLoading, followingState.following.size) {
        Log.d("FollowingReelsContent", "🔍 State changed - isLoading: ${followingState.isLoading}, followingCount: ${followingState.following.size}")
    }

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
                onShowSheet = onShowSheet
            )
        }
    }
}

@Composable
fun FollowingReelsList(
    navController: NavHostController,
    followingUserIds: List<String>,
    reelsViewModel: ReelsScreenViewModel,
    onShowSheet: (SheetType) -> Unit
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
            onClickCommentButton = { onShowSheet(SheetType.Comments) },
            viewModel = reelsViewModel,
            onClickCartButton = { onShowSheet(SheetType.AddToCart) },
            onClickMoreButton = { /* TODO: Handle more options */ },
            reelsList = followingReels,
            isLoggedIn = true, // Since we're in the following tab, user is logged in
            setShowLoginPrompt = { /* Not needed in following tab */ },
            initialPage = 0
        )
    }
}




