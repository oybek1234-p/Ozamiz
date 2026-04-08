package uz.oybek.ozamiz.ui.feature.ai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAiChatBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog
import uz.oybek.ozamiz.ui.feature.premium.isPremiumOrTrial
import uz.oybek.ozamiz.data.user.UserRepository
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController

class AiChatFragment : BaseFragment<FragmentAiChatBinding>() {

    private val viewModel: AiChatViewModel by viewModels()
    private lateinit var chatAdapter: AiChatAdapter
    private lateinit var questionAdapter: SuggestedQuestionAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentAiChatBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentAiChatBinding) {
        setupRecyclerViews(bind)
        setupListeners(bind)
        setupKeyboardListener(bind)
        observeState(bind)
        setupRecyclerViews(bind)
        setupListeners(bind)
        setupKeyboardListener(bind)
        observeState(bind)
        handleInitialArguments()
        setupPremiumListener()
        checkPremiumOnEntry()
    }

    private fun setupPremiumListener() {
        setFragmentResultListener(PremiumBottomSheetDialog.REQUEST_KEY) { _, bundle ->
            val result = bundle.getString(PremiumBottomSheetDialog.RESULT_ACTION)
            if (result == "go_to_payment") {
                findNavController().navigate(R.id.paymentGetCheckFragment)
            }
        }
        setFragmentResultListener("payment_check_submitted") { _, bundle ->
            if (bundle.getBoolean("submitted")) {
                PremiumBottomSheetDialog().show(parentFragmentManager, PremiumBottomSheetDialog.TAG)
            }
        }
    }

    private fun checkPremiumOnEntry() {
        if (!UserRepository.user.isPremiumOrTrial()) {
            PremiumBottomSheetDialog().show(parentFragmentManager, PremiumBottomSheetDialog.TAG)
        }
    }

    private fun checkPremium(action: () -> Unit) {
        if (UserRepository.user.isPremiumOrTrial()) {
            action()
        } else {
            PremiumBottomSheetDialog().show(parentFragmentManager, PremiumBottomSheetDialog.TAG)
        }
    }

    private fun setupKeyboardListener(bind: FragmentAiChatBinding) {
        val root = bind.root

        root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = root.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            // If keypad height is more than 15% of the screen height, keyboard is visible
            val isKeyboardVisible = keypadHeight > screenHeight * 0.15
            mainActivity()?.showBottomSheet(!isKeyboardVisible, false)
        }
    }

    private fun handleInitialArguments() {
        val prompt = arguments?.getString("initialPrompt")
        if (!prompt.isNullOrBlank()) {
            viewModel.sendMessage(prompt, isInitialContext = true)
        }
    }

    private fun setupRecyclerViews(bind: FragmentAiChatBinding) {
        chatAdapter = AiChatAdapter()
        bind.chatRecyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        questionAdapter = SuggestedQuestionAdapter { question ->
            checkPremium { viewModel.sendMessage(question) }
        }
        bind.suggestedQuestionsRecyclerView.adapter = questionAdapter

        bind.greetingTitle.text = getString(R.string.ai_chat_greeting, viewModel.userName)
    }

    private fun setupListeners(bind: FragmentAiChatBinding) {
        bind.toolbar.setNavigationOnClickListener { closeFragment() }

        bind.sendButton.setOnClickListener {
            checkPremium {
                val text = bind.messageEditText.text.toString()
                if (text.isNotBlank()) {
                    viewModel.sendMessage(text)
                    bind.messageEditText.text.clear()
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
                    var lastUserMsgTimestamp = 0L
                    viewModel.messages.collect { messages ->
                        chatAdapter.submitList(messages) {
                            val lastUserMsg = messages.lastOrNull { it.isUser }
                            if (lastUserMsg != null && lastUserMsg.timestamp > lastUserMsgTimestamp) {
                                lastUserMsgTimestamp = lastUserMsg.timestamp
                                val index = messages.indexOf(lastUserMsg)
                                if (index != -1) {
                                    val layoutManager =
                                        bind.chatRecyclerView.layoutManager as? LinearLayoutManager
                                    val scrollRunnable = Runnable {
                                        layoutManager?.scrollToPositionWithOffset(index, 0)
                                    }
                                    bind.chatRecyclerView.post(scrollRunnable)
                                }
                            }
                        }
                        // Toggle empty state visibility based on messages
                        val isEmpty = messages.isEmpty()
                        bind.emptyStateContainer.isVisible = isEmpty
                        bind.chatRecyclerView.isVisible = !isEmpty
                    }
                }

                launch {
                    viewModel.suggestedQuestions.collect { questions ->
                        questionAdapter.submitList(questions)
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        bind.sendButton.isEnabled = !isLoading
                    }
                }
                launch {
                    viewModel.showQuestions.collect { show ->
                    }
                }
            }
        }
    }
}
