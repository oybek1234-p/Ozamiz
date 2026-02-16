package uz.oybek.ozamiz.ui.feature.lessons.admin

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.lessons.LessonRepository
import uz.oybek.ozamiz.data.lessons.model.Quiz
import uz.oybek.ozamiz.data.lessons.model.Screen
import uz.oybek.ozamiz.databinding.DialogAddQuizBinding
import uz.oybek.ozamiz.databinding.DialogAddScreenBinding
import uz.oybek.ozamiz.databinding.FragmentAdminScreensListBinding
import uz.oybek.ozamiz.databinding.FragmentAdminTopicDetailBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

/**
 * Admin panel: Mavzu tafsilotlari - Screens va Quizzes tabs
 */
class AdminTopicDetailFragment : BaseFragment<FragmentAdminTopicDetailBinding>() {

    private val viewModel: AdminTopicDetailViewModel by activityViewModels()
    private var topicId: String = ""
    private var topicTitle: String = ""

    // Image picker for screens
    private var selectedImageUri: Uri? = null
    private var pendingImageCallback: ((Uri) -> Unit)? = null
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            pendingImageCallback?.invoke(it)
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAdminTopicDetailBinding {
        return FragmentAdminTopicDetailBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAdminTopicDetailBinding) {
        // Get arguments
        topicId = arguments?.getString("topicId") ?: ""
        topicTitle = arguments?.getString("topicTitle") ?: "Mavzu"
        
        viewModel.setTopicId(topicId)
        
        setupToolbar(bind)
        setupTabs(bind)
        setupFab(bind)
    }

    private fun setupToolbar(bind: FragmentAdminTopicDetailBinding) {
        bind.toolbar.title = topicTitle
        bind.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupTabs(bind: FragmentAdminTopicDetailBinding) {
        val pagerAdapter = TopicDetailPagerAdapter(this)
        bind.viewPager.adapter = pagerAdapter
        
        TabLayoutMediator(bind.tabLayout, bind.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Kontentlar"
                1 -> "Testlar"
                else -> ""
            }
        }.attach()
    }

    private fun setupFab(bind: FragmentAdminTopicDetailBinding) {
        bind.fab.setOnClickListener {
            when (bind.viewPager.currentItem) {
                0 -> showAddScreenDialog()
                1 -> showAddQuizDialog()
            }
        }
    }

    // =============== SCREEN DIALOGS ===============

