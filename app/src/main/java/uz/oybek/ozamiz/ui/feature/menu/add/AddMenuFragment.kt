package uz.oybek.ozamiz.ui.feature.menu.add

import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isEmpty
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.model.FoodTag
import uz.oybek.ozamiz.data.food.model.UzbekServingUnit
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.menu.model.MenuDifficulty
import uz.oybek.ozamiz.data.menu.model.MenuItemFood
import android.content.res.ColorStateList
import uz.oybek.ozamiz.databinding.FragmentAddMenuBinding
import uz.oybek.ozamiz.databinding.ItemDynamicInputBinding
import uz.oybek.ozamiz.databinding.ItemMenuFoodRowBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.menu.list.MenuTagFilters
import uz.oybek.ozamiz.ui.utils.VaznimColors
import uz.oybek.ozamiz.ui.utils.ImagePicker
import uz.oybek.ozamiz.ui.utils.collectFlowOnStart
import uz.oybek.ozamiz.ui.utils.emoji
import uz.oybek.ozamiz.ui.utils.getArgs
import uz.oybek.ozamiz.ui.utils.loadPhoto
import uz.oybek.ozamiz.ui.utils.registerImagePicker
import uz.oybek.ozamiz.common.util.toReadable
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.titleRes
import kotlin.math.roundToInt

@AndroidEntryPoint
class AddMenuFragment : BaseFragment<FragmentAddMenuBinding>() {

    private val viewModel: AddMenuViewModel by activityViewModels()
    private var selectedImageUri: Uri? = null
    private var menuToEdit: Menu? = null
    private val menuItems = mutableListOf<MenuItemFood>()

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

    override fun viewCreated(bind: FragmentAddMenuBinding) {
        menuToEdit = arguments.getArgs<Menu>() 
            ?: arguments.getArgs<Menu>("menuToEdit") 
            ?: arguments.getArgs<Menu>("menu")

        viewModel.resetState()
        setupListeners(bind)
        setupTagChips(bind)
        observeState(bind)

        if (menuToEdit != null) {
            prefillData(bind, menuToEdit!!)
        } else {
            updateTypeUi(bind, isRecipe = false)
            if (bind.containerStepsList.isEmpty()) {
                addDynamicField(bind.containerStepsList, getString(R.string.menu_add_step_hint))
            }
        }
    }

    private fun updateTypeUi(bind: FragmentAddMenuBinding, isRecipe: Boolean) {
        if (isRecipe) {
            bind.labelName.text = getString(R.string.menu_add_name_label_recipe)
            bind.editName.hint = getString(R.string.menu_add_name_hint_recipe)
            bind.labelItems.text = getString(R.string.menu_add_ingredients_label)
            bind.btnAddIngredient.text = getString(R.string.menu_add_ingredient_btn)
            bind.containerRecipeFields.isVisible = true
        } else {
            bind.labelName.text = getString(R.string.menu_add_name_label_menu)
            bind.editName.hint = getString(R.string.menu_add_name_hint_menu)
            bind.labelItems.text = getString(R.string.menu_add_foods_label)
            bind.btnAddIngredient.text = getString(R.string.menu_add_food_btn)
            bind.containerRecipeFields.isVisible = false
        }
    }

