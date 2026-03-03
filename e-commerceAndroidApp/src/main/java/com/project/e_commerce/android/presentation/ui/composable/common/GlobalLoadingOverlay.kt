package com.project.e_commerce.android.presentation.ui.composable.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.e_commerce.domain.model.LoadingState

/**
 * UI-002: Full-screen loading overlay driven by [LoadingState].
 *
 * Shows a semi-transparent scrim + spinner when [state] is [LoadingState.Loading].
 * Shows an error card when [state] is [LoadingState.Error].
 * Invisible (no-op) for [LoadingState.Idle] and [LoadingState.Success].
 *
 * Usage: render at the top level of a Screen composable, after all content:
 * ```kotlin
 * Box {
 *     YourScreenContent()
 *     GlobalLoadingOverlay(state = uiState.loadingState)
 * }
 * ```
 */
@Composable
fun GlobalLoadingOverlay(
    state: LoadingState,
    modifier: Modifier = Modifier
) {
    val visible = state is LoadingState.Loading || state is LoadingState.Error

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.50f)),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is LoadingState.Loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = Color.White
                        )
                        if (state.message != null) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = state.message,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                is LoadingState.Error -> {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFB00020),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .size(width = 260.dp, height = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                else -> Unit
            }
        }
    }
}
