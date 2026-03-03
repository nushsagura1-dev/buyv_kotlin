package com.project.e_commerce.android.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.project.e_commerce.android.util.CategoryIcons
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotEquals

/**
 * P1 Regression: Category icons display correctly for all known slugs.
 *
 * Covers QA ticket CAT-001 / QA #28.
 *
 * Verifies that CategoryIcons.forSlug() returns a valid (non-zero) drawable
 * resource ID for all category slugs, including unknown ones (fallback icon).
 */
@RunWith(AndroidJUnit4::class)
class CategoryIconsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val knownSlugs = listOf(
        "electronics",
        "fashion",
        "beauty",
        "sports",
        "toys",
        "jewelry"
    )

    // ────────────────────────────────────────────
    // A. All known slugs return non-zero drawable res ID
    // ────────────────────────────────────────────

    @Test
    fun allKnownCategorySlugs_returnValidDrawableResId() {
        knownSlugs.forEach { slug ->
            val resId = CategoryIcons.forSlug(slug)
            assertNotEquals(
                illegal = 0,
                actual = resId,
                message = "Category slug '$slug' must map to a valid drawable resource (got 0)"
            )
        }
    }

    // ────────────────────────────────────────────
    // B. Unknown slug returns the default icon (non-zero)
    // ────────────────────────────────────────────

    @Test
    fun unknownSlug_returnsFallbackDrawableResId() {
        val resId = CategoryIcons.forSlug("unknown-category-xyz")
        assertNotEquals(0, resId, "Unknown slug should return a valid fallback drawable (not 0)")
    }

    // ────────────────────────────────────────────
    // C. Icons render in Compose without crash
    // ────────────────────────────────────────────

    @Test
    fun categoryIcons_renderInCompose_noCrash() {
        composeRule.setContent {
            Column {
                knownSlugs.forEach { slug ->
                    val resId = remember { CategoryIcons.forSlug(slug) }
                    Icon(
                        painter = painterResource(id = resId),
                        contentDescription = slug,
                        modifier = Modifier.testTag("icon_$slug")
                    )
                    Text(
                        text = slug,
                        modifier = Modifier.testTag("label_$slug")
                    )
                }
            }
        }

        // Verify each icon rendered
        knownSlugs.forEach { slug ->
            composeRule.onNodeWithTag("icon_$slug").assertExists()
            composeRule.onNodeWithTag("label_$slug").assertTextEquals(slug)
        }
    }

    // ────────────────────────────────────────────
    // D. Slug matching is case-insensitive
    // ────────────────────────────────────────────

    @Test
    fun slugMatching_isCaseInsensitive() {
        val lowerRes = CategoryIcons.forSlug("electronics")
        val upperRes = CategoryIcons.forSlug("ELECTRONICS")
        val mixedRes = CategoryIcons.forSlug("Electronics")
        assertNotEquals(0, lowerRes)
        kotlin.test.assertEquals(lowerRes, upperRes, "Slug matching must be case-insensitive")
        kotlin.test.assertEquals(lowerRes, mixedRes, "Slug matching must be case-insensitive")
    }

    // ────────────────────────────────────────────
    // E. Empty slug returns fallback (no crash)
    // ────────────────────────────────────────────

    @Test
    fun emptySlug_returnsFallback_noCrash() {
        val resId = CategoryIcons.forSlug("")
        assertNotEquals(0, resId, "Empty slug must return fallback icon, not 0")
    }
}
