package uz.oybek.ozamiz.ui.feature.ai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.databinding.FragmentAiChatBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import uz.oybek.ozamiz.ui.feature.premium.isPremiumOrTrial

class AiChatFragment : BaseFragment<FragmentAiChatBinding>() {
    init {
        showBottomSheet = true
    }

    private val viewModel: AiChatViewModel by viewModels()
    private lateinit var chatAdapter: AiChatAdapter

    private var onGlobalLayoutListener: android.view.ViewTreeObserver.OnGlobalLayoutListener? = null
    private var isKeyboardVisibleLast: Boolean? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentAiChatBinding.inflate(inflater, container, false)

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
        if (UserRepository.user.isPremium()) {
            action()
        } else {
            PremiumBottomSheetDialog().show(parentFragmentManager, PremiumBottomSheetDialog.TAG)
        }
    }

    private fun setupKeyboardListener(bind: FragmentAiChatBinding) {
        ViewCompat.setOnApplyWindowInsetsListener(bind.bottomContainer) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            val isKeyboardVisible = imeInsets.bottom > 0
            mainActivity()?.showBottomSheet(!isKeyboardVisible, false)

            val params = v.layoutParams as android.view.ViewGroup.MarginLayoutParams
            params.bottomMargin = if (isKeyboardVisible) {
                imeInsets.bottom + 16.dpToPx()
            } else {
                16.dpToPx()
            }
            v.layoutParams = params
            insets
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        onGlobalLayoutListener?.let {
            binding.root.viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
        onGlobalLayoutListener = null
        super.onDestroyView()
    }

    private fun handleInitialArguments() {
        val prompt = arguments?.getString("initialPrompt")
        if (!prompt.isNullOrBlank()) {
            showBottomSheet = false
            binding.toolbar.setNavigationIcon(R.drawable.arrow_back)
            binding.toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }
            viewModel.startNewChat()
            viewModel.sendMessage(prompt, isInitialContext = true)
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
                    "Menyu muvaffaqiyatli saqlandi! Uni Home ekranida ko'rishingiz mumkin.",
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).show()
            }
        )
        bind.chatRecyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupListeners(bind: FragmentAiChatBinding) {
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
                        // Toggle chat visibility - it's always visible now since we have a greeting
                        bind.chatRecyclerView.isVisible = messages.isNotEmpty()
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        bind.sendButton.isEnabled = !isLoading
                    }
                }
            }
        }
    }
}
