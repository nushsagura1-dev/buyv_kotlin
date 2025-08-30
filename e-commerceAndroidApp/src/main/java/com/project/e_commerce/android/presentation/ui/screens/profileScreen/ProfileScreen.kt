package com.project.e_commerce.android.presentation.ui.screens.profileScreen

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import coil3.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.project.e_commerce.android.R
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.ui.screens.RequireLoginPrompt
import com.project.e_commerce.android.presentation.ui.composable.composableScreen.public.VideoThumbnail
import com.project.e_commerce.android.presentation.utils.VideoThumbnailUtils
import com.project.e_commerce.android.presentation.viewModel.CartViewModel
import com.project.e_commerce.android.presentation.viewModel.ProductViewModel
import com.project.e_commerce.android.presentation.viewModel.profileViewModel.ProfileViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(navController: NavHostController) {
    Log.d("CrashDebug", "ProfileScreen: entered composable")
    val profileViewModel: ProfileViewModel = koinViewModel()
    val uiState by profileViewModel.uiState.collectAsState()
    val userReels by profileViewModel.userReels.collectAsState()
    val userProducts by profileViewModel.userProducts.collectAsState()
    val userLikedContent by profileViewModel.userLikedContent.collectAsState()
    val userBookmarkedContent by profileViewModel.userBookmarkedContent.collectAsState()
    val cartViewModel: CartViewModel = koinViewModel()
    val cartState by cartViewModel.state.collectAsState()

    // Logout state
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Refresh following data when screen becomes active
    LaunchedEffect(Unit) {
        profileViewModel.refreshFollowingData()
    }
    
    // Refresh profile data when screen becomes active (e.g., returning from EditProfileScreen)
    LaunchedEffect(navController.currentBackStackEntry) {
        profileViewModel.refreshProfile()
    }

    // Refresh profile when returning from other screens
    DisposableEffect(navController.currentBackStackEntry) {
        onDispose {
            // This will trigger when we navigate away and then back
            profileViewModel.refreshProfile()
        }
    }

    // Show error dialog if there's an error
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { profileViewModel.clearError() },
            title = { Text("Error") },
            text = { Text(uiState.error!!) },
            confirmButton = {
                TextButton(onClick = { profileViewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    // Show logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        // Perform logout
                        val auth = FirebaseAuth.getInstance()
                        auth.signOut()
                        // Navigate to login screen
                        navController.navigate(Screens.LoginScreen.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("Yes", color = Color(0xFFFF6F00))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("No", color = Color.Gray)
                }
            }
        )
    }

    // Firebase Auth state
    val auth = remember { FirebaseAuth.getInstance() }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

    // Listen to auth state changes
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            isLoggedIn = fa.currentUser != null
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // Show login prompt if not authenticated
    if (!isLoggedIn) {
        RequireLoginPrompt(
            onLogin = { navController.navigate(Screens.LoginScreen.route) },
            onSignUp = { navController.navigate(Screens.LoginScreen.CreateAccountScreen.route) },
            onDismiss = { /* nothing */ },
            showCloseButton = false
        )
        return
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(
        Pair(R.drawable.ic_reels, R.drawable.ic_reels),
        Pair(R.drawable.ic_products_filled, R.drawable.ic_products),
        Pair(R.drawable.ic_save_filled, R.drawable.ic_save),
        Pair(R.drawable.ic_love_checked, R.drawable.ic_love_un_checked)
    )

    if (uiState.isLoading) {
        Log.d("CrashDebug", "ProfileScreen: loading")
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFFFF6F00),
                modifier = Modifier.size(48.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        item {
            // Top Bar with Logout and Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logout Icon (Left)
                IconButton(onClick = { showLogoutDialog = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_logout),
                        contentDescription = "Logout",
                        tint = Color(0xFFFF6F00),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Right side icons
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.navigate(Screens.ProfileScreen.NotificationScreen.route) }) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                painter = painterResource(id = R.drawable.notification_icon),
                                contentDescription = "Notifications",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color(0xFFFF3D00), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "1",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    IconButton(onClick = { navController.navigate(Screens.ProfileScreen.SettingsScreen.route) }) {
                        Icon(
                            modifier = Modifier.padding(7.dp),
                            painter = painterResource(id = R.drawable.ic_menu),
                            contentDescription = "Menu",
                            tint = Color(0xFFFF6F00)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            // Stats + Profile Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileStat(
                        number = uiState.followingCount.toString(),
                        label = "Following"
                    ) {
                        Log.d("PROFILE_DEBUG", "🔄 Navigating to Following tab")
                        try {
                            val route = Screens.FollowListScreen.createRoute(
                                username = uiState.username,
                                startTab = 1,
                                showFriendsTab = true
                            )
                            Log.d("PROFILE_DEBUG", "🔄 Route created: $route")
                            navController.navigate(route)
                            Log.d("PROFILE_DEBUG", "✅ Navigation successful")
                        } catch (e: Exception) {
                            Log.e("PROFILE_DEBUG", "❌ Navigation failed: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                    
                    // Profile Image - Use real data if available, fallback to default
                    Log.d("PROFILE_DEBUG", "🖼️ ===== PROFILE IMAGE DISPLAY DEBUG =====")
                    Log.d(
                        "PROFILE_DEBUG",
                        "🖼️ uiState.profileImageUrl: '${uiState.profileImageUrl}'"
                    )
                    Log.d(
                        "PROFILE_DEBUG",
                        "🖼️ profileImageUrl is null: ${uiState.profileImageUrl == null}"
                    )
                    Log.d(
                        "PROFILE_DEBUG",
                        "🖼️ profileImageUrl is blank: ${uiState.profileImageUrl.isNullOrBlank()}"
                    )

                    val profileImageUrl = uiState.profileImageUrl
                    if (profileImageUrl != null && profileImageUrl.isNotBlank()) {
                        val isLocalUri = profileImageUrl.startsWith("content://")
                        val isHttpUrl = profileImageUrl.startsWith("http")

                        Log.d("PROFILE_DEBUG", "🖼️ Is local URI: $isLocalUri")
                        Log.d("PROFILE_DEBUG", "🖼️ Is HTTP URL: $isHttpUrl")

                        if (isLocalUri) {
                            Log.w(
                                "PROFILE_DEBUG",
                                "⚠️ WARNING: Profile image is local URI - may not work after app restart!"
                            )
                        }

                        // Normalize the Cloudinary URL
                        val normalizedUrl = if (profileImageUrl.startsWith("//")) {
                            "https:$profileImageUrl"
                        } else if (!profileImageUrl.startsWith("http")) {
                            "https://$profileImageUrl"
                        } else {
                            profileImageUrl
                        }

                        Log.d("PROFILE_DEBUG", "🖼️ Original URL: $profileImageUrl")
                        Log.d("PROFILE_DEBUG", "🖼️ Normalized URL: $normalizedUrl")
                        Log.d(
                            "PROFILE_DEBUG",
                            "🖼️ URL normalization needed: ${normalizedUrl != profileImageUrl}"
                        )

                        // Use AsyncImage instead of rememberAsyncImagePainter for better Cloudinary support
                        Log.d(
                            "PROFILE_DEBUG",
                            "🖼️ Loading profile image using AsyncImage: $normalizedUrl"
                        )

                        // First, let's test with a simple guaranteed working URL
                        val testUrl =
                            "https://www.google.com/images/branding/googlelogo/1x/googlelogo_light_color_272x92dp.png"
                        val finalUrl = if (normalizedUrl.contains("cloudinary")) {
                            Log.d(
                                "PROFILE_DEBUG",
                                "🧪 Using test URL instead of Cloudinary: $testUrl"
                            )
                            testUrl
                        } else {
                            normalizedUrl
                        }

                        AsyncImage(
                            model = finalUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            onSuccess = {
                                Log.e("PROFILE_DEBUG", "✅✅✅ SUCCESS! Image loaded: $finalUrl")
                            },
                            onError = { error ->
                                Log.e("PROFILE_DEBUG", "❌❌❌ ERROR! Image failed: $finalUrl")
                                Log.e(
                                    "PROFILE_DEBUG",
                                    "❌ Error: ${error.result.throwable?.message}"
                                )
                                Log.e("PROFILE_DEBUG", "❌ Full error: ${error.result.throwable}")
                                error.result.throwable?.printStackTrace()
                            },
                            onLoading = {
                                Log.e("PROFILE_DEBUG", "⏳⏳⏳ LOADING started: $finalUrl")
                            },
                            placeholder = painterResource(id = R.drawable.profile),
                            error = painterResource(id = R.drawable.profile)
                        )
                    } else {
                        Log.d(
                            "CrashDebug",
                            "ProfileScreen: loading default profile image (no URL available)"
                        )
                        Log.d("PROFILE_DEBUG", "🖼️ Using default profile image")
                        Image(
                            painter = painterResource(id = R.drawable.profile),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                        )
                    }
                    Log.d("PROFILE_DEBUG", "🖼️ ==========================================")
                    
                    ProfileStat(
                        number = uiState.followersCount.toString(),
                        label = "Followers"
                    ) {
                        Log.d("PROFILE_DEBUG", "🔄 Navigating to Followers tab")
                        try {
                            val route = Screens.FollowListScreen.createRoute(
                                username = uiState.username,
                                startTab = 0,
                                showFriendsTab = true
                            )
                            Log.d("PROFILE_DEBUG", "🔄 Route created: $route")
                            navController.navigate(route)
                            Log.d("PROFILE_DEBUG", "✅ Navigation successful")
                        } catch (e: Exception) {
                            Log.e("PROFILE_DEBUG", "❌ Navigation failed: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            // Name & Username
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = uiState.displayName.ifEmpty { "User" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D3D67)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Image(
                    painter = painterResource(id = R.drawable.verified_badge),
                    contentDescription = "Verified",
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "@${uiState.username.ifEmpty { "user" }}",
                fontSize = 13.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            ProfileStat(
                number = uiState.likesCount.toString(),
                label = "Likes"
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            // Edit / Share Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 60.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { navController.navigate(Screens.ProfileScreen.EditProfileScreen.route) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF176DBA)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                ) {
                    Text("Edit Profile", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { /* Share */ },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFf2f2f2)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                ) {
                    Text("Share Profile", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            Button(
                onClick = { navController.navigate(Screens.ProfileScreen.AddNewContentScreen.route) },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF6F00)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 60.dp)
                    .height(42.dp),
                elevation = ButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Text(
                    "+ Add New Post",
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Following Feed Button removed - no longer needed
        // The Following tab is now implemented directly in the main ReelsView

        item { Spacer(modifier = Modifier.height(20.dp)) }

        item {
            // Tabs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                tabs.forEachIndexed { index, (filledIcon, outlineIcon) ->
                    IconButton(
                        onClick = { selectedTabIndex = index },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (selectedTabIndex == index) filledIcon else outlineIcon
                            ),
                            contentDescription = null,
                            tint = if (selectedTabIndex == index) Color(0xFFFF6F00) else Color.Gray,
                            modifier = Modifier.size(if (index == 0) 30.dp else 20.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Content Grid based on selected tab
        item {
            Log.d("CrashDebug", "ProfileScreen: selectedTabIndex = $selectedTabIndex")
            when (selectedTabIndex) {
                0 -> Log.d(
                    "CrashDebug",
                    "ProfileScreen: Showing Reels with ${userReels.size} reels"
                )

                1 -> Log.d(
                    "CrashDebug",
                    "ProfileScreen: Showing Products with ${userProducts.size} products"
                )

                2 -> Log.d(
                    "CrashDebug",
                    "ProfileScreen: Showing Saved content - Bookmarked: ${userBookmarkedContent.size}, Cart: ${cartState.items.size}"
                )

                3 -> Log.d(
                    "CrashDebug",
                    "ProfileScreen: Showing Likes with ${userLikedContent.size} items"
                )
            }
            when (selectedTabIndex) {
                0 -> {
                    Log.d("CrashDebug", "ProfileScreen: Showing Reels with ${userReels.size} reels")
                    UserReelsGrid(userReels, navController)
                }
                1 -> {
                    Log.d(
                        "CrashDebug",
                        "ProfileScreen: Showing Products with ${userProducts.size} products"
                    )
                    UserProductsGrid(userProducts, navController)
                }
                2 -> {
                    Log.d(
                        "CrashDebug",
                        "ProfileScreen: Showing Saved content - Bookmarked: ${userBookmarkedContent.size}, Cart: ${cartState.items.size}"
                    )
                    // Show bookmarked posts if available, otherwise show cart items
                    if (userBookmarkedContent.isNotEmpty()) {
                        Log.d("CrashDebug", "ProfileScreen: Showing bookmarked posts")
                        UserBookmarkedGrid(userBookmarkedContent, navController)
                    } else {
                        Log.d("CrashDebug", "ProfileScreen: Showing cart items as fallback")
                        UserCartBookmarkGrid(cartState.items, navController)
                    }
                }
                3 -> {
                    Log.d(
                        "CrashDebug",
                        "ProfileScreen: Showing Likes with ${userLikedContent.size} items"
                    )
                    UserLikedGrid(userLikedContent, navController)
                }
            }
        }
        }
    }
}

@Composable
fun ProfileStat(number: String, label: String, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(number, color = Color(0xFF0D3D67), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun UserReelsGrid(reels: List<com.project.e_commerce.android.domain.model.UserPost>, navController: NavHostController) {
    Log.d("CrashDebug", "UserReelsGrid: entered composable with ${reels.size} reels")

    // Debug: Check for duplicate reels
    Log.d("PROFILE_DEBUG", "🎯 ===== REELS GRID DEBUG =====")
    Log.d("PROFILE_DEBUG", "🎯 Total reels received: ${reels.size}")
    reels.forEachIndexed { index, reel ->
        Log.d("PROFILE_DEBUG", "🎯 Reel $index: ID='${reel.id}', Title='${reel.title}'")
        Log.d("PROFILE_DEBUG", "🎯 Reel $index: UserId='${reel.userId}'")
        Log.d("PROFILE_DEBUG", "🎯 Reel $index: MediaUrl='${reel.mediaUrl}'")
    }

    // Check for duplicates by ID
    val uniqueIds = reels.map { it.id }.toSet()
    Log.d("PROFILE_DEBUG", "🎯 Unique reel IDs: ${uniqueIds.size}")
    if (uniqueIds.size != reels.size) {
        Log.w(
            "PROFILE_DEBUG",
            "⚠️ DUPLICATE REELS DETECTED! ${reels.size} reels but only ${uniqueIds.size} unique IDs"
        )
        val duplicates = reels.groupBy { it.id }.filter { it.value.size > 1 }
        duplicates.forEach { (id, duplicateReels) ->
            Log.w("PROFILE_DEBUG", "⚠️ Duplicate ID '$id' appears ${duplicateReels.size} times")
        }
    }

    // Check for duplicates by content
    val uniqueContent = reels.map { "${it.title}:${it.mediaUrl}" }.toSet()
    Log.d("PROFILE_DEBUG", "🎯 Unique reel content: ${uniqueContent.size}")
    if (uniqueContent.size != reels.size) {
        Log.w(
            "PROFILE_DEBUG",
            "⚠️ DUPLICATE CONTENT DETECTED! Same reel data appears multiple times"
        )
    }
    Log.d("PROFILE_DEBUG", "🎯 =============================")

    if (reels.isEmpty()) {
        Log.d("CrashDebug", "UserReelsGrid: no reels to display")
        EmptyStateGrid("No reels yet", "Start creating reels to see them here")
        return
    }

    Log.d("CrashDebug", "UserReelsGrid: displaying ${reels.size} reels")
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(650.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(reels) { reel ->
            Log.d("PROFILE_DEBUG", "🎯 Rendering reel: ID='${reel.id}', Title='${reel.title}'")
            Box(
                modifier = Modifier
                    .background(Color(0xFFF8F8F8))
                    .height(130.dp)
                    .clickable {
                        // Navigate to reels screen with specific reel ID
                        navController.navigate("${Screens.ReelsScreen.route}/${reel.id}")
                    }
            ) {
                // Use smart thumbnail: priority: 1. First image, 2. Video thumbnail, 3. Default
                val bestThumbnail = VideoThumbnailUtils.getBestThumbnail(
                    images = reel.images,
                    videoUrl = reel.mediaUrl,
                    fallbackUrl = reel.thumbnailUrl
                )

                Log.d("CrashDebug", "UserReelsGrid: loading thumbnail for reel with id ${reel.id}")
                // Debug logging - make it more visible
                Log.d("PROFILE_DEBUG", "🎯 ===== REEL THUMBNAIL DEBUG =====")
                Log.d("PROFILE_DEBUG", "🎬 Reel Title: ${reel.title}")
                Log.d("PROFILE_DEBUG", "🎬 Reel ID: ${reel.id}")
                Log.d("PROFILE_DEBUG", "🎬 Images List: ${reel.images}")
                Log.d("PROFILE_DEBUG", "🎬 Images Count: ${reel.images.size}")
                Log.d("PROFILE_DEBUG", "🎬 Video URL: ${reel.mediaUrl}")
                Log.d("PROFILE_DEBUG", "🎬 Thumbnail URL: ${reel.thumbnailUrl}")
                Log.d("PROFILE_DEBUG", "🎬 Best Thumbnail: $bestThumbnail")
                Log.d("PROFILE_DEBUG", "🎬 =================================")

                if (!reel.mediaUrl.isNullOrBlank()) {
                    Log.d(
                        "CrashDebug",
                        "UserReelsGrid: loading VideoThumbnail for reel with mediaUrl: ${reel.mediaUrl}"
                    )
                    VideoThumbnail(
                        videoUri = Uri.parse(reel.mediaUrl),
                        fallbackImageRes = R.drawable.img2,
                        modifier = Modifier.fillMaxSize(),
                        showPlayIcon = false // We'll add our own play icon overlay
                    )
                } else if (!bestThumbnail.isNullOrBlank()) {
                    Log.d(
                        "CrashDebug",
                        "UserReelsGrid: loading AsyncImage with thumbnail $bestThumbnail"
                    )
                    AsyncImage(
                        model = bestThumbnail,
                        contentDescription = "Reel thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(id = R.drawable.img2)
                    )
                } else {
                    Log.d(
                        "CrashDebug",
                        "UserReelsGrid: no thumbnail available for reel with id ${reel.id}"
                    )
                    // Show clean background instead of hardcoded image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE0E0E0))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play),
                            contentDescription = "No thumbnail",
                            tint = Color.Gray,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(48.dp)
                        )
                    }
                }
                
                // Play icon overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_play),
                        contentDescription = "Play reel",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp)
                    )
                }
                
                // Views count at bottom
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = reel.viewsCount.toString(), 
                        color = Color.White, 
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun UserProductsGrid(products: List<com.project.e_commerce.android.domain.model.UserProduct>, navController: NavHostController) {
    Log.d("CrashDebug", "UserProductsGrid: entered composable with ${products.size} products")
    if (products.isEmpty()) {
        Log.d("CrashDebug", "UserProductsGrid: no products to display")
        EmptyStateGrid("No products yet", "Start adding products to see them here")
        return
    }

    Log.d("CrashDebug", "UserProductsGrid: displaying ${products.size} products")
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(650.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(products) { product ->
            Column(
                modifier = Modifier
                    .background(Color(0xFFF8F8F8))
                    .padding(bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    Log.d(
                        "CrashDebug",
                        "UserProductsGrid: loading product image for product with id ${product.id}"
                    )
                    if (product.images.isNotEmpty()) {
                        AsyncImage(
                            model = product.images.first(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            error = painterResource(id = R.drawable.img2)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img2),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Text(
                    product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp, start = 8.dp, end = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (product.stockQuantity > 0) "${product.stockQuantity} left" else "Out of Stock",
                        color = if (product.stockQuantity > 0) Color(0xFF22C55E) else Color(0xFFEB1919),
                        fontSize = 12.sp
                    )
                    Text(
                        "$${product.price}",
                        color = Color(0xFFFF6F00),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun UserCartBookmarkGrid(
    cartItems: List<com.project.e_commerce.android.presentation.viewModel.CartItem>,
    navController: NavHostController
) {
    val productViewModel: ProductViewModel = org.koin.androidx.compose.koinViewModel()
    val allProducts = productViewModel.allProducts

    if (cartItems.isEmpty()) {
        EmptyStateGrid("No saved items yet", "Add items to your favorites and they will show here.")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(650.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(cartItems) { item ->
            Box(
                modifier = Modifier
                    .background(Color(0xFFF8F8F8))
                    .height(130.dp)
                    .clickable {
                        val found = allProducts.find { it.id == item.productId }
                        if (found != null) {
                            productViewModel.selectedProduct = found
                        }
                        navController.navigate("details_screen/${item.productId}")
                    }
            ) {
                // Check if this product has an associated reel with video content
                val associatedProduct = allProducts.find { it.id == item.productId }
                val hasVideoReel = associatedProduct?.reelVideoUrl?.isNotBlank() == true

                if (hasVideoReel && !associatedProduct?.reelVideoUrl.isNullOrBlank()) {
                    Log.d(
                        "CrashDebug",
                        "UserCartBookmarkGrid: Found video reel for product ${item.productId}: ${associatedProduct?.reelVideoUrl}"
                    )
                    VideoThumbnail(
                        videoUri = Uri.parse(associatedProduct!!.reelVideoUrl),
                        fallbackImageRes = R.drawable.img2,
                        modifier = Modifier.fillMaxSize(),
                        showPlayIcon = false // We'll add our own play icon overlay
                    )

                    // Add video play icon overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play),
                            contentDescription = "Play video",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(32.dp)
                        )
                    }
                } else {
                    // Regular product image
                    if (item.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            error = painterResource(id = R.drawable.img2)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img2),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserBookmarkedGrid(bookmarkedContent: List<com.project.e_commerce.android.domain.model.UserPost>, navController: NavHostController) {
    Log.d(
        "CrashDebug",
        "UserBookmarkedGrid: entered composable with ${bookmarkedContent.size} bookmarked posts"
    )
    
    if (bookmarkedContent.isEmpty()) {
        Log.d("CrashDebug", "UserBookmarkedGrid: no bookmarked content to display")
        EmptyStateGrid("No saved posts yet", "Save posts and products to see them here")
        return
    }

    Log.d("CrashDebug", "UserBookmarkedGrid: displaying ${bookmarkedContent.size} bookmarked posts")
    bookmarkedContent.forEach { post ->
        Log.d("CrashDebug", "UserBookmarkedGrid: bookmarked post with id ${post.id}")
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(650.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(bookmarkedContent) { post ->
            Box(
                modifier = Modifier
                    .background(Color(0xFFF8F8F8))
                    .height(130.dp)
                    .clickable {
                        // Navigate to reels screen with specific post ID
                        navController.navigate("${Screens.ReelsScreen.route}/${post.id}")
                    }
            ) {
                Log.d(
                    "CrashDebug",
                    "UserBookmarkedGrid: loading thumbnail for bookmarked post with id ${post.id}"
                )

                if (!post.mediaUrl.isNullOrBlank()) {
                    Log.d(
                        "CrashDebug",
                        "UserBookmarkedGrid: loading VideoThumbnail for post with mediaUrl: ${post.mediaUrl}"
                    )
                    VideoThumbnail(
                        videoUri = Uri.parse(post.mediaUrl),
                        fallbackImageRes = R.drawable.img2,
                        modifier = Modifier.fillMaxSize(),
                        showPlayIcon = false // We'll add our own play icon overlay
                    )
                } else if (post.images.isNotEmpty()) {
                    Log.d(
                        "CrashDebug",
                        "UserBookmarkedGrid: loading first image: ${post.images.first()}"
                    )
                    AsyncImage(
                        model = post.images.first(),
                        contentDescription = "Post image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(id = R.drawable.img2)
                    )
                } else if (!post.thumbnailUrl.isNullOrBlank()) {
                    Log.d(
                        "CrashDebug",
                        "UserBookmarkedGrid: loading thumbnailUrl: ${post.thumbnailUrl}"
                    )
                    AsyncImage(
                        model = post.thumbnailUrl,
                        contentDescription = "Post thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(id = R.drawable.img2)
                    )
                } else {
                    Log.d("CrashDebug", "UserBookmarkedGrid: using fallback image")
                    Image(
                        painter = painterResource(id = R.drawable.img2),
                        contentDescription = "Fallback image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Add video play icon overlay if it's a video
                if (!post.mediaUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play),
                            contentDescription = "Play video",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserLikedGrid(likedContent: List<com.project.e_commerce.android.domain.model.UserPost>, navController: NavHostController) {
    Log.d("CrashDebug", "UserLikedGrid: entered composable with ${likedContent.size} liked posts")
    
    if (likedContent.isEmpty()) {
        Log.d("CrashDebug", "UserLikedGrid: no liked content to display")
        EmptyStateGrid("No likes yet", "Like posts and products to see them here")
        return
    }

    Log.d("CrashDebug", "UserLikedGrid: displaying ${likedContent.size} liked posts")
    likedContent.forEach { post ->
        Log.d("CrashDebug", "UserLikedGrid: liked post with id ${post.id}")
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(650.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(likedContent) { post ->
            Box(
                modifier = Modifier
                    .background(Color(0xFFF8F8F8))
                    .height(130.dp)
                    .clickable {
                        // Navigate to reels screen with specific post ID
                        navController.navigate("${Screens.ReelsScreen.route}/${post.id}")
                    }
            ) {
                Log.d(
                    "CrashDebug",
                    "UserLikedGrid: loading thumbnail for liked post with id ${post.id}"
                )

                if (!post.mediaUrl.isNullOrBlank()) {
                    Log.d(
                        "CrashDebug",
                        "UserLikedGrid: loading VideoThumbnail for post with mediaUrl: ${post.mediaUrl}"
                    )
                    VideoThumbnail(
                        videoUri = Uri.parse(post.mediaUrl),
                        fallbackImageRes = R.drawable.img2,
                        modifier = Modifier.fillMaxSize(),
                        showPlayIcon = false // We'll add our own play icon overlay
                    )
                } else if (post.images.isNotEmpty()) {
                    Log.d(
                        "CrashDebug",
                        "UserLikedGrid: loading first image: ${post.images.first()}"
                    )
                    AsyncImage(
                        model = post.images.first(),
                        contentDescription = "Post image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(id = R.drawable.img2)
                    )
                } else if (!post.thumbnailUrl.isNullOrBlank()) {
                    Log.d("CrashDebug", "UserLikedGrid: loading thumbnailUrl: ${post.thumbnailUrl}")
                    AsyncImage(
                        model = post.thumbnailUrl,
                        contentDescription = "Post thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(id = R.drawable.img2)
                    )
                } else {
                    Log.d("CrashDebug", "UserLikedGrid: using fallback image")
                    Image(
                        painter = painterResource(id = R.drawable.img2),
                        contentDescription = "Fallback image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Add video play icon overlay if it's a video
                if (!post.mediaUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play),
                            contentDescription = "Play video",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateGrid(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(650.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_image),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}
