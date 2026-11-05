package uz.oybek.ozamiz.ui.feature.menu.detail

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.common.util.toReadable
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.databinding.FragmentMenuDetailBinding
import uz.oybek.ozamiz.databinding.ItemMenuFoodRowBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.common.dialog.ConfirmDeleteDialog
import uz.oybek.ozamiz.ui.feature.menu.titleRes
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter
import uz.oybek.ozamiz.ui.utils.VaznimColors
import uz.oybek.ozamiz.ui.utils.collectFlowOnStart
import uz.oybek.ozamiz.ui.utils.emoji
import uz.oybek.ozamiz.ui.utils.getArgs
import uz.oybek.ozamiz.ui.utils.loadPhoto
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.showToast
import uz.oybek.ozamiz.ui.utils.titleRes
import uz.oybek.ozamiz.ui.utils.toBundle

@AndroidEntryPoint
class MenuDetailFragment : BaseFragment<FragmentMenuDetailBinding>() {

    private val viewModel: MenuDetailViewModel by viewModels()
    private var initialMenu: Menu? = null

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentMenuDetailBinding = FragmentMenuDetailBinding.inflate(inflater, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun viewCreated(bind: FragmentMenuDetailBinding) {
        val menu = arguments.getArgs<Menu>() ?: initialMenu
        if (menu == null) {
            val menuId = arguments?.getString("menuId")
            if (!menuId.isNullOrBlank()) {
                viewModel.loadMenuById(menuId)
            } else {
                popBackStack()
                return
            }
        } else {
            viewModel.setMenu(menu)
        }

        setupListeners(bind)
        setupScrollBehavior(bind)
        observeState(bind)
    }

    private fun setupScrollBehavior(bind: FragmentMenuDetailBinding) {
        bind.appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val context = appBarLayout.context
            val isCollapsed = Math.abs(verticalOffset) >= appBarLayout.totalScrollRange - 100

            val tintColor = if (isCollapsed) {
                VaznimColors.getOnSurface(context)
            } else {
                Color.WHITE
            }

            bind.btnBack.imageTintList = ColorStateList.valueOf(tintColor)
            bind.btnBack.backgroundTintList = if (isCollapsed) {
                ColorStateList.valueOf(Color.TRANSPARENT)
            } else {
                null
            }
        }
    }

    private fun setupListeners(bind: FragmentMenuDetailBinding) {
        bind.btnBack.setOnSafeClickListener {
            popBackStack()
        }

        bind.btnEdit.setOnSafeClickListener {
            val currentMenu = viewModel.uiState.value.menu ?: return@setOnSafeClickListener
            navigate(R.id.addMenuFragment, currentMenu.toBundle())
        }

        bind.btnDelete.setOnSafeClickListener {
            showDeleteConfirmation()
        }

        bind.btnLikeDetail.setOnSafeClickListener {
            viewModel.toggleLike()
        }
    }

