package com.project.e_commerce.data.util

/**
 * Cross-platform HTML sanitizer (commonMain — no platform dependencies).
 *
 * Used to clean raw HTML coming from CJ Dropshipping product descriptions
 * before displaying text to the user (PROD-002, UPLOAD-002).
 */
object HtmlSanitizer {

    private val IMG_TAG = Regex("<img[^>]*>", RegexOption.IGNORE_CASE)
    private val BR_TAG = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val ALL_TAGS = Regex("<[^>]+>")
    private val MULTI_SPACES = Regex("[ \\t]{2,}")
    private val MULTI_NEWLINES = Regex("\\n{3,}")

    /**
     * Converts raw HTML to readable plain text.
     * - Removes <img> tags completely
     * - Converts <br> to newlines
     * - Strips all remaining HTML tags
     * - Decodes common HTML entities
     */
    fun toPlainText(html: String?): String {
        if (html.isNullOrBlank()) return ""
        return html
            .replace(IMG_TAG, "")
            .replace(BR_TAG, "\n")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(ALL_TAGS, "")
            .replace(MULTI_SPACES, " ")
            .replace(MULTI_NEWLINES, "\n\n")
            .trim()
    }

    /**
     * Extracts all image URLs from HTML src attributes.
     * Used to build an image carousel from CJ description HTML.
     */
    fun extractImageUrls(html: String?): List<String> {
        if (html.isNullOrBlank()) return emptyList()
        val srcRegex = Regex("""src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return srcRegex.findAll(html).map { it.groupValues[1] }.filter { it.isNotBlank() }.toList()
    }

    /**
     * Returns true when the string contains HTML markup.
     */
    fun containsHtml(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return ALL_TAGS.containsMatchIn(text)
    }
}