    fun showAddScreenDialog(existingScreen: Screen? = null) {
        selectedImageUri = null
        val dialogBinding = DialogAddScreenBinding.inflate(layoutInflater)
        
        existingScreen?.let {
            dialogBinding.editTextTitle.setText(it.title)
            dialogBinding.editTextMainText.setText(it.mainText)
            dialogBinding.editTextKeyInfo.setText(it.keyInfo)
            dialogBinding.editTextOrder.setText(it.order.toString())
            if (it.imageUrl.isNotEmpty()) {
                Glide.with(this).load(it.imageUrl).into(dialogBinding.imagePreview)
            }
        }
        
        dialogBinding.imagePreview.setOnClickListener {
            pendingImageCallback = { uri ->
                Glide.with(this).load(uri).into(dialogBinding.imagePreview)
            }
            imagePickerLauncher.launch("image/*")
        }
        
        val title = if (existingScreen != null) "Kontentni tahrirlash" else "Kontent qo'shish"
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(dialogBinding.root)
            .setPositiveButton("Saqlash", null)
            .setNegativeButton("Bekor qilish", null)
            .create()
        
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val screenTitle = dialogBinding.editTextTitle.text.toString().trim()
                val mainText = dialogBinding.editTextMainText.text.toString().trim()
                val keyInfo = dialogBinding.editTextKeyInfo.text.toString().trim()
                val order = dialogBinding.editTextOrder.text.toString().toIntOrNull() ?: 0
                
                if (screenTitle.isEmpty()) {
                    dialogBinding.inputLayoutTitle.error = "Sarlavhani kiriting"
                    return@setOnClickListener
                }
                
                dialog.dismiss()
                
                viewLifecycleOwner.lifecycleScope.launch {
                    val imageUrl = selectedImageUri?.let { uri ->
                        LessonRepository.uploadImage(uri).getOrNull() 
                            ?: existingScreen?.imageUrl ?: ""
                    } ?: existingScreen?.imageUrl ?: ""
                    
                    val screen = Screen(
                        id = existingScreen?.id ?: "",
                        topicId = topicId,
                        title = screenTitle,
                        imageUrl = imageUrl,
                        mainText = mainText,
                        keyInfo = keyInfo,
                        order = order
                    )
                    
                    if (existingScreen != null) {
                        viewModel.updateScreen(screen)
                    } else {
                        viewModel.addScreen(screen)
                    }
                }
            }
        }
        
        dialog.show()
    }

    // =============== QUIZ DIALOGS ===============

    fun showAddQuizDialog(existingQuiz: Quiz? = null) {
        val dialogBinding = DialogAddQuizBinding.inflate(layoutInflater)
        val optionViews = mutableListOf<android.view.View>()
        
        // Helper to add option view
        fun addOptionView(text: String = "") {
            val optionBinding = uz.oybek.ozamiz.databinding.ItemQuizOptionBinding.inflate(
                layoutInflater, dialogBinding.optionsContainer, false
            )
            optionBinding.editTextOption.setText(text)
            optionBinding.btnRemoveOption.setOnClickListener {
                if (optionViews.size > 2) { // Minimum 2 options required
                    dialogBinding.optionsContainer.removeView(optionBinding.root)
                    optionViews.remove(optionBinding.root)
                } else {
                    Toast.makeText(requireContext(), "Kamida 2 ta variant bo'lishi kerak", Toast.LENGTH_SHORT).show()
                }
            }
            dialogBinding.optionsContainer.addView(optionBinding.root)
            optionViews.add(optionBinding.root)
        }
        
        // Add initial options (existing or 2 empty)
        if (existingQuiz != null) {
            dialogBinding.editTextQuestion.setText(existingQuiz.question)
            existingQuiz.options.forEach { addOptionView(it) }
            dialogBinding.editTextCorrectIndex.setText((existingQuiz.correctIndex + 1).toString())
        } else {
            addOptionView("")
            addOptionView("")
        }
        
        // Add option button
        dialogBinding.btnAddOption.setOnClickListener {
            addOptionView("")
        }
        
        val title = if (existingQuiz != null) "Savolni tahrirlash" else "Savol qo'shish"
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(dialogBinding.root)
            .setPositiveButton("Saqlash", null)
            .setNegativeButton("Bekor qilish", null)
            .create()
        
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val question = dialogBinding.editTextQuestion.text.toString().trim()
                val correctIndex = (dialogBinding.editTextCorrectIndex.text.toString().toIntOrNull() ?: 1) - 1
                
                // Collect options
                val options = mutableListOf<String>()
                for (view in optionViews) {
                    val editText = view.findViewById<android.widget.EditText>(R.id.editTextOption)
                    val optionText = editText.text.toString().trim()
                    if (optionText.isNotEmpty()) {
                        options.add(optionText)
                    }
                }
                
                if (question.isEmpty()) {
                    dialogBinding.inputLayoutQuestion.error = "Savolni kiriting"
                    return@setOnClickListener
                }
                
                if (options.size < 2) {
                    Toast.makeText(requireContext(), "Kamida 2 ta variant kiriting", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                if (correctIndex < 0 || correctIndex >= options.size) {
                    dialogBinding.inputLayoutCorrectIndex.error = "1-${options.size} oralig'ida"
                    return@setOnClickListener
                }
                
                dialog.dismiss()
                
                val quiz = Quiz(
                    id = existingQuiz?.id ?: "",
                    topicId = topicId,
                    question = question,
                    options = options,
                    correctIndex = correctIndex
                )
                
                if (existingQuiz != null) {
                    viewModel.updateQuiz(quiz)
                } else {
                    viewModel.addQuiz(quiz)
                }
            }
        }
        
        dialog.show()
    }

    fun showDeleteConfirmation(itemName: String, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("$itemName ni o'chirish")
            .setMessage("Haqiqatan ham bu ${itemName.lowercase()} ni o'chirmoqchimisiz?")
            .setPositiveButton("O'chirish") { _, _ -> onConfirm() }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }

    // Pager adapter for tabs
    inner class TopicDetailPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ScreensListFragment()
                1 -> QuizzesListFragment()
                else -> ScreensListFragment()
            }
        }
    }
}

/**
 * Tab 1: Screens list
 */
class ScreensListFragment : Fragment() {
    
    private var _binding: FragmentAdminScreensListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AdminTopicDetailViewModel by activityViewModels()
    private lateinit var adapter: ScreensAdapter
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: android.os.Bundle?) =
        FragmentAdminScreensListBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        adapter = ScreensAdapter(
            onEdit = { screen -> 
                (parentFragment as? AdminTopicDetailFragment)?.showAddScreenDialog(screen)
            },
            onDelete = { screen ->
                (parentFragment as? AdminTopicDetailFragment)?.showDeleteConfirmation("Kontent") {
                    viewModel.deleteScreen(screen.id)
                }
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.screensState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.emptyText.isVisible = !state.isLoading && state.screens.isEmpty()
                    adapter.submitList(state.screens)
                    
                    state.error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.clearScreensError()
                    }
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Tab 2: Quizzes list
 */
class QuizzesListFragment : Fragment() {
    
    private var _binding: FragmentAdminScreensListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AdminTopicDetailViewModel by activityViewModels()
    private lateinit var adapter: QuizzesAdapter
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: android.os.Bundle?) =
        FragmentAdminScreensListBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.emptyText.text = "Hali test yo'q"
        
        adapter = QuizzesAdapter(
            onEdit = { quiz -> 
                (parentFragment as? AdminTopicDetailFragment)?.showAddQuizDialog(quiz)
            },
            onDelete = { quiz ->
                (parentFragment as? AdminTopicDetailFragment)?.showDeleteConfirmation("Savol") {
                    viewModel.deleteQuiz(quiz.id)
                }
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.quizzesState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.emptyText.isVisible = !state.isLoading && state.quizzes.isEmpty()
                    adapter.submitList(state.quizzes)
                    
                    state.error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.clearQuizzesError()
                    }
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
