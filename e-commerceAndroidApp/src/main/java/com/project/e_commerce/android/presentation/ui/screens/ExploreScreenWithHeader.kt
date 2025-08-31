package com.project.e_commerce.android.presentation.ui.screens

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.ReelsTopHeader
import com.project.e_commerce.android.presentation.viewModel.ProductViewModel
import com.project.e_commerce.android.presentation.ui.composable.composableScreen.public.VideoThumbnail
import com.project.e_commerce.android.R
import org.koin.androidx.compose.koinViewModel
import android.util.Log
import com.project.e_commerce.android.presentation.utils.UserInfoCache
import com.project.e_commerce.android.presentation.utils.UserDisplayName
import com.project.e_commerce.android.presentation.utils.UserDisplayType



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreScreenWithHeader(
    navController: NavHostController,
    productViewModel: ProductViewModel = koinViewModel()
) {
    Log.d("EXPLORE_DEBUG", "🔍 EXPLORE SCREEN LOADED")
    
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    var selectedTab by remember { 
        mutableStateOf(savedStateHandle?.get("selectedTab") ?: "Explore") 
    }
    
    val headerHeight = 60.dp
    
    // Sync tab state with savedStateHandle when screen loads
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow<String>("selectedTab", "Explore")?.collect { tab ->
            if (tab != selectedTab) {
                Log.d("ExploreScreen", "🔄 Tab state synced from savedStateHandle: $tab")
                selectedTab = tab
            }
        }
    }
    
    // Get real product data from ProductViewModel
    val exploreItems = productViewModel.productReels.map { reel ->
        Log.d("EXPLORE_DEBUG", "🎬 Processing reel: ${reel.id}")
        Log.d("EXPLORE_DEBUG", "  - UserId: '${reel.userId}'")
        Log.d("EXPLORE_DEBUG", "  - UserName: '${reel.userName}' (fallback)")
        Log.d("EXPLORE_DEBUG", "  - ProductName: '${reel.productName}'")

        ExploreItem(
            id = reel.id,
            userId = reel.userId, // Add userId for proper name resolution
            productName = reel.productName,
            productPrice = reel.productPrice,
            productImage = reel.productImage,
            isVideo = reel.video != null,
            videoUri = reel.video,
            imageUris = reel.images,
            userName = reel.userName, // Keep as fallback
            loveCount = reel.love.number,
            isLoved = reel.love.isLoved,
            commentCount = reel.numberOfComments,
            rating = reel.rating
        )
    }
    
    Log.d("EXPLORE_DEBUG", "📊 Final Explore Items Count: ${exploreItems.size}")

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = headerHeight, bottom = 40.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (productViewModel.productReels.isEmpty()) {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material.CircularProgressIndicator(
                            color = Color(0xFF0066CC),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading products...",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            } else if (exploreItems.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "No Content",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No content available",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Check back later for new products",
                            color = Color.Gray.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                ExploreMasonryGrid(
                    items = exploreItems,
                    onItemClick = { item ->
                        // Navigate to the specific reel
                        navController.navigate("${Screens.ReelsScreen.route}/${item.id}")
                    }
                )
            }
            Spacer(modifier = Modifier.height(50.dp))
        }

        // Fixed header at the top
        ReelsTopHeader(
            onClickSearch = { navController.navigate(Screens.ReelsScreen.SearchReelsAndUsersScreen.route) },
            selectedTab = selectedTab,
            onTabChange = { tab ->
                Log.d("ExploreScreen", "🔄 Tab change requested: $selectedTab -> $tab")
                if (tab == "For you" || tab == "Following") {
                    savedStateHandle?.set("selectedTab", tab)
                    Log.d("ExploreScreen", "✅ Tab state updated to: $tab, navigating back to ReelsView")
                    navController.popBackStack()
                } else {
                    selectedTab = tab
                    savedStateHandle?.set("selectedTab", tab)
                    Log.d("ExploreScreen", "✅ Local tab state updated to: $tab")
                }
            },
            onClickExplore = { 
                selectedTab = "Explore"
                savedStateHandle?.set("selectedTab", "Explore")
                Log.d("ExploreScreen", "✅ Explore tab selected")
            },
            headerStyle = HeaderStyle.WHITE_BLACK_TEXT,
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .zIndex(1f)
        )
        
        // Refresh button overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = headerHeight + 8.dp, end = 16.dp)
                .zIndex(2f)
        ) {
            androidx.compose.material3.FloatingActionButton(
                onClick = {
                    productViewModel.getAllProductsFromFirebase()
                },
                modifier = Modifier.size(40.dp),
                containerColor = Color(0xFF0066CC),
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ExploreGridItem(
    item: ExploreItem,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable { onItemClick() }
    ) {
        // Use VideoThumbnail for videos, AsyncImage for images
        if (item.isVideo && item.videoUri != null) {
            VideoThumbnail(
                videoUri = item.videoUri,
                fallbackImageRes = R.drawable.reelsphoto,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                showPlayIcon = false // Don't show play icon in grid view
            )
        } else if (item.imageUris?.isNotEmpty() == true) {
            // Show first image if available
            AsyncImage(
                model = item.imageUris.first(),
                contentDescription = item.productName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            // Fallback to placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Gray.copy(alpha = 0.3f))
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (item.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                        contentDescription = if (item.isVideo) "Video Content" else "No Image",
                        tint = Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (item.isVideo) "Video Post" else item.productName,
                        color = Color.Gray,
                        fontSize = 10.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }

        // Content type indicator (video/image icon)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(10.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = if (item.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                contentDescription = if (item.isVideo) "Video" else "Image",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }

        // Product information overlay at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                )
                .padding(8.dp)
        ) {
            Column {
                // Product name
                Text(
                    text = item.productName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                
                // Product price
                Text(
                    text = "$${item.productPrice}",
                    color = Color(0xFFFF6F00),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                // User info and engagement
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Use UserDisplayName for real user names instead of hardcoded userName
                    Box(
                        modifier = Modifier.weight(1f, false)
                    ) {
                        if (item.userId.isNotBlank()) {
                            UserDisplayName(
                                userId = item.userId,
                                displayType = UserDisplayType.DISPLAY_NAME_ONLY,
                                color = Color.White.copy(alpha = 0.8f),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 10.sp
                                ),
                                maxLines = 1
                            )
                        } else {
                            // Fallback to original userName if userId is blank
                            Text(
                                text = item.userName,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Love count
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Likes",
                                tint = if (item.isLoved) Color.Red else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${item.loveCount}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 9.sp
                            )
                        }
                        
                        // Comment count
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Comment,
                                contentDescription = "Comments",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${item.commentCount}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExploreMasonryGrid(
    items: List<ExploreItem>,
    onItemClick: (ExploreItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Distribute items across 3 columns
    val columns = List(3) { mutableListOf<ExploreItem>() }
    items.forEachIndexed { index, item ->
        columns[index % 3].add(item)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        columns.forEach { columnItems ->
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                columnItems.forEach { item ->
                    val randomHeight = listOf(130.dp, 170.dp, 120.dp, 150.dp).random()
                    ExploreGridItem(
                        item = item,
                        onItemClick = { onItemClick(item) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(randomHeight)
                    )
                }
            }
        }
    }
}

enum class HeaderStyle {
    TRANSPARENT_WHITE_TEXT,
    WHITE_BLACK_TEXT
}

data class ExploreItem(
    val id: String,
    val userId: String, // Add userId for proper name resolution
    val productName: String,
    val productPrice: String,
    val productImage: String,
    val isVideo: Boolean,
    val videoUri: Uri?,
    val imageUris: List<Uri>?,
    val userName: String,
    val loveCount: Int,
    val isLoved: Boolean,
    val commentCount: Int,
    val rating: Double
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewExploreScreenWithHeader() {
    val navController = rememberNavController()
    // Note: This preview won't work properly without real data
    // In real app, it will use ProductViewModel data
}