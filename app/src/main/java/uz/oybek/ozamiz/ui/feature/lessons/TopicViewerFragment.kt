package uz.oybek.ozamiz.ui.feature.lessons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentTopicViewerBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class TopicViewerFragment : BaseFragment<FragmentTopicViewerBinding>() {

    private val viewModel: TopicViewerViewModel by viewModels()
    private var topicId: String = ""
    private var topicTitle: String = ""

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTopicViewerBinding {
        return FragmentTopicViewerBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentTopicViewerBinding) {
        topicId = arguments?.getString("topicId") ?: ""
        topicTitle = arguments?.getString("topicTitle") ?: ""
        
        setupToolbar(bind)
        setupButtons(bind)
        setupBackHandler()
        observeViewModel(bind)
        
        viewModel.loadScreens(topicId)
    }

    private fun setupToolbar(bind: FragmentTopicViewerBinding) {
        bind.toolbar.title = topicTitle
        bind.toolbar.setNavigationOnClickListener {
            showExitConfirmation()
        }
    }

    private fun setupButtons(bind: FragmentTopicViewerBinding) {
        bind.btnPrevious.setOnClickListener {
            viewModel.goToPrevious()
        }
        
        bind.btnNext.setOnClickListener {
            if (viewModel.isLastScreen()) {
                // Navigate to quiz
                navigateToQuiz()
            } else {
                viewModel.goToNext()
            }
        }
    }

    private fun setupBackHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmation()
                }
            }
        )
    }

    private fun observeViewModel(bind: FragmentTopicViewerBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.progressBar.isVisible = state.isLoading
                    
                    state.currentScreen?.let { screen ->
                        // Update UI with screen content
                        bind.textScreenTitle.text = screen.title
                        bind.textMainContent.text = screen.mainText
                        
                        // Key info
                        if (screen.keyInfo.isNotEmpty()) {
                            bind.cardKeyInfo.isVisible = true
                            bind.textKeyInfo.text = screen.keyInfo
                        } else {
                            bind.cardKeyInfo.isVisible = false
                        }
                        
                        if (screen.imageUrl.isNotEmpty()) {
                            bind.imageScreen.isVisible = true
                            Glide.with(this@TopicViewerFragment)
                                .load(screen.imageUrl)
                                .placeholder(R.drawable.user_photo_placeholder)
                                .centerCrop()
                                .into(bind.imageScreen)
                        } else {
                            bind.imageScreen.isVisible = false
                        }
                        
                        bind.btnPrevious.isVisible = !viewModel.isFirstScreen()
                        bind.btnNext.text = if (viewModel.isLastScreen()) "Testni boshlash" else "Keyingisi"
                        
                        bind.scrollView.scrollTo(0, 0)
                    }
                    
                    // Show empty state if no screens
                    if (!state.isLoading && state.screens.isEmpty()) {
                        bind.textScreenTitle.text = "Kontent topilmadi"
                        bind.textMainContent.text = "Bu mavzu uchun hali kontent qo'shilmagan."
                        bind.btnNext.text = "Ortga"
                        bind.btnNext.setOnClickListener { closeFragment() }
                    }
                    
                    state.error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun navigateToQuiz() {
        val args = Bundle().apply {
            putString("topicId", topicId)
            putString("topicTitle", topicTitle)
        }
        navigate(R.id.action_topicViewerFragment_to_quizFragment, args)
    }

    private fun showExitConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Chiqish")
            .setMessage("Haqiqatan ham chiqmoqchimisiz? Jarayon saqlanmaydi.")
            .setPositiveButton("Chiqish") { _, _ -> closeFragment() }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }
}