    private fun renderItems(bind: FragmentAddMenuBinding) {
        bind.containerIngredientsList.removeAllViews()
        val isRecipe = bind.radioGroupType.checkedRadioButtonId == R.id.radioRecipe
        menuItems.forEachIndexed { index, item ->
            val itemBinding = ItemMenuFoodRowBinding.inflate(layoutInflater, bind.containerIngredientsList, true)
            itemBinding.textFoodName.text = item.name
            itemBinding.textFoodPortion.text = item.portionText
            val fiber = item.fiber
            val sugar = item.sugar
            itemBinding.textFoodMacros.text = when {
                fiber > 0.0 && sugar > 0.0 -> getString(
                    R.string.menu_sheet_macros_with_fiber_sugar_format,
                    item.calories,
                    item.protein.toReadable(),
                    item.fat.toReadable(),
                    item.carbs.toReadable(),
                    fiber.toReadable(),
                    sugar.toReadable()
                )
                fiber > 0.0 -> getString(
                    R.string.menu_sheet_macros_with_fiber_format,
                    item.calories,
                    item.protein.toReadable(),
                    item.fat.toReadable(),
                    item.carbs.toReadable(),
                    fiber.toReadable()
                )
                sugar > 0.0 -> getString(
                    R.string.menu_sheet_macros_with_sugar_format,
                    item.calories,
                    item.protein.toReadable(),
                    item.fat.toReadable(),
                    item.carbs.toReadable(),
                    sugar.toReadable()
                )
                else -> getString(
                    R.string.menu_sheet_macros_format,
                    item.calories,
                    item.protein.toReadable(),
                    item.fat.toReadable(),
                    item.carbs.toReadable()
                )
            }
            itemBinding.btnRemoveFood.setOnClickListener {
                menuItems.removeAt(index)
                renderItems(bind)
                updateTotals(bind)
            }
            itemBinding.root.setOnClickListener {
                val sheet = AddMenuItemBottomSheet(initialItem = item, isRecipe = isRecipe) { updated ->
                    menuItems[index] = updated
                    renderItems(bind)
                    updateTotals(bind)
                }
                sheet.show(childFragmentManager, "EditMenuItemBottomSheet")
            }
        }
    }

    private fun updateTotals(bind: FragmentAddMenuBinding) {
        val totalCals = menuItems.sumOf { it.calories }
        val totalP = (menuItems.sumOf { it.protein } * 10.0).roundToInt() / 10.0
        val totalF = (menuItems.sumOf { it.fat } * 10.0).roundToInt() / 10.0
        val totalC = (menuItems.sumOf { it.carbs } * 10.0).roundToInt() / 10.0
        val totalFiber = (menuItems.sumOf { it.fiber } * 10.0).roundToInt() / 10.0
        val totalSugar = (menuItems.sumOf { it.sugar } * 10.0).roundToInt() / 10.0

        bind.editCalories.setText(if (totalCals > 0) totalCals.toString() else "")
        bind.editProtein.setText(if (totalP > 0.0) totalP.toString() else "")
        bind.editFat.setText(if (totalF > 0.0) totalF.toString() else "")
        bind.editCarbs.setText(if (totalC > 0.0) totalC.toString() else "")
        bind.editFiber.setText(if (totalFiber > 0.0) totalFiber.toString() else "")
        bind.editSugar.setText(if (totalSugar > 0.0) totalSugar.toString() else "")
    }

    private fun styleChip(chip: Chip, isChecked: Boolean) {
        val ctx = chip.context
        val density = ctx.resources.displayMetrics.density
        if (isChecked) {
            chip.chipBackgroundColor = ColorStateList.valueOf(VaznimColors.getPrimary(ctx))
            chip.setTextColor(VaznimColors.getOnPrimary(ctx))
            chip.chipStrokeColor = ColorStateList.valueOf(VaznimColors.getPrimary(ctx))
            chip.chipStrokeWidth = 2f * density
        } else {
            chip.chipBackgroundColor = ColorStateList.valueOf(VaznimColors.getSurfaceContainer(ctx))
            chip.setTextColor(VaznimColors.getOnSurface(ctx))
            chip.chipStrokeColor = ColorStateList.valueOf(VaznimColors.getOutlineVariant(ctx))
            chip.chipStrokeWidth = 1f * density
        }
    }

