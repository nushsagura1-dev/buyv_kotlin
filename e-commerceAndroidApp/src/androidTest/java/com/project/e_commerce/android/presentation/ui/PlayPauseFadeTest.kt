package com.project.e_commerce.android.presentation.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P1 – Play/Pause icon auto-fade regression test.
 *
 * QA scenario: When the user taps on a reel, a large Play or Pause icon briefly
 * appears at the center of the screen, then fades out after ~1.5 s.
 * After the fade the icon must not be visible (alpha == 0 or not composed).
 *
 * This test uses a simplified harness that mirrors the real ReelsView overlay
 * behaviour so it can run without the full Koin/ViewModel/ExoPlayer stack.
 */
@RunWith(AndroidJUnit4::class)
class PlayPauseFadeTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ──────────────────────────────────────────────────────────
    // A. Icon appears immediately after tap
    // ──────────────────────────────────────────────────────────

    @Test
    fun playPauseIcon_visibleImmediatelyAfterTap() {
        composeRule.setContent {
            PlayPauseOverlayHarness()
        }

        // Icon should NOT be visible before any tap
        composeRule.onNodeWithTag("play_pause_icon").assertDoesNotExist()

        // Simulate tap
        composeRule.onNodeWithTag("reel_container").performClick()

        // Icon must appear right after the tap
        composeRule.onNodeWithTag("play_pause_icon").assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────
    // B. Icon disappears after the fade delay (simulated with advanceTimeBy)
    // ──────────────────────────────────────────────────────────

    @Test
    fun playPauseIcon_disappearsAfterFadeDelay() {
        composeRule.mainClock.autoAdvance = false

        composeRule.setContent {
            PlayPauseOverlayHarness(fadeDurationMs = 300, visibleDurationMs = 1500)
        }

        // Trigger tap
        composeRule.onNodeWithTag("reel_container").performClick()
        composeRule.mainClock.advanceTimeByFrame()

        // Icon should be visible
        composeRule.onNodeWithTag("play_pause_icon").assertIsDisplayed()

        // Advance past the visible duration + fade duration
        composeRule.mainClock.advanceTimeBy(1500L + 500L)

        // Icon should have faded out
        composeRule
            .onNodeWithTag("play_pause_icon")
            .assertIsNotDisplayed()
    }

    // ──────────────────────────────────────────────────────────
    // C. Correct icon shown: Play → pause icon; Pause → play icon
    // ──────────────────────────────────────────────────────────

    @Test
    fun playingState_showsPauseIcon() {
        composeRule.setContent {
            PlayPauseOverlayHarness(initialPlaying = true)
        }
        composeRule.onNodeWithTag("reel_container").performClick()
        composeRule.onNodeWithTag("pause_icon").assertIsDisplayed()
        composeRule.onNodeWithTag("play_icon").assertDoesNotExist()
    }

    @Test
    fun pausedState_showsPlayIcon() {
        composeRule.setContent {
            PlayPauseOverlayHarness(initialPlaying = false)
        }
        composeRule.onNodeWithTag("reel_container").performClick()
        composeRule.onNodeWithTag("play_icon").assertIsDisplayed()
        composeRule.onNodeWithTag("pause_icon").assertDoesNotExist()
    }

    // ──────────────────────────────────────────────────────────
    // D. Multiple rapid taps reset the fade timer
    // ──────────────────────────────────────────────────────────

    @Test
    fun rapidTaps_eachResetFadeTimer_iconRemainsVisible() {
        composeRule.mainClock.autoAdvance = false

        composeRule.setContent {
            PlayPauseOverlayHarness(fadeDurationMs = 300, visibleDurationMs = 1500)
        }

        val container = composeRule.onNodeWithTag("reel_container")
        container.performClick()
        composeRule.mainClock.advanceTimeBy(1000L) // partway through visible window

        container.performClick() // second tap — should reset timer
        composeRule.mainClock.advanceTimeBy(800L)  // only 800ms since second tap

        // Icon must still be visible (timer was reset by the second tap)
        composeRule.onNodeWithTag("play_pause_icon").assertIsDisplayed()
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Harness composable (mirrors the real ReelsView play/pause overlay)
// ─────────────────────────────────────────────────────────────────────────

/**
 * Minimal test harness composable that replicates the play/pause overlay logic
 * from the real `ReelsView` without requiring the full ViewModel stack.
 *
 * @param fadeDurationMs    Duration of the fadeOut animation in ms.
 * @param visibleDurationMs How long the icon stays visible before fading.
 * @param initialPlaying    If true, tapping shows the Pause icon; else shows Play.
 */
@Composable
private fun PlayPauseOverlayHarness(
    fadeDurationMs: Int    = 300,
    visibleDurationMs: Long = 1_500L,
    initialPlaying: Boolean = true
) {
    var isPlaying   by remember { mutableStateOf(initialPlaying) }
    var showOverlay by remember { mutableStateOf(false) }

    // Auto-hide after visibleDurationMs
    LaunchedEffect(showOverlay) {
        if (showOverlay) {
            kotlinx.coroutines.delay(visibleDurationMs)
            showOverlay = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reel_container")
            .clickable {
                isPlaying   = !isPlaying
                showOverlay = true
            },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = showOverlay,
            exit    = fadeOut(animationSpec = tween(fadeDurationMs))
        ) {
            if (isPlaying) {
                Icon(
                    imageVector        = Icons.Filled.PauseCircle,
                    contentDescription = "Pause",
                    tint               = Color.White,
                    modifier           = Modifier
                        .testTag("pause_icon")
                        .testTag("play_pause_icon")
                )
            } else {
                Icon(
                    imageVector        = Icons.Filled.PlayCircle,
                    contentDescription = "Play",
                    tint               = Color.White,
                    modifier           = Modifier
                        .testTag("play_icon")
                        .testTag("play_pause_icon")
                )
            }
        }
    }
}
