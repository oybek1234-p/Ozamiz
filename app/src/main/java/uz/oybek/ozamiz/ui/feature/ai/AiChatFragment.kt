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

    override fun viewCreated(bind: FragmentAiChatBinding) {
        setupRecyclerViews(bind)
        setupListeners(bind)
        setupKeyboardListener(bind)
        observeState(bind)
        handleInitialArguments()
        setupPremiumListener()
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
            PremiumBottomSheetDialog().show(parentFragmentManager, PremiumBottomSheetDialog.TAG)
        }
    }

    private fun setupKeyboardListener(bind: FragmentAiChatBinding) {
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

                    var bottomPadding = maxOf(imeHeight, navHeight)
                    val multiplier = if (bottomPadding == 0) 0 else 1
                    bottomPadding -= multiplier * toDp(56f, requireContext())
                    bottomPadding = maxOf(0, bottomPadding)

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
            viewModel.run {
                startNewChat()
                sendMessage(prompt, isInitialContext = true) {}
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

    private fun setupListeners(bind: FragmentAiChatBinding) {
        bind.sendButton.setOnClickListener {
            checkPremium {
                val text = bind.messageEditText.text.toString()
                if (text.isNotBlank()) {
                    viewModel.sendMessage(text, done = {})
                    bind.messageEditText.text.clear()

                    // Yangi xabar jo'natilganda, xabarni eng tepaga (top) chiqarish
                    bind.chatRecyclerView.postDelayed({
                        val smoothScroller = object : LinearSmoothScroller(requireContext()) {
                            override fun getVerticalSnapPreference(): Int {
                                return SNAP_TO_START // Tepaga joylashtirish
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
            viewModel.startNewChat()
        }
    }

    private fun observeState(bind: FragmentAiChatBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.messages.collect { messages ->
                        chatAdapter.submitList(messages.toList()) {
                            if (messages.isNotEmpty()) {
                                bind.chatRecyclerView.scrollToPosition(messages.size - 1)
                            }
                        }
                        bind.chatRecyclerView.isVisible = messages.isNotEmpty()
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        bind.sendButton.isEnabled = !isLoading
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
                        bind.chipGroupQuestions.removeAllViews()
                        if (show) {
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
                    }
                }
            }
        }
    }
}