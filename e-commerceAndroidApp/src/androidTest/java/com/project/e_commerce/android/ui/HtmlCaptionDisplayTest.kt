package com.project.e_commerce.android.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.project.e_commerce.data.util.HtmlSanitizer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse

/**
 * P1 Regression: HTML tags must NOT be visible in product captions.
 *
 * Covers QA tickets PROD-002 / QA #23, #25.
 *
 * These tests simulate what a product caption UI shows after passing through
 * the HtmlSanitizer pipeline. They complement the unit tests in HtmlSanitizerTest.kt
 * by running in an actual Android UI context (ensuring Text renders the clean string).
 */
@RunWith(AndroidJUnit4::class)
class HtmlCaptionDisplayTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * Simulate the product caption rendering you'd see in ProductScreen /
     * ReelsView overlay after the mapper has sanitized the CJ HTML description.
     */
    private fun renderCaption(rawHtml: String): String = HtmlSanitizer.toPlainText(rawHtml)

    // ────────────────────────────────────────────
    // A. CJ product description with <img> and <br>
    // ────────────────────────────────────────────

    @Test
    fun htmlCaption_withImgAndBr_rendersCleanText() {
        val rawHtml = """<p><b>Premium Watch</b><br/><img src="https://cdn.cj.com/img.jpg">Water resistant up to 50m</p>"""
        val clean = renderCaption(rawHtml)

        composeRule.setContent {
            Text(
                text = clean,
                modifier = Modifier.testTag("product_caption")
            )
        }

        val displayedText = composeRule
            .onNodeWithTag("product_caption")
            .fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.Text]
            .first()
            .text

        assertFalse(displayedText.contains("<img"), "Raw <img> tag must not appear in UI text")
        assertFalse(displayedText.contains("<br"), "Raw <br> tag must not appear in UI text")
        assertFalse(displayedText.contains("<p>"), "Raw <p> tag must not appear in UI text")
        assertFalse(displayedText.contains("<b>"), "Raw <b> tag must not appear in UI text")
    }

    // ────────────────────────────────────────────
    // B. Caption remains non-empty after sanitization
    // ────────────────────────────────────────────

    @Test
    fun htmlCaption_afterSanitization_retainsProductText() {
        val rawHtml = """<div><h2>Wireless Earbuds</h2><ul><li>40hr battery</li><li>ANC</li></ul></div>"""
        val clean = renderCaption(rawHtml)

        composeRule.setContent {
            Text(
                text = clean,
                modifier = Modifier.testTag("earbuds_caption")
            )
        }

        composeRule.onNodeWithTag("earbuds_caption").assertIsDisplayed()

        val displayedText = composeRule
            .onNodeWithTag("earbuds_caption")
            .fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.Text]
            .first()
            .text

        // Product name should still be visible
        assertFalse(displayedText.isBlank(), "Caption text should not be empty after sanitization")
    }

    // ────────────────────────────────────────────
    // C. Plain text caption is unchanged
    // ────────────────────────────────────────────

    @Test
    fun plainCaption_noHtml_renderedAsIs() {
        val plainText = "Great product for daily use. Fast shipping!"
        val clean = renderCaption(plainText)

        composeRule.setContent {
            Text(
                text = clean,
                modifier = Modifier.testTag("plain_caption")
            )
        }

        composeRule.onNodeWithTag("plain_caption").assertTextContains("Great product for daily use")
    }

    // ────────────────────────────────────────────
    // D. Script tags (XSS prevention)
    // ────────────────────────────────────────────

    @Test
    fun htmlCaption_withScriptTag_scriptNotRendered() {
        val maliciousHtml = """<p>Buy now!</p><script>alert('xss')</script>"""
        val clean = renderCaption(maliciousHtml)

        composeRule.setContent {
            Text(
                text = clean,
                modifier = Modifier.testTag("xss_caption")
            )
        }

        val displayedText = composeRule
            .onNodeWithTag("xss_caption")
            .fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.Text]
            .first()
            .text

        assertFalse(displayedText.contains("<script>"), "Script tags must be stripped")
        assertFalse(displayedText.contains("alert("), "Script content must be stripped")
    }
}
