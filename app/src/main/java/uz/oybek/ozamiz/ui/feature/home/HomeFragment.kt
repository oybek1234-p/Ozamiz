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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ads.AdsConfig
import uz.oybek.ozamiz.data.ads.AdsManager
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.logs.model.UserExercise
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.streak.StreakManager
import uz.oybek.ozamiz.data.streak.StreakRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.databinding.FragmentHomeBinding
import uz.oybek.ozamiz.ui.MainActivity
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.log.food.LogFoodFragment
import uz.oybek.ozamiz.ui.feature.log.exercise.ExerciseAchievementDialog
import uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import uz.oybek.ozamiz.ui.feature.streak.StreakInfoSheet
import uz.oybek.ozamiz.ui.utils.VaznimColors
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.triggerVibration
import java.util.Date
import kotlin.math.absoluteValue

class LogFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by activityViewModels()
    private var dateAdapter: DateScrollerAdapter? = null

    private var animateProgress = true
    private var stateCollectCount = 0
    private var lastSelectedDate: Date? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    init {
        showBottomSheet = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initWeightLogCard()
    }

    private fun initWeightLogCard() {
        lifecycleScope.launch {
            delay(WEIGHT_LOG_DELAY)
            val needShowWeightLog = viewModel.uiState.value.showWeightLogCard
            if (needShowWeightLog) {
                navigate(R.id.photoUploadInfoFragment)
            }
        }
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
        initMealRecommendation()
        observeUiState()
        observeStreakUpdate()
        observeExerciseAdded()
        checkNotificationPermission()
        setupLimitExceededResultListener()
        setupPremiumResultListener()
        setupExerciseAddedResultListener()
        initBannerAd(bind)
        if (animateProgress) {
            lifecycleScope.launch {
                delay(200)
                animateProgress = false
            }
        }
    }

    private fun initBannerAd(bind: FragmentHomeBinding) {
        if (user.isPremium()) return
        val bannerView = AdsManager.createBannerView(
            requireContext(),
            AdsConfig.BANNER_HOME,
            object : BannerAdEventListener {
                override fun onAdLoaded() {
                    bind.bannerAdContainerHome.visibility = android.view.View.VISIBLE
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    bind.bannerAdContainerHome.visibility = android.view.View.GONE
                }

                override fun onAdClicked() {}
                override fun onImpression(impressionData: ImpressionData?) {}
            })
        bind.bannerAdContainerHome.removeAllViews()
        bind.bannerAdContainerHome.addView(bannerView)
    }

    private fun checkPremiumAndNavigate(destinationId: Int) {
        if (user.isPremium()) {
            navigate(destinationId)
        } else {
            PremiumBottomSheetDialog().show(childFragmentManager, PremiumBottomSheetDialog.TAG)
        }
    }

    private fun setupPremiumResultListener() {
        childFragmentManager.setFragmentResultListener(
            PremiumBottomSheetDialog.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val result = bundle.getString(PremiumBottomSheetDialog.RESULT_ACTION)
            if (result == "go_to_payment") {
                val price = bundle.getLong("selected_price", 29000L)
                val isYearly = bundle.getBoolean("is_yearly", false)
                val args = androidx.core.os.bundleOf("price" to price, "is_yearly" to isYearly)
                findNavController().navigate(
                    R.id.paymentGetCheckFragment,
                    args
                )
            }
        }
    }

    private fun setupExerciseAddedResultListener() {
        parentFragmentManager.setFragmentResultListener(
            "exercise_added_request",
            viewLifecycleOwner
        ) { _, bundle ->
            val calories = bundle.getInt("caloriesBurned", 0)
            if (calories > 0) {
                val args = DailyAnalysisFragment.createArgs(
                    type = DailyAnalysisFragment.TYPE_EXERCISE_50,
                    caloriesBurned = calories
                )
                navigate(R.id.action_home_to_dailyAnalysisFragment, args)
            }
        }
    }

    private fun setupLimitExceededResultListener() {

    }

    override fun onResume() {
        super.onResume()
        checkOnboarding()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dateAdapter = null
        lastSelectedDate = null
    }

    private fun checkNotificationPermission() {
        val ctx = context ?: return
        if (viewModel.isSecondOnboardingSheetSeen().not()) return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    ctx, android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkOnboarding() {
        runCatching {
            if (!viewModel.isFirstLogAdded()) {
                showAppOnboarding()
            } else if (!viewModel.isSecondOnboardingSheetSeen()) {
                showSecondOnboardingSheet()
            }
        }
    }

    private fun showAppOnboarding() {
        if (activity == null || isDetached) return
        runCatching {
            onboardingSheet?.dismiss()
        }
        viewModel.setSecondOnboardingSheetSeen(false)
        val name = user.name
        val greetingText = getString(
            if (user.goal == "gain_weight")
                R.string.onboarding_step_1_semirish else R.string.onboarding_step_1_ozish,
            name
        )

        onboardingSheet = AppOnboardingBottomSheet(
            text = greetingText,
            buttonText = getString(R.string.add_food_title),
            onButtonClick = {
                LogFoodFragment.navigate(
                    navController = findNavController(),
                    timeStamp = viewModel.uiState.value.selectedDate.time,
                    meal = MealTime.Breakfast.id,
                    openCamera = false,
                    openEditText = true
                )
                viewModel.setFirstLogAdded(true)
            },
            onLaterClick = { }).apply {
            show(mainActivity()!!.supportFragmentManager, AppOnboardingBottomSheet.TAG)
        }
    }

    private fun showSecondOnboardingSheet() {
        if (activity == null || isDetached) return
        viewModel.setSecondOnboardingSheetSeen(true)

        val name = user.name
        val cal = viewModel.uiState.value.caloriesRemaining
        if (cal <= 0) return
        val remainingText = getString(R.string.onboarding_step_2, name, cal)

        AppOnboardingBottomSheet(
            text = remainingText,
            buttonText = getString(R.string.tushundim_btn),
            title = getString(R.string.ajoyib_title),
            onButtonClick = { }).show(childFragmentManager, AppOnboardingBottomSheet.TAG)
    }

    private var onboardingSheet: AppOnboardingBottomSheet? = null

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
            alpha = 0f
        }
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun initCalorieAndMacros() = binding.mainProgress.apply {
        cardProtein.setOnClickListener {
            checkPremiumAndNavigate(R.id.proteinGuideFragment)
        }
        cardFat.setOnClickListener {
            checkPremiumAndNavigate(R.id.lipidsGuideFragment)
        }
        cardCarbs.setOnClickListener {
            checkPremiumAndNavigate(R.id.carbsGuideFragment)
        }
        btnDetailedInfo.setOnSafeClickListener {
            val currentState = viewModel.uiState.value
            if (currentState.caloriesRemaining < 0) {
                val excessCalories = currentState.caloriesRemaining.absoluteValue
                val exceededTypes = listOf("kaloriya")
                val overages = mapOf("kaloriya" to excessCalories)
                OverageInfoBottomSheet.newInstance(exceededTypes, overages)
                    .show(childFragmentManager, OverageInfoBottomSheet.TAG)
            } else {
                val args = Bundle().apply {
                    putString(MacroInfoFragment.ARG_MACRO_TYPE, MacroInfoFragment.MACRO_CALORIES)
                    putInt(MacroInfoFragment.ARG_CONSUMED, currentState.caloriesConsumed)
                    putInt(MacroInfoFragment.ARG_TARGET, currentState.caloriesTarget)
                }
                navigate(R.id.action_home_to_macroInfoFragment, args)
            }
        }
        showLimitExercises.setOnSafeClickListener {
            val currentState = viewModel.uiState.value
            val excessCalories = (currentState.caloriesConsumed - currentState.caloriesTarget).coerceAtLeast(0)
            if (excessCalories > 0) {
                OverageExerciseBottomSheet.newInstance(excessCalories)
                    .show(childFragmentManager, OverageExerciseBottomSheet.TAG)
            }
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

    private fun initMeals() = binding.apply {
        fun onMealClick(meal: String) {
            LogFoodFragment.navigate(
                navController = findNavController(),
                timeStamp = viewModel.uiState.value.selectedDate.time,
                meal = meal,
                openCamera = false
            )
        }
        cardBreakfast.root.setOnSafeClickListener { onMealClick(MealTime.Breakfast.id) }
        cardBreakfast.btnAddMeal.setOnSafeClickListener { onMealClick(MealTime.Breakfast.id) }
        cardLunch.root.setOnSafeClickListener { onMealClick(MealTime.Lunch.id) }
        cardLunch.btnAddMeal.setOnSafeClickListener { onMealClick(MealTime.Lunch.id) }
        cardDinner.root.setOnSafeClickListener { onMealClick(MealTime.Dinner.id) }
        cardDinner.btnAddMeal.setOnSafeClickListener { onMealClick(MealTime.Dinner.id) }
        cardSnack.root.setOnSafeClickListener { onMealClick(MealTime.Snack.id) }
        cardSnack.btnAddMeal.setOnSafeClickListener { onMealClick(MealTime.Snack.id) }

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
            TodayWatersBottomSheet().show(childFragmentManager, TodayWatersBottomSheet.TAG)
        }
        btnWater250.setOnSafeClickListener {
            viewModel.addWater(250)
            triggerVibration()
        }
        btnWater500.setOnSafeClickListener {
            viewModel.addWater(500)
            triggerVibration()
        }
        btnWater1000.setOnSafeClickListener {
            viewModel.addWater(1000)
            triggerVibration()
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
            TodayExercisesBottomSheet().show(childFragmentManager, TodayExercisesBottomSheet.TAG)
        }
        btnQuickWalk.setOnSafeClickListener {
            val args = Bundle().apply {
                putString(KEY_EXERCISE_ID, UserExercise.PiyodaYurish.name)
            }
            navigate(R.id.addExerciseLogFragment, args)
        }
        btnQuickRun.setOnSafeClickListener {
            val args = Bundle().apply {
                putString(KEY_EXERCISE_ID, UserExercise.Yugurish.name)
            }
            navigate(R.id.addExerciseLogFragment, args)
        }
        btnQuickOther.setOnSafeClickListener {
            navigate(R.id.selectExerciseFragment)
        }
    }

    private fun initWeightLogListener() = binding.apply {
        btnAddWeightHome.setOnSafeClickListener {
            navigate(R.id.addWeightPhotoFragment)
        }
    }

    private fun initMealRecommendation() = binding.apply {
        btnMealRecommendation.setOnSafeClickListener {
            MealRecommendationBottomSheet.newInstance()
                .show(childFragmentManager, MealRecommendationBottomSheet.TAG)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect {
                        stateCollectCount++
                        MainActivity.isAppReady = true
                    }
                }
                launch {
                    viewModel.uiState.map {
                        listOf(
                            it.selectedDate,
                            it.datesForScroller,
                            it.dailySummaries,
                            it.isRefreshing,
                            it.caloriesTarget
                        )
                    }.distinctUntilChanged().collect {
                        updateCalendar(viewModel.uiState.value)
                    }
                }
                launch {
                    viewModel.uiState.map {
                        listOf(
                            it.caloriesRemaining,
                            it.caloriesConsumed,
                            it.caloriesTarget,
                            it.proteinConsumed,
                            it.proteinTarget,
                            it.fatConsumed,
                            it.fatTarget,
                            it.carbsConsumed,
                            it.carbsTarget
                        )
                    }.distinctUntilChanged().collect {
                        updateCalorieAndMacros(viewModel.uiState.value)
                    }
                }
                launch {
                    viewModel.uiState.map {
                        listOf(it.dailyFoodLogs, it.caloriesTarget, it.caloriesConsumed)
                    }.distinctUntilChanged().collect {
                        updateMeals(viewModel.uiState.value)
                    }
                }
                launch {
                    viewModel.uiState.map { it.waterConsumed to it.waterTarget }
                        .distinctUntilChanged().collect {
                            updateWater(viewModel.uiState.value)
                        }
                }
                launch {
                    viewModel.uiState.map { it.exerciseTarget to it.exerciseBurnt }
                        .distinctUntilChanged().collect {
                            updateExercise(viewModel.uiState.value)
                        }
                }
                launch {
                    viewModel.uiState.map { it.streakInfo }.distinctUntilChanged().collect {
                        updateStreak(viewModel.uiState.value)
                    }
                }
                launch {
                    viewModel.uiState.map { it.showWeightLogCard }.distinctUntilChanged().collect {
                        binding.cardWeightLog.isVisible = it
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
                launch {
                    viewModel.showWeightLogEvent.collect {
                        navigate(R.id.photoUploadInfoFragment)
                    }
                }
                launch {
                    viewModel.showDailyAnalysisEvent.collect { data ->
                        val args = DailyAnalysisFragment.createArgs(
                            foodCount = data.foodCount,
                            healthyFoodCount = data.healthyFoodCount,
                            points = data.points,
                            waterMl = data.waterMl,
                            hasExercise = data.hasExercise,
                            hasProteinGoal = data.hasProteinGoal,
                            noSweets = data.noSweets,
                            noBread = data.noBread,
                            type = data.type,
                            caloriesBurned = data.caloriesBurned
                        )
                        navigate(R.id.action_home_to_dailyAnalysisFragment, args)
                    }
                }

                launch {
                    viewModel.showRateAppEvent.collect {
                        delay(WEIGHT_LOG_DELAY)
                        if (isResumed && childFragmentManager.fragments.isEmpty()) {
                            (activity as? MainActivity)?.requestAppReview()
                        }
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

    private fun updateCalorieAndMacros(state: HomeUiState) = binding.apply {
        val useAnimation = true
        val context = requireContext()
        val cPrimary = VaznimColors.getPrimary(context)
        val cYellow = VaznimColors.getYellow(context)
        val cGreen = VaznimColors.getGreen(context)
        val cTrack = VaznimColors.getTrack(context)
        val cOnSurface = VaznimColors.getOnSurface(context)

        mainProgress.apply {
            textCaloriesRemainingLabel.text = if (state.caloriesRemaining < 0) {
                getString(R.string.calories_remaining_label_oshdi)
            } else {
                getString(R.string.calories_remaining_label_qoldi)
            }
            textCaloriesRemainingCount.text = state.caloriesRemaining.absoluteValue.toString()
            textEatenCount.text = getString(R.string.calories_eaten_format, state.caloriesConsumed)
            val effectiveConsumed = if (state.caloriesTarget > 0 && state.caloriesConsumed > state.caloriesTarget + 100) {
                val net = state.caloriesConsumed - state.exerciseBurnt
                if (net <= state.caloriesTarget) {
                    state.caloriesTarget
                } else {
                    net
                }
            } else {
                state.caloriesConsumed
            }

            progressCaloriesCircular.max = state.caloriesTarget
            progressCaloriesCircular.setProgressCompat(
                effectiveConsumed.absoluteValue.coerceAtMost(state.caloriesTarget),
                useAnimation
            )
            btnDetailedInfo.isVisible = true

            val reachedLimit =
                state.caloriesTarget > 0 && effectiveConsumed >= state.caloriesTarget - 100 && effectiveConsumed <= state.caloriesTarget + 100
            val exceededLimit =
                state.caloriesTarget > 0 && effectiveConsumed > state.caloriesTarget + 100

            layoutCalorieText.isVisible = !reachedLimit && !exceededLimit
            layoutLimitInfo.isVisible = reachedLimit || exceededLimit
            textLimitSubtitle.visibility = View.GONE

            layoutNormalProgress.isVisible = reachedLimit.not() && exceededLimit.not()
            macrosContainer.isVisible = !reachedLimit && !exceededLimit
            limitReached.isVisible = reachedLimit
            limitExeed.isVisible = exceededLimit


            val calorieColor = when {
                effectiveConsumed > state.caloriesTarget + 100 -> VaznimColors.getRed(context)
                effectiveConsumed >= state.caloriesTarget - 100 && effectiveConsumed <= state.caloriesTarget + 100 -> cGreen
                else -> cPrimary
            }
            progressCaloriesCircular.setIndicatorColor(calorieColor)
            progressCaloriesCircular.trackColor = cTrack
            textCaloriesRemainingCount.setTextColor(
                if (effectiveConsumed > state.caloriesTarget + 100) VaznimColors.getRed(
                    context
                ) else cOnSurface
            )

            bindMacro(
                progressProtein,
                textProteinValue,
                state.proteinConsumed,
                state.proteinTarget,
                cPrimary,
                cYellow,
                cGreen,
                cTrack,
                cOnSurface,
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
                cOnSurface,
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
                cOnSurface,
                useAnimation
            )
        }
    }

    private fun bindMacro(
        progress: LinearProgressIndicator,
        text: android.widget.TextView,
        consumed: Int,
        target: Int,
        cPrimary: Int,
        cYellow: Int,
        cGreen: Int,
        cTrack: Int,
        cOnSurface: Int,
        animate: Boolean
    ) {
        val isExceededByMoreThan10 = consumed > target + 10
        val isExceeded = consumed > target
        val isCompleted = consumed >= target * 0.95 && consumed <= target + 10

        progress.max = if (isExceeded) consumed else target
        progress.setProgressCompat(consumed, animate)

        val color = when {
            isExceededByMoreThan10 -> cYellow
            isCompleted -> cGreen
            else -> cPrimary
        }
        progress.setIndicatorColor(color)
        progress.trackColor = cTrack
        text.setTextColor(if (isExceededByMoreThan10) cYellow else cOnSurface)

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
        val mealCalories = state.dailyFoodLogs.groupBy { it.mealType }
            .mapValues { it.value.sumOf { f -> f.calories } }
        val target = state.caloriesTarget.toFloat()
        val cPrimary = VaznimColors.getPrimary(context)
        val cOnSurfaceVariant = VaznimColors.getOnSurfaceVariant(context)
        val meals = listOf(
            Triple(
                cardBreakfast, MealTime.Breakfast.id, (target * 0.3f).toInt()
            ),
            Triple(cardLunch, MealTime.Lunch.id, (target * 0.4f).toInt()),
            Triple(cardDinner, MealTime.Dinner.id, (target * 0.2f).toInt()),
            Triple(cardSnack, MealTime.Snack.id, (target * 0.1f).toInt())
        )
        meals.forEach { (card, id, baseMax) ->
            val consumed = mealCalories[id] ?: 0
            val max = if (baseMax > 0) baseMax else 400
            card.progressMeal.max = max
            card.progressMeal.setProgressCompat(consumed.coerceAtMost(max), animate)
            card.progressMeal.setIndicatorColor(cPrimary)

            val hideCalories = consumed >= max || state.caloriesConsumed >= state.caloriesTarget
            card.layoutCalories.isVisible = !hideCalories
            if (!hideCalories) {
                card.textMealCalories.text =
                    getString(R.string.meal_remaining_format, max - consumed)
                card.textMealCalories.setTextColor(cOnSurfaceVariant)
            }
        }
    }

    private fun updateWater(state: HomeUiState) = binding.apply {
        val context = requireContext()
        val litersStr = String.format(LITERS_FORMAT, state.waterConsumed / 1000f)
        val goalLitersStr = String.format(LITERS_FORMAT, state.waterTarget / 1000f)
        val isWaterCompleted = state.waterTarget > 0 && state.waterConsumed >= state.waterTarget

        textCurrentWaterLiters.text = if (isWaterCompleted) {
            getString(R.string.water_bajarildi)
        } else {
            getString(R.string.water_consumed_format, litersStr)
        }
        textCurrentWaterLiters.setTextColor(
            if (isWaterCompleted) VaznimColors.getGreen(context) else VaznimColors.getOnSurface(
                context
            )
        )

        if (isWaterCompleted) {
            val drawable = ContextCompat.getDrawable(context, R.drawable.check_ic)
            drawable?.setTint(VaznimColors.getGreen(context))
            textCurrentWaterLiters.setCompoundDrawablesRelativeWithIntrinsicBounds(
                drawable, null, null, null
            )
            textCurrentWaterLiters.compoundDrawablePadding =
                (4 * resources.displayMetrics.density).toInt()
        } else {
            textCurrentWaterLiters.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null, null, null, null
            )
        }

        textWaterGoal.text = getString(R.string.water_goal_format, goalLitersStr)
        textWaterGoal.isVisible = state.waterTarget > 0
        progressWater.max = state.waterTarget
        progressWater.setProgressCompat(state.waterConsumed, true)
        titleWater.text = getString(R.string.water_title)
    }

    private fun updateExercise(state: HomeUiState) = binding.apply {
        val context = requireContext()
        val cPrimary = VaznimColors.getPrimary(context)
        val cGreen = VaznimColors.getGreen(context)
        val cTrack = VaznimColors.getTrack(context)
        val cOnSurface = VaznimColors.getOnSurface(context)

        progressExercise.max = state.exerciseTarget
        progressExercise.setProgressCompat(state.exerciseBurnt, true)

        val isExerciseFull = state.exerciseBurnt >= state.exerciseTarget && state.exerciseTarget > 0
        progressExercise.setIndicatorColor(if (isExerciseFull) cGreen else cPrimary)
        progressExercise.trackColor = cTrack

        val exRemaining = maxOf(0, state.exerciseTarget - state.exerciseBurnt)
        textExerciseProgress.text = if (exRemaining > 0) {
            getString(R.string.exercise_remaining_format, exRemaining)
        } else {
            getString(R.string.exercise_bajarildi)
        }
        textExerciseProgress.setTextColor(if (isExerciseFull) cGreen else cOnSurface)

        if (isExerciseFull) {
            val drawable = ContextCompat.getDrawable(context, R.drawable.check_ic)
            drawable?.setTint(cGreen)
            textExerciseProgress.setCompoundDrawablesRelativeWithIntrinsicBounds(
                drawable, null, null, null
            )
            textExerciseProgress.compoundDrawablePadding =
                (4 * resources.displayMetrics.density).toInt()
        } else {
            textExerciseProgress.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null, null, null, null
            )
        }

        titleExercise.text = getString(R.string.fat_burn_title)
        val fatBurnt = (state.exerciseBurnt / 7.7).toInt()
        textExerciseStatus.text = getString(R.string.fat_burnt_today, fatBurnt)
        textExerciseStatus.isVisible = state.exerciseBurnt > 0
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
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun observeExerciseAdded() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.exerciseAddedEvent.collect { calories ->
                val args = DailyAnalysisFragment.createArgs(
                    type = DailyAnalysisFragment.TYPE_EXERCISE_50,
                    caloriesBurned = calories
                )
                navigate(R.id.action_home_to_dailyAnalysisFragment, args)
            }
        }
    }

    companion object {
        var currentDate: Date = Date()
        private const val WEIGHT_LOG_DELAY = 2000L
        private const val DATE_FORMAT = "d-MMMM"
        private const val COMMA_SPACE = ", "
        private const val LITERS_FORMAT = "%.1f"
        private const val KEY_WATER_ADDED_REQUEST = "water_added_request"
        private const val KEY_ADDED = "added"
        private const val KEY_EXERCISE_ID = "exerciseId"
    }
}