package com.project.e_commerce.android.presentation.ui.composable.common

import android.text.Html
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.text.style.URLSpan
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/**
 * A Composable that renders HTML text properly.
 *
 * Converts common HTML tags (<p>, <b>, <i>, <u>, <br>, <a>, <ul>, <li>, etc.)
 * into styled Compose text. Falls back to plain text if HTML parsing fails.
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val annotatedString = remember(html) {
        htmlToAnnotatedString(html)
    }

    val mergedStyle = style.merge(TextStyle(color = color))

    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = mergedStyle,
        maxLines = maxLines,
        overflow = overflow,
        onClick = { /* Could handle URL clicks here */ }
    )
}

/**
 * Converts an HTML string to a Compose [AnnotatedString].
 */
private fun htmlToAnnotatedString(html: String): AnnotatedString {
    if (html.isBlank()) return AnnotatedString("")

    return try {
        // Parse HTML using Android's built-in parser
        val spanned: Spanned = Html.fromHtml(
            html,
            Html.FROM_HTML_MODE_COMPACT
        )

        buildAnnotatedString {
            append(spanned.toString())

            // Apply spans from the parsed HTML
            val spans = spanned.getSpans(0, spanned.length, Any::class.java)
            for (span in spans) {
                val start = spanned.getSpanStart(span)
                val end = spanned.getSpanEnd(span)
                when (span) {
                    is StyleSpan -> {
                        when (span.style) {
                            android.graphics.Typeface.BOLD -> {
                                addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                            }
                            android.graphics.Typeface.ITALIC -> {
                                addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                            }
                            android.graphics.Typeface.BOLD_ITALIC -> {
                                addStyle(
                                    SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontStyle = FontStyle.Italic
                                    ), start, end
                                )
                            }
                        }
                    }
                    is UnderlineSpan -> {
                        addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                    }
                    is URLSpan -> {
                        addStyle(
                            SpanStyle(
                                color = Color(0xFF2196F3),
                                textDecoration = TextDecoration.Underline
                            ),
                            start, end
                        )
                        addStringAnnotation("URL", span.url, start, end)
                    }
                    is RelativeSizeSpan -> {
                        // Could handle relative size here
                    }
                    is ForegroundColorSpan -> {
                        addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
                    }
                }
            }
        }
    } catch (e: Exception) {
        // Fallback: strip tags manually
        AnnotatedString(html.replace(Regex("<[^>]*>"), "").trim())
    }
}
