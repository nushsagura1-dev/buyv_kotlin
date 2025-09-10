package com.project.e_commerce.android.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.project.e_commerce.android.R
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Reels
import com.project.e_commerce.android.presentation.viewModel.RecentlyViewedViewModel
import coil3.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.project.e_commerce.android.presentation.ui.navigation.Screens

// Cache for video thumbnails to avoid re-extraction
private val thumbnailCache = mutableMapOf<String, Bitmap?>()

@Composable
fun RecentlyViewedScreen(navController: NavHostController) {
    val recentlyViewedViewModel: RecentlyViewedViewModel = koinViewModel()
    val recentlyViewedReels by recentlyViewedViewModel.recentlyViewedReels.collectAsState()
    val isLoading by recentlyViewedViewModel.isLoading.collectAsState()
    val error by recentlyViewedViewModel.error.collectAsState()

    // Reduced debug logging
    if (error != null) {
        Log.e("RecentlyViewedScreen", "Error: $error")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {

            androidx.compose.material3.IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-20).dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = Color(0xFF0066CC),
                    modifier = Modifier.padding(10.dp)
                )
            }

            Text(
                text = "Recently Viewed",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF0066CC),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )

        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF0066CC))
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error loading recently viewed",
                            color = Color.Red,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { recentlyViewedViewModel.refreshRecentlyViewed() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF0066CC))
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
            }

            recentlyViewedReels.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_eye),
                            contentDescription = "No items",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No recently viewed reels",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Watch some reels to see them here",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            else -> {
                // Grid of recently viewed reels
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(recentlyViewedReels) { reel ->
                        ReelCard(
                            reel = reel,
                            onClick = {
                                // Navigate to reels screen with specific reel ID
                                navController.navigate(Screens.ReelsScreen.route + "/${reel.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReelCard(
    reel: Reels,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var videoThumbnail by remember(reel.id) { mutableStateOf<Bitmap?>(thumbnailCache[reel.id]) }
    var isLoading by remember(reel.id) { mutableStateOf(false) }
    var thumbnailError by remember(reel.id) { mutableStateOf(false) }

    // Extract thumbnail from video if available
    LaunchedEffect(reel.video, reel.id) {
        if (reel.video != null && videoThumbnail == null && !thumbnailError) {
            isLoading = true
            try {
                val retriever = MediaMetadataRetriever()

                // Try different approaches for video thumbnail extraction
                try {
                    // First try: Set data source with URI
                    retriever.setDataSource(context, reel.video)
                } catch (e: Exception) {
                    // Second try: Set data source with URL string
                    retriever.setDataSource(reel.video.toString(), HashMap<String, String>())
                }

                // Extract frame at different time positions if first fails
                val bitmap = try {
                    // Try to get frame at 1 second (often better quality than first frame)
                    retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } catch (e: Exception) {
                    try {
                        // Fallback: get frame at 0.5 seconds
                        retriever.getFrameAtTime(500000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    } catch (e2: Exception) {
                        try {
                            // Fallback: get frame at 0 seconds
                            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        } catch (e3: Exception) {
                            // Last resort: get any frame
                            retriever.frameAtTime
                        }
                    }
                }

                // Scale bitmap if it's too large to save memory
                val scaledBitmap =
                    if (bitmap != null && (bitmap.width > 800 || bitmap.height > 800)) {
                        val scale = minOf(800f / bitmap.width, 800f / bitmap.height)
                        val scaledWidth = (bitmap.width * scale).toInt()
                        val scaledHeight = (bitmap.height * scale).toInt()
                        Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
                    } else {
                        bitmap
                    }

                videoThumbnail = scaledBitmap
                thumbnailCache[reel.id] = scaledBitmap
                Log.d("ReelCard", "✅ Successfully extracted thumbnail for reel ${reel.id}")

                retriever.release()
            } catch (e: Exception) {
                Log.e("ReelCard", "❌ Error extracting video thumbnail for ${reel.id}", e)
                thumbnailError = true
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(0.5.dp, Color.LightGray, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray)
        ) {
            when {
                // Priority 1: Show video thumbnail if available (best quality for reels)
                videoThumbnail != null -> {
                    Image(
                        bitmap = videoThumbnail!!.asImageBitmap(),
                        contentDescription = reel.contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Priority 2: Show loading indicator while extracting video thumbnail
                isLoading && reel.video != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF0066CC),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                // Priority 3: Show first image if video thumbnail failed or no video
                !reel.images.isNullOrEmpty() -> {
                    AsyncImage(
                        model = reel.images.first(),
                        contentDescription = reel.contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(id = R.drawable.img_2),
                        placeholder = painterResource(id = R.drawable.img_2)
                    )
                }
                // Priority 4: Show video placeholder if thumbnail failed but it's a video
                reel.video != null && thumbnailError -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1a1a1a)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Video thumbnail unavailable",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                // Priority 5: Show product image as fallback
                reel.productImage.isNotEmpty() -> {
                    AsyncImage(
                        model = reel.productImage,
                        contentDescription = reel.contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(id = R.drawable.img_2),
                        placeholder = painterResource(id = R.drawable.img_2)
                    )
                }
                // Priority 6: Show fallback image as last resort
                else -> {
                    Image(
                        painter = painterResource(id = reel.fallbackImageRes),
                        contentDescription = reel.contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Loading indicator overlay (only if not using it as main content)
            if (isLoading && reel.video != null && videoThumbnail == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Play icon overlay for videos (to indicate it's a video reel)
            if (reel.video != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.Black.copy(0.6f), CircleShape)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play video",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // User name
        Text(
            text = reel.userName,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = Color(0xFF0066CC),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Content description or product name
        Text(
            text = if (reel.contentDescription.isNotEmpty()) reel.contentDescription else reel.productName,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Product price if available
        if (reel.productPrice.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${reel.productPrice}$",
                color = Color(0xFFFF6F00),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RecentlyViewedScreenPreview() {
    val navController = rememberNavController()
    RecentlyViewedScreen(navController)
}