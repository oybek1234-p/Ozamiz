package uz.oybek.ozamiz.ui.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import androidx.core.os.bundleOf
import androidx.transition.TransitionManager
import com.google.android.material.progressindicator.CircularProgressIndicator
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ads.AdsConfig
import uz.oybek.ozamiz.data.ads.AdsManager
import uz.oybek.ozamiz.data.config.AppPreference
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.logs.model.UserExercise
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.streak.StreakManager
import uz.oybek.ozamiz.data.streak.StreakRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.user.model.daysFromRegister
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.databinding.FragmentHomeBinding
import uz.oybek.ozamiz.ui.MainActivity
import android.net.Uri
import androidx.fragment.app.setFragmentResultListener
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.log.food.FoodCameraFragment
import uz.oybek.ozamiz.ui.feature.log.food.AnalyzeResultFragment
import uz.oybek.ozamiz.ui.feature.log.food.LogFoodFragment
import uz.oybek.ozamiz.ui.feature.log.exercise.ExerciseAchievementDialog
import uz.oybek.ozamiz.ui.feature.log.water.LogWaterDialog
import uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import uz.oybek.ozamiz.ui.feature.streak.StreakInfoSheet
import uz.oybek.ozamiz.ui.feature.streak.StreakFreezeSheet
import uz.oybek.ozamiz.ui.utils.VaznimColors
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.triggerVibration
import java.util.Date
import kotlin.math.absoluteValue

class LogFragment : BaseFragment<FragmentHomeBinding>() {

    override val screenName: String = "HomeFragment"

    private val viewModel: HomeViewModel by activityViewModels()
    private var dateAdapter: DateScrollerAdapter? = null

    private var animateProgress = true
    private var stateCollectCount = 0
    private var lastSelectedDate: Date? = null
    private var hasShownFeedbackDialogThisSession = false

