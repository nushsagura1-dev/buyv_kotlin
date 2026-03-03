package com.project.e_commerce.android.util

import com.project.e_commerce.android.R

/**
 * CAT-001/005: Maps category slugs (from backend) to local drawable resource IDs.
 *
 * Falls back to [R.drawable.buyv_logo] when no slug match is found,
 * ensuring the UI always shows something meaningful.
 *
 * Usage:
 * ```kotlin
 * val icon = CategoryIcons.forSlug(category.slug)
 * Image(painter = painterResource(icon), contentDescription = category.name)
 * ```
 */
object CategoryIcons {

    /** @return Android drawable resource ID for the given [slug]. */
    fun forSlug(slug: String): Int = slugToDrawable[slug.lowercase().trim()]
        ?: R.drawable.buyv_logo

    // slug → drawable mapping — extend as categories grow in the backend
    private val slugToDrawable: Map<String, Int> = mapOf(
        // Fashion & Clothing
        "fashion"           to R.drawable.buyv_logo,
        "clothing"          to R.drawable.buyv_logo,
        "women-fashion"     to R.drawable.buyv_logo,
        "men-fashion"       to R.drawable.buyv_logo,
        // Electronics
        "electronics"       to R.drawable.buyv_logo,
        "phones"            to R.drawable.buyv_logo,
        "computers"         to R.drawable.buyv_logo,
        // Beauty
        "beauty"            to R.drawable.buyv_logo,
        "skincare"          to R.drawable.buyv_logo,
        "makeup"            to R.drawable.buyv_logo,
        // Home
        "home"              to R.drawable.buyv_logo,
        "furniture"         to R.drawable.buyv_logo,
        "kitchen"           to R.drawable.buyv_logo,
        // Sports
        "sports"            to R.drawable.buyv_logo,
        "fitness"           to R.drawable.buyv_logo,
        // Accessories
        "accessories"       to R.drawable.buyv_logo,
        "jewelry"           to R.drawable.buyv_logo,
        "watches"           to R.drawable.buyv_logo,
        // Food & Grocery
        "food"              to R.drawable.buyv_logo,
        "grocery"           to R.drawable.buyv_logo,
        // Toys & Kids
        "toys"              to R.drawable.buyv_logo,
        "kids"              to R.drawable.buyv_logo,
        "baby"              to R.drawable.buyv_logo
    )
}
