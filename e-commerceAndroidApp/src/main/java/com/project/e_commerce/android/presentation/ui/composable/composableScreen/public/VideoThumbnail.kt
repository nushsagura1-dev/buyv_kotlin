package com.project.e_commerce.android.presentation.ui.composable.composableScreen.public

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

@Composable
fun VideoThumbnail(
    videoUri: Uri?,
    fallbackImageRes: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showPlayIcon: Boolean = true
) {
    val context = LocalContext.current
    var thumbnail by remember(videoUri) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(videoUri) { mutableStateOf(true) }
    var hasError by remember(videoUri) { mutableStateOf(false) }

    LaunchedEffect(videoUri) {
        if (videoUri != null) {
            try {
                isLoading = true
                hasError = false

                val bitmap = withContext(Dispatchers.IO) {
                    extractVideoThumbnail(videoUri, context)
                }

                thumbnail = bitmap
                isLoading = false

                if (bitmap == null) {
                    hasError = true
                    Log.w("VideoThumbnail", "Failed to extract thumbnail from: $videoUri")
                }
            } catch (e: Exception) {
                Log.e("VideoThumbnail", "Error extracting thumbnail: ${e.message}", e)
                hasError = true
                isLoading = false
            }
        } else {
            isLoading = false
            hasError = true
        }
    }

    Box(modifier = modifier) {
        when {
            isLoading -> {
                // Show loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Gray,
                        strokeWidth = 2.dp
                    )
                }
            }

            thumbnail != null -> {
                // Show extracted thumbnail
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = "Video thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }

            else -> {
                // Show fallback image
                Image(
                    painter = painterResource(id = fallbackImageRes),
                    contentDescription = "Fallback thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
        }

        // Add play icon overlay if requested and not loading
        if (showPlayIcon && !isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        CircleShape
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private suspend fun extractVideoThumbnail(uri: Uri, context: android.content.Context): Bitmap? {
    return try {
        val retriever = MediaMetadataRetriever()

        // Try to set data source from URI
        retriever.setDataSource(context, uri)

        // Extract frame at 1 second (1,000,000 microseconds)
        // If video is shorter, this will get the first available frame
        val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

        retriever.release()
        bitmap

    } catch (e: Exception) {
        Log.e("VideoThumbnail", "Failed to extract thumbnail: ${e.message}", e)
        try {
            // Try with HTTP URL if URI failed
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(uri.toString(), HashMap<String, String>())

            val bitmap =
                retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

            retriever.release()
            bitmap

        } catch (e2: Exception) {
            Log.e(
                "VideoThumbnail",
                "Failed to extract thumbnail with HTTP method: ${e2.message}",
                e2
            )
            null
        }
    }
}