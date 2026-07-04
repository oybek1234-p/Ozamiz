package uz.oybek.ozamiz.ui.feature.ai

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.SimpleItemAnimator
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAiChatBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog
import uz.oybek.ozamiz.ui.utils.toDp

class AiChatFragment : BaseFragment<FragmentAiChatBinding>() {
    init {
        showBottomSheet = true
    }

    private val viewModel: AiChatViewModel by viewModels()
    private lateinit var chatAdapter: AiChatAdapter
    private var currentChipsQuestions: List<String>? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentAiChatBinding.inflate(inflater, container, false)

    override fun onResume() {
        super.onResume()
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
    }

    override fun onPause() {
        super.onPause()
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private var isSendButtonActive: Boolean? = null

    override fun viewCreated(bind: FragmentAiChatBinding) {
        setupRecyclerViews(bind)
        setupListeners(bind)
        setupKeyboardListener(bind)
        observeState(bind)
        handleInitialArguments()
        setupPremiumListener()

        // Boshlang'ich holatda tugma kichik va shaffofroq bo'ladi
        bind.sendButton.scaleX = 0.85f
        bind.sendButton.scaleY = 0.85f
        bind.sendButton.alpha = 0.5f
    }

    private fun setupPremiumListener() {
        setFragmentResultListener(PremiumBottomSheetDialog.REQUEST_KEY) { _, bundle ->
            val result = bundle.getString(PremiumBottomSheetDialog.RESULT_ACTION)
            if (result == "go_to_payment") {
                val price = bundle.getLong("selected_price", 29000L)
                val isYearly = bundle.getBoolean("is_yearly", false)
                val args = androidx.core.os.bundleOf("price" to price, "is_yearly" to isYearly)
                findNavController().navigate(R.id.paymentGetCheckFragment, args)
            }
        }
        setFragmentResultListener("payment_check_submitted") { _, bundle ->
            if (bundle.getBoolean("submitted")) {
                PremiumBottomSheetDialog().show(parentFragmentManager, PremiumBottomSheetDialog.TAG)
            }
        }
    }

    private fun checkPremium(action: () -> Unit) {
        if (viewModel.isUserPremium) {
            action()
        } else {
            if (viewModel.getDailyAiMessageCount() >= AiChatViewModel.FREE_DAILY_MESSAGE_LIMIT) {
                PremiumBottomSheetDialog().show(parentFragmentManager, PremiumBottomSheetDialog.TAG)
            } else {
                action()
            }
        }
    }

    private fun setupKeyboardListener(bind: FragmentAiChatBinding) {
        val bottomMarginOffset = toDp(56f, requireContext())
        ViewCompat.setOnApplyWindowInsetsListener(bind.root) { v, insets ->
            insets
        }
        ViewCompat.setWindowInsetsAnimationCallback(
            bind.root,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                    val navHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

                    val bottomPadding = if (imeHeight == 0) {
                        0
                    } else {
                        maxOf(0, imeHeight - bottomMarginOffset - navHeight)
                    }

                    binding.root.setPadding(0, 0, 0, bottomPadding)

                    return insets
                }
            }
        )
    }

    private fun handleInitialArguments() {
        val prompt = arguments?.getString("initialPrompt")
        if (!prompt.isNullOrBlank()) {
            showBottomSheet = false
            binding.toolbar.setNavigationIcon(R.drawable.arrow_back)
            binding.toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }
            if (viewModel.isUserPremium) {
                viewModel.run {
                    startNewChat()
                    sendMessage(prompt, isInitialContext = true) {}
                }
            } else {
                PremiumBottomSheetDialog().show(parentFragmentManager, PremiumBottomSheetDialog.TAG)
            }
            arguments?.remove("initialPrompt")
        } else {
            binding.toolbar.navigationIcon = null
        }
    }

    private fun setupRecyclerViews(bind: FragmentAiChatBinding) {
        chatAdapter = AiChatAdapter(
            onRecreateClick = {
                checkPremium {
                    viewModel.sendMessage("Boshqa menyu bering.")
                }
            },
            onSaveMenuClick = { text ->
                viewModel.saveMenu(text)
                com.google.android.material.snackbar.Snackbar.make(
                    bind.root,
                    "Menyu muvaffaqiyatli saqlandi!",
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).show()
            }
        )
        bind.chatRecyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            // Lag bo'lmasligi uchun faqat update (change) animatsiyasi o'chiriladi
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
    }

    private fun updateSendButtonState(bind: FragmentAiChatBinding, hasText: Boolean, isLoading: Boolean) {
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

        bind.sendButton.setOnClickListener {
            checkPremium {
                val text = bind.messageEditText.text.toString()
                if (text.isNotBlank()) {
                    viewModel.sendMessage(text, done = {})
                    bind.messageEditText.text.clear()

                    bind.chatRecyclerView.postDelayed({
                        val smoothScroller = object : LinearSmoothScroller(requireContext()) {
                            override fun getVerticalSnapPreference(): Int {
                                return SNAP_TO_START
                            }
                        }

                        val lastPos = chatAdapter.itemCount - 1
                        if (lastPos >= 0) {
                            smoothScroller.targetPosition = lastPos
                            bind.chatRecyclerView.layoutManager?.startSmoothScroll(smoothScroller)
                        }
                    }, 100)
                }
            }
        }

        bind.btnNewChat.setOnClickListener {
            checkPremium {
                viewModel.startNewChat()
            }
        }
    }

    private fun observeState(bind: FragmentAiChatBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.messages.collect { messages ->
                        val layoutManager = bind.chatRecyclerView.layoutManager as? LinearLayoutManager
                        val lastVisibleItem = layoutManager?.findLastVisibleItemPosition() ?: -1
                        val itemCount = chatAdapter.itemCount
                        val shouldScrollToBottom = lastVisibleItem >= itemCount - 3 || lastVisibleItem == -1

                        chatAdapter.submitList(messages) {
                            if (messages.isNotEmpty() && shouldScrollToBottom) {
                                val lastIndex = messages.size - 1
                                val lastViewHolder = bind.chatRecyclerView.findViewHolderForAdapterPosition(lastIndex)
                                if (lastViewHolder != null) {
                                    val topOfLastItem = lastViewHolder.itemView.top
                                    val minTopMargin = toDp(16f, requireContext())
                                    if (topOfLastItem > minTopMargin) {
                                        bind.chatRecyclerView.smoothScrollToPosition(lastIndex)
                                    }
                                } else {
                                    bind.chatRecyclerView.smoothScrollToPosition(lastIndex)
                                }
                            }
                        }
                        bind.chatRecyclerView.isVisible = messages.isNotEmpty()
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        val hasText = !bind.messageEditText.text.isNullOrBlank()
                        updateSendButtonState(bind, hasText, isLoading)
                    }
                }

                launch {
                    combine(
                        viewModel.showQuestions,
                        viewModel.suggestedQuestions
                    ) { show, questions ->
                        show to questions
                    }.collect { (show, questions) ->
                        bind.quickQuestionsScroll.isVisible = show && questions.isNotEmpty()
                        if (show) {
                            if (questions != currentChipsQuestions) {
                                currentChipsQuestions = questions
                                bind.chipGroupQuestions.removeAllViews()
                                questions.forEach { question ->
                                    val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                                        text = question
                                        isClickable = true
                                        isFocusable = true
                                        setOnClickListener {
                                            checkPremium {
                                                viewModel.sendMessage(question)
                                                bind.chatRecyclerView.postDelayed({
                                                    val smoothScroller = object : LinearSmoothScroller(requireContext()) {
                                                        override fun getVerticalSnapPreference(): Int {
                                                            return SNAP_TO_START
                                                        }
                                                    }
                                                    val lastPos = chatAdapter.itemCount - 1
                                                    if (lastPos >= 0) {
                                                        smoothScroller.targetPosition = lastPos
                                                        bind.chatRecyclerView.layoutManager?.startSmoothScroll(smoothScroller)
                                                    }
                                                }, 100)
                                            }
                                        }
                                    }
                                    bind.chipGroupQuestions.addView(chip)
                                }
                            }
                        } else {
                            if (currentChipsQuestions != null) {
                                currentChipsQuestions = null
                                bind.chipGroupQuestions.removeAllViews()
                            }
                        }
                    }
                }
            }
        }
    }
}