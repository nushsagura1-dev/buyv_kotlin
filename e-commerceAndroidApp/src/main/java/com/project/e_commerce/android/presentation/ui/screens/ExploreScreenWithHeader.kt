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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import coil3.compose.AsyncImagePainter
import com.project.e_commerce.android.presentation.ui.navigation.Screens
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.ReelsTopHeader
import com.project.e_commerce.android.presentation.utils.CloudinaryUtils
import com.project.e_commerce.android.presentation.viewModel.ProductViewModel
import org.koin.androidx.compose.koinViewModel
import android.util.Log



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExploreScreenWithHeader(
    navController: NavHostController,
    productViewModel: ProductViewModel = koinViewModel()
) {
    // SIMPLE TEST MESSAGE
    Log.d("EXPLORE_DEBUG", "🔍 EXPLORE SCREEN LOADED - DEBUG IS WORKING!")
    
    var selectedTab by remember { mutableStateOf("Explore") }
    val headerHeight = 60.dp
    
    // Get real product data from ProductViewModel
    val exploreItems = productViewModel.productReels.map { reel ->
        // Debug: Log the raw reel data
        Log.d("EXPLORE_DEBUG", "DEBUG: Raw Reel Data:")
        Log.d("EXPLORE_DEBUG", "  - ID: ${reel.id}")
        Log.d("EXPLORE_DEBUG", "  - Product Name: ${reel.productName}")
        Log.d("EXPLORE_DEBUG", "  - Product Price: ${reel.productPrice}")
        Log.d("EXPLORE_DEBUG", "  - Product Image: ${reel.productImage}")
        Log.d("EXPLORE_DEBUG", "  - Video URI: ${reel.video}")
        Log.d("EXPLORE_DEBUG", "  - Images List: ${reel.images}")
        Log.d("EXPLORE_DEBUG", "  - Is Video: ${reel.video != null}")
        
        ExploreItem(
            id = reel.id,
            productName = reel.productName,
            productPrice = reel.productPrice,
            productImage = reel.productImage,
            isVideo = reel.video != null,
            videoUri = reel.video,
            imageUris = reel.images,
            userName = reel.userName,
            loveCount = reel.love.number,
            isLoved = reel.love.isLoved,
            commentCount = reel.numberOfComments,
            rating = reel.rating
        )
    }
    
    // Debug: Log the final explore items
    Log.d("EXPLORE_DEBUG", "DEBUG: Final Explore Items Count: ${exploreItems.size}")
    exploreItems.forEachIndexed { index, item ->
        Log.d("EXPLORE_DEBUG", "DEBUG: Explore Item $index:")
        Log.d("EXPLORE_DEBUG", "  - ID: ${item.id}")
        Log.d("EXPLORE_DEBUG", "  - Product Name: ${item.productName}")
        Log.d("EXPLORE_DEBUG", "  - Price: ${item.productPrice}")
        Log.d("EXPLORE_DEBUG", "  - Product Image: ${item.productImage}")
        Log.d("EXPLORE_DEBUG", "  - Video URI: ${item.videoUri}")
        Log.d("EXPLORE_DEBUG", "  - Product Image: ${item.productImage}")
        Log.d("EXPLORE_DEBUG", "  - Image URIs: ${item.imageUris}")
        Log.d("EXPLORE_DEBUG", "  - Is Video: ${item.isVideo}")
    }
    
    // TEST: Add a test item to verify AsyncImage is working
    val testItem = ExploreItem(
        id = "test",
        productName = "Test Product",
        productPrice = "$99.99",
        productImage = "https://picsum.photos/200/300",
        isVideo = false,
        videoUri = null,
        imageUris = null,
        userName = "Test User",
        loveCount = 0,
        isLoved = false,
        commentCount = 0,
        rating = 5.0
    )
    
    val allItems = exploreItems + testItem

    Box(Modifier.fillMaxSize()) {
        // TEST: Simple test image at the top to verify AsyncImage is working
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = headerHeight, bottom = 40.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Test image to verify AsyncImage is working
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(16.dp)
                    .background(Color.LightGray, RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = "https://picsum.photos/400/100",
                    contentDescription = "Test Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    imageLoader = com.project.e_commerce.android.EcommerceApp.imageLoader
                )
                Text(
                    text = "Test Image (should show above)",
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
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
                    items = allItems,
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
                if (tab == "For you" || tab == "Following") {
                    navController.popBackStack()
                } else {
                    selectedTab = tab
                }
            },
            onClickExplore = { selectedTab = "Explore" },
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
                    // Refresh the product data
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
        // Main content (image or video)
        // Determine which image to show based on priority
        val imageToShow = when {
            // 1. If it has product images, show first image (highest priority)
            item.imageUris != null && item.imageUris.isNotEmpty() -> {
                val url = item.imageUris.first().toString()
                Log.d("EXPLORE_DEBUG", "DEBUG: Using FIRST PRODUCT IMAGE: $url")
                CloudinaryUtils.normalizeCloudinaryUrl(url)
            }
            // 2. If no images but has video (which is always true), show video thumbnail
            item.videoUri != null -> {
                val url = item.videoUri.toString()
                Log.d("EXPLORE_DEBUG", "DEBUG: Using VIDEO thumbnail (no images available): $url")
                CloudinaryUtils.normalizeCloudinaryUrl(url)
            }
            // 3. Fallback to product image (should rarely happen since video is required)
            item.productImage.isNotEmpty() -> {
                Log.d("EXPLORE_DEBUG", "DEBUG: Using PRODUCT IMAGE fallback: ${item.productImage}")
                CloudinaryUtils.normalizeCloudinaryUrl(item.productImage)
            }
            // 4. This should never happen since video is always required
            else -> {
                Log.d("EXPLORE_DEBUG", "DEBUG: ERROR - No video found for item ${item.id} (this shouldn't happen)")
                null
            }
        }
        
        // Debug: Log the final image URL being used
        Log.d("EXPLORE_DEBUG", "DEBUG: FINAL imageToShow for item ${item.id}: $imageToShow")
        Log.d("EXPLORE_DEBUG", "DEBUG: Item ${item.id} - videoUri: ${item.videoUri}")
        Log.d("EXPLORE_DEBUG", "DEBUG: Item ${item.id} - imageUris: ${item.imageUris}")
        Log.d("EXPLORE_DEBUG", "DEBUG: Item ${item.id} - productImage: ${item.productImage}")
        Log.d("EXPLORE_DEBUG", "DEBUG: Item ${item.id} - Post Type: ${if (item.imageUris?.isNotEmpty() == true) "IMAGE" else "VIDEO-ONLY"} post")
        Log.d("EXPLORE_DEBUG", "DEBUG: Item ${item.id} - Has Images: ${item.imageUris?.isNotEmpty() ?: false}")
        Log.d("EXPLORE_DEBUG", "DEBUG: Item ${item.id} - Has Video: ${item.videoUri != null}")
        Log.d("EXPLORE_DEBUG", "DEBUG: Item ${item.id} - Has Product Image: ${item.productImage.isNotEmpty()}")
        Log.d("EXPLORE_DEBUG", "DEBUG: Item ${item.id} - Will Show: ${if (imageToShow != null) "Image/Video" else "ERROR Placeholder"}")
        
        // Additional debug for Cloudinary URL handling
        if (imageToShow != null) {
            Log.d("EXPLORE_DEBUG", "DEBUG: Item ${item.id} - Is Cloudinary URL: ${CloudinaryUtils.isCloudinaryUrl(imageToShow)}")
            Log.d("EXPLORE_DEBUG", "DEBUG: Item ${item.id} - Is Cloudinary Image: ${CloudinaryUtils.isCloudinaryImageUrl(imageToShow)}")
            Log.d("EXPLORE_DEBUG", "DEBUG: Item ${item.id} - Is Cloudinary Video: ${CloudinaryUtils.isCloudinaryVideoUrl(imageToShow)}")
            Log.d("EXPLORE_DEBUG", "DEBUG: Item ${item.id} - Using Custom Cloudinary Fetcher: ${CloudinaryUtils.isCloudinaryUrl(imageToShow)}")
        }
        
        // TEST: Add a simple test image to verify AsyncImage is working
        if (item.id == "test") {
            AsyncImage(
                model = "https://picsum.photos/200/300",
                contentDescription = "Test Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                imageLoader = com.project.e_commerce.android.EcommerceApp.imageLoader
            )
        } else if (imageToShow != null) {
            // Use our custom ImageLoader with Cloudinary fetcher
            AsyncImage(
                model = imageToShow,
                contentDescription = item.productName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                imageLoader = com.project.e_commerce.android.EcommerceApp.imageLoader,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Loading -> {
                            Log.d("EXPLORE_DEBUG", "DEBUG: 🟡 Image LOADING for item ${item.id} - URL: $imageToShow")
                        }
                        is AsyncImagePainter.State.Success -> {
                            Log.d("EXPLORE_DEBUG", "DEBUG: 🟢 Image LOADED SUCCESSFULLY for item ${item.id} - URL: $imageToShow")
                        }
                        is AsyncImagePainter.State.Error -> {
                            Log.d("EXPLORE_DEBUG", "DEBUG: 🔴 Image FAILED to load for item ${item.id}")
                            Log.d("EXPLORE_DEBUG", "DEBUG: 🔴 Error details: ${state.result}")
                            Log.d("EXPLORE_DEBUG", "DEBUG: 🔴 Attempted URL: $imageToShow")
                        }
                        is AsyncImagePainter.State.Empty -> {
                            Log.d("EXPLORE_DEBUG", "DEBUG: ⚪ Image state is EMPTY for item ${item.id} - URL: $imageToShow")
                        }
                    }
                }
            )
        } else {
            // Fallback to product image
            if (item.productImage.isNotEmpty()) {
                AsyncImage(
                    model = CloudinaryUtils.normalizeCloudinaryUrl(item.productImage),
                    contentDescription = item.productName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    imageLoader = com.project.e_commerce.android.EcommerceApp.imageLoader
                )
            } else {
                // No image available - show placeholder
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
        }

        // Content type indicator (video/image icon)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .background(Color.Black.copy(alpha = 0.95f), shape = RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = if (item.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                contentDescription = if (item.isVideo) "Video" else "Image",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Product information overlay at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Color.Black.copy(alpha = 0.7f),
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
                    Text(
                        text = item.userName,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                    
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
                                imageVector = Icons.Default.Image,
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
                    val randomHeight = listOf(130.dp, 170.dp, 120.dp).random()
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