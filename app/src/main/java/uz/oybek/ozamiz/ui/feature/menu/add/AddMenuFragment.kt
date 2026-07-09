package uz.oybek.ozamiz.ui.feature.menu.add

import android.net.Uri
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAddMenuBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.loadPhoto

class AddMenuFragment : BaseFragment<FragmentAddMenuBinding>() {

    private val viewModel: AddMenuViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.imagePreview.isVisible = true
            binding.containerUploadInfo.isVisible = false
            binding.imagePreview.loadPhoto(it.toString())
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentAddMenuBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentAddMenuBinding) {
        setupListeners(bind)
        observeState(bind)
    }

    private fun setupListeners(bind: FragmentAddMenuBinding) {
        bind.btnBack.setOnClickListener { findNavController().popBackStack() }
        bind.cardImagePicker.setOnClickListener { imagePickerLauncher.launch("image/*") }
        bind.btnAddCategory.setOnClickListener { findNavController().navigate(R.id.categoryManagerFragment) }

        bind.btnSave.setOnClickListener {
            val categoryIds = bind.chipGroupCategories.checkedChipIds.map { id ->
                bind.chipGroupCategories.findViewById<Chip>(id).tag as String
            }

            viewModel.saveMenu(
                name = bind.editName.text.toString(),
                description = bind.editDescription.text.toString(),
                ingredients = bind.editIngredients.text.toString(),
                calories = bind.editCalories.text.toString(),
                protein = bind.editProtein.text.toString(),
                fat = bind.editFat.text.toString(),
                carbs = bind.editCarbs.text.toString(),
                selectedCategoryIds = categoryIds,
                imageUri = selectedImageUri
            )
        }
    }

    private fun observeState(bind: FragmentAddMenuBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.progressBar.isVisible = state.isLoading
                    bind.btnSave.isEnabled = !state.isLoading

                    if (state.isSaved) {
                        Toast.makeText(context, "Muvaffaqiyatli saqlandi!", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }

                    if (state.isValidationError) {
                        Toast.makeText(context, "Barcha maydonlarni to'ldiring", Toast.LENGTH_SHORT).show()
                        viewModel.clearValidationError()
                    }

                    updateCategoryChips(bind, state)
                }
            }
        }
    }

    private fun updateCategoryChips(bind: FragmentAddMenuBinding, state: AddMenuUiState) {
        if (bind.chipGroupCategories.childCount != state.categories.size && state.categories.isNotEmpty()) {
            bind.chipGroupCategories.removeAllViews()
            state.categories.forEach { category ->
                bind.chipGroupCategories.addView(
                    Chip(ContextThemeWrapper(requireContext(), com.google.android.material.R.style.Widget_Material3_Chip_Filter)).apply {
                        text = category.name
                        tag = category.id
                        isCheckable = true
                    }
                )
            }
        }
    }
}
