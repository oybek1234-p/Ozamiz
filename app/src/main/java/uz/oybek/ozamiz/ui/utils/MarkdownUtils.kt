package uz.oybek.ozamiz.ui.utils

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import java.util.regex.Pattern

object MarkdownUtils {

    fun formatMarkdown(text: String?): CharSequence {
        if (text == null) return ""
        val cleanedText = cleanText(text)
        val builder = SpannableStringBuilder()
        
        val boldPattern = Pattern.compile("\\*\\*(.*?)\\*\\*")
        val boldMatcher = boldPattern.matcher(cleanedText)
        
        var lastIndex = 0
        while (boldMatcher.find()) {
            builder.append(cleanedText.substring(lastIndex, boldMatcher.start()))
            val boldText = boldMatcher.group(1) ?: ""
            val start = builder.length
            builder.append(boldText)
            builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            lastIndex = boldMatcher.end()
        }
        builder.append(cleanedText.substring(lastIndex))
        
        return builder
    }

    fun cleanText(text: String): String {
        return text.replace("|", "")
            .replace("***", "")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
}
