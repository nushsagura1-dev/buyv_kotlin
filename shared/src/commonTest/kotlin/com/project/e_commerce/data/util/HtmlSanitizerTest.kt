package com.project.e_commerce.data.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for HtmlSanitizer — PROD-002 / UPLOAD-002 regression coverage.
 */
class HtmlSanitizerTest {

    // --------- toPlainText ---------

    @Test
    fun toPlainText_emptyString_returnsEmpty() {
        assertEquals("", HtmlSanitizer.toPlainText(""))
    }

    @Test
    fun toPlainText_plainText_untouched() {
        assertEquals("Hello world", HtmlSanitizer.toPlainText("Hello world"))
    }

    @Test
    fun toPlainText_singleTag_stripped() {
        assertEquals("bold", HtmlSanitizer.toPlainText("<b>bold</b>"))
    }

    @Test
    fun toPlainText_brTag_replacedWithNewline() {
        val result = HtmlSanitizer.toPlainText("line1<br>line2")
        assertTrue(result.contains("\n"), "Expected newline for <br>")
    }

    @Test
    fun toPlainText_pTag_replacedWithNewline() {
        val result = HtmlSanitizer.toPlainText("<p>paragraph</p>")
        assertTrue(result.contains("paragraph"))
    }

    @Test
    fun toPlainText_nestedTags_allStripped() {
        val input = "<div><p><strong>Nested</strong> text</p></div>"
        assertEquals("Nested text", HtmlSanitizer.toPlainText(input).trim())
    }

    @Test
    fun toPlainText_htmlEntities_decoded() {
        // &amp; → &
        val result = HtmlSanitizer.toPlainText("A &amp; B")
        assertTrue(result.contains("&"), "Expected & decoded from &amp;")
    }

    @Test
    fun toPlainText_scriptTag_stripped() {
        val input = "<script>alert('xss')</script>Safe text"
        assertEquals("Safe text", HtmlSanitizer.toPlainText(input).trim())
    }

    // --------- containsHtml ---------

    @Test
    fun containsHtml_plainText_false() {
        assertFalse(HtmlSanitizer.containsHtml("No HTML here"))
    }

    @Test
    fun containsHtml_withTag_true() {
        assertTrue(HtmlSanitizer.containsHtml("<p>Some paragraph</p>"))
    }

    @Test
    fun containsHtml_onlyEntity_true() {
        assertTrue(HtmlSanitizer.containsHtml("A &amp; B"))
    }

    // --------- extractImageUrls ---------

    @Test
    fun extractImageUrls_noImages_emptyList() {
        val result = HtmlSanitizer.extractImageUrls("<p>No images here</p>")
        assertTrue(result.isEmpty())
    }

    @Test
    fun extractImageUrls_singleImg_returnsUrl() {
        val html = """<img src="https://example.com/photo.jpg" />"""
        val urls = HtmlSanitizer.extractImageUrls(html)
        assertEquals(1, urls.size)
        assertEquals("https://example.com/photo.jpg", urls.first())
    }

    @Test
    fun extractImageUrls_multipleImgs_returnsAll() {
        val html = """
            <img src="https://cdn.test/a.jpg"/>
            <img src="https://cdn.test/b.png" alt="second"/>
        """.trimIndent()
        val urls = HtmlSanitizer.extractImageUrls(html)
        assertEquals(2, urls.size)
    }

    @Test
    fun extractImageUrls_cloudinaryUrl_preserved() {
        val cloudinaryUrl = "https://res.cloudinary.com/demo/image/upload/sample.jpg"
        val html = """<img src="$cloudinaryUrl">"""
        val urls = HtmlSanitizer.extractImageUrls(html)
        assertTrue(urls.any { it.contains("cloudinary.com") })
    }
}
