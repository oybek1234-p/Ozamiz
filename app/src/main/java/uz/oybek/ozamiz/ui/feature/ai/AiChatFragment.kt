package uz.oybek.ozamiz.ui.feature.ai

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ai.aiChat.ChatMessage
import uz.oybek.ozamiz.data.dietolog.model.DietologActionType
import uz.oybek.ozamiz.data.tracker.model.MealType
import uz.oybek.ozamiz.databinding.BottomSheetLimitReachedBinding
import uz.oybek.ozamiz.databinding.FragmentAiChatBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.collectFlowOnStart
import uz.oybek.ozamiz.ui.utils.getArgs
import uz.oybek.ozamiz.ui.utils.hideKeyboard
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.showKeyboard
import uz.oybek.ozamiz.ui.utils.toDp
import uz.oybek.ozamiz.ui.utils.toast

@AndroidEntryPoint
class AiChatFragment : BaseFragment<FragmentAiChatBinding>() {

    private data class SuggestionItem(val icon: String, val textResId: Int)

    companion object {
        private val centerGreetingResIds = intArrayOf(
            R.string.ai_chat_greeting_1,
            R.string.ai_chat_greeting_2,
            R.string.ai_chat_greeting_3,
            R.string.ai_chat_greeting_4,
            R.string.ai_chat_greeting_5
        )

        private val suggestionPool = listOf(
            SuggestionItem("🍞", R.string.ai_chat_suggestion_pool_1),
            SuggestionItem("🧁", R.string.ai_chat_suggestion_pool_2),
            SuggestionItem("🍲", R.string.ai_chat_suggestion_pool_3),
            SuggestionItem("🥟", R.string.ai_chat_suggestion_pool_4),
            SuggestionItem("⏰", R.string.ai_chat_suggestion_pool_5),
            SuggestionItem("💧", R.string.ai_chat_suggestion_pool_6),
            SuggestionItem("🍢", R.string.ai_chat_suggestion_pool_7),
            SuggestionItem("🍎", R.string.ai_chat_suggestion_pool_8),
            SuggestionItem("⚡", R.string.ai_chat_suggestion_pool_9),
            SuggestionItem("🤔", R.string.ai_chat_suggestion_pool_10),
            SuggestionItem("🥚", R.string.ai_chat_suggestion_pool_11),
            SuggestionItem("🥗", R.string.ai_chat_suggestion_pool_12),
            SuggestionItem("☕", R.string.ai_chat_suggestion_pool_13),
            SuggestionItem("🎯", R.string.ai_chat_suggestion_pool_14),
            SuggestionItem("🥜", R.string.ai_chat_suggestion_pool_15),
            SuggestionItem("🥛", R.string.ai_chat_suggestion_pool_16),
            SuggestionItem("🏃‍♂️", R.string.ai_chat_suggestion_pool_17),
            SuggestionItem("🍔", R.string.ai_chat_suggestion_pool_18),
            SuggestionItem("📉", R.string.ai_chat_suggestion_pool_19),
            SuggestionItem("⭐", R.string.ai_chat_suggestion_pool_20)
        )
    }

    init {
        showBottomSheet = true
    }

