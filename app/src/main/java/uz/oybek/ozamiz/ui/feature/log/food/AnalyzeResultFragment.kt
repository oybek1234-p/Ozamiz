package uz.oybek.ozamiz.ui.feature.log.food

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ads.AdsConfig
import uz.oybek.ozamiz.data.ads.AdsManager
import uz.oybek.ozamiz.data.ai.analyzer.FoodAnalysis
import uz.oybek.ozamiz.data.ai.analyzer.FoodAnalysisResult
import uz.oybek.ozamiz.data.analytics.AnalyticsManager
import uz.oybek.ozamiz.data.ai.analyzer.FoodAnalyzer
import uz.oybek.ozamiz.data.config.AppPreference
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.logs.LogRepository
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.utils.formatToHhMmA
import uz.oybek.ozamiz.databinding.BottomSheetCalorieResultBinding
import uz.oybek.ozamiz.databinding.ItemMacroDisplayBinding
import uz.oybek.ozamiz.ui.MainActivity
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import uz.oybek.ozamiz.ui.utils.VaznimColors
import uz.oybek.ozamiz.ui.utils.playSuccessSound
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds

class AnalyzeResultFragment : BaseFragment<BottomSheetCalorieResultBinding>() {

    private val viewModel: AnalyzeResultViewModel by viewModels()
    private lateinit var ingredientsAdapter: IngredientsAdapter
    private var hasPlayedConfetti = false
    private var hasPlayedSuccessSound = false
    private var loadingTextJob: Job? = null

