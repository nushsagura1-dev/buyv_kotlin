package com.project.e_commerce.android.presentation.ui.composable.composableScreen.public

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.SHOW_BUFFERING_ALWAYS
import android.util.Log
import android.widget.TextView
import android.view.Gravity
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    uri: Uri?,
    isPlaying: Boolean,
    onPlaybackStarted: () -> Unit
) {

    val context = LocalContext.current
    Log.d("VideoPlayer", "🎥 VideoPlayer composable called with URI: $uri, isPlaying: $isPlaying")
    
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showPlayButton by remember { mutableStateOf(false) }
    
    Log.d("VideoPlayer", "🎥 VideoPlayer state initialized - hasError: $hasError, errorMessage: $errorMessage")

    // Validate URI upfront
    if (uri == null || uri.toString().isBlank() || !uri.toString().startsWith("http")) {
        Log.w("VideoPlayer", "🎥 Invalid URI: $uri")
        hasError = true
        errorMessage = "Invalid video URL"
        isLoading = false
    }
    
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var shouldCallPlaybackStarted by remember { mutableStateOf(false) }
    
    DisposableEffect(uri) {
        Log.d("VideoPlayer", "🎥 DisposableEffect started for URI: $uri")
        
        var localExoPlayer: ExoPlayer? = null

        if (!hasError) {
            // Create ExoPlayer safely
            runCatching {
                Log.d("VideoPlayer", "🎥 Creating ExoPlayer instance")
                val player = ExoPlayer.Builder(context).build()
                localExoPlayer = player
                exoPlayer = player
                Log.d("VideoPlayer", "🎥 ExoPlayer created successfully")

                // Add error listener first
                player.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("VideoPlayer", "🎥 PLAYER ERROR: ${error.message}", error)
                        hasError = true
                        errorMessage = "Playback failed"
                        isLoading = false
                        showPlayButton = false
                    }
                    
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.d("VideoPlayer", "🎥 Playback state changed: $playbackState")
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                isLoading = true
                                showPlayButton = false
                            }

                            Player.STATE_READY -> {
                                isLoading = false
                                showPlayButton = !isPlaying
                                if (isPlaying && !shouldCallPlaybackStarted) {
                                    shouldCallPlaybackStarted = true
                                }
                            }

                            Player.STATE_ENDED -> {
                                Log.d("VideoPlayer", "🎥 Playback ended, looping")
                                player.seekTo(0)
                                if (isPlaying) {
                                    player.play()
                                }
                            }

                            Player.STATE_IDLE -> {
                                isLoading = false
                                showPlayButton = true
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        showPlayButton = !isPlaying
                        if (isPlaying && !shouldCallPlaybackStarted) {
                            shouldCallPlaybackStarted = true
                        }
                    }
                })

                // Setup media item safely
                if (uri != null) {
                    try {
                        Log.d("VideoPlayer", "🎥 Setting media item for URI: $uri")
                        val mediaItem = MediaItem.fromUri(uri)
                        player.setMediaItem(mediaItem)
                        Log.d("VideoPlayer", "✅ Media item set successfully")

                        player.prepare()
                        Log.d("VideoPlayer", "✅ Player prepared successfully")

                        // Only start playing if explicitly requested
                        player.playWhenReady = isPlaying
                        if (isPlaying) {
                            player.play()
                            Log.d("VideoPlayer", "✅ Autoplay started")
                        }

                    } catch (e: Exception) {
                        Log.e("VideoPlayer", "🎥 Error setting up media", e)
                        hasError = true
                        errorMessage = "Failed to load video"
                        isLoading = false
                    }
                } else {
                    Log.w("VideoPlayer", "🎥 URI is null, cannot create media item")
                    hasError = true
                    errorMessage = "No video URL provided"
                    isLoading = false
                }

            }.onFailure { e ->
                Log.e("VideoPlayer", "🎥 Error creating ExoPlayer", e)
                hasError = true
                errorMessage = "Video player initialization failed"
                isLoading = false
            }
        }
        
        onDispose {
            Log.d("VideoPlayer", "🎥 Disposing ExoPlayer")
            localExoPlayer?.let { player ->
                runCatching {
                    if (player.isPlaying) {
                        Log.d("VideoPlayer", "🎥 Pausing player before disposal")
                        player.pause()
                    }
                    Log.d("VideoPlayer", "🎥 Releasing player")
                    player.release()
                    Log.d("VideoPlayer", "🎥 Player released successfully")
                }.onFailure { e ->
                    Log.e("VideoPlayer", "🎥 Error during player disposal", e)
                }
            }
            exoPlayer = null
        }
    }
    
    // Handle the callback in a separate LaunchedEffect
    LaunchedEffect(shouldCallPlaybackStarted) {
        if (shouldCallPlaybackStarted) {
            Log.d("VideoPlayer", "🎥 Calling onPlaybackStarted callback")
            onPlaybackStarted()
            shouldCallPlaybackStarted = false
        }
    }
    
    // Handle play/pause state changes
    LaunchedEffect(isPlaying) {
        Log.d("VideoPlayer", "🎥 LaunchedEffect triggered - isPlaying: $isPlaying")
        exoPlayer?.let { player ->
            if (isPlaying) {
                Log.d("VideoPlayer", "🎥 Starting playback from LaunchedEffect")
                player.play()
            } else {
                Log.d("VideoPlayer", "🎥 Pausing playback from LaunchedEffect")
                player.pause()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasError) {
            Log.w("VideoPlayer", "🎥 Showing error UI: $errorMessage")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video Error",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Video unavailable",
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Log.d("VideoPlayer", "🎥 Rendering AndroidView for ExoPlayer")
            AndroidView(
                factory = { context ->
                    Log.d("VideoPlayer", "🎥 Creating AndroidView factory")
                    PlayerView(context).apply {
                        useController = false // HIDE ALL DEFAULT CONTROLS
                        setShowBuffering(SHOW_BUFFERING_ALWAYS)
                        Log.d("VideoPlayer", "🎥 PlayerView created")
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { playerView ->
                    Log.d("VideoPlayer", "🎥 Updating PlayerView with ExoPlayer")
                    exoPlayer?.let { player ->
                        playerView.player = player
                        Log.d("VideoPlayer", "🎥 PlayerView updated with ExoPlayer")
                    }
                }
            )

            // Show loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Show play button when paused
            if (showPlayButton && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            exoPlayer?.play()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                CircleShape
                            )
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}
