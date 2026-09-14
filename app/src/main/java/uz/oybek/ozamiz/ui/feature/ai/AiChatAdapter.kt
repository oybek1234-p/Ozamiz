package uz.oybek.ozamiz.ui.feature.ai

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.R
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.ai.aiChat.ChatMessage
import uz.oybek.ozamiz.databinding.ItemChatMessageBinding
import uz.oybek.ozamiz.ui.utils.MarkdownUtils

class AiChatAdapter : ListAdapter<ChatMessage, AiChatAdapter.MessageViewHolder>(DiffCallback) {

    companion object {
        private const val PAYLOAD_TEXT_UPDATE = "payload_text_update"
    }

    var activeTurnMinHeight: Int = 0
    var onFeedbackClick: ((message: ChatMessage, isLiked: Boolean, userQuestion: String) -> Unit)? = null

    private var lastAnimatedPosition = -1
    private var isInitialLoad = true

    override fun onCurrentListChanged(
        previousList: MutableList<ChatMessage>,
        currentList: MutableList<ChatMessage>
    ) {
        super.onCurrentListChanged(previousList, currentList)
        // Yangi xabar kelishi bilan oldingi AI xabari avtomatik wrap_content (minimumHeight = 0) bo'ladi
        if (previousList.isNotEmpty() && currentList.size > previousList.size) {
            notifyItemChanged(previousList.size - 1)
        }

        if (currentList.isEmpty()) {
            lastAnimatedPosition = -1
            isInitialLoad = true
        } else if (isInitialLoad) {
            lastAnimatedPosition = if (currentList.size > 2) currentList.size - 1 else -1
            isInitialLoad = false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return MessageViewHolder(ItemChatMessageBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(
        holder: MessageViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val item = getItem(position)
        val isLastItem = position == itemCount - 1

        if (isLastItem && !item.isUser && activeTurnMinHeight > 0) {
            holder.itemView.minimumHeight = activeTurnMinHeight
        } else {
            holder.itemView.minimumHeight = 0
        }

        if (payloads.contains(PAYLOAD_TEXT_UPDATE)) {
            holder.updateContent(item)
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val item = getItem(position)
        val isLastItem = position == itemCount - 1

        if (isLastItem && !item.isUser && activeTurnMinHeight > 0) {
            holder.itemView.minimumHeight = activeTurnMinHeight
        } else {
            holder.itemView.minimumHeight = 0
        }

        holder.bind(item)
        val currentPos = holder.bindingAdapterPosition
        if (currentPos != RecyclerView.NO_POSITION && currentPos > lastAnimatedPosition) {
            lastAnimatedPosition = currentPos
            holder.runEntranceAnimation()
        }
    }

    override fun onViewRecycled(holder: MessageViewHolder) {
        super.onViewRecycled(holder)
        holder.stopThinkingAnimation()
    }

    override fun onViewDetachedFromWindow(holder: MessageViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.stopThinkingAnimation()
    }

    inner class MessageViewHolder(
        private val binding: ItemChatMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val animators = mutableListOf<android.animation.Animator>()

        fun updateContent(message: ChatMessage) {
            if (message.isUser) {
                binding.userMessageLayout.isVisible = true
                binding.aiMessageLayout.isVisible = false
                binding.userMessageText.text = MarkdownUtils.formatMarkdown(message.text)
            } else {
                binding.userMessageLayout.isVisible = false
                binding.aiMessageLayout.isVisible = true

                if (message.isThinking || message.text.isBlank()) {
                    binding.thinkingContainer.isVisible = true
                    binding.aiMessageText.isVisible = false
                    binding.feedbackContainer.isVisible = false
                    startThinkingAnimation()
                } else {
                    stopThinkingAnimation()
                    binding.thinkingContainer.isVisible = false
                    binding.aiMessageText.isVisible = true
                    binding.aiMessageText.text = MarkdownUtils.formatMarkdown(message.text)
                    binding.feedbackContainer.isVisible = true

                    val defaultColor = com.google.android.material.color.MaterialColors.getColor(
                        binding.root,
                        com.google.android.material.R.attr.colorOutline
                    )
                    val activeColor = com.google.android.material.color.MaterialColors.getColor(
                        binding.root,
                        R.attr.colorPrimary
                    )

                    when (message.feedback) {
                        "like" -> {
                            binding.btnLike.imageTintList = android.content.res.ColorStateList.valueOf(activeColor)
                            binding.btnDislike.imageTintList = android.content.res.ColorStateList.valueOf(defaultColor)
                        }
                        "dislike" -> {
                            binding.btnLike.imageTintList = android.content.res.ColorStateList.valueOf(defaultColor)
                            binding.btnDislike.imageTintList = android.content.res.ColorStateList.valueOf(activeColor)
                        }
                        else -> {
                            binding.btnLike.imageTintList = android.content.res.ColorStateList.valueOf(defaultColor)
                            binding.btnDislike.imageTintList = android.content.res.ColorStateList.valueOf(defaultColor)
                        }
                    }

                    binding.btnLike.setOnClickListener {
                        val pos = bindingAdapterPosition
                        val userQuestion = if (pos != RecyclerView.NO_POSITION) {
                            currentList.take(pos).findLast { it.isUser }?.text.orEmpty()
                        } else ""
                        onFeedbackClick?.invoke(message, true, userQuestion)
                    }

                    binding.btnDislike.setOnClickListener {
                        val pos = bindingAdapterPosition
                        val userQuestion = if (pos != RecyclerView.NO_POSITION) {
                            currentList.take(pos).findLast { it.isUser }?.text.orEmpty()
                        } else ""
                        onFeedbackClick?.invoke(message, false, userQuestion)
                    }
                }
            }
        }

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

            updateContent(message)
        }

        fun startThinkingAnimation() {
            if (animators.isNotEmpty()) return

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

        fun stopThinkingAnimation() {
            animators.forEach { it.cancel() }
            animators.clear()

            binding.dot1.translationY = 0f
            binding.dot1.alpha = 1f
            binding.dot2.translationY = 0f
            binding.dot2.alpha = 1f
            binding.dot3.translationY = 0f
            binding.dot3.alpha = 1f
        }

        fun runEntranceAnimation() {
            val targetView = if (binding.userMessageLayout.isVisible) {
                binding.userMessageLayout
            } else {
                binding.aiMessageLayout
            }

            targetView.alpha = 0f
            targetView.scaleX = 0.9f
            targetView.scaleY = 0.9f
            targetView.translationY = 20f

            targetView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return if (oldItem.id.isNotBlank() && newItem.id.isNotBlank()) {
                oldItem.id == newItem.id
            } else {
                oldItem.timestamp == newItem.timestamp && oldItem.isUser == newItem.isUser
            }
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: ChatMessage, newItem: ChatMessage): Any? {
            return if (oldItem.id == newItem.id && (oldItem.text != newItem.text || oldItem.isThinking != newItem.isThinking || oldItem.feedback != newItem.feedback)) {
                PAYLOAD_TEXT_UPDATE
            } else {
                null
            }
        }
    }
}
