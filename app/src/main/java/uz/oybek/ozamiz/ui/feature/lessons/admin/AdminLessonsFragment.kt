package uz.oybek.ozamiz.ui.feature.lessons.admin

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.lessons.LessonRepository
import uz.oybek.ozamiz.data.lessons.model.Section
import uz.oybek.ozamiz.data.lessons.model.Topic
import uz.oybek.ozamiz.databinding.DialogAddSectionBinding
import uz.oybek.ozamiz.databinding.DialogAddTopicBinding
import uz.oybek.ozamiz.databinding.FragmentAdminLessonsBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

/**
 * Admin panel uchun darsliklar boshqaruvi fragmenti
 * Bo'limlar va mavzularni qo'shish, tahrirlash, o'chirish
 */
class AdminLessonsFragment : BaseFragment<FragmentAdminLessonsBinding>() {

    private val viewModel: AdminLessonsViewModel by viewModels()
    private lateinit var adapter: AdminLessonsAdapter
    
    // Topic qo'shish/tahrirlash uchun rasm
    private var selectedImageUri: Uri? = null
    private var currentEditingTopic: Topic? = null
    private var currentSectionId: String? = null
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            // Dialog ochiq bo'lsa, rasmni ko'rsatish (callback orqali)
            pendingImageCallback?.invoke(it)
        }
    }
    
    private var pendingImageCallback: ((Uri) -> Unit)? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAdminLessonsBinding {
        return FragmentAdminLessonsBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAdminLessonsBinding) {
        setupToolbar(bind)
        setupRecyclerView(bind)
        setupFab(bind)
        observeViewModel(bind)
    }

    private fun setupToolbar(bind: FragmentAdminLessonsBinding) {
        bind.toolbar.setNavigationOnClickListener {
            closeFragment()
        }
    }

    private fun setupRecyclerView(bind: FragmentAdminLessonsBinding) {
        adapter = AdminLessonsAdapter(
            onSectionEdit = { section -> showEditSectionDialog(section) },
            onSectionDelete = { section -> showDeleteConfirmation("Bo'lim") { viewModel.deleteSection(section.id) } },
            onSectionAddTopic = { section -> showAddTopicDialog(section.id) },
            onTopicClick = { topic -> navigateToTopicDetail(topic) },
            onTopicEdit = { topic -> showEditTopicDialog(topic) },
            onTopicDelete = { topic -> showDeleteConfirmation("Mavzu") { viewModel.deleteTopic(topic.id) } }
        )
        bind.recyclerView.adapter = adapter
    }

    private fun setupFab(bind: FragmentAdminLessonsBinding) {
        bind.fabAddSection.setOnClickListener {
            showAddSectionDialog()
        }
    }

    private fun observeViewModel(bind: FragmentAdminLessonsBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.progressBar.isVisible = state.isLoading
                    bind.emptyText.isVisible = !state.isLoading && state.items.isEmpty()
                    adapter.submitList(state.items)
                    
                    state.error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    // =============== SECTION DIALOGS ===============

    private fun showAddSectionDialog() {
        val dialogBinding = DialogAddSectionBinding.inflate(layoutInflater)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Bo'lim qo'shish")
            .setView(dialogBinding.root)
            .setPositiveButton("Saqlash") { _, _ ->
                val title = dialogBinding.editTextTitle.text.toString().trim()
                val order = dialogBinding.editTextOrder.text.toString().toIntOrNull() ?: 0
                if (title.isNotEmpty()) {
                    viewModel.addSection(title, order)
                }
            }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }

    private fun showEditSectionDialog(section: Section) {
        val dialogBinding = DialogAddSectionBinding.inflate(layoutInflater)
        dialogBinding.editTextTitle.setText(section.title)
        dialogBinding.editTextOrder.setText(section.order.toString())
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Bo'limni tahrirlash")
            .setView(dialogBinding.root)
            .setPositiveButton("Saqlash") { _, _ ->
                val title = dialogBinding.editTextTitle.text.toString().trim()
                val order = dialogBinding.editTextOrder.text.toString().toIntOrNull() ?: section.order
                if (title.isNotEmpty()) {
                    viewModel.updateSection(section.copy(title = title, order = order))
                }
            }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }

    // =============== TOPIC DIALOGS ===============

    private fun showAddTopicDialog(sectionId: String) {
        currentSectionId = sectionId
        selectedImageUri = null
        
        val dialogBinding = DialogAddTopicBinding.inflate(layoutInflater)
        
        dialogBinding.imagePreview.setOnClickListener {
            pendingImageCallback = { uri ->
                Glide.with(this).load(uri).into(dialogBinding.imagePreview)
            }
            imagePickerLauncher.launch("image/*")
        }
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mavzu qo'shish")
            .setView(dialogBinding.root)
            .setPositiveButton("Saqlash", null) // Set null to override later
            .setNegativeButton("Bekor qilish", null)
            .create()
        
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = dialogBinding.editTextTitle.text.toString().trim()
                val order = dialogBinding.editTextOrder.text.toString().toIntOrNull() ?: 0
                
                if (title.isEmpty()) {
                    dialogBinding.inputLayoutTitle.error = "Nomini kiriting"
                    return@setOnClickListener
                }
                
                dialog.dismiss()
                
                // Upload image if selected, then save topic
                viewLifecycleOwner.lifecycleScope.launch {
                    val imageUrl = selectedImageUri?.let { uri ->
                        LessonRepository.uploadImage(uri).getOrNull() ?: ""
                    } ?: ""
                    
                    val topic = Topic(
                        sectionId = sectionId,
                        title = title,
                        imageUrl = imageUrl,
                        order = order,
                        isLocked = order > 0 // First topic is unlocked
                    )
                    viewModel.addTopic(topic)
                }
            }
        }
        
        dialog.show()
    }

    private fun showEditTopicDialog(topic: Topic) {
        currentEditingTopic = topic
        selectedImageUri = null
        
        val dialogBinding = DialogAddTopicBinding.inflate(layoutInflater)
        dialogBinding.editTextTitle.setText(topic.title)
        dialogBinding.editTextOrder.setText(topic.order.toString())
        
        if (topic.imageUrl.isNotEmpty()) {
            Glide.with(this).load(topic.imageUrl).into(dialogBinding.imagePreview)
        }
        
        dialogBinding.imagePreview.setOnClickListener {
            pendingImageCallback = { uri ->
                Glide.with(this).load(uri).into(dialogBinding.imagePreview)
            }
            imagePickerLauncher.launch("image/*")
        }
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mavzuni tahrirlash")
            .setView(dialogBinding.root)
            .setPositiveButton("Saqlash", null)
            .setNegativeButton("Bekor qilish", null)
            .create()
        
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = dialogBinding.editTextTitle.text.toString().trim()
                val order = dialogBinding.editTextOrder.text.toString().toIntOrNull() ?: topic.order
                
                if (title.isEmpty()) {
                    dialogBinding.inputLayoutTitle.error = "Nomini kiriting"
                    return@setOnClickListener
                }
                
                dialog.dismiss()
                
                viewLifecycleOwner.lifecycleScope.launch {
                    val imageUrl = selectedImageUri?.let { uri ->
                        LessonRepository.uploadImage(uri).getOrNull() ?: topic.imageUrl
                    } ?: topic.imageUrl
                    
                    viewModel.updateTopic(topic.copy(title = title, imageUrl = imageUrl, order = order))
                }
            }
        }
        
        dialog.show()
    }

    // =============== NAVIGATION ===============

    private fun navigateToTopicDetail(topic: Topic) {
        val args = Bundle().apply {
            putString("topicId", topic.id)
            putString("topicTitle", topic.title)
        }
        navigate(R.id.action_adminLessonsFragment_to_adminTopicDetailFragment, args)
    }

    private fun showDeleteConfirmation(itemName: String, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("$itemName ni o'chirish")
            .setMessage("Haqiqatan ham bu ${itemName.lowercase()} ni o'chirmoqchimisiz?")
            .setPositiveButton("O'chirish") { _, _ -> onConfirm() }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }
}
