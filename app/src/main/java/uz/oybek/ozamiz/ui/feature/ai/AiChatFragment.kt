package uz.oybek.ozamiz.ui.feature.ai

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.SimpleItemAnimator
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.utils.logMessage
import uz.oybek.ozamiz.databinding.FragmentAiChatBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import uz.oybek.ozamiz.ui.utils.toDp

class AiChatFragment : BaseFragment<FragmentAiChatBinding>() {
    init {
        showBottomSheet = true
    }

    private val viewModel: AiChatViewModel by viewModels()
    private lateinit var chatAdapter: AiChatAdapter
    private var isSendButtonActive: Boolean? = null
    private var shouldAnchorNewQuestion = false
    private var capturedParentHeight: Int = 0

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentAiChatBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentAiChatBinding) {
        setupRecyclerView(bind)
        setupListeners(bind)
        handleInitialArguments()
        setupKeyboardListener(bind)
        observeState(bind)

        bind.sendButton.scaleX = 0.85f
        bind.sendButton.scaleY = 0.85f
        bind.sendButton.alpha = 0.5f

        focusMessageInput(bind)
    }

    private fun setupRecyclerView(bind: FragmentAiChatBinding) {
        chatAdapter = AiChatAdapter().apply {
            onFeedbackClick = { message, isLiked, userQuestion ->
                viewModel.onFeedbackClick(message, isLiked, userQuestion)
                val toastText = if (isLiked) "Fikringiz uchun rahmat!" else "Fikringiz qabul qilindi"
                android.widget.Toast.makeText(requireContext(), toastText, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        bind.chatRecyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = false
            }
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
    }

    private fun focusMessageInput(bind: FragmentAiChatBinding) {
        bind.messageEditText.post {
            if (isAdded) {
                bind.messageEditText.requestFocus()
                val imm =
                    requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.showSoftInput(
                    bind.messageEditText,
                    android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshLimitState()
    }

    private fun showChatLimitReachedDialog() {
        val act = activity ?: return
        if (act.isFinishing || act.isDestroyed) return
        val isPremium = user.isPremium()

        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(act)
        val limitBinding = uz.oybek.ozamiz.databinding.BottomSheetLimitReachedBinding.inflate(act.layoutInflater)
        dialog.setContentView(limitBinding.root)

        limitBinding.textLimitTitle.text = getString(uz.oybek.ozamiz.R.string.ai_chat_limit_title)
        limitBinding.textLimitMessage.text = if (isPremium) {
            getString(uz.oybek.ozamiz.R.string.ai_chat_limit_msg_premium)
        } else {
            getString(uz.oybek.ozamiz.R.string.ai_chat_limit_msg_free)
        }

        limitBinding.btnManualSearch.isVisible = false
        limitBinding.btnBuyPremium.isVisible = !isPremium
        limitBinding.btnBuyPremium.setOnClickListener {
            try {
                if (dialog.isShowing) dialog.dismiss()
            } catch (_: Exception) {}
            val navHostFragment = act.supportFragmentManager.findFragmentById(uz.oybek.ozamiz.R.id.container) as? androidx.navigation.fragment.NavHostFragment
            try {
                navHostFragment?.navController?.navigate(uz.oybek.ozamiz.R.id.premiumFragment)
            } catch (_: Exception) {}
        }

        if (!act.isFinishing && !act.isDestroyed) {
            dialog.show()
        }
    }

    private fun checkCanSend(action: () -> Unit) {
        if (viewModel.canSendMessage()) {
            action()
        } else {
            showChatLimitReachedDialog()
        }
    }

    private fun setupKeyboardListener(bind: FragmentAiChatBinding) {
        var isImeAnimationRunning = false

        val applyPadding = { insets: WindowInsetsCompat ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomMarginOffset = if (showBottomSheet) toDp(56f, requireContext()) else 0
            val bottomPadding = if (imeInsets.bottom == 0) {
                0
            } else {
                maxOf(0, imeInsets.bottom - bottomMarginOffset - systemBars.bottom)
            }
            logMessage("KeyboardInsets -> ime.bottom: ${imeInsets.bottom}, systemBars.bottom: ${systemBars.bottom}, bottomMarginOffset: $bottomMarginOffset, final bottomPadding: $bottomPadding", tag = "AiChatKeyboard")
            bind.root.setPadding(0, 0, 0, bottomPadding)
        }

        ViewCompat.setOnApplyWindowInsetsListener(bind.root) { _, insets ->
            if (!isImeAnimationRunning) {
                applyPadding(insets)
            }
            insets
        }

        ViewCompat.setWindowInsetsAnimationCallback(
            bind.root,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                        isImeAnimationRunning = true
                    }
                    super.onPrepare(animation)
                }

                override fun onStart(
                    animation: WindowInsetsAnimationCompat,
                    bounds: WindowInsetsAnimationCompat.BoundsCompat
                ): WindowInsetsAnimationCompat.BoundsCompat {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                        isImeAnimationRunning = true
                    }
                    return super.onStart(animation, bounds)
                }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    applyPadding(insets)
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    super.onEnd(animation)
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                        isImeAnimationRunning = false
                    }
                }
            }
        )
    }

    private fun handleInitialArguments() {
        val prompt = arguments?.getString("initialPrompt")
        val showBackButton = arguments?.getBoolean("showBackButton", false) ?: false
        if (!prompt.isNullOrBlank() || showBackButton) {
            showBottomSheet = false
            binding.toolbar.setNavigationIcon(R.drawable.arrow_back)
            binding.toolbar.setNavigationOnClickListener {
                popBackStack()
            }
            if (!prompt.isNullOrBlank()) {
                val visibleHeight = binding.chatRecyclerView.height
                chatAdapter.activeTurnMinHeight = maxOf(0, visibleHeight - toDp(60f, requireContext()))
                shouldAnchorNewQuestion = true
                if (viewModel.canSendMessage()) {
                    viewModel.sendMessage(prompt) {}
                } else {
                    showChatLimitReachedDialog()
                }
                arguments?.remove("initialPrompt")
            }
        } else {
            binding.toolbar.navigationIcon = null
        }
    }

    private fun updateSendButtonState(
        bind: FragmentAiChatBinding,
        hasText: Boolean,
        isLoading: Boolean
    ) {
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

        if (shouldBeActive) {
            bind.sendButton.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .rotation(360f)
                .setDuration(250)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.4f))
                .start()
        } else {
            val targetScale = if (isLoading) 0.65f else 0.85f
            val targetAlpha = if (isLoading) 0.35f else 0.5f
            bind.sendButton.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .alpha(targetAlpha)
                .rotation(0f)
                .setDuration(200)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
                }
    }

    private fun setupListeners(bind: FragmentAiChatBinding) {
        bind.messageEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                updateSendButtonState(bind, hasText, viewModel.isLoading.value)
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        bind.messageEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                bind.sendButton.performClick()
                true
            } else {
                false
            }
        }

        bind.sendButton.setOnClickListener {
            val text = bind.messageEditText.text.toString()
            if (text.isNotBlank() && !viewModel.isLoading.value) {
                checkCanSend {
                    // 1. Parent (RecyclerView) balandligini klaviatura yopilishidan OLDIN hisoblab olamiz
                    capturedParentHeight = bind.chatRecyclerView.height
                    shouldAnchorNewQuestion = true

                    viewModel.sendMessage(text, ignoreLimit = false, done = {})
                    bind.messageEditText.text.clear()

                    // 2. Klaviatura o'lchov olingandan keyin yopiladi
                    hideKeyboardAndClearFocus(bind)
                }
            }
        }

        bind.btnNewChat.setOnClickListener {
            viewModel.startNewChat(true)
            chatAdapter.activeTurnMinHeight = 0
            focusMessageInput(bind)
        }
    }

    private fun hideKeyboardAndClearFocus(bind: FragmentAiChatBinding) {
        bind.messageEditText.clearFocus()
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(bind.messageEditText.windowToken, 0)
    }

    private val centerGreetings = listOf(
        "Bugun sizga qanday yordam bera olaman?",
        "Ovqatlanish yoki vazn haqida istalgan savolingizni bering",
        "Savolingiz bormi? Maslahat berishga tayyorman",
        "Ratsioningiz yoki kaloriyangiz haqida gaplashamizmi?",
        "Sog'lom turmush tarzi bo'yicha qanday yordam kerak?"
    )

    private fun observeState(bind: FragmentAiChatBinding) {
        // Messages collector — mustaqil launch blokda
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.messages.collect { messages ->
                android.util.Log.d("AiChatDebug", ">>> messages.collect FIRED: size=${messages.size}")
                val isEmpty = messages.isEmpty()
                if (isEmpty && !bind.emptyStateLayout.isVisible) {
                    bind.emptyStateTitle.text = centerGreetings.random()
                }
                bind.emptyStateLayout.isVisible = isEmpty
                bind.chatRecyclerView.isVisible = !isEmpty

                val wasEmpty = chatAdapter.itemCount == 0
                chatAdapter.submitList(messages.toList()) {
                    if (shouldAnchorNewQuestion && messages.isNotEmpty()) {
                        shouldAnchorNewQuestion = false
                        val userPos = messages.indexOfLast { it.isUser }
                        val aiPos = messages.indexOfLast { !it.isUser }

                        if (userPos >= 0) {
                            bind.chatRecyclerView.post {
                                val lm = bind.chatRecyclerView.layoutManager as? LinearLayoutManager
                                val userView = lm?.findViewByPosition(userPos)
                                val userHeight = userView?.height ?: toDp(60f, requireContext())
                                val rv = bind.chatRecyclerView

                                val fullOpenRvHeight = rv.height + bind.root.paddingBottom
                                val availableHeight = maxOf(0, fullOpenRvHeight - userHeight - rv.paddingBottom - rv.paddingTop)

                                logMessage("FullOpenCalc -> rv.height: ${rv.height}, rootPaddingBottom: ${bind.root.paddingBottom}, fullOpenRvHeight: $fullOpenRvHeight, userHeight: $userHeight, availableHeight: $availableHeight", tag = "AiChatKeyboard")

                                chatAdapter.activeTurnMinHeight = availableHeight

                                if (aiPos >= 0) {
                                    chatAdapter.notifyItemChanged(aiPos)
                                }

                                bind.chatRecyclerView.post {
                                    logMessage("Scrolling -> smooth scroll to userPos: $userPos", tag = "AiChatKeyboard")
                                    val smoothScroller = object : LinearSmoothScroller(requireContext()) {
                                        override fun getVerticalSnapPreference(): Int {
                                            return SNAP_TO_START
                                        }
                                    }
                                    smoothScroller.targetPosition = userPos
                                    lm?.startSmoothScroll(smoothScroller)
                                }
                            }
                        }
                    } else if (wasEmpty && messages.isNotEmpty()) {
                        bind.chatRecyclerView.scrollToPosition(messages.size - 1)
                    }
                }
            }
        }

        // Remaining va isLoading — repeatOnLifecycle ichida
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.remainingMessages.collect { remaining ->
                        if (remaining <= 0) {
                            bind.messageEditText.hint = getString(uz.oybek.ozamiz.R.string.ai_chat_limit_hint)
                        } else {
                            bind.messageEditText.hint = "Xabar yozing..."
                        }
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        val hasText = !bind.messageEditText.text.isNullOrBlank()
                        updateSendButtonState(bind, hasText, isLoading)
                    }
                }
            }
        }
    }
}