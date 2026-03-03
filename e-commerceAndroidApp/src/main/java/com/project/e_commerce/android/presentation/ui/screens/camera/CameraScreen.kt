package com.project.e_commerce.android.presentation.ui.screens.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.project.e_commerce.android.presentation.viewModel.CameraViewModel
import com.project.e_commerce.domain.platform.FilterInfo
import org.koin.androidx.compose.koinViewModel

/**
 * 2.18 — CameraScreen (Android Compose)
 *
 * Full-screen camera preview with:
 *   • CameraX PreviewView (16:9 / fill)
 *   • Filter strip — horizontal LazyRow at the bottom
 *   • Record button (center) — tap to start/stop
 *   • Flip camera button (top-right)
 *   • CAMERA + RECORD_AUDIO permission request on first launch
 *
 * Navigation:
 *   When recording completes the [onVideoReady] callback fires with the
 *   local file URI so the parent nav-graph can navigate to upload/trim.
 *
 * Usage in NavGraph:
 * ```kotlin
 * composable("camera") {
 *     CameraScreen(
 *         onVideoReady = { uri -> navController.navigate("reel_publish?uri=$uri") },
 *         onDismiss    = { navController.popBackStack() }
 *     )
 * }
 * ```
 */
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = koinViewModel(),
    onVideoReady: (outputUri: String) -> Unit = {},
    onDismiss:    ()            -> Unit = {}
) {
    val context = LocalContext.current

    // ── Permission handling ──────────────────────────────────────
    val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    val allGranted = requiredPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    var permissionsGranted by remember { mutableStateOf(allGranted) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) permissionLauncher.launch(requiredPermissions)
    }

    // ── ViewModel state ─────────────────────────────────────────
    val isRecording    by viewModel.isRecording.collectAsState()
    val selectedFilter by viewModel.selectedFilterId.collectAsState()
    val errorMessage   by viewModel.errorMessage.collectAsState()
    val lastOutputUri  by viewModel.lastOutputUri.collectAsState()

    // Navigate away when recording completes
    LaunchedEffect(lastOutputUri) {
        lastOutputUri?.let { uri -> onVideoReady(uri) }
    }

    // ── UI ────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        if (permissionsGranted) {
            // ── Camera preview ─────────────────────────────────────
            CameraPreview(
                viewModel  = viewModel,
                modifier   = Modifier.fillMaxSize()
            )
        } else {
            PermissionDeniedPlaceholder(
                onRequestAgain = { permissionLauncher.launch(requiredPermissions) }
            )
        }

        // ── Top bar ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close camera",
                    tint = Color.White)
            }
            IconButton(
                onClick  = viewModel::flipCamera,
                modifier = Modifier.testTag("flip_camera_button")
            ) {
                Icon(Icons.Filled.FlipCameraAndroid, contentDescription = "Flip camera",
                    tint = Color.White)
            }
        }

        // ── Bottom strip: filters + record button ─────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilterStrip(
                filters         = viewModel.availableFilters,
                selectedId      = selectedFilter,
                onFilterSelected = viewModel::selectFilter
            )

            Spacer(modifier = Modifier.height(24.dp))

            RecordButton(
                isRecording = isRecording,
                enabled     = permissionsGranted,
                onClick     = { viewModel.toggleRecording() }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text  = if (isRecording) "Tap to stop" else "Hold to record",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }

        // ── Error snackbar ─────────────────────────────────────────
        errorMessage?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action   = {
                    TextButton(onClick = viewModel::clearError) {
                        Text("OK", color = MaterialTheme.colorScheme.inversePrimary)
                    }
                }
            ) { Text(msg, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
    }
}

// ─────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────

/**
 * AndroidView wrapper that binds a [PreviewView] to the CameraController.
 */
@Composable
private fun CameraPreview(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory  = { ctx ->
            PreviewView(ctx).also { previewView ->
                viewModel.cameraController.attachPreviewView(previewView)
                viewModel.cameraController.startPreview()
            }
        },
        modifier = modifier,
        onRelease = { viewModel.cameraController.stopPreview() }
    )
}

/**
 * Horizontal filter chip strip. Active chip is highlighted in accent colour.
 */
@Composable
private fun FilterStrip(
    filters: List<FilterInfo>,
    selectedId: String,
    onFilterSelected: (String) -> Unit
) {
    LazyRow(
        contentPadding     = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("filter_strip")
    ) {
        items(filters, key = { it.id }) { filter ->
            FilterChip(filter = filter, isSelected = filter.id == selectedId) {
                onFilterSelected(filter.id)
            }
        }
    }
}

@Composable
private fun FilterChip(
    filter: FilterInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFFF5722) else Color.White.copy(alpha = 0.2f),
        label       = "filterChipBg"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .testTag("filter_chip_${filter.id}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text      = filter.name,
            color     = Color.White,
            fontSize  = 12.sp,
            maxLines  = 1
        )
    }
}

/**
 * Pulsing red circle while recording; white ring when idle.
 */
@Composable
private fun RecordButton(
    isRecording: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val outerColor by animateColorAsState(
        targetValue = if (isRecording) Color(0xFFFF1744) else Color.White,
        label       = "recordOuter"
    )
    val innerColor by animateColorAsState(
        targetValue = if (isRecording) Color(0xFFFF1744).copy(alpha = 0.3f)
                      else Color.White.copy(alpha = 0.15f),
        label       = "recordInner"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(innerColor)
            .border(4.dp, outerColor, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("record_button"),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFF1744))
            )
        }
    }
}

/**
 * Shown when CAMERA permission is permanently denied.
 */
@Composable
private fun BoxScope.PermissionDeniedPlaceholder(onRequestAgain: () -> Unit) {
    Column(
        modifier            = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector         = Icons.Filled.NoPhotography,
            contentDescription  = null,
            tint                = Color.White,
            modifier            = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text     = "Camera permission required",
            color    = Color.White,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRequestAgain) {
            Text("Grant Permission", color = Color(0xFFFF5722))
        }
    }
}
