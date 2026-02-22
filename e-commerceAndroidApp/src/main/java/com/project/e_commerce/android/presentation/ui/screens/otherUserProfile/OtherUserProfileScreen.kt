package com.project.e_commerce.android.presentation.ui.screens.otherUserProfile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.project.e_commerce.android.R
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.viewModel.otherUserProfile.OtherUserProfileViewModel
import org.koin.androidx.compose.koinViewModel
import android.util.Log
import com.project.e_commerce.android.presentation.utils.VideoThumbnailUtils
import com.project.e_commerce.android.presentation.ui.composable.composableScreen.public.VideoThumbnail
import android.net.Uri
import androidx.core.net.toUri

@Composable
fun OtherUserProfileScreen(
    navController: NavHostController,
    userId: String
) {
    val viewModel: OtherUserProfileViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val currentUserProvider: com.project.e_commerce.data.local.CurrentUserProvider = org.koin.compose.koinInject()
    var currentUserId by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        currentUserId = currentUserProvider.getCurrentUserId()
    }
    
    // Load user profile data
    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            viewModel.loadUserProfile(userId)
        }
    }
    
    // Check if current user is following this user
    val isFollowing = uiState.isFollowing
    
    // Show error if any
    if (uiState.error != null) {
        LaunchedEffect(uiState.error) {
            // Clear error after showing
            viewModel.clearError()
        }
    }
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(
        Pair(R.drawable.ic_reels, R.drawable.ic_reels),
        Pair(R.drawable.ic_products_filled, R.drawable.ic_products)
    )
    
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFFF6F00))
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // Top Bar with Back Button and Follow/Unfollow Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button (Left)
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = Color(0xFFFF6F00),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    // Follow/Unfollow Button (Right)
                    if (currentUserId != null && currentUserId != userId) {
                        Button(
                            onClick = { 
                                if (isFollowing) {
                                    viewModel.unfollowUser(userId)
                                } else {
                                    viewModel.followUser(userId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (isFollowing) Color.Gray else Color(0xFFFF6F00)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = if (isFollowing) "Following" else "Follow",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
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
                            // Navigate to following list
                            navController.navigate(Screens.FollowListScreen.createRoute(
                                username = uiState.username,
                                startTab = 1,
                                showFriendsTab = false
                            ))
                        }
                        
                        // Profile Image
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF0F0F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.profileImageUrl != null) {
                                AsyncImage(
                                    model = uiState.profileImageUrl,
                                    contentDescription = "Profile Picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    error = painterResource(id = R.drawable.profile)
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.profile),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.size(80.dp)
                                )
                            }
                        }
                        
                        ProfileStat(
                            number = uiState.followersCount.toString(),
                            label = "Followers"
                        ) {
                            // Navigate to followers list
                            navController.navigate(Screens.FollowListScreen.createRoute(
                                username = uiState.username,
                                startTab = 0,
                                showFriendsTab = false
                            ))
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
                    // Verification badge temporarily hidden - keeping code for future use
                    /*
                    Image(
                        painter = painterResource(id = R.drawable.verified_badge),
                        contentDescription = "Verified",
                        modifier = Modifier.size(22.dp)
                    )
                    */
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
                    number = uiState.postsCount.toString(),
                    label = "Posts"
                )
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item {
                // Share Button (only)
                Button(
                    onClick = { /* Share */ },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFf2f2f2)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 60.dp)
                        .height(42.dp)
                ) {
                    Text("Share Profile", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
            }
            
            item { Spacer(modifier = Modifier.height(12.dp)) }
            
            // Bio/Description
            if (uiState.bio.isNotEmpty()) {
                item {
                    Text(
                        text = uiState.bio,
                        fontSize = 16.sp,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            item { Spacer(modifier = Modifier.height(20.dp)) }
            
            item {
                // Tabs Row (only Reels and Products)
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
                Log.d("OtherUserProfile", "🎯 Selected Tab: $selectedTabIndex")
                Log.d("OtherUserProfile", "🎬 User Reels count: ${uiState.userReels.size}")
                Log.d("OtherUserProfile", "📦 User Products count: ${uiState.userProducts.size}")
                
                when (selectedTabIndex) {
                    0 -> {
                        // Reels Tab
                        Log.d("OtherUserProfile", "🎬 Showing Reels Tab with ${uiState.userReels.size} reels")
                        UserReelsGrid(uiState.userReels, navController)
                    }
                    1 -> {
                        // Products Tab
                        Log.d("OtherUserProfile", "📦 Showing Products Tab with ${uiState.userProducts.size} products")
                        UserProductsGrid(uiState.userProducts, navController)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStat(
    number: String, 
    label: String, 
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = number, 
            color = Color(0xFF0D3D67), 
            fontWeight = FontWeight.Bold, 
            fontSize = 18.sp
        )
        Text(
            text = label, 
            fontSize = 14.sp, 
            color = Color.Gray
        )
    }
}

@Composable
fun UserReelsGrid(reels: List<com.project.e_commerce.android.domain.model.UserPost>, navController: NavHostController) {
    if (reels.isEmpty()) {
        EmptyStateGrid("No reels yet", "This user hasn't posted any reels")
        return
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(650.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(reels, key = { it.id }) { reel ->
            Box(
                modifier = Modifier
                    .background(Color(0xFFF8F8F8))
                    .height(130.dp)
                    .clickable {
                        // Navigate to reels screen with specific reel ID
                        navController.navigate("${Screens.ReelsScreen.route}/${reel.id}")
                    }
            ) {
                // Use smart thumbnail
                val bestThumbnail = VideoThumbnailUtils.getBestThumbnail(
                    images = reel.images,
                    videoUrl = reel.mediaUrl,
                    fallbackUrl = reel.thumbnailUrl
                )
                
                Log.d("OtherUserProfile", "🎬 Processing reel ${reel.id}:")
                Log.d("OtherUserProfile", "  - MediaUrl: ${reel.mediaUrl}")
                Log.d("OtherUserProfile", "  - Images: ${reel.images}")
                Log.d("OtherUserProfile", "  - ThumbnailUrl: ${reel.thumbnailUrl}")
                Log.d("OtherUserProfile", "  - BestThumbnail: $bestThumbnail")
                
                if (!bestThumbnail.isNullOrBlank()) {
                    Log.d("OtherUserProfile", "✅ Using thumbnail: $bestThumbnail")
                    AsyncImage(
                        model = bestThumbnail,
                        contentDescription = "Reel thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(id = R.drawable.img_2),
                        onSuccess = { 
                            Log.d("OtherUserProfile", "✅ Thumbnail loaded successfully: $bestThumbnail")
                        },
                        onError = { error ->
                            Log.e("OtherUserProfile", "❌ Thumbnail failed to load: $bestThumbnail")
                            Log.e("OtherUserProfile", "❌ Error: ${error.result.throwable?.message}")
                        }
                    )
                } else if (!reel.mediaUrl.isNullOrBlank()) {
                    Log.d("OtherUserProfile", "🎬 Using VideoThumbnail component for: ${reel.mediaUrl}")
                    VideoThumbnail(
                        videoUri = reel.mediaUrl.toUri(),
                        fallbackImageRes = R.drawable.img_2,
                        modifier = Modifier.fillMaxSize(),
                        showPlayIcon = false
                    )
                } else {
                    Log.d("OtherUserProfile", "⚠️ No thumbnail available, showing fallback")
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
    if (products.isEmpty()) {
        EmptyStateGrid("No products yet", "This user hasn't added any products")
        return
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(650.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(products, key = { it.id }) { product ->
            Box(
                modifier = Modifier
                    .background(Color(0xFFF8F8F8))
                    .height(130.dp)
                    .clickable {
                        // Navigate to product details
                        navController.navigate(Screens.ProductScreen.DetailsScreen.route + "/${product.id}")
                    }
            ) {
                if (product.images.isNotEmpty()) {
                    AsyncImage(
                        model = product.images.first(),
                        contentDescription = "Product image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(id = R.drawable.img_2)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE0E0E0))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_products),
                            contentDescription = "No image",
                            tint = Color.Gray,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(48.dp)
                        )
                    }
                }
                
                // Product info overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(4.dp)
                ) {
                    Text(
                        text = product.name,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
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
            .height(400.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_products),
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = Color(0xFF0D3D67),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