    private fun setupTagChips(bind: FragmentAddMenuBinding, extraTags: Set<FoodTag> = emptySet()) {
        bind.chipGroupSelectedCategories.removeAllViews()
        val baseTags = MenuTagFilters.availableFilters.mapNotNull { it.tag }
        val allTags = (baseTags + extraTags).distinct()

        allTags.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                this.tag = tag
                text = "${tag.emoji} ${getString(tag.titleRes)}"
                isCheckable = true
                isClickable = true
                val isChecked = viewModel.uiState.value.selectedTags.contains(tag)
                this.isChecked = isChecked
                styleChip(this, isChecked)
                setOnClickListener {
                    viewModel.toggleTag(tag)
                }
            }
            bind.chipGroupSelectedCategories.addView(chip)
        }
    }

    private fun prefillData(bind: FragmentAddMenuBinding, menu: Menu) {
        this.menuToEdit = menu
        bind.editName.setText(menu.name)
        bind.editAbout.setText(menu.description)

        if (!menu.imageUrl.isNullOrBlank()) {
            bind.imagePreview.isVisible = true
            bind.containerUploadInfo.isVisible = false
            bind.imagePreview.loadPhoto(menu.imageUrl)
        }

        bind.editCalories.setText(menu.totalCalories.toString())
        bind.editProtein.setText(menu.totalProtein.toString())
        bind.editFat.setText(menu.totalFat.toString())
        bind.editCarbs.setText(menu.totalCarbs.toString())
        bind.editFiber.setText(if (menu.totalFiber > 0.0) menu.totalFiber.toString() else "")
        bind.editSugar.setText(if (menu.totalSugar > 0.0) menu.totalSugar.toString() else "")

        if (menu.isRecipe) {
            bind.radioRecipe.isChecked = true
            updateTypeUi(bind, isRecipe = true)
            bind.editPrepTime.setText((menu.prepTimeMinutes ?: 15).toString())
        } else {
            bind.radioMenu.isChecked = true
            updateTypeUi(bind, isRecipe = false)
        }

        when (menu.difficulty) {
            MenuDifficulty.HARD -> bind.radioDifficultyHard.isChecked = true
            MenuDifficulty.MEDIUM -> bind.radioDifficultyMedium.isChecked = true
            MenuDifficulty.EASY -> bind.radioDifficultyEasy.isChecked = true
        }

        menuItems.clear()
        menuItems.addAll(menu.items)
        renderItems(bind)

        bind.containerStepsList.removeAllViews()
        if (menu.steps.isNotEmpty()) {
            menu.steps.forEach { step ->
                addDynamicField(bind.containerStepsList, getString(R.string.menu_add_step_hint), step)
            }
        } else {
            addDynamicField(bind.containerStepsList, getString(R.string.menu_add_step_hint))
        }

        val selectedTags = menu.foodTags
        viewModel.setSelectedTags(selectedTags)
        setupTagChips(bind, selectedTags)
    }

    private fun setupListeners(bind: FragmentAddMenuBinding) {
        bind.btnBack.setOnClickListener { popBackStack() }

        bind.btnAiFill.setOnSafeClickListener {
            val sheet = uz.oybek.ozamiz.ui.feature.menu.ai.GenerateMenuAiBottomSheet { generatedMenu ->
                prefillData(bind, generatedMenu)
            }
            sheet.show(childFragmentManager, uz.oybek.ozamiz.ui.feature.menu.ai.GenerateMenuAiBottomSheet.TAG)
        }

        bind.cardImagePicker.setOnClickListener {
            imagePickerLauncher.launch(ImagePicker.IMAGE)
        }

        bind.btnAddIngredient.setOnClickListener {
            val isRecipe = bind.radioGroupType.checkedRadioButtonId == R.id.radioRecipe
            val sheet = AddMenuItemBottomSheet(isRecipe = isRecipe) { newItem ->
                menuItems.add(newItem)
                renderItems(bind)
                updateTotals(bind)
            }
            sheet.show(childFragmentManager, "AddMenuItemBottomSheet")
        }

        bind.btnAddStep.setOnClickListener {
            addDynamicField(bind.containerStepsList, getString(R.string.menu_add_step_hint))
        }

        bind.btnSelectCategories.visibility = View.GONE

        bind.radioGroupType.setOnCheckedChangeListener { _, checkedId ->
            val isRecipe = checkedId == R.id.radioRecipe
            updateTypeUi(bind, isRecipe)
        }

        bind.btnSave.setOnClickListener {
            val name = bind.editName.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(requireContext(), getString(R.string.menu_error_empty_name), Toast.LENGTH_SHORT).show()
                bind.editName.requestFocus()
                return@setOnClickListener
            }

            val isRecipe = bind.radioGroupType.checkedRadioButtonId == R.id.radioRecipe

            if (menuItems.isEmpty()) {
                val errorMsg = if (isRecipe) {
                    getString(R.string.menu_error_empty_ingredients)
                } else {
                    getString(R.string.menu_error_empty_foods)
                }
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val steps = if (isRecipe) getDynamicFieldValues(bind.containerStepsList) else emptyList()
            if (isRecipe && steps.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.menu_error_empty_steps), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val difficulty = when (bind.radioGroupDifficulty.checkedRadioButtonId) {
                R.id.radioDifficultyHard -> MenuDifficulty.HARD
                R.id.radioDifficultyMedium -> MenuDifficulty.MEDIUM
                else -> MenuDifficulty.EASY
            }

            viewModel.saveMenu(
                menuId = menuToEdit?.id,
                name = name,
                description = bind.editAbout.text.toString().trim(),
                isRecipe = isRecipe,
                items = menuItems.toList(),
                steps = steps,
                prepTimeMinutes = if (isRecipe) bind.editPrepTime.text.toString().toIntOrNull() ?: 15 else null,
                difficulty = difficulty,
                imageUri = selectedImageUri,
                existingImageUrl = menuToEdit?.imageUrl
            )
        }
    }

    private fun addDynamicField(container: ViewGroup, hint: String, initialText: String = "") {
        val fieldBinding = ItemDynamicInputBinding.inflate(layoutInflater, container, true)
        fieldBinding.editInput.hint = hint
        if (initialText.isNotBlank()) {
            fieldBinding.editInput.setText(initialText)
        }
        fieldBinding.btnRemove.setOnClickListener {
            container.removeView(fieldBinding.root)
        }
    }

    private fun getDynamicFieldValues(container: ViewGroup): List<String> {
        val values = mutableListOf<String>()
        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i)
            val binding = ItemDynamicInputBinding.bind(view)
            val text = binding.editInput.text.toString().trim()
            if (text.isNotBlank()) {
                values.add(text)
            }
        }
        return values
    }

    private fun observeState(bind: FragmentAddMenuBinding) {
        collectFlowOnStart(viewModel.uiState) { state ->
            bind.progressBar.isVisible = state.isLoading
            bind.btnSave.isInvisible = state.isLoading
            bind.btnSave.isEnabled = !state.isLoading

            for (i in 0 until bind.chipGroupSelectedCategories.childCount) {
                val chip = bind.chipGroupSelectedCategories.getChildAt(i) as? Chip ?: continue
                val tag = chip.tag as? FoodTag ?: MenuTagFilters.availableFilters.mapNotNull { it.tag }.getOrNull(i)
                if (tag != null) {
                    val shouldBeChecked = state.selectedTags.contains(tag)
                    if (chip.isChecked != shouldBeChecked) {
                        chip.isChecked = shouldBeChecked
                    }
                    styleChip(chip, shouldBeChecked)
                }
            }

            if (state.isValidationError) {
                Toast.makeText(requireContext(), getString(R.string.menu_error_fill_fields), Toast.LENGTH_SHORT).show()
            }
        }

        collectFlowOnStart(viewModel.uiEffect) { effect ->
            when (effect) {
                is AddMenuUiEffect.SaveSuccess -> {
                    Toast.makeText(requireContext(), getString(R.string.menu_saved_success), Toast.LENGTH_SHORT).show()
                    popBackStack()
                }
                is AddMenuUiEffect.SaveError -> {
                    val msg = effect.error ?: getString(R.string.unknown_error)
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        const val KEY_MENU_UPDATED = "updated_menu"
    }
}
