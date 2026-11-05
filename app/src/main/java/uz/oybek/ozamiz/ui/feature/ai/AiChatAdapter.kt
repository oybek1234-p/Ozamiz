package uz.oybek.ozamiz.ui.feature.ai

import android.animation.Animator
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.util.Property
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.common.util.DateFormatter
import uz.oybek.ozamiz.data.ai.aiChat.ChatMessage
import uz.oybek.ozamiz.data.dietolog.model.AnalyzedFoodItem
import uz.oybek.ozamiz.data.dietolog.model.AnalyzedFoodPayload
import uz.oybek.ozamiz.data.dietolog.model.DietologActionItem
import uz.oybek.ozamiz.data.tracker.model.MealType
import uz.oybek.ozamiz.databinding.ItemChatMessageBinding
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter
import uz.oybek.ozamiz.ui.modelUtils.getDisplayName
import uz.oybek.ozamiz.ui.utils.SpannableChatFormatter
import uz.oybek.ozamiz.ui.utils.VaznimColors

class AiChatAdapter(
    var onFeedbackClick: ((message: ChatMessage, isLiked: Boolean, userQuestion: String) -> Unit)? = null,
    var onConfirmFoodClick: ((message: ChatMessage, payload: AnalyzedFoodPayload) -> Unit)? = null,
    var onActionClick: ((message: ChatMessage, actionItem: DietologActionItem) -> Unit)? = null
) : ListAdapter<ChatMessage, AiChatAdapter.MessageViewHolder>(DiffCallback) {

    companion object {
        const val FEEDBACK_LIKE = "like"
        const val FEEDBACK_DISLIKE = "dislike"
    }

    private val animatedItemIds = mutableSetOf<Long>()
    private val feedbackMap = mutableMapOf<String, String?>()
    private var initialHistoryLoaded = false

    override fun onCurrentListChanged(
        previousList: MutableList<ChatMessage>,
        currentList: MutableList<ChatMessage>
    ) {
        super.onCurrentListChanged(previousList, currentList)
        if (currentList.isEmpty()) {
            animatedItemIds.clear()
            feedbackMap.clear()
            initialHistoryLoaded = false
            return
        }
        if (!initialHistoryLoaded) {
            val now = System.currentTimeMillis()
            currentList.forEach { msg ->
                if (!msg.isThinking && (now - msg.timestamp > 5000L)) {
                    animatedItemIds.add(getItemKey(msg))
                }
            }
            initialHistoryLoaded = true
        }
    }

    private fun getItemKey(item: ChatMessage): Long {
        return if (item.id.isNotBlank()) {
            item.id.hashCode().toLong()
        } else {
            item.timestamp xor (if (item.isUser) 1L else 2L)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder =
        MessageViewHolder(
            ItemChatMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.minimumHeight = 0
        holder.bind(item)

        if (!item.isThinking) {
            animatedItemIds.remove("thinking_placeholder".hashCode().toLong())
        }

        val key = getItemKey(item)
        if (animatedItemIds.add(key)) {
            holder.runEntranceAnimation(item)
        } else {
            holder.resetTransform()
        }
    }

    override fun onViewRecycled(holder: MessageViewHolder) {
        super.onViewRecycled(holder)
        holder.onRecycled()
    }

    override fun onViewDetachedFromWindow(holder: MessageViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.stopThinkingAnimation()
    }

    inner class MessageViewHolder(
        private val binding: ItemChatMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var boundMessage: ChatMessage? = null
        private val defTint by lazy { ColorStateList.valueOf(VaznimColors.getOutline(itemView.context)) }
        private val actTint by lazy { ColorStateList.valueOf(VaznimColors.getPrimary(itemView.context)) }
        private val dots by lazy { with(binding) { listOf(dot1, dot2, dot3) } }
        private val animators by lazy { createThinkingAnimators() }

        init {
            with(binding) {
                btnLike.setOnClickListener {
                    val msg = boundMessage ?: return@setOnClickListener
                    val msgKey = msg.id.ifBlank { msg.timestamp.toString() }
                    val current = feedbackMap[msgKey] ?: msg.feedback
                    val newFeedback = if (current == FEEDBACK_LIKE) null else FEEDBACK_LIKE
                    feedbackMap[msgKey] = newFeedback

                    it.animate().scaleX(1.3f).scaleY(1.3f).setDuration(120).withEndAction {
                        it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).setInterpolator(OvershootInterpolator(2f)).start()
                    }.start()

                    bindFeedbackDirect(newFeedback)
                    handleFeedback(newFeedback == FEEDBACK_LIKE)
                }
                btnDislike.setOnClickListener {
                    val msg = boundMessage ?: return@setOnClickListener
                    val msgKey = msg.id.ifBlank { msg.timestamp.toString() }
                    val current = feedbackMap[msgKey] ?: msg.feedback
                    val newFeedback = if (current == FEEDBACK_DISLIKE) null else FEEDBACK_DISLIKE
                    feedbackMap[msgKey] = newFeedback

                    it.animate().scaleX(1.3f).scaleY(1.3f).setDuration(120).withEndAction {
                        it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).setInterpolator(OvershootInterpolator(2f)).start()
                    }.start()

                    bindFeedbackDirect(newFeedback)
                    handleFeedback(false)
                }
                btnAddToTracker.setOnClickListener {
                    boundMessage?.let { msg ->
                        msg.analyzedFood?.let { food -> animateConfirmTracker(msg, food) }
                    }
                }
            }
        }

        private fun handleFeedback(isLiked: Boolean) {
            boundMessage?.let { msg ->
                val pos = bindingAdapterPosition
                val userQ = if (pos != RecyclerView.NO_POSITION) {
                    currentList.take(pos).findLast { it.isUser }?.text.orEmpty()
                } else {
                    ""
                }
                onFeedbackClick?.invoke(msg, isLiked, userQ)
            }
        }

        fun bind(message: ChatMessage) = with(binding) {
            boundMessage = message
            userMessageLayout.animate().cancel()
            aiMessageLayout.animate().cancel()
            userMessageLayout.resetTransform()
            aiMessageLayout.resetTransform()

            userMessageLayout.isVisible = message.isUser
            aiMessageLayout.isVisible = !message.isUser

            if (message.isUser) {
                setTextWithMeasure(userMessageText, message.text)
            } else {
                val isThinking = message.isThinking || message.text.isBlank()
                thinkingContainer.isVisible = isThinking
                aiMessageText.isVisible = !isThinking
                feedbackContainer.isVisible = !isThinking

                if (isThinking) {
                    startThinkingAnimation()
                } else {
                    stopThinkingAnimation()
                    setTextWithMeasure(aiMessageText, message.text)
                    bindFoodPayload(message)
                    bindFeedback(message)
                }
            }
        }

        private fun setTextWithMeasure(textView: TextView, text: String) {
            if (textView.tag == text) return
            textView.tag = text
            SpannableChatFormatter.setMarkdown(textView, text)
        }

        private fun bindFoodPayload(message: ChatMessage) = with(binding) {
            val food = message.analyzedFood
            cardFoodAnalysis.isVisible = food != null && food.items.isNotEmpty()
            if (food == null || food.items.isEmpty()) return@with

            val ctx = itemView.context
            tvFoodItemsTitle.text = food.items.joinToString(", ") { it.name }

            val timeStr = DateFormatter.format(DateFormatter.currentTime(), DateFormatter.PATTERN_HOUR_MINUTE)
            val meal = MealType.fromIdOrNull(food.mealType) ?: MealType.getMealByTime()
            tvFoodMealTime.text = ctx.getString(R.string.meal_date_format, meal.getDisplayName(ctx), timeStr)

            bindFoodItemsList(food.items)
            tvFoodCaloriesBadge.text = NutritionFormatter.kcal(food.totalCalories)

            val macroRes = R.string.dietolog_macro_gram_format
            tvProteinValue.text = ctx.getString(macroRes, food.totalProtein.toInt())
            tvFatValue.text = ctx.getString(macroRes, food.totalFat.toInt())
            tvCarbsValue.text = ctx.getString(macroRes, food.totalCarbs.toInt())

            btnAddToTracker.isVisible = !message.isAddedToTracker
            layoutAddedStatus.isVisible = message.isAddedToTracker

            if (message.isAddedToTracker) {
                layoutAddedStatus.resetTransform()
            } else {
                btnAddToTracker.resetTransform()
                btnAddToTracker.isEnabled = true
                btnAddToTracker.text = ctx.getString(R.string.dietolog_action_add_to_tracker)
            }
            bindActionsList(message, message.actions)
        }

        private fun animateConfirmTracker(message: ChatMessage, food: AnalyzedFoodPayload) = with(binding) {
            btnAddToTracker.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(150)
                .withEndAction {
                    btnAddToTracker.isVisible = false
                    layoutAddedStatus.apply {
                        alpha = 0f
                        scaleX = 0.85f
                        scaleY = 0.85f
                        isVisible = true
                        animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(250)
                            .setInterpolator(OvershootInterpolator(1.2f))
                            .start()
                    }
                }
                .start()
            onConfirmFoodClick?.invoke(message, food)
        }

        private fun bindFoodItemsList(items: List<AnalyzedFoodItem>) = with(binding) {
            layoutFoodItemsList.isVisible = items.size > 1
            if (items.size <= 1) return@with

            val ctx = itemView.context
            while (layoutFoodItemsList.childCount > items.size) {
                layoutFoodItemsList.removeViewAt(layoutFoodItemsList.childCount - 1)
            }

            val density = ctx.resources.displayMetrics.density
            val pad = (4 * density).toInt()
            val dotSize = (6 * density).toInt()

            items.forEachIndexed { i, item ->
                val row = if (i < layoutFoodItemsList.childCount) {
                    layoutFoodItemsList.getChildAt(i) as LinearLayout
                } else {
                    createFoodItemRow(ctx, dotSize, pad, density).also { layoutFoodItemsList.addView(it) }
                }
                (row.getChildAt(1) as TextView).text = item.name
                (row.getChildAt(2) as TextView).text = NutritionFormatter.kcal(item.calories)
            }
        }

        private fun createFoodItemRow(
            ctx: Context,
            dotSize: Int,
            pad: Int,
            density: Float
        ): LinearLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, pad, 0, pad)

            addView(View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                    marginEnd = (8 * density).toInt()
                }
                background = ContextCompat.getDrawable(ctx, R.drawable.bg_macro_icon)
                backgroundTintList = actTint
            })
            addView(TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(VaznimColors.getOnSurface(ctx))
                typeface = ResourcesCompat.getFont(ctx, R.font.roboto_regular)
            })
            addView(TextView(ctx).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(VaznimColors.getOutline(ctx))
                typeface = ResourcesCompat.getFont(ctx, R.font.roboto_medium)
            })
        }

        private fun bindActionsList(message: ChatMessage, actions: List<DietologActionItem>) = with(binding) {
            layoutActionsContainer.isVisible = actions.isNotEmpty()
            if (actions.isEmpty()) return@with

            val ctx = itemView.context
            while (layoutActionsContainer.childCount > actions.size) {
                layoutActionsContainer.removeViewAt(layoutActionsContainer.childCount - 1)
            }
            val radius = (12 * ctx.resources.displayMetrics.density).toInt()

            actions.forEachIndexed { i, action ->
                val button = if (i < layoutActionsContainer.childCount) {
                    layoutActionsContainer.getChildAt(i) as MaterialButton
                } else {
                    MaterialButton(
                        ctx,
                        null,
                        com.google.android.material.R.attr.materialButtonOutlinedStyle
                    ).apply {
                        cornerRadius = radius
                        layoutActionsContainer.addView(this)
                    }
                }
                button.text = action.text
                button.isEnabled = !action.isCompleted
                button.setOnClickListener { onActionClick?.invoke(message, action) }
            }
        }

        private fun bindFeedbackDirect(feedback: String?) = with(binding) {
            btnLike.imageTintList = if (feedback == FEEDBACK_LIKE) actTint else defTint
            btnDislike.imageTintList = if (feedback == FEEDBACK_DISLIKE) actTint else defTint
        }

        private fun bindFeedback(message: ChatMessage) {
            val msgKey = message.id.ifBlank { message.timestamp.toString() }
            val feedback = feedbackMap[msgKey] ?: message.feedback
            bindFeedbackDirect(feedback)
        }

        private fun createThinkingAnimators(): List<Animator> {
            val totalDuration = 1200L
            return dots.mapIndexed { index, dot ->
                val delayFraction = index * 0.16f
                val peakFraction = delayFraction + 0.16f
                val endFraction = delayFraction + 0.32f

                val kfTrans0 = Keyframe.ofFloat(0f, 0f)
                val kfTransStart = Keyframe.ofFloat(delayFraction, 0f)
                val kfTransPeak = Keyframe.ofFloat(peakFraction, -8f)
                val kfTransEnd = Keyframe.ofFloat(endFraction, 0f)
                val kfTrans1 = Keyframe.ofFloat(1f, 0f)

                val kfAlpha0 = Keyframe.ofFloat(0f, 0.35f)
                val kfAlphaStart = Keyframe.ofFloat(delayFraction, 0.35f)
                val kfAlphaPeak = Keyframe.ofFloat(peakFraction, 1.0f)
                val kfAlphaEnd = Keyframe.ofFloat(endFraction, 0.35f)
                val kfAlpha1 = Keyframe.ofFloat(1f, 0.35f)

                val pvhTrans = PropertyValuesHolder.ofKeyframe(
                    View.TRANSLATION_Y,
                    kfTrans0, kfTransStart, kfTransPeak, kfTransEnd, kfTrans1
                )
                val pvhAlpha = PropertyValuesHolder.ofKeyframe(
                    View.ALPHA,
                    kfAlpha0, kfAlphaStart, kfAlphaPeak, kfAlphaEnd, kfAlpha1
                )

                ObjectAnimator.ofPropertyValuesHolder(dot, pvhTrans, pvhAlpha).apply {
                    duration = totalDuration
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.RESTART
                    interpolator = AccelerateDecelerateInterpolator()
                }
            }
        }

        fun startThinkingAnimation() {
            animators.forEach { if (!it.isStarted) it.start() }
        }

        fun stopThinkingAnimation() {
            if (animators.any { it.isStarted }) {
                animators.forEach { it.cancel() }
                dots.forEach { dot ->
                    dot.translationY = 0f
                    dot.alpha = 1f
                }
            }
        }

        fun runEntranceAnimation(item: ChatMessage) {
            if (item.isUser) {
                val target = binding.userMessageLayout
                target.alpha = 0f
                target.translationY = 20f
                target.scaleX = 0.96f
                target.scaleY = 0.96f

                val duration = 240L
                val interpolator = DecelerateInterpolator(1.8f)

                target.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(duration)
                    .setInterpolator(interpolator)
                    .start()
            } else {
                binding.aiMessageLayout.resetTransform()

                if (item.isThinking) {
                    binding.thinkingContainer.alpha = 0f
                    binding.thinkingContainer.animate()
                        .alpha(1f)
                        .setDuration(180L)
                        .start()
                } else {
                    binding.aiMessageText.alpha = 0f
                    binding.aiMessageText.animate()
                        .alpha(1f)
                        .setDuration(220L)
                        .setInterpolator(DecelerateInterpolator())
                        .start()

                    if (binding.feedbackContainer.isVisible) {
                        binding.feedbackContainer.alpha = 0f
                        binding.feedbackContainer.animate()
                            .alpha(1f)
                            .setDuration(240L)
                            .start()
                    }

                    if (binding.cardFoodAnalysis.isVisible) {
                        binding.cardFoodAnalysis.alpha = 0f
                        binding.cardFoodAnalysis.scaleX = 0.96f
                        binding.cardFoodAnalysis.scaleY = 0.96f
                        binding.cardFoodAnalysis.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(280L)
                            .setInterpolator(OvershootInterpolator(1.2f))
                            .start()
                    }
                }
            }
        }

        fun resetTransform() {
            binding.userMessageLayout.resetTransform()
            binding.aiMessageLayout.resetTransform()
            binding.cardFoodAnalysis.resetTransform()
        }

        fun onRecycled() {
            binding.userMessageLayout.animate().cancel()
            binding.aiMessageLayout.animate().cancel()
            binding.cardFoodAnalysis.animate().cancel()
            resetTransform()
            stopThinkingAnimation()
        }

        private fun View.resetTransform() {
            alpha = 1f
            scaleX = 1f
            scaleY = 1f
            translationY = 0f
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(old: ChatMessage, new: ChatMessage): Boolean {
            if (old.id == new.id && old.id.isNotBlank()) return true
            if (old.id == "thinking_placeholder" && !new.isUser) return true
            if (new.id == "thinking_placeholder" && !old.isUser) return true
            return old.timestamp == new.timestamp && old.isUser == new.isUser
        }

        override fun areContentsTheSame(old: ChatMessage, new: ChatMessage) = old == new
    }
}
