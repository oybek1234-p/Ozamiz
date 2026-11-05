package uz.oybek.ozamiz.ui.feature.log.food

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ads.AdsConfig
import uz.oybek.ozamiz.data.analyzer.model.AnalyzedIngredient
import uz.oybek.ozamiz.data.tracker.model.FoodModel
import uz.oybek.ozamiz.databinding.BottomSheetCalorieResultBinding
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
import uz.oybek.ozamiz.data.dietolog.engine.DietologMessagePool
import uz.oybek.ozamiz.ui.utils.loadPhoto
import uz.oybek.ozamiz.ui.utils.showAppSnackBar
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class AnalyzeResultFragment : BaseFragment<BottomSheetCalorieResultBinding>() {

    private val viewModel: AnalyzeResultViewModel by viewModels()
    private var ingredientsAdapter: IngredientsAdapter? = null
    private var loadingTextJob: Job? = null
    private var currentMealHintText: String? = null

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
        currentMealHintText = null
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
            val resolvedMealType = MealType.fromId(state.mealType ?: state.food?.mealType, state.logDate)
            val hintText = currentMealHintText ?: when (resolvedMealType) {
                MealType.BREAKFAST -> DietologMessagePool.breakfastLoseWeightMessages.random()
                MealType.LUNCH -> DietologMessagePool.lunchAndActivityMessages.random()
                MealType.DINNER -> DietologMessagePool.dinnerMessages.random()
                MealType.SNACK -> DietologMessagePool.snackIntervalMessages.random()
            }.also { currentMealHintText = it }

            textAnalyzingHint.text = hintText
            textAnalyzingHint.isVisible = true

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

        textViewProteinValue.text = NutritionFormatter.gr(food.protein)
        textViewCarbsValue.text = NutritionFormatter.gr(food.carbs)
        textViewFatsValue.text = NutritionFormatter.gr(food.fat)

        renderFoodPhoto(food.photoUrl)
        renderTimestamp(food)
        renderIngredients(food.ingredients, state.isGainWeight)
        renderHealthScore(food.healthLevel)
        renderAdvice(food.advice)
        renderActionButtons(state)
    }

    private fun renderFoodPhoto(photoUrl: String?) {
        binding.imageViewFoodPhoto.isVisible = true
        if (!photoUrl.isNullOrEmpty()) {
            tryPrecalculateDimensions(photoUrl)
            binding.imageViewFoodPhoto.loadPhoto(
                photo = photoUrl,
                onResourceReady = { width, height ->
                    adjustFoodPhotoDimensions(width, height)
                }
            )
            binding.imageViewFoodPhoto.setOnClickListener {
                showFullScreenImage(photoUrl)
            }
        } else {
            binding.imageViewFoodPhoto.loadPhoto(R.drawable.placeholder_food)
            binding.imageViewFoodPhoto.setOnClickListener(null)
            adjustFoodPhotoDimensions(
                resources.displayMetrics.widthPixels,
                (260 * resources.displayMetrics.density).roundToInt()
            )
        }
    }

    private fun adjustFoodPhotoDimensions(imageWidth: Int, imageHeight: Int) {
        if (imageWidth <= 0 || imageHeight <= 0) return

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val maxHeightPx = (260 * displayMetrics.density).roundToInt()
        val maxWidthPx = screenWidth

        val imageRatio = imageWidth.toFloat() / imageHeight.toFloat()
        val containerRatio = maxWidthPx.toFloat() / maxHeightPx.toFloat()

        val targetWidth: Int
        val targetHeight: Int

        if (imageRatio > containerRatio) {
            // Landscape bo'lsa: kengligi ekran kengligicha, balandligi esa 260dp dan kichik
            targetWidth = maxWidthPx
            targetHeight = (maxWidthPx / imageRatio).roundToInt().coerceAtMost(maxHeightPx)
        } else {
            // Portrait yoki kvadrat bo'lsa: balandligi 260dp, kengligi esa proporsional hisoblanadi
            targetHeight = maxHeightPx
            targetWidth = (maxHeightPx * imageRatio).roundToInt().coerceAtMost(maxWidthPx)
        }

        binding.imageViewFoodPhoto.updateLayoutParams<ViewGroup.LayoutParams> {
            width = targetWidth
            height = targetHeight
        }
    }

    private fun tryPrecalculateDimensions(photoUrl: String) {
        try {
            val uri = photoUrl.toUri()
            val ctx = context ?: return
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }

            val stream = if (uri.scheme == "content") {
                ctx.contentResolver.openInputStream(uri)
            } else if (uri.scheme == "file" || uri.scheme == null) {
                val path = uri.path ?: photoUrl
                File(path).takeIf { it.exists() }?.inputStream()
            } else null

            stream?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            if (options.outWidth > 0 && options.outHeight > 0) {
                val rotation = getExifRotation(ctx, uri, photoUrl)
                val (w, h) = if (rotation == 90 || rotation == 270) {
                    options.outHeight to options.outWidth
                } else {
                    options.outWidth to options.outHeight
                }
                adjustFoodPhotoDimensions(w, h)
            }
        } catch (_: Throwable) {
            // Glide onResourceReady orqali o'rnatadi
        }
    }

    private fun getExifRotation(context: Context, uri: Uri, pathOrUrl: String): Int {
        return try {
            val exif = if (uri.scheme == "content") {
                context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
            } else {
                val path = uri.path ?: pathOrUrl
                ExifInterface(path)
            }
            when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Throwable) {
            0
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

    private fun renderIngredients(ingredients: List<AnalyzedIngredient>, isGainWeight: Boolean) {
        val show = ingredients.isNotEmpty()
        binding.layoutFoodItemsContainer.isVisible = show
        binding.recyclerFoodItems.isVisible = show
        if (show) {
            val adapter = ingredientsAdapter ?: IngredientsAdapter(
                ingredients = ingredients,
                isGainWeight = isGainWeight,
                onItemClick = { ingredient, position ->
                    openEditIngredient(ingredient, position)
                },
                onEditClick = { ingredient, position ->
                    openEditIngredient(ingredient, position)
                },
                onDeleteClick = { _, position ->
                    viewModel.deleteFoodItem(position)
                }
            ).also {
                ingredientsAdapter = it
                binding.recyclerFoodItems.adapter = it
            }
            adapter.updateData(ingredients, isGainWeight)
        }
    }

    private fun openEditIngredient(ingredient: AnalyzedIngredient, position: Int) {
        EditIngredientBottomSheet(
            args = EditIngredientArgs(ingredient, position),
            onSave = { updatedItem -> viewModel.updateFoodItem(position, updatedItem) },
            onDelete = { viewModel.deleteFoodItem(position) }
        ).show(parentFragmentManager, null)
    }

    private fun renderHealthScore(score: Int) {
        binding.tvHealthScoreInline.isVisible = false
    }

    private var isAdviceExpanded = false

    private fun renderAdvice(adviceStr: String?) {
        val rawAdvice = adviceStr?.trim()
        val cleanAdvice = rawAdvice
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val showAdvice = !cleanAdvice.isNullOrBlank()
        binding.layoutAdvice.isVisible = showAdvice
        binding.cardAiHealth.isVisible = showAdvice
        if (!showAdvice) return

        binding.textViewAdvice.text = cleanAdvice
        isAdviceExpanded = false
        binding.textViewAdvice.maxLines = 4
        binding.textViewAdviceMore.setText(R.string.analyze_result_advice_more)

        binding.textViewAdvice.post {
            val layout = binding.textViewAdvice.layout
            if (layout != null) {
                val lines = layout.lineCount
                val isEllipsized =
                    lines > 4 || (lines > 0 && layout.getEllipsisCount(lines - 1) > 0)
                binding.textViewAdviceMore.isVisible = isEllipsized
            }
        }

        val toggleAction = {
            isAdviceExpanded = !isAdviceExpanded
            if (isAdviceExpanded) {
                binding.textViewAdvice.maxLines = Int.MAX_VALUE
                binding.textViewAdviceMore.setText(R.string.analyze_result_advice_less)
            } else {
                binding.textViewAdvice.maxLines = 2
                binding.textViewAdviceMore.setText(R.string.analyze_result_advice_more)
            }
        }
        binding.textViewAdviceMore.setOnClickListener { toggleAction() }
        binding.textViewAdvice.setOnClickListener { toggleAction() }
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

    private fun openEditNutrition() {
        val food = viewModel.uiState.value.food ?: return
        EditNutritionBottomSheet(
            args = EditNutritionArgs(
                calories = food.calories,
                protein = food.protein.orZero(),
                carbs = food.carbs.orZero(),
                fat = food.fat.orZero()
            ),
            onSave = { calories, protein, carbs, fat ->
                viewModel.updateNutrition(calories, protein, carbs, fat)
            }
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

        buttonEditNutrition.setOnClickListener { openEditNutrition() }
        cardCalorieBanner.setOnClickListener { openEditNutrition() }

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