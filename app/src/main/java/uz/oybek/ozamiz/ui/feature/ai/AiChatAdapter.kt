package uz.oybek.ozamiz.ui.feature.ai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.ai.model.ChatMessage
import uz.oybek.ozamiz.databinding.ItemChatMessageBinding
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.text.Spanned
import java.util.regex.Pattern
import uz.oybek.ozamiz.databinding.ItemChatThinkingBinding

class AiChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_AI = 1
        private const val TYPE_THINKING = 2
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when {
            item.isThinking -> TYPE_THINKING
            item.isUser -> TYPE_USER
            else -> TYPE_AI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_THINKING -> ThinkingViewHolder(
                ItemChatThinkingBinding.inflate(inflater, parent, false)
            )
            else -> MessageViewHolder(
                ItemChatMessageBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is MessageViewHolder) {
            holder.bind(item)
        }
    }

    class MessageViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.apply {
                val cleanedText = cleanText(message.text)
                if (message.isUser) {
                    userMessageLayout.isVisible = true
                    aiMessageLayout.isVisible = false
                    userMessageText.text = formatMarkdown(cleanedText)
                } else {
                    userMessageLayout.isVisible = false
                    aiMessageLayout.isVisible = true
                    aiMessageText.text = formatMarkdown(cleanedText)
                }
            }
        }

        private fun cleanText(text: String): String {
            return text.replace("|", "")
                .replace("***", "")
                .replace(Regex("\\n{3,}"), "\n\n") // Normalize multiple newlines
                .trim()
        }

        private fun formatMarkdown(text: String): CharSequence {
            val builder = SpannableStringBuilder()
            val pattern = Pattern.compile("\\*\\*(.*?)\\*\\*")
            val matcher = pattern.matcher(text)
            
            var lastIndex = 0
            while (matcher.find()) {
                builder.append(text.substring(lastIndex, matcher.start()))
                val boldText = matcher.group(1) ?: ""
                val start = builder.length
                builder.append(boldText)
                builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                lastIndex = matcher.end()
            }
            builder.append(text.substring(lastIndex))
            return builder
        }
    }

    class ThinkingViewHolder(binding: ItemChatThinkingBinding) :
        RecyclerView.ViewHolder(binding.root)

    object DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem.timestamp == newItem.timestamp

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem == newItem
    }
}
