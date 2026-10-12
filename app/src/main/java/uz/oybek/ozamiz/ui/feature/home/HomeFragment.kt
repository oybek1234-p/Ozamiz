package uz.oybek.ozamiz.ui.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.progressindicator.BaseProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ads.AdsConfig
import uz.oybek.ozamiz.data.profile.model.UserGoal
import uz.oybek.ozamiz.data.streak.model.StreakStatus
import uz.oybek.ozamiz.data.tracker.model.DailyCalorieMacroSummary
import uz.oybek.ozamiz.data.tracker.model.DailyExerciseSummary
import uz.oybek.ozamiz.data.tracker.model.DailyMealsSummary
import uz.oybek.ozamiz.data.tracker.model.DailyWaterSummary
import uz.oybek.ozamiz.data.tracker.model.MealProgressData
import uz.oybek.ozamiz.databinding.FragmentHomeBinding
import uz.oybek.ozamiz.databinding.ItemHomeMealCardBinding
import uz.oybek.ozamiz.notification.permission.NotificationPermissionUtils
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.log.food.FoodCameraArgs
import uz.oybek.ozamiz.ui.feature.log.food.LogFoodArgs
import uz.oybek.ozamiz.ui.feature.log.water.LogWaterDialog
import uz.oybek.ozamiz.ui.feature.streak.StreakInfoSheet
import uz.oybek.ozamiz.ui.feature.streak.StreakUiEffect
import uz.oybek.ozamiz.ui.feature.streak.StreakViewModel
import uz.oybek.ozamiz.ui.modelUtils.MealTime
import uz.oybek.ozamiz.ui.utils.animateIntegerText
import uz.oybek.ozamiz.ui.utils.appColors
import uz.oybek.ozamiz.ui.utils.collectFlowOnStart
import uz.oybek.ozamiz.ui.utils.loadBannerAd
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.toBundle
import java.util.Locale
import kotlin.math.absoluteValue

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by viewModels()
    private val streakViewModel: StreakViewModel by viewModels()
    private var dateAdapter: DateScrollerAdapter? = null
    private var isHomeFirstUpdated: Boolean = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            context?.let { ctx ->
                NotificationPermissionUtils.openNotificationSettings(ctx)
            }
        }
        updateNotificationBanner()
    }

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentHomeBinding = FragmentHomeBinding.inflate(
        inflater, container, false
    )

    override fun onStart() {
        super.onStart()
        viewModel.checkDateCalendar()
    }

    override fun viewCreated(bind: FragmentHomeBinding) {
        initCalendarUI(bind)
        initCalorieMacroUI(bind)
        initMealsUI(bind)
        initWaterUI(bind)
        initExerciseUI(bind)
        initFeatureCardsUI(bind)

        collectFlowOnStart(
            viewModel.uiState.map { it.isLoading }.distinctUntilChanged()
        ) { isLoading ->
            animateLoadingState(bind, isLoading)
        }

        collectFlowOnStart(
            streakViewModel.uiState.map {
                it.streakInfo.currentStreak to (it.status == StreakStatus.FROZEN)
            }.distinctUntilChanged()
        ) { (count, isFrozen) ->
            bindStreakToolbar(count, isFrozen)
        }

        collectFlowOnStart(viewModel.effects, ::handleEffect)
        collectFlowOnStart(streakViewModel.effects, ::handleStreakEffect)

        bind.btnHomeNotifications.setOnSafeClickListener {
            navigate(R.id.notificationsFragment)
        }

        initNotificationBanner(bind)
        loadBanner()
    }

    override fun onResume() {
        super.onResume()
        updateNotificationBanner()
    }

    private fun initNotificationBanner(bind: FragmentHomeBinding) =
        with(bind.layoutNotificationBanner) {
            btnNotificationBannerClose.setOnSafeClickListener {
                root.isVisible = false
            }
            btnNotificationBannerEnable.setOnSafeClickListener {
                NotificationPermissionUtils.requestPermission(
                    this@HomeFragment,
                    requestPermissionLauncher
                )
            }
            updateNotificationBanner()
        }

    private fun updateNotificationBanner() {
        val context = context ?: return
        val isGranted = NotificationPermissionUtils.isPermissionGranted(context)
        bindingT?.layoutNotificationBanner?.root?.isVisible = !isGranted
    }

    private fun initCalendarUI(bind: FragmentHomeBinding) = with(bind) {
        dateAdapter = DateScrollerAdapter { date ->
            viewModel.onDateSelected(date)
        }
        dateScrollerRecyclerView.apply {
            adapter = dateAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            itemAnimator = null
        }
        updateCalendarToggleVisual(dateScrollerRecyclerView.isVisible)
        layoutCalendarToggle.setOnClickListener {
            val willBeVisible = !dateScrollerRecyclerView.isVisible
            androidx.transition.TransitionManager.beginDelayedTransition(
                root,
                androidx.transition.ChangeBounds().apply {
                    duration = 200
                }
            )
            dateScrollerRecyclerView.isVisible = willBeVisible
            updateCalendarToggleVisual(willBeVisible)
        }
        swipeRefreshLayout.apply {
            setOnRefreshListener { viewModel.refresh() }
            setProgressBackgroundColorSchemeColor(appColors.colorSurfaceContainer)
            setColorSchemeColors(appColors.colorPrimary)
        }
        collectFlowOnStart(
            viewModel.uiState.map {
                it.calendar
            }.distinctUntilChanged(), ::bindCalendar
        )
    }

    private fun updateCalendarToggleVisual(isOpen: Boolean) = with(binding) {
        iconCalendarArrow.imageTintList = if (isOpen) {
            appColors.colorStateList(appColors.colorPrimary)
        } else {
            appColors.colorStateList(appColors.colorOnSurfaceVariant)
        }
    }

    private fun initCalorieMacroUI(bind: FragmentHomeBinding) = with(bind.mainProgress) {
        btnSettingsHero.setOnSafeClickListener { navigate(R.id.goalInfoFragment) }
        cardProtein.setOnSafeClickListener { navigate(R.id.proteinGuideFragment) }
        cardFat.setOnSafeClickListener { navigate(R.id.lipidsGuideFragment) }
        cardCarbs.setOnSafeClickListener { navigate(R.id.carbsGuideFragment) }
        btnHowFatBurned.setOnSafeClickListener {
            showInfoSheet(R.string.how_to_burn_fat_title, R.string.how_to_burn_fat_content)
        }
        btnHungryWhatToEat.setOnSafeClickListener {
            showInfoSheet(R.string.hungry_what_to_eat_title, R.string.hungry_what_to_eat_content)
        }
        collectFlowOnStart(
            viewModel.uiState.map { it.profile.userGoal to it.calorieMacro }.distinctUntilChanged()
        ) { (userGoal, calorieMacro) ->
            bindCalorieMacro(calorieMacro, userGoal)
        }
    }

    private fun showInfoSheet(@StringRes titleRes: Int, @StringRes contentRes: Int) {
        InfoBottomSheet.newInstance(
            title = getString(titleRes), content = getString(contentRes)
        ).show(childFragmentManager, null)
    }

    private fun initMealsUI(bind: FragmentHomeBinding) {
        MealTime.entries.forEach { meal ->
            val card = getMealCardBinding(meal)
            card.imgMealIcon.setImageResource(meal.iconRes)
            card.textMealName.setText(meal.nameRes)
            card.root.setOnSafeClickListener { openLogFood(meal.id) }
            card.btnAddMeal.setOnSafeClickListener { viewModel.onAddMealClicked(meal.id) }
        }
        collectFlowOnStart(viewModel.uiState.map { it.meals }.distinctUntilChanged(), ::bindMeals)
    }

    private fun initWaterUI(bind: FragmentHomeBinding) = with(bind.cardWater) {
        root.setOnSafeClickListener { showWaterDialog() }
        btnAddWater.setOnSafeClickListener { showWaterDialog() }
        collectFlowOnStart(viewModel.uiState.map { it.water }.distinctUntilChanged(), ::bindWater)
    }

    private fun initExerciseUI(bind: FragmentHomeBinding) = with(bind.cardExercise) {
        root.setOnSafeClickListener { navigate(R.id.selectExerciseFragment) }
        btnAddExercise.setOnSafeClickListener { navigate(R.id.selectExerciseFragment) }
        collectFlowOnStart(
            viewModel.uiState.map { it.exercise }.distinctUntilChanged(), ::bindExercise
        )
    }

    private fun initFeatureCardsUI(bind: FragmentHomeBinding) = with(bind) {
        streakDisplayLayout.setOnSafeClickListener {
            StreakInfoSheet.show(childFragmentManager)
        }
        cardWeightLog.root.setOnSafeClickListener { navigate(R.id.addWeightPhotoFragment) }
        cardWeightLog.btnAddWeightHome.setOnSafeClickListener { navigate(R.id.addWeightPhotoFragment) }
        cardWhatToEat.root.setOnSafeClickListener { navigate(R.id.plateRuleFragment) }
        cardDietologistHome.root.setOnSafeClickListener { navigate(R.id.aiChatFragment) }
        cardPremiumChecking.root.setOnSafeClickListener { navigate(R.id.premiumFragment) }
        collectFlowOnStart(
            viewModel.uiState.map { it.profile }.distinctUntilChanged(), ::bindProfile
        )
    }

    private fun bindCalendar(state: CalendarUiState) = with(binding) {
        swipeRefreshLayout.isRefreshing = state.isRefreshing
        toolbarTitle.text = uz.oybek.ozamiz.ui.utils.DateExtensions.formatTodayOrDate(
            root.context, state.selectedDate
        )
        dateAdapter?.submitList(state.items)
    }

    private fun bindCalorieMacro(
        state: DailyCalorieMacroSummary, userGoal: UserGoal
    ) = with(binding.mainProgress) {
        textCaloriesEatenCount.text = state.consumedCalories.toString()
        textCaloriesBurnedCount.text = state.burnedCalories.toString()

        val remainingNew = state.remainingCalories.absoluteValue
        val oldRemaining = textCaloriesRemainingCount.text.toString().toIntOrNull() ?: remainingNew

        val isExceeded = state.isExceeded
        textCaloriesRemainingLabel.setText(
            if (isExceeded) R.string.calories_remaining_label_oshdi
            else R.string.calories_remaining_label_qoldi
        )

        if (!isHomeFirstUpdated || (oldRemaining != 0 && oldRemaining != remainingNew)) {
            textCaloriesRemainingCount.animateIntegerText(oldRemaining, remainingNew)
        } else {
            textCaloriesRemainingCount.text = remainingNew.toString()
        }

        val showWarning = state.isExceededWarning
        val warningColor = appColors.colorCalorieExceeded
        textCaloriesRemainingCount.setTextColor(if (showWarning) warningColor else appColors.colorOnSurface)
        textCaloriesRemainingLabel.setTextColor(if (showWarning) warningColor else appColors.colorOnSurfaceVariant)

        progressCaloriesCircular.setProgressData(
            consumed = state.consumedCalories,
            target = state.targetCalories,
            isExceeded = showWarning,
            normalColor = appColors.colorPrimary,
            exceededColor = warningColor
        )

        limitReached.isVisible = state.isLimitReached
        if (state.isLimitReached) {
            val isGain = userGoal.isGain
            textLimitEmoji.text = if (isGain) "🎉" else "🔥"
            textLimitReachedMessage.setText(
                if (isGain) R.string.home_goal_reached_gain_message
                else R.string.home_goal_reached_loss_message
            )
            btnHungryWhatToEat.isVisible = !isGain
        }

        bindMacro(
            progress = progressProtein,
            text = textProteinValue,
            consumed = state.proteinConsumed,
            target = state.proteinTarget,
            isExceeded = state.isProteinExceeded,
            showWarning = state.isProteinWarning,
            normalColor = appColors.colorPrimary
        )
        bindMacro(
            progress = progressFat,
            text = textFatValue,
            consumed = state.fatConsumed,
            target = state.fatTarget,
            isExceeded = state.isFatExceeded,
            showWarning = state.isFatWarning,
            normalColor = appColors.colorPrimary
        )
        bindMacro(
            progress = progressCarbs,
            text = textCarbsValue,
            consumed = state.carbsConsumed,
            target = state.carbsTarget,
            isExceeded = state.isCarbsExceeded,
            showWarning = state.isCarbsWarning,
            normalColor = appColors.colorPrimary
        )

        isHomeFirstUpdated = true
    }

    private fun bindMacro(
        progress: BaseProgressIndicator<*>,
        text: TextView,
        consumed: Int,
        target: Int,
        isExceeded: Boolean,
        showWarning: Boolean = false,
        normalColor: Int
    ) {
        val warningColor = appColors.colorCalorieExceeded
        val indicatorExceededColor = if (showWarning) warningColor else normalColor
        progress.setProgressData(
            consumed = consumed,
            target = target,
            isExceeded = showWarning,
            normalColor = normalColor,
            exceededColor = indicatorExceededColor
        )
        text.setTextColor(if (showWarning) warningColor else appColors.colorOnSurfaceVariant)
        text.text = getString(
            if (isExceeded) R.string.macro_exceeded_format else R.string.macro_remaining_format,
            (target - consumed).absoluteValue
        )
    }

    private fun getMealCardBinding(mealTime: MealTime): ItemHomeMealCardBinding = with(binding) {
        when (mealTime) {
            MealTime.Breakfast -> cardBreakfast
            MealTime.Lunch -> cardLunch
            MealTime.Dinner -> cardDinner
            MealTime.Snack -> cardSnack
        }
    }

    private fun bindMeals(state: DailyMealsSummary) {
        MealTime.entries.forEach { meal ->
            val card = getMealCardBinding(meal)
            val data = state[meal.type]
            bindMealCard(card, data, meal.getColor(appColors))
        }
    }

    private fun bindMealCard(
        card: ItemHomeMealCardBinding, data: MealProgressData, mealColor: Int
    ) = with(card) {
        imgMealIcon.imageTintList = appColors.colorStateList(mealColor)
        progressMeal.setProgressData(
            consumed = data.consumedCalories,
            target = data.targetCalories,
            isExceeded = false,
            normalColor = appColors.colorPrimary,
            exceededColor = appColors.colorPrimary
        )
        val isExceeded = data.targetCalories > 0 && data.consumedCalories > data.targetCalories
        val diff = (data.targetCalories - data.consumedCalories).absoluteValue
        val hasEaten = data.summaryText.isNotBlank()

        textLastEaten.isVisible = true
        textLastEaten.setTextColor(appColors.colorOnSurfaceVariant)

        textLastEaten.text = when {
            hasEaten && isExceeded -> getString(
                R.string.meal_exceeded_with_summary_format,
                diff,
                data.summaryText
            )

            hasEaten -> getString(
                R.string.meal_remaining_with_summary_format,
                diff,
                data.summaryText
            )

            isExceeded -> getString(R.string.meal_calories_exceeded_format, diff)
            else -> getString(R.string.meal_calories_remaining_format, diff)
        }
    }

    private fun bindWater(state: DailyWaterSummary) = with(binding.cardWater) {
        val litersConsumed = state.waterConsumedMl / 1000f
        val litersTarget = state.waterTargetMl / 1000f
        val remainingLiters = maxOf(0f, litersTarget - litersConsumed)

        val remainingStr = if (remainingLiters % 1f == 0f) {
            remainingLiters.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", remainingLiters)
        }

        textCurrentWaterLiters.text =
            getString(R.string.water_remaining_format, remainingStr)
        progressWater.setIndicatorColor(appColors.colorWaterProgress)
        if (progressWater.progress != state.progressPercent) {
            val shouldAnimate = !isHomeFirstUpdated || progressWater.progress > 0
            progressWater.setProgressCompat(state.progressPercent, shouldAnimate)
        }
    }

    private fun bindExercise(state: DailyExerciseSummary) = with(binding.cardExercise) {
        val remainingKcal = maxOf(0, state.exerciseTargetKcal - state.exerciseBurntKcal)
        textExerciseProgress.text =
            getString(R.string.meal_calories_remaining_format, remainingKcal)
        progressExercise.setIndicatorColor(appColors.colorExerciseProgress)
        if (progressExercise.progress != state.progressPercent) {
            val shouldAnimate = !isHomeFirstUpdated || progressExercise.progress > 0
            progressExercise.setProgressCompat(state.progressPercent, shouldAnimate)
        }
    }

    private fun bindProfile(state: HomeProfileUiState) = with(binding) {
        cardWeightLog.root.isVisible = state.showWeightLogCard
        cardPremiumChecking.root.isVisible = state.paymentChecking
        bindNotificationBadge(state.unreadMessages)
    }

    private fun bindNotificationBadge(count: Int) = with(binding) {
        if (count > 0) {
            badgeNotificationCount.isVisible = true
            badgeNotificationCount.text = if (count > 99) "99+" else count.toString()
        } else {
            badgeNotificationCount.isVisible = false
        }
    }

    private fun bindStreakToolbar(count: Int, isFrozen: Boolean) = with(binding) {
        streakDisplayLayout.isVisible = true
        val ctx = root.context

        when {
            isFrozen -> {
                imageStreakIcon.alpha = 1.0f
                imageStreakIcon.imageTintList =
                    appColors.colorStateList(appColors.colorStreakFrozen)
                textStreakCount.isVisible = true
                textStreakCount.text = count.toString()
                textStreakCount.setTextColor(appColors.colorStreakFrozen)
            }

            count > 0 -> {
                imageStreakIcon.alpha = 1.0f
                imageStreakIcon.imageTintList =
                    appColors.colorStateList(appColors.colorStreakActive)
                textStreakCount.isVisible = true
                textStreakCount.text = count.toString()
                textStreakCount.setTextColor(appColors.colorStreakActive)
            }

            else -> {
                imageStreakIcon.alpha = 0.35f
                imageStreakIcon.imageTintList =
                    appColors.colorStateList(appColors.colorStreakInactive)
                textStreakCount.isVisible = false
            }
        }
    }

    private fun showWaterDialog() {
        LogWaterDialog().show(childFragmentManager, null)
    }

    private fun handleEffect(effect: HomeUiEffect) {
        when (effect) {
            is HomeUiEffect.NavigateToFoodCamera -> {
                val args = FoodCameraArgs(mealType = effect.mealType)
                navigate(R.id.foodCameraFragment, args.toBundle())
            }

            is HomeUiEffect.ShowFoodLimitSheet -> {
                navigate(R.id.foodLimitBottomSheet, effect.args.toBundle())
            }
        }
    }

    private fun handleStreakEffect(effect: StreakUiEffect) {
        when (effect) {
            is StreakUiEffect.ShowStreakSheet -> {
                StreakInfoSheet.show(childFragmentManager)
            }

            else -> Unit
        }
    }

    private fun openLogFood(mealId: String) {
        val args = LogFoodArgs(mealType = mealId)
        navigate(R.id.logFoodFragment, args.toBundle())
    }

    private fun loadBanner() {
        binding.bannerAdContainerHome.loadBannerAd(AdsConfig.BANNER_HOME)
    }

    private fun BaseProgressIndicator<*>.setProgressData(
        consumed: Int,
        target: Int,
        isExceeded: Boolean = false,
        normalColor: Int = appColors.colorCalorieProgress,
        exceededColor: Int = appColors.colorCalorieExceeded
    ) {
        val maxVal = (if (isExceeded) consumed else target).coerceAtLeast(1)
        val targetProgress = consumed.coerceAtMost(maxVal)
        if (max != maxVal) {
            max = maxVal
        }
        if (progress != targetProgress) {
            val shouldAnimate = !isHomeFirstUpdated || progress > 0
            setProgressCompat(targetProgress, shouldAnimate)
        }
        setIndicatorColor(if (isExceeded) exceededColor else normalColor)
    }

    private fun animateLoadingState(bind: FragmentHomeBinding, isLoading: Boolean) {
        val animDuration = 150L
        if (isLoading) {
            bind.swipeRefreshLayout.isEnabled = false

            if (!isHomeFirstUpdated) {
                bind.progressBarOverall.animate().cancel()
                bind.progressBarOverall.alpha = 1f
                bind.progressBarOverall.isVisible = true

                bind.contentScrollView.animate().cancel()
                bind.contentScrollView.alpha = 0f
                bind.contentScrollView.isInvisible = true
            } else {
                bind.progressBarOverall.apply {
                    animate().cancel()
                    if (!isVisible) {
                        alpha = 0f
                        isVisible = true
                    }
                    animate().alpha(1f).setDuration(animDuration).start()
                }

                bind.contentScrollView.apply {
                    animate().cancel()
                    animate().alpha(0f).setDuration(animDuration).withEndAction {
                        if (viewModel.uiState.value.isLoading) {
                            isInvisible = true
                        }
                    }.start()
                }
            }
        } else {
            bind.swipeRefreshLayout.isEnabled = true

            if (!isHomeFirstUpdated) {
                bind.progressBarOverall.animate().cancel()
                bind.progressBarOverall.isVisible = false

                bind.contentScrollView.animate().cancel()
                bind.contentScrollView.alpha = 1f
                bind.contentScrollView.isVisible = true
            } else {
                bind.progressBarOverall.apply {
                    animate().cancel()
                    if (isVisible) {
                        animate().alpha(0f).setDuration(animDuration).withEndAction {
                            isVisible = false
                        }.start()
                    }
                }

                bind.contentScrollView.apply {
                    animate().cancel()
                    isVisible = true
                    if (alpha < 1f) {
                        animate().alpha(1f).setDuration(animDuration).start()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        bindingT?.contentScrollView?.animate()?.cancel()
        bindingT?.progressBarOverall?.animate()?.cancel()
        super.onDestroyView()
        dateAdapter = null
    }
}