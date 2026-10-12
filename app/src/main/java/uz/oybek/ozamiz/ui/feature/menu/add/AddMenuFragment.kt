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
import uz.oybek.ozamiz.data.menu.MenuCategories
import uz.oybek.ozamiz.databinding.FragmentAddMenuBinding
import uz.oybek.ozamiz.databinding.ItemDynamicInputBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.loadPhoto
import uz.oybek.ozamiz.ui.utils.registerImagePicker

class AddMenuFragment : BaseFragment<FragmentAddMenuBinding>() {

    private val viewModel: AddMenuViewModel by activityViewModels()
    private var selectedImageUri: Uri? = null
    private val selectedCategories = mutableSetOf<FilterItem>()

    private val imagePickerLauncher = registerImagePicker { uri ->
        selectedImageUri = uri
        binding.imagePreview.isVisible = true
        binding.containerUploadInfo.isVisible = false
        binding.imagePreview.loadPhoto(uri.toString())
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentAddMenuBinding.inflate(inflater, container, false)

    private var menuToEdit: uz.oybek.ozamiz.data.menu.model.Menu? = null

    override fun viewCreated(bind: FragmentAddMenuBinding) {
        menuToEdit = arguments?.getParcelable("menuToEdit")

        viewModel.resetState()
        setupListeners(bind)
        observeState(bind)

        if (menuToEdit != null) {
            prefillData(bind, menuToEdit!!)
        } else {
            if (bind.containerIngredientsList.isEmpty()) {
                addDynamicField(bind.containerIngredientsList, "Masalliq nomi...")
            }
            if (bind.containerStepsList.isEmpty()) {
                addDynamicField(bind.containerStepsList, "Tayyorlash bosqichi...")
            }
        }
    }

    private fun prefillData(bind: FragmentAddMenuBinding, menu: uz.oybek.ozamiz.data.menu.model.Menu) {
        bind.editName.setText(menu.name)
        if (menu.imageUrl != null) {
            binding.imagePreview.isVisible = true
            binding.containerUploadInfo.isVisible = false
            binding.imagePreview.loadPhoto(menu.imageUrl!!)
        }
        
        bind.editCalories.setText(menu.calories.toString())
        bind.editProtein.setText(menu.protein.toString())
        bind.editFat.setText(menu.fat.toString())
        bind.editCarbs.setText(menu.carbs.toString())
        bind.editFiber.setText(menu.fiber.toString())
        bind.editSugar.setText(menu.sugar.toString())
        bind.editSalt.setText(menu.salt.toString())
        bind.editServingSize.setText(menu.servingSize)
        
        if (menu.type == "menu") {
            bind.radioMenu.isChecked = true
            bind.editPrepTime.setText(menu.preparationTime.toString())
        } else {
            bind.radioFood.isChecked = true
            bind.editAbout.setText(menu.about)
        }

        bind.containerIngredientsList.removeAllViews()
        if (menu.ingredients.isNotEmpty()) {
            menu.ingredients.forEach {
                val fieldBinding = ItemDynamicInputBinding.inflate(layoutInflater, bind.containerIngredientsList, true)
                fieldBinding.editInput.hint = "Masalliq nomi..."
                fieldBinding.editInput.setText(it)
                fieldBinding.btnRemove.setOnClickListener {
                    bind.containerIngredientsList.removeView(fieldBinding.root)
                }
            }
        } else addDynamicField(bind.containerIngredientsList, "Masalliq nomi...")

        bind.containerStepsList.removeAllViews()
        if (menu.preparationMethod.isNotEmpty()) {
            menu.preparationMethod.forEach {
                val fieldBinding = ItemDynamicInputBinding.inflate(layoutInflater, bind.containerStepsList, true)
                fieldBinding.editInput.hint = "Tayyorlash bosqichi..."
                fieldBinding.editInput.setText(it)
                fieldBinding.btnRemove.setOnClickListener {
                    bind.containerStepsList.removeView(fieldBinding.root)
                }
            }
        } else addDynamicField(bind.containerStepsList, "Tayyorlash bosqichi...")

        val allItems = MenuCategories.mainMeals + MenuCategories.methods + MenuCategories.diets + MenuCategories.ingredients
        val matchingCategories = allItems.filter { menu.categoryIds.contains(it.id.name) }
        if (matchingCategories.isNotEmpty()) {
            viewModel.setSelectedCategories(matchingCategories)
        }
    }

    private fun setupListeners(bind: FragmentAddMenuBinding) {
        bind.btnBack.setOnClickListener { popBackStack() }
        bind.cardImagePicker.setOnClickListener { imagePickerLauncher.launch("image/*") }

        bind.btnAddIngredient.setOnClickListener {
            addDynamicField(bind.containerIngredientsList, "Masalliq nomi...")
        }

        bind.btnAddStep.setOnClickListener {
            addDynamicField(bind.containerStepsList, "Tayyorlash bosqichi...")
        }

        bind.btnSelectCategories.setOnClickListener {
            navigate(R.id.categorySelectionFragment)
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
                menuId = menuToEdit?.id,
                imageUrl = menuToEdit?.imageUrl,
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
                servingSize = bind.editServingSize.text.toString(),
                selectedCategoryIds = categoryIds,
                imageUri = selectedImageUri,
                type = type,
                preparationMethod = getDynamicFieldValues(bind.containerStepsList),
                about = if (!isMenu) bind.editAbout.text.toString() else "",
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
                        viewModel.clearSavedState()
                        Toast.makeText(context, "Muvaffaqiyatli saqlandi!", Toast.LENGTH_SHORT)
                            .show()
                        try {
                            findNavController().previousBackStackEntry
                                ?.savedStateHandle?.set(KEY_MENU_UPDATED, state.savedMenu)
                        } catch (e: Exception) {
                            // ignore
                        }
                        popBackStack()
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

    companion object {
        const val KEY_MENU_UPDATED = "updated_menu"
    }
}
