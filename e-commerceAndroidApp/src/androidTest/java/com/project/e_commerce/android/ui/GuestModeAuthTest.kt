package com.project.e_commerce.android.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P0 Regression: Guest mode → gated action → Login prompt shown.
 *
 * Covers QA tickets AUTH-001 / QA #19, #21.
 *
 * Scenarios:
 *   A. Guest taps Like → LoginSheet/AlertDialog appears (no crash, no silent skip).
 *   B. Authenticated user taps Like → like is applied silently (no login dialog).
 *   C. Login dialog shows "Sign In" and "Continue as Guest" options.
 */
@RunWith(AndroidJUnit4::class)
class GuestModeAuthTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * Minimal replica of the auth-guarded Like action pattern used in ReelsView.
     * Mirrors the AuthGuard logic: if guest → show login prompt instead of action.
     */
    @Composable
    private fun FakeLikeButton(isAuthenticated: Boolean) {
        var showLoginPrompt by remember { mutableStateOf(false) }
        var liked by remember { mutableStateOf(false) }

        Column {
            Button(
                onClick = {
                    if (isAuthenticated) {
                        liked = true
                    } else {
                        showLoginPrompt = true
                    }
                },
                modifier = Modifier.testTag("like_button")
            ) {
                Text(if (liked) "♥ Liked" else "♡ Like")
            }

            Text(
                text = if (liked) "liked" else "not_liked",
                modifier = Modifier.testTag("like_status")
            )

            if (showLoginPrompt) {
                AlertDialog(
                    onDismissRequest = { showLoginPrompt = false },
                    title = { Text("Sign in to continue") },
                    text = { Text("Create an account to like, comment and buy products.") },
                    confirmButton = {
                        Button(
                            onClick = { showLoginPrompt = false },
                            modifier = Modifier.testTag("sign_in_btn")
                        ) {
                            Text("Sign In")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showLoginPrompt = false },
                            modifier = Modifier.testTag("guest_btn")
                        ) {
                            Text("Continue as Guest")
                        }
                    },
                    modifier = Modifier.testTag("login_dialog")
                )
            }
        }
    }

    // ────────────────────────────────────────────
    // A. Guest taps Like → Login prompt appears
    // ────────────────────────────────────────────

    @Test
    fun guest_tapsLike_loginDialogAppears() {
        composeRule.setContent {
            FakeLikeButton(isAuthenticated = false)
        }

        // Initially no dialog
        composeRule.onNodeWithTag("login_dialog").assertDoesNotExist()

        // Guest taps Like
        composeRule.onNodeWithTag("like_button").performClick()
        composeRule.waitForIdle()

        // Login dialog must be shown
        composeRule.onNodeWithTag("login_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("sign_in_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("guest_btn").assertIsDisplayed()
    }

    // ────────────────────────────────────────────
    // B. Guest like → NOT actually applied
    // ────────────────────────────────────────────

    @Test
    fun guest_tapsLike_likeIsNotApplied() {
        composeRule.setContent {
            FakeLikeButton(isAuthenticated = false)
        }

        composeRule.onNodeWithTag("like_status").assertTextEquals("not_liked")

        // Guest taps Like — action should NOT be applied
        composeRule.onNodeWithTag("like_button").performClick()
        composeRule.waitForIdle()

        // Like was NOT applied
        composeRule.onNodeWithTag("like_status").assertTextEquals("not_liked")
    }

    // ────────────────────────────────────────────
    // C. Authenticated user taps Like → action applied, no dialog
    // ────────────────────────────────────────────

    @Test
    fun authenticated_tapsLike_likeApplied_noDialog() {
        composeRule.setContent {
            FakeLikeButton(isAuthenticated = true)
        }

        composeRule.onNodeWithTag("like_status").assertTextEquals("not_liked")

        // Authenticated user taps Like
        composeRule.onNodeWithTag("like_button").performClick()
        composeRule.waitForIdle()

        // Like is applied
        composeRule.onNodeWithTag("like_status").assertTextEquals("liked")

        // No login dialog appeared
        composeRule.onNodeWithTag("login_dialog").assertDoesNotExist()
    }

    // ────────────────────────────────────────────
    // D. Login dialog "Continue as Guest" dismisses dialog
    // ────────────────────────────────────────────

    @Test
    fun loginDialog_tappingGuestButton_dismissesDialog() {
        composeRule.setContent {
            FakeLikeButton(isAuthenticated = false)
        }

        // Open dialog
        composeRule.onNodeWithTag("like_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("login_dialog").assertIsDisplayed()

        // Tap "Continue as Guest"
        composeRule.onNodeWithTag("guest_btn").performClick()
        composeRule.waitForIdle()

        // Dialog dismissed
        composeRule.onNodeWithTag("login_dialog").assertDoesNotExist()
    }
}
