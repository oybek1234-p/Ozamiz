package uz.oybek.ozamiz.ui.foods

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf // Import bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult // Import setFragmentResult
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.foods.FoodCategory
import uz.oybek.ozamiz.databinding.DialogAddEditCategoryBinding
import uz.oybek.ozamiz.ui.loadPhoto
import uz.oybek.ozamiz.ui.showToast

class AddEditCategoryDialog : DialogFragment() {

    private var _binding: DialogAddEditCategoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddEditCategoryViewModel by viewModels()

    companion object {
        const val TAG = "AddEditCategoryDialog"
        const val REQUEST_KEY = "category_dialog_request" // Request key for fragment result
        const val RESULT_SUCCESS = "result_success" // Key for success result
    }

    // Rasm tanlash uchun launcher
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.selectedImageUri = it
            binding.includeImagePickerCategory.imageViewPicked.loadPhoto(it)
            binding.includeImagePickerCategory.layoutAddImagePlaceholder.isVisible = false
            binding.includeImagePickerCategory.buttonRemoveImage.isVisible = true
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddEditCategoryBinding.inflate(LayoutInflater.from(context))

        val categoryJson = arguments?.getString("categoryJson")
        viewModel.initialize(categoryJson)

        setupViews(binding)
        setupListeners(binding)
        observeViewModel(binding)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setCancelable(false) // Prevent dismissal initially
            .create()
    }

    private fun setupViews(bind: DialogAddEditCategoryBinding) {
        bind.includeImagePickerCategory.buttonRemoveImage.setOnClickListener {
            viewModel.selectedImageUri = null
            val imageUrl = viewModel.uiState.value?.categoryToEdit?.imageUrl
            if (!imageUrl.isNullOrEmpty()) {
                bind.includeImagePickerCategory.imageViewPicked.loadPhoto(imageUrl)
                bind.includeImagePickerCategory.layoutAddImagePlaceholder.isVisible = false
                bind.includeImagePickerCategory.buttonRemoveImage.isVisible = true
            } else {
                bind.includeImagePickerCategory.imageViewPicked.setImageResource(0)
                bind.includeImagePickerCategory.layoutAddImagePlaceholder.isVisible = true
                bind.includeImagePickerCategory.buttonRemoveImage.isVisible = false
            }
        }
        bind.cardImagePickerCategory.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun setupListeners(bind: DialogAddEditCategoryBinding) {
        bind.buttonSaveCategory.setOnClickListener {
            val name = bind.editTextCategoryName.text.toString()
            val order = bind.editTextCategoryOrder.text.toString()
            viewModel.saveCategory(name, order)
        }
        bind.buttonCancelCategory.setOnClickListener {
            dismiss()
        }
    }

    private fun observeViewModel(bind: DialogAddEditCategoryBinding) {
        viewModel.uiState.observe(this) { state ->
            bind.progressBarSaveCategory.isVisible = state.isSaving
            bind.layoutButtons.isVisible = !state.isSaving
            dialog?.setCancelable(!state.isSaving) // Allow canceling only when not saving
            bind.textViewDialogTitle.text = state.title

            if (state.categoryToEdit != null && bind.editTextCategoryName.text.isNullOrEmpty()) {
                bind.editTextCategoryName.setText(state.categoryToEdit.name)
                bind.editTextCategoryOrder.setText(state.categoryToEdit.order.toString())
                if (!state.categoryToEdit.imageUrl.isNullOrEmpty()) {
                    bind.includeImagePickerCategory.imageViewPicked.loadPhoto(state.categoryToEdit.imageUrl)
                    bind.includeImagePickerCategory.layoutAddImagePlaceholder.isVisible = false
                    bind.includeImagePickerCategory.buttonRemoveImage.isVisible = true
                }
            }

            state.errorMessage?.let {
                showToast(it)
                viewModel.clearError()
            }

            if (state.saveSuccess) {
                val message = if (state.categoryToEdit == null) "Kategoriya qo'shildi" else "Kategoriya yangilandi"
                // Send result back to FoodsFragment
                setFragmentResult(REQUEST_KEY, bundleOf(RESULT_SUCCESS to true))
                showToast(message)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}