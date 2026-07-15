package uz.oybek.ozamiz.ui.feature.menu.add

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.menu.FilterItem
import uz.oybek.ozamiz.databinding.FragmentAddMenuBinding
import uz.oybek.ozamiz.databinding.ItemDynamicInputBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.loadPhoto

class AddMenuFragment : BaseFragment<FragmentAddMenuBinding>() {

    private val viewModel: AddMenuViewModel by activityViewModels()
    private var selectedImageUri: Uri? = null
    private val selectedCategories = mutableSetOf<FilterItem>()

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

        if (bind.containerIngredientsList.isEmpty()) {
            addDynamicField(bind.containerIngredientsList, "Masalliq nomi...")
        }
        if (bind.containerStepsList.isEmpty()) {
            addDynamicField(bind.containerStepsList, "Tayyorlash bosqichi...")
        }
    }

    private fun setupListeners(bind: FragmentAddMenuBinding) {
        bind.btnBack.setOnClickListener { findNavController().popBackStack() }
        bind.cardImagePicker.setOnClickListener { imagePickerLauncher.launch("image/*") }

        bind.btnAddIngredient.setOnClickListener {
            addDynamicField(bind.containerIngredientsList, "Masalliq nomi...")
        }

        bind.btnAddStep.setOnClickListener {
            addDynamicField(bind.containerStepsList, "Tayyorlash bosqichi...")
        }

        bind.btnSelectCategories.setOnClickListener {
            findNavController().navigate(R.id.action_addMenuFragment_to_categorySelectionFragment)
        }

        bind.radioGroupType.setOnCheckedChangeListener { _, checkedId ->
            val isMenu = checkedId == R.id.radioMenu
            bind.containerMenuFields.visibility =
                if (isMenu) View.VISIBLE else View.GONE
            bind.containerFoodFields.visibility =
                if (isMenu) View.GONE else View.VISIBLE
        }

        bind.btnSave.setOnClickListener {
            val categoryIds = selectedCategories.map { it.id.name }
            val isMenu = bind.radioGroupType.checkedRadioButtonId == R.id.radioMenu
            val type = if (isMenu) "menu" else "food"

            viewModel.saveMenu(
                name = bind.editName.text.toString(),
                description = "",
                ingredients = getDynamicFieldValues(bind.containerIngredientsList),
                calories = bind.editCalories.text.toString(),
                protein = bind.editProtein.text.toString(),
                fat = bind.editFat.text.toString(),
                carbs = bind.editCarbs.text.toString(),
                fiber = bind.editFiber.text.toString(),
                sugar = bind.editSugar.text.toString(),
                salt = bind.editSalt.text.toString(),
                selectedCategoryIds = categoryIds,
                imageUri = selectedImageUri,
                type = type,
                preparationMethod = getDynamicFieldValues(bind.containerStepsList),
                about = if (!isMenu) bind.editAbout.text.toString() else "",
                richIn = if (!isMenu) bind.editRichIn.text.toString() else "",
                consumptionTime = if (!isMenu) bind.editConsumptionTime.text.toString() else "",
                preparationTime = if (isMenu) bind.editPrepTime.text.toString() else ""
            )
        }
    }

    private fun addDynamicField(container: ViewGroup, hint: String) {
        val fieldBinding = ItemDynamicInputBinding.inflate(layoutInflater, container, true)
        fieldBinding.editInput.hint = hint
        fieldBinding.btnRemove.setOnClickListener {
            container.removeView(fieldBinding.root)
        }
    }

    private fun getDynamicFieldValues(container: ViewGroup): List<String> {
        val values = mutableListOf<String>()
        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i)
            val binding = ItemDynamicInputBinding.bind(view)
            val text = binding.editInput.text.toString()
            if (text.isNotBlank()) {
                values.add(text)
            }
        }
        return values
    }

    private fun updateSelectedCategoriesChips() {
        binding.chipGroupSelectedCategories.removeAllViews()
        selectedCategories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = "${category.emoji} ${category.name}"
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    selectedCategories.removeIf { it.id == category.id }
                    viewModel.setSelectedCategories(selectedCategories.toList())
                }
            }
            binding.chipGroupSelectedCategories.addView(chip)
        }
    }

    private fun observeState(bind: FragmentAddMenuBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedCategories.collect { categories ->
                    selectedCategories.clear()
                    selectedCategories.addAll(categories)
                    updateSelectedCategoriesChips()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.progressBar.isVisible = state.isLoading
                    bind.btnSave.isEnabled = !state.isLoading

                    if (state.isSaved) {
                        Toast.makeText(context, "Muvaffaqiyatli saqlandi!", Toast.LENGTH_SHORT)
                            .show()
                        findNavController().popBackStack()
                    }

                    if (state.isValidationError) {
                        Toast.makeText(context, "Barcha maydonlarni to'ldiring", Toast.LENGTH_SHORT)
                            .show()
                        viewModel.clearValidationError()
                    }
                }
            }
        }
    }
}
