package com.project.e_commerce.domain.model

import kotlinx.serialization.Serializable

/**
 * Enriched category model synced with the admin panel fields (CAT-001, CAT-005).
 *
 * Previously only had [id], [name], [image].
 * New fields: [nameArabic], [slug], [iconUrl], [displayOrder], [isActive].
 */
@Serializable
data class Category(
    val id: String = "",
    val name: String = "",
    val nameArabic: String = "",
    val slug: String = "",
    /** Remote icon URL; if blank the platform uses a local drawable/SF Symbol fallback. */
    val iconUrl: String = "",
    val displayOrder: Int = 0,
    val isActive: Boolean = true,
    /** Legacy field kept for backward compatibility with CJ product mapping. */
    val image: String = ""
) {
    /** Returns the best available icon source: remote URL or empty to trigger local fallback. */
    val effectiveIconUrl: String get() = iconUrl.ifBlank { image }
    /** True when this category should be shown in the category row. */
    val shouldDisplay: Boolean get() = isActive
}
