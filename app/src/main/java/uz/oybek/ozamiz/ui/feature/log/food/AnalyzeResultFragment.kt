package uz.oybek.ozamiz.ui.feature.log.food

import android.graphics.Color
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ads.AdsConfig
import uz.oybek.ozamiz.data.analyzer.model.AnalyzedIngredient
import uz.oybek.ozamiz.data.tracker.model.FoodModel
import uz.oybek.ozamiz.databinding.BottomSheetCalorieResultBinding
import uz.oybek.ozamiz.databinding.ItemMacroDisplayBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.common.dialog.ConfirmDeleteDialog
import uz.oybek.ozamiz.ui.common.dialog.ConfirmSaveDialog
import uz.oybek.ozamiz.ui.utils.collectFlowOnStart
import uz.oybek.ozamiz.ui.utils.loadBannerAd
import uz.oybek.ozamiz.ui.utils.SoundHelper
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.showFullScreenImage
import uz.oybek.ozamiz.data.tracker.model.MealType
import uz.oybek.ozamiz.common.util.orZero
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter
import uz.oybek.ozamiz.ui.modelUtils.getDisplayName
import uz.oybek.ozamiz.ui.utils.loadPhoto
import uz.oybek.ozamiz.ui.utils.showAppSnackBar
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class AnalyzeResultFragment : BaseFragment<BottomSheetCalorieResultBinding>() {

    private val viewModel: AnalyzeResultViewModel by viewModels()
    private var ingredientsAdapter: IngredientsAdapter? = null
    private var loadingTextJob: Job? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomSheetCalorieResultBinding {
        return BottomSheetCalorieResultBinding.inflate(
            inflater,
            container,
            false
        )
    }

    override fun viewCreated(bind: BottomSheetCalorieResultBinding) {
        setupToolbar(bind)
        setupListeners(bind)
        setupBackPressed()
        observeViewModel()
        loadBanner()
    }

    override fun onDestroyView() {
        stopSequentialLoadingText()
        ingredientsAdapter = null
        super.onDestroyView()
    }

    private fun setupToolbar(bind: BottomSheetCalorieResultBinding) {
        bind.toolbar.setBackgroundColor(Color.TRANSPARENT)
        bind.toolbar.background = Color.TRANSPARENT.toDrawable()
        bind.toolbar.elevation = 0f

        bind.toolbar.inflateMenu(R.menu.menu_analyze_result)
        bind.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_favorite -> {
                    viewModel.toggleFavorite()
                    true
                }

                R.id.action_share -> {
                    viewModel.shareFood()
                    true
                }

                else -> false
            }
        }
        bind.toolbar.setNavigationOnClickListener { attemptExit() }
    }

    private fun setupBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    attemptExit()
                }
            }
        )
    }

    private fun attemptExit() {
        val state = viewModel.uiState.value
        val ctx = context ?: return
        if (state.food != null && !state.isSaved) {
            ConfirmSaveDialog.show(
                context = ctx,
                onConfirm = { viewModel.onSaveClick() },
                onCancel = {
                    viewModel.cancelAndExit()
                }
            )
        } else {
            viewModel.cancelAndExit()
        }
    }

    private fun observeViewModel() {
        collectFlowOnStart(viewModel.uiState, ::renderState)
        collectFlowOnStart(viewModel.events, ::handleEvent)
    }

    private fun renderState(state: AnalyzeResultUiState) = with(binding) {
        val isAnalyzing = state.isAnalyzing
        cardLoadingState.isVisible = isAnalyzing
        cardErrorState.isVisible = state.isError
        layoutMacros.isVisible = !isAnalyzing && !state.isError
        layoutPortionCounter.isVisible = !isAnalyzing && !state.isError
        cardCalorieBanner.isVisible = !isAnalyzing && !state.isError
        buttonFavorite.isVisible = !isAnalyzing && state.food != null
        buttonShareFloating.isVisible = !isAnalyzing && state.food != null

        if (isAnalyzing) {
            startSequentialLoadingText()
            renderFoodPhoto(state.initialImageUri)
            state.initialDescription?.takeIf { it.isNotBlank() }?.let {
                textViewFoodName.text = it
                layoutFoodName.isVisible = true
            }
            layoutBottomActions.isVisible = true
            layoutHorizontalActions.isVisible = true
            buttonSaveToLog.isVisible = false
            layoutSaveContainer.isVisible = false
            buttonCancel.isVisible = true
            buttonCancel.text = getString(R.string.cancel)
            buttonCancel.icon = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.cancel_ic
            )
            return
        }

        stopSequentialLoadingText()

        val food = state.food ?: return
        textViewCalories.text = food.calories.toString()
        textViewFoodName.text = food.description
        layoutFoodName.isVisible = true
        textViewPortionCount.text = NutritionFormatter.portion(state.portionMultiplier)

        bindMacro(
            layoutProtein, getString(R.string.protein),
            food.protein,
            R.drawable.egg_ic
        )
        bindMacro(
            layoutCarbs,
            getString(R.string.carbs_hint),
            food.carbs, R.drawable.bread
        )
        bindMacro(
            layoutFats,
            getString(R.string.fat_hint),
            food.fat,
            R.drawable.fat_ic
        )

        renderFoodPhoto(food.photoUrl)
        renderTimestamp(food)
        renderIngredients(food.ingredients)
        renderAdvice(food.advice)
        renderActionButtons(state)
    }

    private fun renderFoodPhoto(photoUrl: String?) {
        binding.imageViewFoodPhoto.isVisible = true
        if (!photoUrl.isNullOrEmpty()) {
            binding.imageViewFoodPhoto.loadPhoto(photoUrl)
            binding.imageViewFoodPhoto.setOnClickListener {
                showFullScreenImage(photoUrl)
            }
        } else {
            binding.imageViewFoodPhoto.loadPhoto(R.drawable.placeholder_food)
            binding.imageViewFoodPhoto.setOnClickListener(null)
        }
    }

    private fun renderTimestamp(food: FoodModel) {
        val timeStr = DateFormat.getTimeFormat(requireContext())
            .format(food.timestamp)
        val mealStr =
            MealType.fromId(food.mealType, food.timestamp)
                .getDisplayName(requireContext())
        binding.textViewTimestamp.text = getString(
            R.string.meal_date_format,
            mealStr, timeStr
        )
        binding.textViewTimestamp.isVisible = true
    }

    private fun renderIngredients(ingredients: List<AnalyzedIngredient>) {
        val show = ingredients.isNotEmpty()
        binding.layoutFoodItemsContainer.isVisible = show
        binding.recyclerFoodItems.isVisible = show
        if (show) {
            val adapter = ingredientsAdapter ?: IngredientsAdapter(
                ingredients = ingredients,
                onItemClick = { ingredient, position ->
                    EditIngredientBottomSheet(
                        args = EditIngredientArgs(ingredient, position),
                        onSave = { updatedItem -> viewModel.updateFoodItem(position, updatedItem) },
                        onDelete = { viewModel.deleteFoodItem(position) }
                    ).show(parentFragmentManager, null)
                }
            ).also {
                ingredientsAdapter = it
                binding.recyclerFoodItems.adapter = it
            }
            adapter.updateData(ingredients)
        }
    }

    private fun renderAdvice(adviceStr: String?) {
        val cleanAdvice = adviceStr?.trim()
        val showAdvice = !cleanAdvice.isNullOrBlank()
        binding.layoutAdvice.isVisible = showAdvice
        binding.cardAiHealth.isVisible = showAdvice
        binding.textViewAdvice.text = cleanAdvice
    }

    private fun renderActionButtons(state: AnalyzeResultUiState) = with(binding) {
        buttonFavorite.setImageResource(
            if (state.isFavorite) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
        )
        layoutHorizontalActions.isVisible = !state.isSaved
        buttonSaveToLog.isVisible = !state.isSaved
        layoutSaveContainer.isVisible = !state.isSaved
        progressBarButton.isVisible = state.isLoading || state.isSaving
        buttonSaveToLog.isEnabled = !state.isLoading && !state.isSaving

        if (!state.isLoading && !state.isSaving) {
            buttonSaveToLog.text = getString(R.string.save)
            buttonSaveToLog.icon = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.check_ic
            )
        } else {
            buttonSaveToLog.text = ""
            buttonSaveToLog.icon = null
        }

        buttonCancel.isVisible = !state.isSaved
        buttonCancel.text = getString(R.string.delete)
        buttonCancel.icon = ContextCompat.getDrawable(requireContext(), R.drawable.delete_ic)

        layoutRecallActions.isVisible = state.isSaved
        buttonReLogFull.isVisible = state.isSaved
        buttonDelete.isVisible = state.isSaved
    }

    private fun handleEvent(event: AnalyzeResultEvent) {
        if (!isAdded || isDetached || context == null) return
        when (event) {
            is AnalyzeResultEvent.Saved -> popBackStack(R.id.home, false)
            is AnalyzeResultEvent.ReLogged -> {
                mainActivity()?.showSnackbar(getString(R.string.food_log_relog_success))
                popBackStack()
            }
            is AnalyzeResultEvent.Deleted -> popBackStack()
            is AnalyzeResultEvent.NavigateBack -> popBackStack()
            is AnalyzeResultEvent.FoodSavedToFavorites -> {
                showAppSnackBar(
                    message = getString(R.string.analyze_result_food_saved_to_favorites),
                    anchorView = binding.layoutBottomActions
                )
            }
            is AnalyzeResultEvent.PromptShare -> shareFoodAnalysis(event.food)
            is AnalyzeResultEvent.PlayCelebration -> handleCelebration(event.score)
        }
    }

    private fun handleCelebration(score: Int) {
        if (score == 10) {
            binding.confettiAnimation.isVisible = true
            binding.confettiAnimation.playAnimation()
        }
        if (score >= 8) {
            SoundHelper.playSuccessSound(context)
        }
    }

    private fun bindMacro(
        layout: ItemMacroDisplayBinding,
        name: String,
        value: Double?,
        iconRes: Int
    ) {
        layout.imageViewMacroIcon.setImageResource(iconRes)
        layout.imageViewMacroIcon.isVisible = true
        layout.textViewMacroName.text = name
        layout.textViewMacroValue.text = NutritionFormatter.gr(value)
    }

    private fun openEditField(
        @StringRes titleRes: Int,
        value: Double,
        type: FoodFieldType,
        onSave: (Double) -> Unit
    ) {
        EditFoodFieldBottomSheet(
            args = EditFoodFieldArgs(titleRes = titleRes, currentValue = value, fieldType = type),
            onSave = onSave
        ).show(parentFragmentManager, null)
    }

    private fun setupListeners(bind: BottomSheetCalorieResultBinding) = with(bind) {
        buttonPortionMinus.setOnClickListener { viewModel.decreasePortion() }
        buttonPortionPlus.setOnClickListener { viewModel.increasePortion() }
        buttonSaveToLog.setOnSafeClickListener { viewModel.onSaveClick() }
        buttonFavorite.setOnClickListener { viewModel.toggleFavorite() }
        buttonShareFloating.setOnSafeClickListener { viewModel.shareFood() }

        buttonCancel.setOnSafeClickListener { viewModel.cancelAndExit() }
        buttonReLogFull.setOnSafeClickListener { viewModel.reLog() }
        buttonRetryAnalyze.setOnSafeClickListener { viewModel.retryAnalyze() }

        buttonDelete.setOnSafeClickListener {
            val ctx = context ?: return@setOnSafeClickListener
            ConfirmDeleteDialog.show(
                context = ctx,
                titleRes = R.string.confirm_delete_title,
                messageRes = R.string.confirm_delete_log_message,
                onConfirm = { viewModel.deleteLog() }
            )
        }

        buttonBackFloating.setOnClickListener { attemptExit() }

        layoutProtein.root.setOnClickListener {
            val currentVal = viewModel.uiState.value.food?.protein.orZero()
            openEditField(
                R.string.analyze_edit_protein_title,
                currentVal, FoodFieldType.PROTEIN
            ) {
                viewModel.updateProtein(it)
            }
        }

        layoutCarbs.root.setOnClickListener {
            val currentVal = viewModel.uiState.value.food?.carbs.orZero()
            openEditField(
                R.string.analyze_edit_carbs_title,
                currentVal, FoodFieldType.CARBS
            ) {
                viewModel.updateCarbs(it)
            }
        }

        layoutFats.root.setOnClickListener {
            val currentVal = viewModel.uiState.value.food?.fat.orZero()
            openEditField(
                R.string.analyze_edit_fat_title,
                currentVal, FoodFieldType.FAT
            ) {
                viewModel.updateFat(it)
            }
        }

        buttonAddIngredient.setOnClickListener {
            AddIngredientBottomSheet { ingredient ->
                viewModel.addIngredient(ingredient)
            }.show(childFragmentManager, null)
        }
    }

    private fun startSequentialLoadingText() {
        if (loadingTextJob?.isActive == true) return
        loadingTextJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.loadingText.text = getString(R.string.analyze_loading_identifying)
            delay(2000.milliseconds)
            binding.loadingText.text = getString(R.string.analyze_loading_calculating)
            delay(2000.milliseconds)
            binding.loadingText.text = getString(R.string.analyze_loading_ready)
        }
    }

    private fun stopSequentialLoadingText() {
        loadingTextJob?.cancel()
        loadingTextJob = null
    }

    private fun loadBanner() {
        binding.bannerContainer.loadBannerAd(AdsConfig.BANNER_HOME)
    }
}