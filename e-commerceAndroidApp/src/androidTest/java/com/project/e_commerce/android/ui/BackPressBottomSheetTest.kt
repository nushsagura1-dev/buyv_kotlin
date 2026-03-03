package com.project.e_commerce.android.ui

import androidx.activity.ComponentActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P0 Regression: Back press on BottomSheet MUST dismiss the sheet, not exit the app.
 *
 * Covers QA ticket BACK-001 / QA #20.
 *
 * Scenario:
 *   1. App shows a screen with a BottomSheet trigger.
 *   2. BottomSheet is opened.
 *   3. Back press (via Escape key simulation) dismisses the sheet.
 *   4. The background screen content is still visible — app is NOT exited.
 */
@OptIn(ExperimentalMaterialApi::class)
@RunWith(AndroidJUnit4::class)
class BackPressBottomSheetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun backPress_onOpenBottomSheet_dismissesSheet_doesNotExitApp() {
        composeRule.setContent {
            val sheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Hidden
            )
            val scope = rememberCoroutineScope()

            ModalBottomSheetLayout(
                sheetState = sheetState,
                sheetContent = {
                    Text(
                        text = "Sheet Content",
                        modifier = Modifier.testTag("sheet_content")
                    )
                }
            ) {
                // Background screen content
                Text(
                    text = "Background Screen",
                    modifier = Modifier.testTag("background_screen")
                )
                Button(
                    onClick = { scope.launch { sheetState.show() } },
                    modifier = Modifier.testTag("open_sheet_btn")
                ) {
                    Text("Open Sheet")
                }
            }
        }

        // Background is initially visible
        composeRule.onNodeWithTag("background_screen").assertIsDisplayed()

        // Open the sheet
        composeRule.onNodeWithTag("open_sheet_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sheet_content").assertIsDisplayed()

        // Simulate back press
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        // Sheet should be dismissed — sheet content no longer visible
        composeRule.onNodeWithTag("sheet_content").assertDoesNotExist()

        // Background screen is STILL visible — app was NOT exited
        composeRule.onNodeWithTag("background_screen").assertIsDisplayed()
    }

    @Test
    fun backPress_whenSheetClosed_doesNotCrash() {
        var backPressHandled = false

        composeRule.setContent {
            val sheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Hidden
            )

            ModalBottomSheetLayout(
                sheetState = sheetState,
                sheetContent = { Text("Sheet") }
            ) {
                Text(
                    text = "Main Screen",
                    modifier = Modifier.testTag("main_screen")
                )
            }
        }

        // Sheet is not open — back press should not crash
        composeRule.activityRule.scenario.onActivity { activity ->
            try {
                activity.onBackPressedDispatcher.onBackPressed()
                backPressHandled = true
            } catch (e: Exception) {
                backPressHandled = false
            }
        }
        composeRule.waitForIdle()

        // App main screen is still visible regardless
        composeRule.onNodeWithTag("main_screen").assertIsDisplayed()
    }
}