    private fun observeState(bind: FragmentMenuDetailBinding) {
        collectFlowOnStart(viewModel.uiState) { state ->
            val menu = state.menu ?: return@collectFlowOnStart
            bind.btnDelete.isVisible = state.isAdmin
            bind.btnEdit.isVisible = state.isAdmin
            bindMenuDetails(bind, menu, state)
        }

        collectFlowOnStart(viewModel.uiEffect) { effect ->
            when (effect) {
                is MenuDetailUiEffect.DeleteSuccess -> {
                    parentFragmentManager.setFragmentResult(
                        KEY_MENU_DELETED,
                        androidx.core.os.bundleOf(EXTRA_DELETED_MENU_ID to effect.menuId)
                    )
                    popBackStack()
                }

                is MenuDetailUiEffect.DeleteError -> {
                    showToast(effect.error ?: getString(R.string.unknown_error))
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindMenuDetails(
        bind: FragmentMenuDetailBinding, menu: Menu, state: MenuDetailUiState
    ) = with(bind) {
        val ctx = requireContext()

        // 16:9 Image
        if (!menu.imageUrl.isNullOrBlank()) {
            imageMenu.loadPhoto(menu.imageUrl)
        } else {
            imageMenu.setImageResource(R.drawable.placeholder_food)
        }

        // Type Badge (📖 Retsept / 🍽️ Menyu)
        val isRecipe = menu.isRecipe
        if (isRecipe) {
            textTypeBadge.text = "📖 " + ctx.getString(R.string.menu_type_badge_recipe)
        } else {
            textTypeBadge.text = "🍽️ " + ctx.getString(R.string.menu_type_badge_menu)
        }

        // Like Button
        textDetailLikesCount.text = menu.likesCount.toString()
        val textColor = VaznimColors.getOnSurface(ctx)
        textDetailLikesCount.setTextColor(textColor)

        if (menu.isLiked) {
            imageDetailLikeIcon.setImageResource(R.drawable.ic_heart_filled)
            val primaryColor = VaznimColors.getPrimary(ctx)
            imageDetailLikeIcon.imageTintList = ColorStateList.valueOf(primaryColor)
        } else {
            imageDetailLikeIcon.setImageResource(R.drawable.ic_heart_outline)
            val inactiveTint = VaznimColors.getOnSurfaceVariant(ctx)
            imageDetailLikeIcon.imageTintList = ColorStateList.valueOf(inactiveTint)
        }

        // Title
        textName.text = menu.name

        val typePart = if (isRecipe) {
            "📖 ${ctx.getString(R.string.menu_type_badge_recipe)}"
        } else {
            "🥗 ${ctx.getString(R.string.menu_type_badge_menu)}"
        }
        val prepMinutes = menu.prepTimeMinutes ?: if (isRecipe) 25 else 15
        val prepPart = "⏱️ ${ctx.getString(R.string.menu_feed_minutes_format, prepMinutes)}"
        val diffPart = ctx.getString(menu.difficulty.titleRes)
        val weightPart = if (menu.totalWeightGrams > 0.0) {
            NutritionFormatter.gr(menu.totalWeightGrams)
        } else null

        textMenuMeta.isVisible = true
        textMenuMeta.text = if (weightPart != null) {
            "$typePart • $prepPart • $weightPart • $diffPart"
        } else {
            "$typePart • $prepPart • $diffPart"
        }

        // Description
        textDescription.isVisible = menu.description.isNotBlank()
        textDescription.text = menu.description

        // Nutrition Cards (Calories, Protein, Fat, Carbs, Fiber, Sugar)
        textFactCalories.text =
            ctx.getString(R.string.menu_feed_calories_format, menu.totalCalories)
        textProtein.text = NutritionFormatter.gr(menu.totalProtein)
        textFat.text = NutritionFormatter.gr(menu.totalFat)
        textCarbs.text = NutritionFormatter.gr(menu.totalCarbs)
        textFiber.text = NutritionFormatter.gr(menu.totalFiber)
        textSugar.text = NutritionFormatter.gr(menu.totalSugar)
        layoutSecondaryNutrients.isVisible = menu.totalFiber > 0.0 || menu.totalSugar > 0.0

        // Ingredients / Food Items List
        val displayItems =
            if (state.calculatedItems.isNotEmpty()) state.calculatedItems else menu.items
        labelIngredients.isVisible = displayItems.isNotEmpty()
        labelIngredients.text = if (isRecipe) {
            ctx.getString(R.string.menu_details_ingredients_title)
        } else {
            ctx.getString(R.string.menu_details_foods_title)
        }

        containerIngredientsList.removeAllViews()
        displayItems.forEach { item ->
            val itemBinding =
                ItemMenuFoodRowBinding.inflate(layoutInflater, containerIngredientsList, false)
            itemBinding.btnRemoveFood.isVisible = false
            itemBinding.textFoodName.text = item.name
            itemBinding.textFoodPortion.text = item.portionText
            val fiber = item.fiber
            val sugar = item.sugar
            itemBinding.textFoodMacros.text = when {
                fiber > 0.0 && sugar > 0.0 -> ctx.getString(
                    R.string.menu_sheet_macros_with_fiber_sugar_format,
                    item.calories,
                    item.protein.toReadable(),
                    item.fat.toReadable(),
                    item.carbs.toReadable(),
                    fiber.toReadable(),
                    sugar.toReadable()
                )

                fiber > 0.0 -> ctx.getString(
                    R.string.menu_sheet_macros_with_fiber_format,
                    item.calories,
                    item.protein.toReadable(),
                    item.fat.toReadable(),
                    item.carbs.toReadable(),
                    fiber.toReadable()
                )

                sugar > 0.0 -> ctx.getString(
                    R.string.menu_sheet_macros_with_sugar_format,
                    item.calories,
                    item.protein.toReadable(),
                    item.fat.toReadable(),
                    item.carbs.toReadable(),
                    sugar.toReadable()
                )

                else -> ctx.getString(
                    R.string.menu_sheet_macros_format,
                    item.calories,
                    item.protein.toReadable(),
                    item.fat.toReadable(),
                    item.carbs.toReadable()
                )
            }
            containerIngredientsList.addView(itemBinding.root)
        }

        // Steps (only if isRecipe and steps exist)
        val hasSteps = isRecipe && menu.steps.isNotEmpty()
        labelPreparationMethod.isVisible = hasSteps
        containerSteps.isVisible = hasSteps
        divider1.isVisible = hasSteps

        containerSteps.removeAllViews()
        if (hasSteps) {
            menu.steps.forEachIndexed { index, step ->
                val stepView =
                    layoutInflater.inflate(R.layout.item_instruction_step, containerSteps, false)
                val textStepNumber = stepView.findViewById<TextView>(R.id.textStepNumber)
                val textStepContent = stepView.findViewById<TextView>(R.id.textStepContent)
                textStepNumber.text = (index + 1).toString()
                textStepContent.text = step
                containerSteps.addView(stepView)
            }
        }

        // Compact FoodTags placed at the very bottom (secondary)
        chipGroupCategoryTags.removeAllViews()
        val tags = menu.foodTags
        chipGroupCategoryTags.isVisible = tags.isNotEmpty()

        val tagBg = VaznimColors.getSurfaceContainer(ctx)
        val tagStroke = VaznimColors.getOutlineVariant(ctx)
        val tagText = VaznimColors.getOnSurfaceVariant(ctx)
        val radius12dp = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics
        )
        val stroke1dp = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics
        )
        tags.forEach { tag ->
            val chip = Chip(ctx).apply {
                text = "${tag.emoji} ${ctx.getString(tag.titleRes)}"
                isCheckable = false
                isClickable = false
                setEnsureMinTouchTargetSize(false)
                chipMinHeight = 0f
                chipCornerRadius = radius12dp
                chipStrokeWidth = stroke1dp
                chipStrokeColor = ColorStateList.valueOf(tagStroke)
                chipBackgroundColor = ColorStateList.valueOf(tagBg)
                setTextColor(tagText)
                textSize = 12f
            }
            chipGroupCategoryTags.addView(chip)
        }
    }

    private fun showDeleteConfirmation() {
        val ctx = context ?: return
        ConfirmDeleteDialog.show(
            context = ctx,
            titleRes = R.string.menu_details_delete_confirm_title,
            messageRes = R.string.menu_details_delete_confirm_message,
            confirmTextRes = R.string.menu_details_delete_confirm_yes,
            cancelTextRes = R.string.menu_details_delete_confirm_no,
            onConfirm = {
                viewModel.deleteMenu(viewModel.uiState.value.menu?.id ?: return@show)
            })
    }

    companion object {
        const val KEY_MENU_DELETED = "key_menu_deleted"
        const val EXTRA_DELETED_MENU_ID = "extra_deleted_menu_id"
    }
}