    private val viewModel: AiChatViewModel by viewModels()
    private val args: AiChatArgs? by lazy { arguments.getArgs<AiChatArgs>() }
    private var chatAdapter: AiChatAdapter? = null
    private var isSendButtonActive: Boolean? = null
    private var pendingAnchorUserText: String? = null
    private var expectedUserMessageCount: Int = 0
    private var defaultPaddingBottom: Int = 0
    private var currentTargetPadding: Int = 0
    private var initialAnchorPadding: Int = 0
    private var paddingAnimator: ValueAnimator? = null
    private var isQuestionActive: Boolean = false
    private var activeUserQuestionPos: Int = -1

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAiChatBinding = FragmentAiChatBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentAiChatBinding) {
        setupRecyclerView(bind)
        setupListeners(bind)
        setupKeyboardListener(bind)
        handleInitialArguments(bind)
        setupEmptyStateSuggestions(bind)
        observeState(bind)
        initSendButtonVisual(bind)
        setupMessageInput(bind)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshLimitState()
    }

    private fun initSendButtonVisual(bind: FragmentAiChatBinding) {
        bind.sendButton.scaleX = 0.85f
        bind.sendButton.scaleY = 0.85f
        bind.sendButton.alpha = 0.5f
    }

    private fun setupRecyclerView(bind: FragmentAiChatBinding) {
        val adapter = AiChatAdapter(
            onFeedbackClick = { _, isLiked, _ ->
                viewModel.onFeedbackClicked(isLiked)
            },
            onConfirmFoodClick = { message, payload ->
                viewModel.confirmFoodIntake(message, payload)
            },
            onActionClick = { message, actionItem ->
                when (actionItem.actionType) {
                    DietologActionType.CONFIRM_FOOD_TRACKER -> {
                        message.analyzedFood?.let { viewModel.confirmFoodIntake(message, it) }
                    }
                    DietologActionType.OPEN_PREMIUM -> navigate(R.id.premiumFragment)
                    DietologActionType.OPEN_MENU -> navigate(R.id.menuListFragment)
                    DietologActionType.OPEN_WATER_TRACKER -> navigate(R.id.logWaterDialog)
                }
            }
        )
        chatAdapter = adapter

        bind.chatRecyclerView.apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = false
            }
            itemAnimator = null
            clipToPadding = false

            defaultPaddingBottom = paddingBottom
            currentTargetPadding = paddingBottom
            initialAnchorPadding = paddingBottom

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        isQuestionActive = false
                    }
                }
            })

            addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
                val newHeight = bottom - top
                val oldHeight = oldBottom - oldTop
                if (newHeight != oldHeight && adapter.itemCount > 0) {
                    // Agar savol aktiv bo'lsa va klaviatura ochilib-yopilsa, layoutni qayta hisoblaymiz
                    if (isQuestionActive && activeUserQuestionPos >= 0) {
                        val lm = layoutManager as? LinearLayoutManager ?: return@addOnLayoutChangeListener
                        val userView = lm.findViewByPosition(activeUserQuestionPos)
                        val userH = userView?.height ?: toDp(64f, requireContext())
                        initialAnchorPadding = maxOf(defaultPaddingBottom, newHeight - userH)
                        currentTargetPadding = initialAnchorPadding
                        paddingAnimator?.cancel()
                        setPadding(paddingLeft, paddingTop, paddingRight, initialAnchorPadding)
                        lm.scrollToPositionWithOffset(activeUserQuestionPos, 0)
                    }
                }
            }
        }
    }

    private fun animatePadding(
        targetPadding: Int,
        duration: Long = 240L,
        onUpdate: (() -> Unit)? = null,
        onEnd: (() -> Unit)? = null
    ) {
        val rv = binding?.chatRecyclerView ?: return
        currentTargetPadding = targetPadding
        paddingAnimator?.cancel()
        val startPadding = rv.paddingBottom
        if (startPadding == targetPadding) {
            onUpdate?.invoke()
            onEnd?.invoke()
            return
        }
        paddingAnimator = ValueAnimator.ofInt(startPadding, targetPadding).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { anim ->
                val value = anim.animatedValue as Int
                val r = binding?.chatRecyclerView ?: return@addUpdateListener
                r.setPadding(r.paddingLeft, r.paddingTop, r.paddingRight, value)
                onUpdate?.invoke()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd?.invoke()
                }
            })
            start()
        }
    }

    private fun setupKeyboardListener(bind: FragmentAiChatBinding) {
        ViewCompat.setWindowInsetsAnimationCallback(
            bind.root,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    super.onEnd(animation)
                    // Klaviatura ochilganda mavjud xabarlar sakramasligi uchun
                    // keraksiz avtomatik scroll olib tashlandi
                }
            }
        )
    }

    private fun scrollLastItemToBottom(smooth: Boolean = true) {
        val bind = binding ?: return
        val adapter = chatAdapter ?: return
        val lastIndex = adapter.itemCount - 1
        if (lastIndex < 0) return

        val rv = bind.chatRecyclerView
        val lm = rv.layoutManager as? LinearLayoutManager ?: return
        val lastView = lm.findViewByPosition(lastIndex)

        if (smooth) {
            if (lastView != null && rv.height > 0) {
                val desiredBottom = rv.height - rv.paddingBottom
                val currentBottom = lm.getDecoratedBottom(lastView)
                val dy = currentBottom - desiredBottom
                // Faqatgina element ko'rinmay qolgandagina scroll qiladi (taqalib tursa tegmaydi)
                if (dy > 4) {
                    rv.smoothScrollBy(0, dy, DecelerateInterpolator(1.8f), 240)
                }
            } else {
                val scroller = object : LinearSmoothScroller(rv.context) {
                    override fun getVerticalSnapPreference(): Int = SNAP_TO_END
                    override fun calculateTimeForScrolling(dx: Int): Int =
                        minOf(240, super.calculateTimeForScrolling(dx))
                }
                scroller.targetPosition = lastIndex
                lm.startSmoothScroll(scroller)
            }
        } else {
            val h = if (lastView != null) lm.getDecoratedMeasuredHeight(lastView) else 0
            if (h > 0 && rv.height > 0) {
                val targetOffset = rv.height - h - rv.paddingBottom
                lm.scrollToPositionWithOffset(lastIndex, targetOffset)
            } else {
                rv.scrollToPosition(lastIndex)
            }
        }
    }

    private fun setupMessageInput(bind: FragmentAiChatBinding) {
        // Input bosilganda yoki focus tushganda ekran siltanib ketmasligi uchun
        // to'g'ridan-to'g'ri scroll chaqirilmaydi, foydalanuvchi qayerda turgan bo'lsa o'sha joyda qoladi
        bind.messageEditText.clearFocus()
    }

    private fun showChatLimitReachedDialog(isPremium: Boolean) {
        val act = activity ?: return
        if (act.isFinishing || act.isDestroyed) return

        val dialog = BottomSheetDialog(act)
        val limitBinding = BottomSheetLimitReachedBinding.inflate(act.layoutInflater)
        dialog.setContentView(limitBinding.root)

        limitBinding.textLimitTitle.text = getString(R.string.ai_chat_limit_title)
        limitBinding.textLimitMessage.text = if (isPremium) {
            getString(R.string.ai_chat_limit_msg_premium)
        } else {
            getString(R.string.ai_chat_limit_msg_free)
        }

        limitBinding.btnManualSearch.isVisible = false
        limitBinding.btnBuyPremium.isVisible = !isPremium
        limitBinding.btnBuyPremium.setOnSafeClickListener {
            if (dialog.isShowing) dialog.dismiss()
            navigate(R.id.premiumFragment)
        }

        dialog.show()
    }

    private fun handleInitialArguments(bind: FragmentAiChatBinding) {
        val prompt = args?.initialPrompt
        val showBackButton = args?.showBackButton ?: false
        val hasBackStack = navController()?.previousBackStackEntry != null

        if (!prompt.isNullOrBlank() || showBackButton || hasBackStack) {
            showBottomSheet = false
            bind.toolbar.setNavigationIcon(R.drawable.arrow_back)
            bind.toolbar.setNavigationOnClickListener { popBackStack() }
            if (!prompt.isNullOrBlank()) {
                pendingAnchorUserText = prompt.trim()
                expectedUserMessageCount = (chatAdapter?.currentList?.count { it.isUser } ?: 0) + 1
                viewModel.onSendClicked(prompt)
            }
        } else {
            bind.toolbar.navigationIcon = null
        }
    }

    private fun updateSendButtonState(
        bind: FragmentAiChatBinding,
        hasText: Boolean,
        isLoading: Boolean
    ) {
        val uiState = viewModel.uiState.value
        val isPremiumLimitFinished = uiState.isPremium && uiState.remainingMessages <= 0
        if (isPremiumLimitFinished) {
            bind.sendButton.isEnabled = false
            bind.sendButton.scaleX = 0.85f
            bind.sendButton.scaleY = 0.85f
            bind.sendButton.alpha = 0.35f
            return
        }

        val shouldBeActive = hasText && !isLoading
        bind.sendButton.isEnabled = shouldBeActive

        if (isSendButtonActive == shouldBeActive) {
            if (!shouldBeActive) {
                val targetScale = if (isLoading) 0.65f else 0.85f
                val targetAlpha = if (isLoading) 0.35f else 0.5f
                bind.sendButton.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .alpha(targetAlpha)
                    .setDuration(200)
                    .start()
            }
            return
        }

        isSendButtonActive = shouldBeActive

        val targetScale = if (shouldBeActive) 1.0f else if (isLoading) 0.65f else 0.85f
        val targetAlpha = if (shouldBeActive) 1.0f else if (isLoading) 0.35f else 0.5f
        val targetRotation = if (shouldBeActive) 360f else 0f
        val interpolator = if (shouldBeActive) {
            OvershootInterpolator(1.4f)
        } else {
            DecelerateInterpolator()
        }

        bind.sendButton.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .alpha(targetAlpha)
            .rotation(targetRotation)
            .setDuration(if (shouldBeActive) 250 else 200)
            .setInterpolator(interpolator)
            .start()
    }

    private fun setupListeners(bind: FragmentAiChatBinding) {
        bind.messageEditText.addTextChangedListener { text ->
            updateSendButtonState(bind, !text.isNullOrBlank(), viewModel.uiState.value.isLoading)
        }

        bind.messageEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                bind.sendButton.performClick()
                true
            } else {
                false
            }
        }

        bind.sendButton.setOnSafeClickListener {
            val text = bind.messageEditText.text?.toString().orEmpty()
            sendUserQuestion(text)
        }

        bind.btnNewChat.setOnSafeClickListener {
            isQuestionActive = false
            activeUserQuestionPos = -1
            pendingAnchorUserText = null
            expectedUserMessageCount = 0
            paddingAnimator?.cancel()
            currentTargetPadding = defaultPaddingBottom
            initialAnchorPadding = defaultPaddingBottom
            bind.chatRecyclerView.setPadding(
                bind.chatRecyclerView.paddingLeft,
                bind.chatRecyclerView.paddingTop,
                bind.chatRecyclerView.paddingRight,
                defaultPaddingBottom
            )
            viewModel.startNewChat(true)
            setupEmptyStateSuggestions(bind)
            bind.messageEditText.showKeyboard()
        }
    }

    private fun observeState(bind: FragmentAiChatBinding) {
        collectFlowOnStart(
            viewModel.uiState.map { it.messages }.distinctUntilChanged()
        ) { messages ->
            renderMessages(bind, messages)
        }

        collectFlowOnStart(
            viewModel.uiState.map { it.remainingMessages to it.isPremium }.distinctUntilChanged()
        ) { (remaining, isPremium) ->
            renderLimitState(bind, remaining, isPremium)
        }

        collectFlowOnStart(
            viewModel.uiState.map { it.isLoading }.distinctUntilChanged()
        ) { isLoading ->
            renderLoading(bind, isLoading)
        }

        collectFlowOnStart(viewModel.effects, ::handleEffect)
    }

    private fun renderMessages(bind: FragmentAiChatBinding, messages: List<ChatMessage>) {
        val isEmpty = messages.isEmpty()
        if (isEmpty && !bind.emptyStateLayout.isVisible) {
            bind.emptyStateTitle.text = getString(centerGreetingResIds.random())
            setupEmptyStateSuggestions(bind)
        }
        bind.emptyStateLayout.isVisible = isEmpty
        bind.chatRecyclerView.isVisible = !isEmpty

        val adapter = chatAdapter ?: return
        val wasEmpty = adapter.itemCount == 0

        adapter.submitList(messages) {
            val pendingText = pendingAnchorUserText
            val userPos = if (!pendingText.isNullOrBlank()) {
                val userCount = messages.count { it.isUser }
                if (userCount >= expectedUserMessageCount) {
                    val matched = messages.indexOfLast { it.isUser && it.text.trim() == pendingText }
                    if (matched >= 0) matched else messages.indexOfLast { it.isUser }
                } else {
                    -1
                }
            } else {
                -1
            }

            if (userPos >= 0) {
                pendingAnchorUserText = null
                expectedUserMessageCount = 0
                isQuestionActive = true
                activeUserQuestionPos = userPos

                bind.chatRecyclerView.post {
                    val ctx = context ?: return@post
                    val rv = bind.chatRecyclerView
                    val lm = rv.layoutManager as? LinearLayoutManager ?: return@post

                    val userView = lm.findViewByPosition(userPos)
                    val userHeight = userView?.height ?: toDp(64f, ctx)

                    val rvHeight = rv.height
                    initialAnchorPadding = maxOf(defaultPaddingBottom, rvHeight - userHeight)
                    currentTargetPadding = initialAnchorPadding

                    lm.scrollToPositionWithOffset(userPos, 0)
                    animatePadding(initialAnchorPadding, duration = 240L) {
                        lm.scrollToPositionWithOffset(userPos, 0)
                    }
                }
            } else if (isQuestionActive && activeUserQuestionPos >= 0) {
                val aiPos = messages.indexOfLast { !it.isUser }
                if (aiPos > activeUserQuestionPos) {
                    bind.chatRecyclerView.post {
                        val rv = bind.chatRecyclerView
                        val lm = rv.layoutManager as? LinearLayoutManager ?: return@post
                        val aiView = lm.findViewByPosition(aiPos)

                        fun updatePadding(aiHeight: Int) {
                            if (aiHeight <= 0) return
                            val newPadding = maxOf(defaultPaddingBottom, initialAnchorPadding - aiHeight)
                            currentTargetPadding = newPadding
                            if (rv.paddingBottom != newPadding) {
                                rv.setPadding(rv.paddingLeft, rv.paddingTop, rv.paddingRight, newPadding)
                            }
                            lm.scrollToPositionWithOffset(activeUserQuestionPos, 0)
                        }

                        fun bindAiViewListener(view: View) {
                            view.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                                override fun onLayoutChange(
                                    v: View?,
                                    left: Int, top: Int, right: Int, bottom: Int,
                                    oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
                                ) {
                                    val h = bottom - top
                                    if (h > 0 && h != (oldBottom - oldTop) && isQuestionActive) {
                                        updatePadding(h)
                                    }
                                }
                            })
                            if (view.height > 0) {
                                updatePadding(view.height)
                            }
                        }

                        if (aiView != null) {
                            bindAiViewListener(aiView)
                        } else {
                            val attachListener = object : RecyclerView.OnChildAttachStateChangeListener {
                                override fun onChildViewAttachedToWindow(view: View) {
                                    if (rv.getChildAdapterPosition(view) == aiPos) {
                                        rv.removeOnChildAttachStateChangeListener(this)
                                        bindAiViewListener(view)
                                    }
                                }
                                override fun onChildViewDetachedFromWindow(view: View) {}
                            }
                            rv.addOnChildAttachStateChangeListener(attachListener)
                        }
                        lm.scrollToPositionWithOffset(activeUserQuestionPos, 0)
                    }
                }
            } else if (wasEmpty && messages.isNotEmpty()) {
                bind.chatRecyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun renderLimitState(bind: FragmentAiChatBinding, remaining: Int, isPremium: Boolean) {
        val isLimitFinished = remaining <= 0
        if (isPremium && isLimitFinished) {
            bind.messageEditText.isEnabled = false
            bind.messageEditText.setText("")
            bind.messageEditText.hint = getString(R.string.ai_chat_limit_premium_finished)
            bind.sendButton.isEnabled = false
            bind.sendButton.scaleX = 0.85f
            bind.sendButton.scaleY = 0.85f
            bind.sendButton.alpha = 0.35f
        } else {
            bind.messageEditText.isEnabled = true
            bind.messageEditText.hint = getString(R.string.ai_chat_input_hint_default)
            val hasText = !bind.messageEditText.text.isNullOrBlank()
            updateSendButtonState(bind, hasText, viewModel.uiState.value.isLoading)
        }
    }

    private fun renderLoading(bind: FragmentAiChatBinding, isLoading: Boolean) {
        val hasText = !bind.messageEditText.text.isNullOrBlank()
        updateSendButtonState(bind, hasText, isLoading)
    }

    private fun handleEffect(effect: AiChatUiEffect) {
        when (effect) {
            is AiChatUiEffect.ShowToast -> toast(effect.messageRes)
            is AiChatUiEffect.NavigateToPremium -> navigate(R.id.premiumFragment)
            is AiChatUiEffect.ShowLimitReachedDialog -> showChatLimitReachedDialog(effect.isPremium)
        }
    }

    private fun getTimeBasedMealSuggestion(): SuggestionItem {
        return when (MealType.getMealByTime()) {
            MealType.BREAKFAST -> SuggestionItem("🍳", R.string.ai_chat_suggestion_meal_breakfast)
            MealType.LUNCH -> SuggestionItem("🥗", R.string.ai_chat_suggestion_meal_lunch)
            MealType.DINNER -> SuggestionItem("🌙", R.string.ai_chat_suggestion_meal_dinner)
            MealType.SNACK -> SuggestionItem("🍵", R.string.ai_chat_suggestion_meal_snack)
        }
    }

    private fun setupEmptyStateSuggestions(bind: FragmentAiChatBinding) {
        val s1 = SuggestionItem("📊", R.string.ai_chat_suggestion_daily_analysis)
        val s2 = getTimeBasedMealSuggestion()
        val s3 = suggestionPool.shuffled().first()

        bindSuggestionCard(bind.cardSuggestion1, bind.tvSuggestionIcon1, bind.tvSuggestionText1, s1)
        bindSuggestionCard(bind.cardSuggestion2, bind.tvSuggestionIcon2, bind.tvSuggestionText2, s2)
        bindSuggestionCard(bind.cardSuggestion3, bind.tvSuggestionIcon3, bind.tvSuggestionText3, s3)

        val cards = listOf(bind.cardSuggestion1, bind.cardSuggestion2, bind.cardSuggestion3)
        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationY = 16f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setStartDelay(index * 40L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun bindSuggestionCard(
        card: View,
        iconTv: TextView,
        textTv: TextView,
        item: SuggestionItem
    ) {
        val text = getString(item.textResId)
        iconTv.text = item.icon
        textTv.text = text
        card.setOnSafeClickListener {
            card.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(90)
                .withEndAction {
                    card.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    sendUserQuestion(text)
                }
                .start()
        }
    }

    private fun sendUserQuestion(text: String) {
        val bind = binding ?: return
        val trimmed = text.trim()
        if (trimmed.isNotBlank() && !viewModel.uiState.value.isLoading) {
            if (!viewModel.canSendMessage()) {
                viewModel.onSendClicked(trimmed)
                return
            }
            pendingAnchorUserText = trimmed
            expectedUserMessageCount = (chatAdapter?.currentList?.count { it.isUser } ?: 0) + 1
            bind.sendButton.animate()
                .scaleX(0.82f)
                .scaleY(0.82f)
                .setDuration(90)
                .withEndAction {
                    bind.sendButton.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(160)
                        .setInterpolator(OvershootInterpolator(2.0f))
                        .start()
                }
                .start()

            viewModel.onSendClicked(trimmed)
            bind.messageEditText.text?.clear()
            bind.messageEditText.clearFocus()
            bind.messageEditText.hideKeyboard()
        }
    }

    override fun onDestroyView() {
        paddingAnimator?.cancel()
        paddingAnimator = null
        bindingT?.let { bind ->
            bind.sendButton.animate()?.cancel()
            bind.cardSuggestion1.animate()?.cancel()
            bind.cardSuggestion2.animate()?.cancel()
            bind.cardSuggestion3.animate()?.cancel()
            bind.chatRecyclerView.adapter = null
        }
        chatAdapter = null
        super.onDestroyView()
    }
}