    private fun startSequentialLoadingText() {
        if (loadingTextJob?.isActive == true) return
        loadingTextJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.loadingText.text = "Ovqatni aniqlayapmiz..."
            delay(2000.milliseconds)
            binding.loadingText.text = "Kaloriyasini hisoblayapmiz..."
            delay(2000.milliseconds)
            binding.loadingText.text = "Tayyor!"
        }
    }

    private fun stopSequentialLoadingText() {
        loadingTextJob?.cancel()
        loadingTextJob = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val logId = it.getString(KEY_LOG_ID)
            val userId = it.getString(KEY_USER_ID)
            val rawTimestamp = it.getLong(KEY_TIMESTAMP, -1L)
            val timestamp = if (rawTimestamp > 0) rawTimestamp else System.currentTimeMillis()
            viewModel.setLogDate(Date(timestamp))

            val rawMeal = it.getString(KEY_MEAL_TYPE) ?: it.getString("meal")
            val meal = uz.oybek.ozamiz.data.food.model.MealTime.getValidMeal(rawMeal, timestamp)

            if (logId != null) {
                viewModel.loadLog(logId, Date(timestamp), userId)
            } else {
                val imageUri =
                    (it.getParcelable(KEY_IMAGE_URI) ?: it.getString(
                        KEY_IMAGE_URI
                    )?.toUri())
                val audioUri =
                    (it.getParcelable(KEY_AUDIO_URI) ?: it.getString(
                        KEY_AUDIO_URI
                    )?.toUri())
                val description = it.getString(KEY_PENDING_DESCRIPTION) ?: ""
                if (audioUri != null) {
                    viewModel.analyzeAudio(audioUri, meal, Date(timestamp))
                } else {
                    viewModel.startAnalyze(imageUri, description, meal, Date(timestamp))
                }
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): BottomSheetCalorieResultBinding {
        return BottomSheetCalorieResultBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: BottomSheetCalorieResultBinding) {

        setFragmentResultListener(EditIngredientBottomSheet.REQUEST_KEY) { _, bundle ->
            val action = bundle.getString(EditIngredientBottomSheet.RESULT_ACTION)
                ?: EditIngredientBottomSheet.ACTION_SAVE
            val position = bundle.getInt(EditIngredientBottomSheet.RESULT_POSITION, -1)
            if (position == -1) return@setFragmentResultListener

            if (action == EditIngredientBottomSheet.ACTION_DELETE) {
                AnalyticsManager.logFeatureUsed("analyze_ingredient_deleted")
                viewModel.deleteFoodItem(position)
            } else {
                AnalyticsManager.logFeatureUsed("analyze_ingredient_edited")
                val json = bundle.getString(EditIngredientBottomSheet.RESULT_INGREDIENT)
                    ?: return@setFragmentResultListener
                val item = com.google.gson.Gson()
                    .fromJson(json, uz.oybek.ozamiz.data.ai.analyzer.AnalyzedIngredient::class.java)
                viewModel.updateFoodItem(position, item)
            }
        }

        binding.toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        binding.toolbar.background =
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        binding.toolbar.elevation = 0f

        binding.toolbar.inflateMenu(R.menu.menu_analyze_result)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
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
        setupListeners()
        setupUI()
        loadBanner()

        if (arguments?.getBoolean(KEY_IS_FROM_FAVORITE) == true) {
            val args = requireArguments()
            val logId = args.getString(KEY_LOG_ID)
            if (logId == null || viewModel.state.value.result == null) {
                val analysis = FoodAnalysis(
                    id = args.getString(KEY_DESCRIPTION).hashCode(),
                    photoUrl = args.getString(KEY_PHOTO_URL)?.takeIf { it != "null" },
                    result = FoodAnalysisResult(
                        name = args.getString(KEY_DESCRIPTION) ?: "",
                        calories = args.getDouble(KEY_CALORIES).toInt(),
                        protein = args.getDouble(KEY_PROTEIN),
                        fat = args.getDouble(KEY_FAT),
                        carbs = args.getDouble(KEY_CARBS),
                        healthScore = 0
                    ),
                    mealType = args.getString(KEY_MEAL_TYPE) ?: ""
                )
                viewModel.setRecallResult(analysis)
            }
        }
    }

    private fun loadBanner() {
        if (user.isPremium()) {
            binding.bannerContainer.isVisible = false
            return
        }
        try {
            val bannerView = AdsManager.createBannerView(requireContext(), AdsConfig.BANNER_HOME)
            binding.bannerContainer.removeAllViews()
            binding.bannerContainer.addView(bannerView)
            binding.bannerContainer.isVisible = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showAnalyzing(isAnalyzing: Boolean) {
        val wasAnalyzing = binding.lottieLoadingCalories.isVisible && !isAnalyzing

        binding.layoutMacros.isVisible = !isAnalyzing
        binding.layoutPortionCounter.isVisible = !isAnalyzing
        binding.cardCalorieBanner.isVisible = !isAnalyzing
        binding.cardLoadingState.isVisible = isAnalyzing
        binding.cardErrorState.isVisible = false  // analyzing boshlanganda error yashiriladi
        binding.buttonFavorite.isVisible = !isAnalyzing && viewModel.state.value.result != null
        binding.buttonShareFloating.isVisible = !isAnalyzing && viewModel.state.value.result != null

        if (isAnalyzing) {
            startSequentialLoadingText()
            binding.layoutBottomActions.visibility = View.VISIBLE
            binding.layoutRecallActions.isVisible = false
            binding.layoutHorizontalActions.isVisible = true
            binding.buttonSaveToLog.isVisible = false
            binding.layoutSaveContainer.isVisible = false
            binding.buttonCancel.isVisible = true
            binding.buttonCancel.text = getString(R.string.delete)
            binding.buttonCancel.icon =
                ContextCompat.getDrawable(requireContext(), R.drawable.delete_ic)

            binding.healthBall.isVisible = false
            binding.cardAiHealth.isVisible = false
        } else {
            stopSequentialLoadingText()
            binding.cardLoadingState.isVisible = false
            binding.cardCalorieBanner.isVisible = true
            binding.layoutPortionCounter.isVisible = true
            binding.cardAiHealth.isVisible = true
            binding.healthBall.isVisible = false

            binding.buttonCancel.isVisible = true
            binding.buttonCancel.text = getString(R.string.delete)
            binding.buttonCancel.icon =
                ContextCompat.getDrawable(requireContext(), R.drawable.delete_ic)
            binding.buttonSaveToLog.isVisible = true
            binding.layoutSaveContainer.isVisible = true
            val isSaved = viewModel.state.value.isSaved
            binding.buttonSaveToLog.text =
                if (isSaved) getString(R.string.food_saved) else getString(R.string.save)
            binding.buttonSaveToLog.icon =
                ContextCompat.getDrawable(requireContext(), R.drawable.check_ic)

            if (wasAnalyzing) {
                binding.layoutBottomActions.alpha = 0f
                binding.layoutBottomActions.visibility = View.VISIBLE
                binding.layoutBottomActions.animate().alpha(1f).setDuration(500).start()
            } else {
                binding.layoutBottomActions.visibility = View.VISIBLE
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBarButton.isVisible = show
        binding.buttonSaveToLog.isEnabled = !show
        binding.buttonFavorite.isEnabled = !show
        binding.buttonCancel.isEnabled = !show
        binding.buttonReLogFull.isEnabled = !show
        binding.buttonDelete.isEnabled = !show

        if (show) {
            binding.buttonSaveToLog.text = ""
            binding.buttonSaveToLog.icon = null
        } else {
            val isSaved = viewModel.state.value.isSaved
            binding.buttonSaveToLog.text =
                if (isSaved) getString(R.string.food_saved) else getString(R.string.save)
            binding.buttonSaveToLog.icon =
                ContextCompat.getDrawable(requireContext(), R.drawable.check_ic)
        }
    }

    private fun setupUI() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                showAnalyzing(state.isAnalyzing)

                if (state.isAnalyzing) {
                    if (!state.pendingDescription.isNullOrBlank()) {
                        binding.textViewFoodName.text =
                            if (state.pendingDescription == "pending_description_voice") {
                                getString(R.string.voice_input_pending)
                            } else {
                                state.pendingDescription
                            }
                        binding.layoutFoodName.isVisible = true
                    }

                    binding.imageViewFoodPhoto.isVisible = true
                    if (state.pendingImageUri != null) {
                        Glide.with(this@AnalyzeResultFragment).load(state.pendingImageUri)
                            .placeholder(R.drawable.placeholder_food)
                            .error(R.drawable.placeholder_food).into(binding.imageViewFoodPhoto)
                    } else {
                        Glide.with(this@AnalyzeResultFragment).load(R.drawable.placeholder_food)
                            .into(binding.imageViewFoodPhoto)
                    }
                    return@collect
                }

                showLoading(state.isLoading || state.isSaving)

                if (state.isError) {
                    binding.cardErrorState.isVisible = true
                    binding.cardCalorieBanner.isVisible = false
                    binding.layoutMacros.isVisible = false
                    binding.layoutPortionCounter.isVisible = false
                    binding.cardAiHealth.isVisible = false
                    binding.layoutFoodItemsContainer.isVisible = false
                    binding.layoutBottomActions.isVisible = false
                    binding.buttonFavorite.isVisible = false
                    binding.buttonShareFloating.isVisible = false
                    return@collect
                }
                binding.cardErrorState.isVisible = false

                val analysisResult = state.result?.result ?: run {
                    if (!state.pendingDescription.isNullOrBlank()) {
                        binding.textViewFoodName.text =
                            if (state.pendingDescription == "pending_description_voice") {
                                getString(R.string.voice_input_pending)
                            } else {
                                state.pendingDescription
                            }
                        binding.layoutFoodName.isVisible = true
                    }
                    return@collect
                }

                updateMacrosAndCalorie(analysisResult)
                val photoUrl = state.result.photoUrl
                if (!photoUrl.isNullOrEmpty() && photoUrl != "null") {
                    binding.imageViewFoodPhoto.isVisible = true
                    Glide.with(this@AnalyzeResultFragment).load(photoUrl)
                        .placeholder(R.drawable.placeholder_food).error(R.drawable.placeholder_food)
                        .into(binding.imageViewFoodPhoto)

                    binding.imageViewFoodPhoto.setOnClickListener {
                        uz.oybek.ozamiz.ui.common.dialog.FullScreenImageDialog.newInstance(
                            photoUrl
                        ).show(parentFragmentManager, "full_image")
                    }
                } else if (state.pendingImageUri != null) {
                    binding.imageViewFoodPhoto.isVisible = true
                    Glide.with(this@AnalyzeResultFragment).load(state.pendingImageUri)
                        .placeholder(R.drawable.placeholder_food).error(R.drawable.placeholder_food)
                        .into(binding.imageViewFoodPhoto)
                } else {
                    binding.imageViewFoodPhoto.isVisible = true
                    Glide.with(this@AnalyzeResultFragment).load(R.drawable.placeholder_food)
                        .into(binding.imageViewFoodPhoto)
                }
                binding.textViewFoodName.text = analysisResult.name.replace(
                    Regex("^[\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\u2600-\\u27BF\\uFE0F\\u200D\\s]+"), ""
                ).trim()
                binding.layoutFoodName.isVisible = true
                val portion = analysisResult.portionMultiplier ?: 1.0
                currentPortionMultiplier = portion
                val countText = if (currentPortionMultiplier % 1.0 == 0.0) {
                    currentPortionMultiplier.toInt().toString()
                } else {
                    String.format(java.util.Locale.US, "%.1f", currentPortionMultiplier)
                }
                binding.textViewPortionCount.text = countText

                binding.healthBall.isVisible = false
                val timestampMs = analysisResult.timestamp.takeIf { it > 0 } ?: (arguments?.getLong(
                    KEY_TIMESTAMP,
                    -1L
                )?.takeIf { it > 0 }) ?: System.currentTimeMillis()
                val timeStr = android.text.format.DateFormat.getTimeFormat(requireContext())
                    .format(Date(timestampMs))
                val rawMeal = state.result.mealType
                val mealStr = when (rawMeal.lowercase()) {
                    "breakfast", "nonushta" -> getString(R.string.category_breakfast)
                    "lunch", "tushlik" -> getString(R.string.category_lunch)
                    "dinner", "kechki ovqat", "kechki" -> getString(R.string.category_dinner)
                    "snack", "perekus" -> getString(R.string.category_snack)
                    else -> rawMeal.takeIf { it.isNotBlank() } ?: ""
                }
                val fullTimeBadge = if (mealStr.isNotBlank()) "$mealStr • $timeStr" else timeStr
                binding.textViewTimestamp.text = fullTimeBadge
                binding.textViewTimestamp.isVisible = true

                val foodItemsList = analysisResult.getNormalizedFoodItems()
                val showTarkibi = foodItemsList.isNotEmpty()
                binding.layoutFoodItemsContainer.isVisible = showTarkibi
                binding.recyclerFoodItems.isVisible = showTarkibi

                if (foodItemsList.isNotEmpty()) {
                    if (!::ingredientsAdapter.isInitialized) {
                        ingredientsAdapter = IngredientsAdapter(
                            ingredients = foodItemsList, onItemClick = { ingredient, position ->
                                EditIngredientBottomSheet.newInstance(ingredient, position)
                                    .show(parentFragmentManager, EditIngredientBottomSheet.TAG)
                            })
                        binding.recyclerFoodItems.adapter = ingredientsAdapter
                    } else {
                        ingredientsAdapter.updateData(foodItemsList)
                    }
                }


                if (!hasPlayedConfetti) {
                    val score = analysisResult.healthScore ?: 0

                    if (score == 10) {
                        hasPlayedConfetti = true
                        binding.confettiAnimation.isVisible = true
                        binding.confettiAnimation.playAnimation()
                    }
                }
                if (!hasPlayedSuccessSound) {
                    val score = analysisResult.healthScore ?: 0
                    if (score >= 8) {
                        hasPlayedSuccessSound = true
                        playSuccessSound()
                    }
                }
                val adviceStr = analysisResult.advice?.trim()
                val showAdvice = !adviceStr.isNullOrBlank()
                binding.layoutAdvice.isVisible = showAdvice
                binding.cardAiHealth.isVisible = showAdvice
                binding.textViewAdvice.text = adviceStr

                binding.buttonFavorite.isVisible = !state.isAnalyzing && state.result != null
                binding.buttonFavorite.setImageResource(
                    if (state.isFavorite) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
                )
                binding.buttonShareFloating.isVisible = !state.isAnalyzing && state.result != null

                binding.layoutHorizontalActions.isVisible = !state.isRecallMode
                binding.buttonSaveToLog.isVisible = !state.isRecallMode
                binding.layoutSaveContainer.isVisible = !state.isRecallMode
                if (!state.isLoading && !state.isSaving) {
                    binding.buttonSaveToLog.text =
                        if (state.isSaved) getString(R.string.food_saved) else getString(R.string.save)
                    binding.buttonSaveToLog.icon =
                        ContextCompat.getDrawable(requireContext(), R.drawable.check_ic)
                }

                binding.buttonCancel.isVisible = !state.isRecallMode
                binding.buttonCancel.text = getString(R.string.delete)
                binding.buttonCancel.icon = ContextCompat.getDrawable(
                    requireContext(), R.drawable.delete_ic
                )
                binding.layoutRecallActions.isVisible = state.isRecallMode
                binding.buttonReLogFull.isVisible = state.isRecallMode
                binding.buttonDelete.isVisible =
                    state.isRecallMode && arguments?.getString(KEY_LOG_ID) != null
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.collect { event ->
                when (event) {
                    is AnalyzeResultEvent.Saved -> {
                        val todaySummary = withContext(Dispatchers.IO) {
                            LogRepository.getDailySummarySync(Date(), forceRefresh = true)
                        }
                        if (!isAdded || isDetached || context == null) return@collect
                        val currentFoodCals = viewModel.state.value.result?.result?.getEffectiveCalories() ?: 0
                        val effectiveConsumed = maxOf(todaySummary.consumedCalories, currentFoodCals)
                        val targetCalories =
                            if (user.targetCalories > 0) user.targetCalories else DietController.calculateTargetCalories(
                                user
                            )
                        val isGoalReached =
                            targetCalories > 0 && effectiveConsumed >= (targetCalories - 100)

                        val todayStr = DateUtils.todayString()
                        if (isGoalReached && AppPreference.lastDailyGoalReachedDate != todayStr) {
                            AppPreference.lastDailyGoalReachedDate = todayStr
                            navigate(R.id.action_analyzeResultFragment_to_dailyGoalAnalysisFragment)
                        } else {
                            popBackStack(R.id.home, false)
                        }
                    }

                    is AnalyzeResultEvent.PromptShare -> {
                        shareFoodAnalysis(event)
                    }
                }
            }
        }
    }

    private fun updateMacrosAndCalorie(result: FoodAnalysisResult) {
        binding.textViewFoodName.text = result.name
        val totalCalories = result.getEffectiveCalories()
        binding.textViewCalories.text = totalCalories.toString()

        bindMacro(
            binding.layoutProtein,
            getString(R.string.protein),
            result.getEffectiveProtein(),
            R.drawable.egg_ic
        )
        bindMacro(
            binding.layoutCarbs,
            getString(R.string.carbs_hint),
            result.getEffectiveCarbs(),
            R.drawable.bread
        )
        bindMacro(
            binding.layoutFats,
            getString(R.string.fat_hint),
            result.getEffectiveFat(),
            R.drawable.fat_ic
        )
    }

    @SuppressLint("SetTextI18n")
    private fun bindMacro(
        layout: ItemMacroDisplayBinding, name: String, value: Double?, iconRes: Int
    ) {
        layout.imageViewMacroIcon.setImageResource(iconRes)
        layout.imageViewMacroIcon.isVisible = true
        layout.textViewMacroName.text = name
        val totalValue = (value ?: 0.0)
        layout.textViewMacroValue.text = getString(R.string.gram_format, totalValue.toInt())
    }


    private var currentPortionMultiplier: Double = 1.0

    private fun setupListeners() {
        binding.buttonPortionMinus.setOnClickListener {
            if (currentPortionMultiplier > 0.5) {
                currentPortionMultiplier -= 0.5
                val countText = if (currentPortionMultiplier % 1.0 == 0.0) {
                    currentPortionMultiplier.toInt().toString()
                } else {
                    String.format(java.util.Locale.US, "%.1f", currentPortionMultiplier)
                }
                binding.textViewPortionCount.text = countText
                viewModel.scalePortion(currentPortionMultiplier)
            }
        }

        binding.buttonPortionPlus.setOnClickListener {
            if (currentPortionMultiplier < 10.0) {
                currentPortionMultiplier += 0.5
                val countText = if (currentPortionMultiplier % 1.0 == 0.0) {
                    currentPortionMultiplier.toInt().toString()
                } else {
                    String.format(java.util.Locale.US, "%.1f", currentPortionMultiplier)
                }
                binding.textViewPortionCount.text = countText
                viewModel.scalePortion(currentPortionMultiplier)
            }
        }

        binding.buttonSaveToLog.setOnSafeClickListener {
            viewModel.onSaveClick()
        }

        binding.buttonFavorite.setOnClickListener {
            viewModel.toggleFavorite()
        }

        binding.buttonShareFloating.setOnSafeClickListener {
            viewModel.shareFood()
        }

        binding.buttonCancel.setOnSafeClickListener {
            AnalyticsManager.logFeatureUsed("analyze_food_deleted")
            viewModel.cancelAndExit()
            popBackStack(R.id.home, false)
        }

        binding.buttonReLogFull.setOnSafeClickListener {
            viewModel.reLog()
        }

        binding.buttonRetryAnalyze.setOnSafeClickListener {
            viewModel.retryAnalyze()
        }

        binding.buttonDelete.setOnSafeClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_log_message)
                .setPositiveButton(R.string.yes) { _, _ ->
                    AnalyticsManager.logFeatureUsed("analyze_food_deleted")
                    viewModel.deleteLog { success ->
                        if (success && isAdded && !isDetached && view != null) {
                            popBackStack()
                        }
                    }
                }.setNegativeButton(R.string.no, null).show()
        }
        binding.buttonBackFloating.setOnClickListener {
            val isRecall = viewModel.state.value.isRecallMode
            if (isRecall) {
                popBackStack()
            } else {
                popBackStack(R.id.home, false)
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            val isRecall = viewModel.state.value.isRecallMode
            if (isRecall) {
                popBackStack()
            } else {
                popBackStack(R.id.home, false)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    isEnabled = false
                    val isRecall = viewModel.state.value.isRecallMode
                    if (isRecall) {
                        if (!popBackStack()) {
                            isEnabled = true
                        }
                    } else {
                        if (!popBackStack(R.id.home, false)) {
                            isEnabled = true
                        }
                    }
                }
            })

        val calorieEditListener = View.OnClickListener {
            val res = viewModel.state.value.result?.result
            val currentVal = res?.getEffectiveCalories()?.toDouble() ?: 0.0
            showEditNumberDialog(getString(R.string.kaloriya), currentVal) { newValue ->
                viewModel.updateCalories(newValue)
            }
        }
        binding.buttonEditCalories.setOnClickListener(calorieEditListener)
        binding.calorieCard.setOnClickListener(calorieEditListener)

        binding.layoutProtein.root.setOnClickListener {
            val res = viewModel.state.value.result?.result
            val currentVal = res?.getEffectiveProtein() ?: 0.0
            showEditNumberDialog(getString(R.string.edit_protein_title), currentVal) { newValue ->
                viewModel.updateProtein(newValue)
            }
        }
        binding.layoutCarbs.root.setOnClickListener {
            val res = viewModel.state.value.result?.result
            val currentVal = res?.getEffectiveCarbs() ?: 0.0
            showEditNumberDialog(getString(R.string.edit_carbs_title), currentVal) { newValue ->
                viewModel.updateCarbs(newValue)
            }
        }
        binding.layoutFats.root.setOnClickListener {
            val res = viewModel.state.value.result?.result
            val currentVal = res?.getEffectiveFat() ?: 0.0
            showEditNumberDialog(getString(R.string.edit_fat_title), currentVal) { newValue ->
                viewModel.updateFat(newValue)
            }
        }

        binding.buttonAddIngredient.setOnClickListener {
            AddIngredientBottomSheet().show(childFragmentManager, AddIngredientBottomSheet.TAG)
        }

        childFragmentManager.setFragmentResultListener(
            AddIngredientBottomSheet.REQUEST_KEY, viewLifecycleOwner
        ) { _, bundle ->
            val input = bundle.getString(AddIngredientBottomSheet.RESULT_INPUT)
            val singleJson = bundle.getString(AddIngredientBottomSheet.RESULT_SINGLE_JSON)
            val singleResult = singleJson?.let {
                uz.oybek.ozamiz.data.utils.gson?.fromJson(
                    it, uz.oybek.ozamiz.data.ai.analyzer.SingleIngredientResult::class.java
                )
            }
            if (!input.isNullOrBlank()) {
                AnalyticsManager.logFeatureUsed("analyze_ingredient_added")
                viewModel.addIngredientWithAi(input, singleResult)
            }
        }
    }

    private fun showEditNumberDialog(
        title: String, currentValue: Double, onSave: (Double) -> Unit
    ) {
        val input = android.widget.EditText(requireContext())
        val initialText = if (currentValue % 1.0 == 0.0) currentValue.toInt()
            .toString() else currentValue.toString()
        input.setText(initialText)
        input.setSelection(initialText.length)
        input.inputType =
            android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.marginStart = resources.getDimensionPixelSize(R.dimen.dimen_24)
        params.marginEnd = resources.getDimensionPixelSize(R.dimen.dimen_24)
        input.layoutParams = params
        container.addView(input)

        val dialog = AlertDialog.Builder(requireContext()).setTitle(title).setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val rawText = input.text.toString().replace(',', '.').trim()
                val newValue = rawText.toDoubleOrNull()
                if (newValue != null) {
                    onSave(newValue)
                }
            }.setNegativeButton(R.string.cancel, null).create()

        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        dialog.setOnShowListener {
            input.postDelayed({
                if (isAdded) {
                    input.requestFocus()
                    val length = input.text?.length ?: 0
                    input.setSelection(length)
                    val imm =
                        requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                    imm?.showSoftInput(
                        input,
                        android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
                    )
                }
            }, 100)
        }

        dialog.show()
    }

    private fun shareFoodAnalysis(event: AnalyzeResultEvent.PromptShare) {
        val cleanTitle = event.name.replace(
            Regex("^[\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\u2600-\\u27BF\\uFE0F\\u200D\\s]+"), ""
        ).trim()

        AnalyticsManager.logFeatureUsed(
            featureName = "food_shared",
            extraParams = mapOf(
                "user_id" to user.uid,
                "user_name" to user.name,
                "food_name" to cleanTitle
            )
        )

        val timeStr = if (event.timestamp > 0) {
            android.text.format.DateFormat.getTimeFormat(requireContext())
                .format(Date(event.timestamp))
        } else ""

        val shareText = buildShareText(
            name = event.name,
            mealType = event.mealType,
            timeStr = timeStr,
            calories = event.calories,
            protein = event.protein,
            fat = event.fat,
            carbs = event.carbs,
            advice = event.advice
        )

        val imageSource: Any? = event.photoUrl?.takeIf { it.isNotBlank() && it != "null" }
            ?: viewModel.state.value.pendingImageUri
            ?: arguments?.getParcelable<Uri>(KEY_IMAGE_URI)
            ?: arguments?.getString(KEY_IMAGE_URI)?.takeIf { it.isNotBlank() && it != "null" }

        viewLifecycleOwner.lifecycleScope.launch {
            val shareUri: Uri? = withContext(Dispatchers.IO) {
                if (imageSource != null) {
                    try {
                        val bitmap = Glide.with(requireContext().applicationContext)
                            .asBitmap()
                            .load(imageSource)
                            .submit()
                            .get()

                        val shareFile = File(requireContext().cacheDir, "shared_food.jpg")
                        shareFile.outputStream().use { out ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.provider",
                            shareFile
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } else null
            }

            if (!isAdded || isDetached || context == null) return@launch

            val intent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, cleanTitle)
                putExtra(Intent.EXTRA_TITLE, cleanTitle)

                if (shareUri != null) {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val clipData = android.content.ClipData.newUri(requireContext().contentResolver, cleanTitle, shareUri)
                    clipData.addItem(android.content.ClipData.Item(shareText))
                    this.clipData = clipData
                } else {
                    type = "text/plain"
                    this.clipData = android.content.ClipData.newPlainText(cleanTitle, shareText)
                }
            }

            try {
                val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Food Analysis", shareText)
                clipboard?.setPrimaryClip(clip)
            } catch (e: Exception) {
                // Ignore
            }

            val chooser = Intent.createChooser(intent, "Ovqat tahlilini ulashish").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(chooser)
        }
    }

    private fun buildShareText(
        name: String,
        mealType: String,
        timeStr: String,
        calories: Int,
        protein: Double?,
        fat: Double?,
        carbs: Double?,
        advice: String?
    ): String {
        val cleanName = name.replace(
            Regex("^[\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\u2600-\\u27BF\\uFE0F\\u200D\\s]+"), ""
        ).trim()

        val mealStr = when (mealType.lowercase()) {
            "breakfast", "nonushta" -> getString(R.string.category_breakfast)
            "lunch", "tushlik" -> getString(R.string.category_lunch)
            "dinner", "kechki ovqat", "kechki" -> getString(R.string.category_dinner)
            "snack", "perekus" -> getString(R.string.category_snack)
            else -> mealType.takeIf { it.isNotBlank() } ?: ""
        }

        val fullBadge = if (mealStr.isNotBlank() && timeStr.isNotBlank()) {
            "$mealStr • $timeStr"
        } else mealStr.ifBlank { timeStr }

        val p = (protein ?: 0.0).toInt()
        val f = (fat ?: 0.0).toInt()
        val c = (carbs ?: 0.0).toInt()

        val sb = StringBuilder()
        sb.append("🥗 $cleanName\n\n")

        if (fullBadge.isNotBlank()) {
            sb.append("🍽 Mahall: $fullBadge\n")
        }
        sb.append("🔥 Kaloriya: $calories kkal\n")
        sb.append("🥩 Oqsil: ${p}g | 🥑 Yog': ${f}g | 🍞 Uglevod: ${c}g\n\n")

        val cleanAdvice = advice?.trim()
        if (!cleanAdvice.isNullOrBlank()) {
            sb.append("💡 Tavsiya:\n$cleanAdvice\n\n")
        }

        sb.append("📱 Vaznim ilovasi orqali tahlil qilindi")

        return sb.toString()
    }

    companion object {
        const val KEY_IMAGE_URI = "imageUri"
        const val KEY_AUDIO_URI = "audioUri"
        const val KEY_PENDING_DESCRIPTION = "pendingDescription"
        const val KEY_LOG_ID = "logId"
        const val KEY_USER_ID = "userId"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_IS_FROM_FAVORITE = "isFromFavorite"
        const val KEY_DESCRIPTION = "description"
        const val KEY_CALORIES = "calories"
        const val KEY_PROTEIN = "protein"
        const val KEY_FAT = "fat"
        const val KEY_CARBS = "carbs"
        const val KEY_PHOTO_URL = "photoUrl"
        const val KEY_MEAL_TYPE = "mealType"
    }
}