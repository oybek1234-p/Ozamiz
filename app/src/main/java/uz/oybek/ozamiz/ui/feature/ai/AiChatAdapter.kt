package uz.oybek.ozamiz.ui.feature.ai

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.ai.aiChat.ChatMessage
import uz.oybek.ozamiz.databinding.ItemChatMessageBinding
import uz.oybek.ozamiz.databinding.ItemChatThinkingBinding
import uz.oybek.ozamiz.ui.utils.MarkdownUtils

class AiChatAdapter(
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback) {

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

    private var lastAnimatedPosition = -1
    private var isInitialLoad = true

    override fun onCurrentListChanged(
        previousList: MutableList<ChatMessage>,
        currentList: MutableList<ChatMessage>
    ) {
        super.onCurrentListChanged(previousList, currentList)
        if (currentList.isEmpty()) {
            lastAnimatedPosition = -1
            isInitialLoad = true
        } else if (isInitialLoad) {
            lastAnimatedPosition = if (currentList.size > 2) currentList.size - 1 else -1
            isInitialLoad = false
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
            val currentPos = holder.bindingAdapterPosition
            if (currentPos != RecyclerView.NO_POSITION && currentPos > lastAnimatedPosition) {
                lastAnimatedPosition = currentPos
                holder.runEntranceAnimation()
            }
        } else if (holder is ThinkingViewHolder) {
            holder.startAnimation()
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is ThinkingViewHolder) {
            holder.stopAnimation()
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is ThinkingViewHolder) {
            holder.stopAnimation()
        }
    }

    class MessageViewHolder(
        private val binding: ItemChatMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.userMessageLayout.apply {
                alpha = 1f
                scaleX = 1f
                scaleY = 1f
                translationY = 0f
            }
            binding.aiMessageLayout.apply {
                alpha = 1f
                scaleX = 1f
                scaleY = 1f
                translationY = 0f
            }

            binding.apply {
                val cleanedText = message.text
                if (message.isUser) {
                    userMessageLayout.isVisible = true
                    aiMessageLayout.isVisible = false
                    userMessageText.text = MarkdownUtils.formatMarkdown(cleanedText)
                } else {
                    userMessageLayout.isVisible = false
                    aiMessageLayout.isVisible = true
                    aiMessageText.text = MarkdownUtils.formatMarkdown(cleanedText)
                }
            }
        }

        fun runEntranceAnimation() {
            val targetView = if (binding.userMessageLayout.isVisible) {
                binding.userMessageLayout
            } else {
                binding.aiMessageLayout
            }

            targetView.alpha = 0f
            targetView.scaleX = 0.85f
            targetView.scaleY = 0.85f
            targetView.translationY = 40f

            targetView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(250)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    class ThinkingViewHolder(private val binding: ItemChatThinkingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val animators = mutableListOf<android.animation.Animator>()

        fun startAnimation() {
            stopAnimation()

            val dots = listOf(binding.dot1, binding.dot2, binding.dot3)
            dots.forEachIndexed { index, dot ->
                dot.translationY = 0f
                dot.alpha = 0.3f

                val jumpAnim =
                    ObjectAnimator.ofFloat(dot, "translationY", 0f, -10f, 0f)
                        .apply {
                            duration = 800
                            startDelay = index * 200L
                            repeatCount = android.animation.ValueAnimator.INFINITE
                            repeatMode = android.animation.ValueAnimator.RESTART
                        }

                val fadeAnim =
                    ObjectAnimator.ofFloat(dot, "alpha", 0.3f, 1f, 0.3f).apply {
                        duration = 800
                        startDelay = index * 200L
                        repeatCount = android.animation.ValueAnimator.INFINITE
                        repeatMode = android.animation.ValueAnimator.RESTART
                    }

                jumpAnim.start()
                fadeAnim.start()

                animators.add(jumpAnim)
                animators.add(fadeAnim)
            }
        }

        fun stopAnimation() {
            animators.forEach { it.cancel() }
            animators.clear()

            binding.dot1.translationY = 0f
            binding.dot1.alpha = 1f
            binding.dot2.translationY = 0f
            binding.dot2.alpha = 1f
            binding.dot3.translationY = 0f
            binding.dot3.alpha = 1f
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem.timestamp == newItem.timestamp

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem == newItem
    }
}
