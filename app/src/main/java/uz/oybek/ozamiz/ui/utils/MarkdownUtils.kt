package uz.oybek.ozamiz.ui.utils

import android.text.Spanned
import androidx.core.text.HtmlCompat

object MarkdownUtils {

    fun formatMarkdown(text: String?): CharSequence {
        if (text.isNullOrBlank()) return ""
        val preparedHtml = markdownToHtml(text)
        return try {
            val spanned = HtmlCompat.fromHtml(preparedHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
            trimTrailingWhitespace(spanned)
        } catch (e: Exception) {
            text
        }
    }

    private fun markdownToHtml(raw: String): String {
        var text = raw.trim()

        // Remove/convert Markdown headers (# Header, ## Header, ### Header -> <b>Header</b>)
        text = text.replace(Regex("(?m)^#{1,6}\\s*(.+)$"), "<b>$1</b>")

        // Normalize bullet points (* or - at line start to • )
        text = text.replace(Regex("(?m)^[\\*\\-]\\s+"), "• ")

        // Convert Markdown **bold** to <b>bold</b> precisely within lines
        text = text.replace(Regex("\\*\\*([^\\*\\n]+?)\\*\\*"), "<b>$1</b>")

        // If there's an odd unclosed ** during streaming, close it safely
        if (text.count { it == '*' } % 2 != 0 && text.contains("**")) {
            val lastDouble = text.lastIndexOf("**")
            if (lastDouble >= 0) {
                text = text.substring(0, lastDouble) + "<b>" + text.substring(lastDouble + 2) + "</b>"
            }
        }

        // Convert Markdown *italic* or _italic_ to <i>italic</i>
        text = text.replace(Regex("(?<!\\*)\\*([^\\*\\n]+?)\\*(?!\\*)"), "<i>$1</i>")
        text = text.replace(Regex("(?<!_)_([^_\\n]+?)_(?!_)"), "<i>$1</i>")

        // Replace newlines with <br/>
        text = text.replace("\n", "<br/>")

        return text
    }

    private fun trimTrailingWhitespace(source: CharSequence): CharSequence {
        var i = source.length
        while (--i >= 0 && Character.isWhitespace(source[i])) {
        }
        return source.subSequence(0, i + 1)
    }
}
