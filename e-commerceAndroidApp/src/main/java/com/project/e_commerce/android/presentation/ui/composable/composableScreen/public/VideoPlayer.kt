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
    
    Log.d("VideoPlayer", "🎥 VideoPlayer state initialized - hasError: $hasError, errorMessage: $errorMessage")
    
    if (uri == null) {
        Log.w("VideoPlayer", "🎥 URI is null, setting error state")
        hasError = true
        errorMessage = "No video URI provided"
    }
    
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var shouldCallPlaybackStarted by remember { mutableStateOf(false) }
    
    DisposableEffect(uri) {
        Log.d("VideoPlayer", "🎥 DisposableEffect started for URI: $uri")
        
        var localExoPlayer: ExoPlayer? = null
        
        // Create ExoPlayer safely
        runCatching {
            Log.d("VideoPlayer", "🎥 Creating ExoPlayer instance")
            ExoPlayer.Builder(context).build()
        }.onSuccess { player ->
            localExoPlayer = player
            exoPlayer = player
            Log.d("VideoPlayer", "🎥 ExoPlayer created successfully")
            
            // Setup media item safely
            runCatching {
                Log.d("VideoPlayer", "🎥 Setting media item for URI: $uri")
                if (uri != null) {
                    val mediaItem = MediaItem.fromUri(uri)
                    player.setMediaItem(mediaItem)
                    Log.d("VideoPlayer", " Media item set successfully")
                    
                    player.prepare()
                    Log.d("VideoPlayer", " Player prepared successfully")
                    player.playWhenReady = true
                    player.play()
                    Log.d("VideoPlayer", " Autoplay called, player should start")
                    
                    if (isPlaying) {
                        Log.d("VideoPlayer", " Starting playback")
                        shouldCallPlaybackStarted = true
                        Log.d("VideoPlayer", " Playback started successfully")
                    }
                } else {
                    Log.w("VideoPlayer", " URI is null, cannot create media item")
                    hasError = true
                    errorMessage = "No video URI provided"
                }
                
                // Add listener for errors
                player.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("VideoPlayer", "🎥 PLAYER ERROR: ${error.message}", error)
                        hasError = true
                        errorMessage = "Playback error: ${error.message}"
                    }
                    
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.d("VideoPlayer", "🎥 Playback state changed: $playbackState")
                        if (playbackState == Player.STATE_ENDED) {
                            Log.d("VideoPlayer", "🎥 Playback ended, looping")
                            player.seekTo(0)
                            player.play()
                        }
                    }
                })
            }.onFailure { e ->
                Log.e("VideoPlayer", "🎥 CRASH: Error setting up ExoPlayer", e)
                hasError = true
                errorMessage = "Setup error: ${e.message}"
            }
        }.onFailure { e ->
            Log.e("VideoPlayer", "🎥 CRASH: Error creating ExoPlayer", e)
            hasError = true
            errorMessage = "Creation error: ${e.message}"
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
                shouldCallPlaybackStarted = true
            } else {
                Log.d("VideoPlayer", "🎥 Pausing playback from LaunchedEffect")
                player.pause()
            }
        }
    }
    
    if (hasError) {
        Log.w("VideoPlayer", "🎥 Showing error UI: $errorMessage")
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Video unavailable",
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = Color.Gray,
                    fontSize = 14.sp,
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
                    useController = false // HIDE ALL DEFAULT CONTROLS! (progress, play, seek, etc)
                    Log.d("VideoPlayer", "🎥 PlayerView created")
                }
            },
            modifier = modifier.fillMaxSize(),
            update = { playerView ->
                Log.d("VideoPlayer", "🎥 Updating PlayerView with ExoPlayer")
                exoPlayer?.let { player ->
                    playerView.player = player
                    Log.d("VideoPlayer", "🎥 PlayerView updated with ExoPlayer")
                }
            }
        )
    }
}