    private fun animateIntegerText(
        textView: android.widget.TextView,
        fromValue: Int,
        toValue: Int,
        duration: Long = 450L,
        formatter: ((Int) -> String)? = null
    ) {
        if (fromValue == toValue) {
            textView.text = formatter?.invoke(toValue) ?: toValue.toString()
            return
        }
        val animator = ValueAnimator.ofInt(fromValue, toValue).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val valInt = anim.animatedValue as Int
                textView.text = formatter?.invoke(valInt) ?: valInt.toString()
            }
        }
        animator.start()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val bundle = viewModel.pendingCameraArgs
            if (bundle != null) {
                navigate(R.id.foodCameraFragment, bundle)
                viewModel.setPendingCameraArgs(null)
            }
        }
    }

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentHomeBinding) {
        initCalendar()
        initCalorieAndMacros()
        initMeals()
        initWater()
        initExercise()
        initWeightLogListener()
        initFeaturesCards()
        initMealRecommendation()
        initCustomCards()
        observeUiState()
        observeStreakUpdate()
        observeExerciseAdded()
        checkNotificationPermission()
        setupLimitExceededResultListener()
        setupExerciseAddedResultListener()
        loadBanner()
        if (animateProgress) {
            lifecycleScope.launch {
                delay(200)
                animateProgress = false
            }
        }
    }

    private fun loadBanner() {
        if (user.isPremium()) {
            binding.bannerAdContainerHome.isVisible = false
            return
        }
        try {
            val bannerView = AdsManager.createBannerView(requireContext(), AdsConfig.BANNER_HOME)
            binding.bannerAdContainerHome.removeAllViews()
            binding.bannerAdContainerHome.addView(bannerView)
            binding.bannerAdContainerHome.isVisible = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupExerciseAddedResultListener() {
        // Exercise added praise screen disabled per user requirement
    }

    private fun setupLimitExceededResultListener() {
        // Disabled popup screens per user request
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dateAdapter = null
        lastSelectedDate = null
    }

    private fun checkNotificationPermission() {
        val ctx = context ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    ctx, android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun initCalendar() = binding.apply {
        if (dateAdapter == null) {
            dateAdapter = DateScrollerAdapter { date ->
                viewModel.onDateSelected(date)
                currentDate = date
            }
        }
        dateScrollerRecyclerView.apply {
            adapter = dateAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            itemAnimator = null
            alpha = 1f
        }
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
        toolbarTitle.setOnClickListener {
            val isVisible = dateScrollerRecyclerView.visibility == View.VISIBLE
            TransitionManager.beginDelayedTransition(appBarLayout)
            if (isVisible) {
                dateScrollerRecyclerView.visibility = View.GONE
                toolbarTitle.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_arrow_down,
                    0
                )
            } else {
                dateScrollerRecyclerView.visibility = View.VISIBLE
                toolbarTitle.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_arrow_up,
                    0
                )
            }
        }
    }

    private fun initCalorieAndMacros() = binding.mainProgress.apply {
        cardProtein.setOnClickListener {
            navigate(R.id.proteinGuideFragment)
        }
        cardFat.setOnClickListener {
            navigate(R.id.lipidsGuideFragment)
        }
        cardCarbs.setOnClickListener {
            navigate(R.id.carbsGuideFragment)
        }
        btnHowFatBurned.setOnSafeClickListener {
            InfoBottomSheet.newInstance(
                getString(R.string.how_to_burn_fat_title),
                getString(R.string.how_to_burn_fat_content)
            ).show(childFragmentManager, InfoBottomSheet.TAG)
        }
        btnHungryWhatToEat.setOnSafeClickListener {
            InfoBottomSheet.newInstance(
                getString(R.string.hungry_what_to_eat_title),
                getString(R.string.hungry_what_to_eat_content)
            ).show(childFragmentManager, InfoBottomSheet.TAG)
        }
    }

    private fun openFoodCamera(mealId: String) {
        checkLimitAndProceed {
            val timestamp = viewModel.uiState.value.selectedDate.time
            val cameraBundle = bundleOf(
                "mealType" to mealId,
                "timestamp" to timestamp,
                "openCamera" to true
            )
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.CAMERA
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                navigate(R.id.foodCameraFragment, cameraBundle)
            } else {
                viewModel.setPendingCameraArgs(cameraBundle)
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    private fun initMeals() = binding.apply {
        fun onMealClick(meal: String) {
            LogFoodFragment.navigate(
                fragment = this@LogFragment,
                timeStamp = viewModel.uiState.value.selectedDate.time,
                meal = meal,
                openCamera = false
            )
        }

        fun onAddMealQuickClick(meal: String) {
            openFoodCamera(meal)
        }

        cardBreakfast.root.setOnSafeClickListener { onMealClick(MealTime.Breakfast.id) }
        cardBreakfast.btnAddMeal.setOnSafeClickListener { onAddMealQuickClick(MealTime.Breakfast.id) }
        cardLunch.root.setOnSafeClickListener { onMealClick(MealTime.Lunch.id) }
        cardLunch.btnAddMeal.setOnSafeClickListener { onAddMealQuickClick(MealTime.Lunch.id) }
        cardDinner.root.setOnSafeClickListener { onMealClick(MealTime.Dinner.id) }
        cardDinner.btnAddMeal.setOnSafeClickListener { onAddMealQuickClick(MealTime.Dinner.id) }
        cardSnack.root.setOnSafeClickListener { onMealClick(MealTime.Snack.id) }
        cardSnack.btnAddMeal.setOnSafeClickListener { onAddMealQuickClick(MealTime.Snack.id) }

        cardBreakfast.imgMealIcon.setImageResource(R.drawable.breakfast_simple)
        cardBreakfast.textMealName.text = getString(R.string.breakfast)
        cardLunch.imgMealIcon.setImageResource(R.drawable.lunch_simple)
        cardLunch.textMealName.text = getString(R.string.lunch)
        cardDinner.imgMealIcon.setImageResource(R.drawable.dinner_simple)
        cardDinner.textMealName.text = getString(R.string.dinner)
        cardSnack.imgMealIcon.setImageResource(R.drawable.apple_simple)
        cardSnack.textMealName.text = getString(R.string.snack)
    }

    private fun initWater() = binding.apply {
        cardWater.setOnSafeClickListener {
            LogWaterDialog().show(childFragmentManager, "LogWaterDialog")
        }
        btnAddWater.setOnSafeClickListener {
            LogWaterDialog().show(childFragmentManager, "LogWaterDialog")
        }
        childFragmentManager.setFragmentResultListener(
            KEY_WATER_ADDED_REQUEST, viewLifecycleOwner
        ) { _, bundle ->
            val added = bundle.getBoolean(KEY_ADDED, false)
            if (added) {
                triggerVibration()
            }
        }
    }

    private fun initExercise() = binding.apply {
        cardExercise.setOnSafeClickListener {
            navigate(R.id.selectExerciseFragment)
        }
        btnAddExercise.setOnSafeClickListener {
            navigate(R.id.selectExerciseFragment)
        }
    }

    private fun initWeightLogListener() = binding.apply {
        cardWeightLog.setOnSafeClickListener {
            navigate(R.id.addWeightPhotoFragment)
        }
        btnAddWeightHome.setOnSafeClickListener {
            navigate(R.id.addWeightPhotoFragment)
        }
    }

    private fun initFeaturesCards() = binding.apply {
        cardWhatToEat.setOnSafeClickListener {
            navigate(R.id.plateRuleFragment)
        }
    }

    private fun initMealRecommendation() = binding.apply {
        // card_meal_recommendation_home was removed from layout
    }

    private fun initCustomCards() = binding.apply {
        cardDietologistHome.setOnSafeClickListener {
            val bundle = Bundle().apply {
                putBoolean("showBackButton", true)
            }
            navigate(R.id.aiChatFragment, bundle)
        }
        cardCommunityHome.setOnSafeClickListener {
            val bundle = Bundle().apply {
                putBoolean("showBackButton", true)
                putBoolean("selectWeightLoss", true)
            }
            navigate(R.id.communityFragment, bundle)
        }
        cardPremiumChecking.setOnSafeClickListener {
            navigate(R.id.premiumFragment)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        updateCalendar(state)
                        updateCalorieAndMacros(state)
                        updateMeals(state)
                        updateWater(state)
                        updateExercise(state)
                        updateStreak(state)
                        updateAppFeedbackCard(state)
                        binding.cardWeightLog.isVisible = state.showWeightLogCard
                        binding.cardPremiumChecking.isVisible = state.paymentChecking
                    }
                }
                launch {
                    viewModel.showStreakFreezeEvent.collect {
                        StreakFreezeSheet.show(childFragmentManager)
                    }
                }
                launch {
                    viewModel.showStreakSheetEvent.collect { (streakInfo, returnedAfterManyDays) ->
                        val sheet = StreakInfoSheet.newInstance(
                            streakInfo = streakInfo,
                            isNewAction = true,
                            returnedAfterManyDays = returnedAfterManyDays
                        )
                        sheet.show(childFragmentManager, StreakInfoSheet.TAG)
                    }
                }
            }
        }
    }

    private fun updateCalendar(state: HomeUiState) = binding.apply {
        progressBarOverall.isVisible = false
        contentScrollView.isVisible = true
        swipeRefreshLayout.isRefreshing = state.isRefreshing

        toolbarTitle.text = if (DateUtils.isSameDay(state.selectedDate, Date())) {
            getString(R.string.today) + COMMA_SPACE + DateUtils.getFormattedDateString(
                state.selectedDate, DATE_FORMAT
            )
        } else {
            DateUtils.getFormattedDateString(state.selectedDate, DATE_FORMAT)
        }

        dateAdapter?.apply {
            submitList(state.datesForScroller)
            setSelectedDate(state.selectedDate)
            updateData(state.dailySummaries, state.caloriesTarget, true)
        }
        val dateChanged = lastSelectedDate == null || !DateUtils.isSameDay(
            lastSelectedDate, state.selectedDate
        )
        if (dateChanged) {
            lastSelectedDate = state.selectedDate
            dateScrollerRecyclerView.post {
                val selectedIndex = dateStartsFromOffset(state)
                if (selectedIndex != -1) {
                    (dateScrollerRecyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                        selectedIndex,
                        (root.width / 2) - (64 * resources.displayMetrics.density).toInt()
                    )
                    if (dateScrollerRecyclerView.alpha == 0f) {
                        dateScrollerRecyclerView.alpha = 1f
                    }
                }
            }
        }
    }

    private fun dateStartsFromOffset(state: HomeUiState): Int {
        val list = dateAdapter?.currentList ?: return -1
        return list.indexOfFirst { DateUtils.isSameDay(it, state.selectedDate) }
    }

    private fun updateGoalPrediction() = binding.apply {
        cardGoalPrediction.isVisible = false
    }

    private fun updateYesterdayEval() = binding.apply {

    }

    private fun updateCalorieAndMacros(state: HomeUiState) = binding.apply {
        updateGoalPrediction()
        updateYesterdayEval()
        viewModel.checkDateReset(state.selectedDate)
        val useAnimation = true
        val context = requireContext()
        val cPrimary = VaznimColors.getPrimary(context)
        val cYellow = VaznimColors.getYellow(context)
        val cGreen = VaznimColors.getGreen(context)
        val cTrack = VaznimColors.getTrack(context)
        val cOnSurface = VaznimColors.getOnSurface(context)
        val cOnSurfaceVariant = VaznimColors.getOnSurfaceVariant(context)

        mainProgress.apply {
            textCaloriesEatenCount.text = state.caloriesConsumed.toString()
            textCaloriesBurnedCount.text = state.exerciseBurnt.toString()

            val remainingNew = state.caloriesRemaining.absoluteValue
            val oldRemaining = viewModel.lastCaloriesRemaining

            val cOnSurfaceVariant = VaznimColors.getOnSurfaceVariant(context)
            textCaloriesRemainingLabel.text = if (state.caloriesRemaining < 0) {
                getString(R.string.calories_remaining_label_oshdi)
            } else {
                getString(R.string.calories_remaining_label_qoldi)
            }
            textCaloriesRemainingLabel.setTextColor(cOnSurfaceVariant)
            animateIntegerText(
                textCaloriesRemainingCount,
                oldRemaining ?: remainingNew,
                remainingNew,
                450L
            )
            viewModel.lastCaloriesRemaining = remainingNew

            val effectiveConsumed =
                if (state.caloriesTarget > 0 && state.caloriesConsumed > state.caloriesTarget + 100) {
                    val net = state.caloriesConsumed - state.exerciseBurnt
                    if (net <= state.caloriesTarget) {
                        state.caloriesTarget
                    } else {
                        net
                    }
                } else {
                    state.caloriesConsumed
                }

            val maxTarget = if (state.caloriesTarget > 0) state.caloriesTarget else 2000
            progressCaloriesCircular.max = maxTarget
            progressCaloriesCircular.setProgressCompat(
                effectiveConsumed.absoluteValue.coerceAtMost(maxTarget),
                useAnimation
            )

            val currentUser = user
            val targetW = currentUser.targetWeight ?: 0.0
            val isGainWeight = currentUser.isGainWeight || (targetW > 0.0 && currentUser.weight > 0.0 && targetW > currentUser.weight)

            val reachedLimit =
                state.caloriesTarget > 0 && effectiveConsumed >= state.caloriesTarget - 100
            layoutCalorieText.isVisible = true
            layoutNormalProgress.isVisible = true
            macrosContainer.isVisible = true
            limitReached.isVisible = reachedLimit

            if (reachedLimit) {
                if (isGainWeight) {
                    textLimitEmoji.text = "🎉"
                    textLimitReachedMessage.text = "Maqsad bajarildi!"
                    btnHungryWhatToEat.visibility = android.view.View.GONE
                } else {
                    textLimitEmoji.text = "🔥"
                    textLimitReachedMessage.text = "Maqsad bajarildi, tez orada tanangiz yog' yoqishga o'tadi, ertagacha faqat suv ichishingiz mumkin"
                    btnHungryWhatToEat.visibility = android.view.View.VISIBLE
                }
            }

            val calorieColor = when {
                effectiveConsumed > state.caloriesTarget + 100 -> {
                    if (isGainWeight) cGreen else cYellow
                }
                effectiveConsumed >= state.caloriesTarget - 100 -> cGreen
                else -> cPrimary
            }
            progressCaloriesCircular.setIndicatorColor(calorieColor)
            progressCaloriesCircular.trackColor = cTrack
            textCaloriesRemainingCount.setTextColor(cOnSurface)

            bindMacro(
                progressProtein,
                textProteinValue,
                state.proteinConsumed,
                state.proteinTarget,
                cPrimary,
                cYellow,
                cGreen,
                cTrack,
                cOnSurfaceVariant,
                useAnimation
            )

            bindMacro(
                progressFat,
                textFatValue,
                state.fatConsumed,
                state.fatTarget,
                cPrimary,
                cYellow,
                cGreen,
                cTrack,
                cOnSurfaceVariant,
                useAnimation
            )

            bindMacro(
                progressCarbs,
                textCarbsValue,
                state.carbsConsumed,
                state.carbsTarget,
                cPrimary,
                cYellow,
                cGreen,
                cTrack,
                cOnSurfaceVariant,
                useAnimation
            )
        }
    }

    private fun bindMacro(
        progress: CircularProgressIndicator,
        text: android.widget.TextView,
        consumed: Int,
        target: Int,
        cPrimary: Int,
        cYellow: Int,
        cGreen: Int,
        cTrack: Int,
        cOnSurfaceVariant: Int,
        animate: Boolean
    ) {
        val isExceededByMoreThan10 = consumed > target + 10
        val isExceeded = consumed > target
        val isCompleted = consumed >= target * 0.95 && consumed <= target + 10

        val maxVal = (if (isExceeded) consumed else target).coerceAtLeast(1)
        progress.max = maxVal
        progress.setProgressCompat(consumed.coerceAtMost(maxVal), animate)

        val color = when {
            isExceededByMoreThan10 -> cYellow
            isCompleted -> cGreen
            else -> cPrimary
        }
        progress.setIndicatorColor(color)
        progress.trackColor = cTrack
        text.setTextColor(cOnSurfaceVariant)

        val diff = (target - consumed).absoluteValue
        text.text = if (consumed <= target) {
            getString(R.string.macro_remaining_format, diff)
        } else {
            getString(R.string.macro_exceeded_format, diff)
        }
    }

    private fun updateMeals(state: HomeUiState) = binding.apply {
        val animate = animateProgress
        val context = requireContext()
        val mealLogsMap = state.dailyFoodLogs.groupBy { log ->
            val ts = (log.timestamp as? Number)?.toLong() ?: System.currentTimeMillis()
            MealTime.getValidMeal(log.mealType, ts)
        }
        val target = state.caloriesTarget.toFloat()
        val cPrimary = VaznimColors.getPrimary(context)
        val cYellow = VaznimColors.getYellow(context)
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

        data class MealConfig(
            val card: uz.oybek.ozamiz.databinding.ItemHomeMealCardBinding,
            val id: String,
            val baseMax: Int,
            val baseTitle: String
        )

        val meals = listOf(
            MealConfig(
                cardBreakfast,
                MealTime.Breakfast.id,
                (target * 0.3f).toInt(),
                getString(R.string.breakfast)
            ),
            MealConfig(
                cardLunch,
                MealTime.Lunch.id,
                (target * 0.4f).toInt(),
                getString(R.string.lunch)
            ),
            MealConfig(
                cardDinner,
                MealTime.Dinner.id,
                (target * 0.2f).toInt(),
                getString(R.string.dinner)
            ),
            MealConfig(
                cardSnack,
                MealTime.Snack.id,
                (target * 0.1f).toInt(),
                getString(R.string.snack)
            )
        )
        meals.forEach { (card, id, baseMax, baseTitle) ->
            val logs = mealLogsMap[id] ?: emptyList()
            val consumed = logs.sumOf { it.calories }
            val max = if (baseMax > 0) baseMax else 400
            card.progressMeal.max = max
            card.progressMeal.setProgressCompat(consumed.coerceAtMost(max), animate)

            val indicatorColor = if (consumed > max) cYellow else cPrimary
            card.progressMeal.setIndicatorColor(indicatorColor)

            card.textMealName.text = baseTitle
            card.layoutCalories.isVisible = true

            val foodNames = logs.mapNotNull { it.description.takeIf { d -> d.isNotBlank() } }
                .joinToString(", ")

            if (foodNames.isNotEmpty()) {
                card.textLastEaten.text = "$consumed/$max kkal • $foodNames"
            } else {
                card.textLastEaten.text = "$consumed/$max kkal"
            }
        }
    }

    private fun updateWater(state: HomeUiState) = binding.apply {
        val litersConsumed = state.waterConsumed / 1000f
        val litersTarget = if (state.waterTarget > 0) state.waterTarget / 1000f else 2.5f
        val waterTargetMl = if (state.waterTarget > 0) state.waterTarget else 2500

        titleWater.text = "Suv"
        textWaterValue.text = String.format(java.util.Locale.US, "%.1f ichildi", litersConsumed)
        textCurrentWaterLiters.text =
            String.format(java.util.Locale.US, "Maqsad %.1f litr", litersTarget)

        val waterProgress =
            ((state.waterConsumed.toFloat() / waterTargetMl) * 100).toInt().coerceIn(0, 100)
        progressWater.max = 100
        progressWater.setProgressCompat(waterProgress, animateProgress)
    }

    private fun updateExercise(state: HomeUiState) = binding.apply {
        val targetKcal = if (state.exerciseTarget > 0) state.exerciseTarget else 300
        titleExercise.text = "Harakat"
        textExerciseValue.text = "${state.exerciseBurnt} kal yoqildi"
        textExerciseProgress.text = "Maqsad $targetKcal kal"

        val exerciseProgress =
            ((state.exerciseBurnt.toFloat() / targetKcal) * 100).toInt().coerceIn(0, 100)
        progressExercise.max = 100
        progressExercise.setProgressCompat(exerciseProgress, animateProgress)
    }

    private fun updateStreak(state: HomeUiState) = binding.apply {
        val streakInfo = state.streakInfo
        streakDisplayLayout.isVisible = streakInfo != null
        if (streakInfo != null) {
            textStreakCount.text = streakInfo.currentStreak.toString()
            val isActive = StreakManager.isStreakActive(streakInfo)
            if (isActive) {
                imageStreakIcon.alpha = 1.0f
                textStreakCount.alpha = 1.0f
            } else {
                imageStreakIcon.alpha = 0.4f
                textStreakCount.alpha = 0.4f
            }
            if (!animateProgress && streakDisplayLayout.scaleX == 1f) {
                streakDisplayLayout.animate().scaleX(1.1f).scaleY(1.1f).setDuration(300)
                    .withEndAction {
                        streakDisplayLayout.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300)
                            .start()
                    }.start()
            }
            streakDisplayLayout.setOnClickListener {
                showStreakSheet(streakInfo, null)
            }
        }
    }

    private fun showStreakSheet(
        streakInfo: StreakInfo?,
        streakUpdateResult: StreakManager.StreakUpdateResult?
    ) {
        streakInfo ?: return
        try {
            StreakInfoSheet.newInstance(
                streakInfo,
                isNewAction = streakUpdateResult?.streakContinued ?: false,
                returnedAfterManyDays = streakUpdateResult?.returnedAfterManyDays ?: false
            ).show(childFragmentManager, StreakInfoSheet.TAG)
        } catch (_: Exception) {
        }
    }

    private fun observeStreakUpdate() {
        viewLifecycleOwner.lifecycleScope.launch {
            StreakRepository.levelFlow.collect {
                if (it != null) {
                    try {
                        showStreakSheet(viewModel.uiState.value.streakInfo, it)
                        StreakRepository.clearStreakResult()
                        val currentStreak = it.updatedUser.streakInfo?.currentStreak ?: 0
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun observeExerciseAdded() {
        // Exercise added praise screen disabled per user requirement
    }

    private fun updateAppFeedbackCard(state: HomeUiState) {
        val currentStreak = state.streakInfo?.currentStreak ?: 0
        val isFeedbackDone = UserProfileRepository.user.isRated
        val shouldShow = currentStreak >= 3 && !isFeedbackDone

        binding.cardAppFeedback.isVisible = shouldShow

        if (shouldShow) {
            binding.cardAppFeedback.setOnSafeClickListener {
                showAppFeedbackDialog()
            }
        }
    }

    private fun showAppFeedbackDialog() {
        val dialog = AppFeedbackDialogFragment.newInstance()
        dialog.onDismissCallback = {
            val isDone = UserProfileRepository.user.isRated
            binding.cardAppFeedback.isVisible = !isDone
        }
        dialog.show(childFragmentManager, "AppFeedbackDialogFragment")
    }

    companion object {
        var currentDate: Date = Date()
        private const val DATE_FORMAT = "d-MMMM"
        private const val COMMA_SPACE = ", "
        private const val LITERS_FORMAT = "%.1f"
        private const val KEY_WATER_ADDED_REQUEST = "water_added_request"
        private const val KEY_ADDED = "added"
        private const val KEY_EXERCISE_ID = "exerciseId"
    }
}