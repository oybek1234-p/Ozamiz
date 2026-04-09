package uz.oybek.ozamiz.ui.feature.home

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.level.LevelManager
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.utils.dateApiFormat
import uz.oybek.ozamiz.databinding.FragmentHomeBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.log.food.DailyLimitFragment
import uz.oybek.ozamiz.ui.feature.log.food.LogFoodFragment
import java.util.Calendar
import java.util.Date
import kotlin.math.absoluteValue

class LogFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by activityViewModels()
    private var dateAdapter: DateScrollerAdapter? = null

    private lateinit var exerciseAdapter: HomeExerciseLogAdapter
    private lateinit var recommendedFoodAdapter: HomeRecommendedFoodAdapter
    private var lastShownLevelIndex: Int = -1
    private var lastShownStreak: Int = -1

    private fun getLastSeenStreak(): Int {
        val prefs = requireContext().getSharedPreferences(
            "level_prefs",
            android.content.Context.MODE_PRIVATE
        )
        return prefs.getInt("last_seen_streak_milestone", 0)
    }

    private fun saveLastSeenStreak(streak: Int) {
        val prefs = requireContext().getSharedPreferences(
            "level_prefs",
            android.content.Context.MODE_PRIVATE
        )
        prefs.edit().putInt("last_seen_streak_milestone", streak).apply()
    }

    private fun getLastSeenLevelIndex(): Int {
        val prefs = requireContext().getSharedPreferences(
            "level_prefs",
            android.content.Context.MODE_PRIVATE
        )
        return prefs.getInt("last_seen_level_index", 0)
    }

    private fun saveLastSeenLevelIndex(index: Int) {
        val prefs = requireContext().getSharedPreferences(
            "level_prefs",
            android.content.Context.MODE_PRIVATE
        )
        prefs.edit().putInt("last_seen_level_index", index).apply()
    }

    private fun getPersistentDate(key: String): String? {
        val prefs = requireContext().getSharedPreferences(
            "achievement_prefs",
            android.content.Context.MODE_PRIVATE
        )
        return prefs.getString(key, null)
    }

    private fun savePersistentDate(key: String, date: String) {
        val prefs = requireContext().getSharedPreferences(
            "achievement_prefs",
            android.content.Context.MODE_PRIVATE
        )
        prefs.edit().putString(key, date).apply()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
    }

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentHomeBinding) {
        setupViews(bind)
        if (lastState != null) {
            updateUi(lastState!!, false)
        }
        observeUiState(bind)
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private var lastState: HomeUiState? = null

    private fun currentDate() = viewModel.uiState.value.selectedDate.time
    private fun setupViews(bind: FragmentHomeBinding) {
        if (dateAdapter == null) {
            dateAdapter = DateScrollerAdapter { date ->
                viewModel.onDateSelected(date)
                currentDate = date
            }
        }
        bind.dateScrollerRecyclerView.apply {
            adapter = dateAdapter
            layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL, false
            )
            itemAnimator = null
        }
        bind.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
        exerciseAdapter = HomeExerciseLogAdapter { log -> }
        bind.recyclerExerciseLogs.apply {
            adapter = exerciseAdapter
            layoutManager = LinearLayoutManager(context)
            isNestedScrollingEnabled = false
        }

        recommendedFoodAdapter = HomeRecommendedFoodAdapter { menu ->
            val args = Bundle().apply {
                putParcelable("menu", menu)
            }
            navigate(R.id.menuDetailFragment, args)
        }
        bind.recyclerFoodRecommendations.apply {
            adapter = recommendedFoodAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        bind.btnQuickWalk.setOnClickListener {
            val args = Bundle().apply {
                putString("exerciseId", "PiyodaYurish")
            }
            navigate(R.id.addExerciseLogFragment, args)
        }

        bind.btnQuickRun.setOnClickListener {
            val args = Bundle().apply {
                putString("exerciseId", "Yugurish")
            }
            navigate(R.id.addExerciseLogFragment, args)
        }

        bind.btnQuickOther.setOnClickListener {
            navigate(R.id.selectExerciseFragment)
        }
        fun onMealClick(meal: String) {
            LogFoodFragment.navigate(
                navController = findNavController(),
                timeStamp = currentDate(),
                meal = meal
            )
        }
        bind.cardBreakfast.root.setOnClickListener { onMealClick(MealTime.Breakfast.id) }
        bind.cardBreakfast.btnAddMeal.setOnClickListener { onMealClick(MealTime.Breakfast.id) }
        bind.cardLunch.root.setOnClickListener { onMealClick(MealTime.Lunch.id) }
        bind.cardLunch.btnAddMeal.setOnClickListener { onMealClick(MealTime.Lunch.id) }
        bind.cardDinner.root.setOnClickListener { onMealClick(MealTime.Dinner.id) }
        bind.cardDinner.btnAddMeal.setOnClickListener { onMealClick(MealTime.Dinner.id) }
        bind.cardSnack.root.setOnClickListener { onMealClick(MealTime.Snack.id) }
        bind.cardSnack.btnAddMeal.setOnClickListener { onMealClick(MealTime.Snack.id) }

        bind.cardBreakfast.imgMealIcon.setImageResource(R.drawable.breakfast_ic)
        bind.cardBreakfast.textMealName.text = getString(R.string.breakfast)
        bind.cardLunch.imgMealIcon.setImageResource(R.drawable.lunch_ic)
        bind.cardLunch.textMealName.text = getString(R.string.lunch)
        bind.cardDinner.imgMealIcon.setImageResource(R.drawable.dinner_ic)
        bind.cardDinner.textMealName.text = getString(R.string.dinner)
        bind.cardSnack.imgMealIcon.setImageResource(R.drawable.snack_ic)
        bind.cardSnack.textMealName.text = getString(R.string.snack)

        bind.cardMenus.setOnClickListener {
            navigate(R.id.action_home_to_menuListFragment)
        }
        bind.cardDietolog.setOnClickListener {
            navigate(R.id.aiChatFragment)
        }
        bind.mainProgress.btnHowItWorks.setOnClickListener {
            if (viewModel.uiState.value.isCalorieExceeded || viewModel.uiState.value.isCalorieOptimal) {
                DailyLimitFragment.newInstance(DailyLimitFragment.TYPE_CALORIES)
                    .show(childFragmentManager, DailyLimitFragment.TAG)
            } else {
                navigate(R.id.action_home_to_calorieInfoFragment)
            }
        }
        bind.mainProgress.cardProtein.setOnClickListener {
            val args = Bundle().apply {
                putString(MacroInfoFragment.ARG_MACRO_TYPE, MacroInfoFragment.MACRO_PROTEIN)
            }
            navigate(R.id.action_home_to_macroInfoFragment, args)
        }

        bind.mainProgress.cardFat.setOnClickListener {
            val args = Bundle().apply {
                putString(MacroInfoFragment.ARG_MACRO_TYPE, MacroInfoFragment.MACRO_FAT)
            }
            navigate(R.id.action_home_to_macroInfoFragment, args)
        }

        bind.mainProgress.cardCarbs.setOnClickListener {
            val args = Bundle().apply {
                putString(MacroInfoFragment.ARG_MACRO_TYPE, MacroInfoFragment.MACRO_CARBS)
            }
            navigate(R.id.action_home_to_macroInfoFragment, args)
        }

        bind.btnOpenWaterDialog.setOnClickListener {
            uz.oybek.ozamiz.ui.feature.log.water.LogWaterDialog()
                .show(childFragmentManager, "LogWaterDialog")
        }

        bind.coinDisplayLayout.setOnClickListener {
            navigate(R.id.action_home_to_levelFragment)
        }

        bind.mainProgress.btnDetailedInfo.setOnClickListener {
            DailyLimitFragment.newInstance(DailyLimitFragment.TYPE_CALORIES)
                .show(childFragmentManager, DailyLimitFragment.TAG)
        }

        viewModel.onHomeEntered()
        setupDailyTip(bind)
    }

    private fun setupDailyTip(bind: FragmentHomeBinding) {
        bind.btnCloseTip.setOnClickListener {
            viewModel.onTipDismissed()
        }
    }


    companion object {
        var currentDate: Date = Date()
    }

    private fun getCurrentMealType(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> MealTime.Breakfast.id
            in 12..16 -> MealTime.Lunch.id
            in 17..22 -> MealTime.Dinner.id
            else -> MealTime.Snack.id
        }
    }

    private fun observeUiState(bind: FragmentHomeBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        bind.progressBarOverall.isVisible = state.isInitialLoading
                        bind.contentScrollView.isVisible = !state.isInitialLoading
                        bind.swipeRefreshLayout.isRefreshing = state.isRefreshing

                        if (!state.isInitialLoading) {
                            val shouldAnimate =
                                !binding.progressBarOverall.isVisible // Only animate if content was already visible
                            updateUi(state, shouldAnimate)
                        }

                        if (state.waterJustAdded) {
                            if (DateUtils.isSameDay(state.selectedDate, Date())) {
                                val todayStr = Date().dateApiFormat()
                                if (getPersistentDate("last_water_motivation_shown") != todayStr) {
                                    showWaterMotivationSnackbar()
                                    savePersistentDate("last_water_motivation_shown", todayStr)
                                }
                                checkWaterGoalReached(state)
                            }
                            viewModel.clearWaterJustAdded()
                        }

                        if (state.exerciseJustAddedType != null) {
                            if (DateUtils.isSameDay(state.selectedDate, Date())) {
                                val todayStr = Date().dateApiFormat()
                                if (getPersistentDate("last_exercise_motivation_shown") != todayStr) {
                                    showExerciseMotivationSnackbar(state.exerciseJustAddedType)
                                    savePersistentDate("last_exercise_motivation_shown", todayStr)
                                }
                                check70PercentExercise(state)
                            }
                            viewModel.clearExerciseJustAdded()
                        }

                        if (state.foodJustAdded) {
                            if (DateUtils.isSameDay(state.selectedDate, Date())) {
                                checkCalorieOptimalAchievement(state)
                            }
                            viewModel.clearFoodJustAdded()
                        }

                        if (state.pointsJustAdded == 0) {
                            bind.textPointsCount.text = state.userPoints.toString()
                        }

                        if (state.pointsJustAdded != 0) {
                            playBounceAnimation(bind, state.userPoints, state.pointsJustAdded)
                            showFloatingPointsDelta(bind, state.pointsJustAdded)
                        }

                        if (state.pointsJustAdded > 0) {
                            checkLevelUp(state.userPoints)
                            state.streakInfo?.currentStreak?.let { checkStreakMilestone(it) }
                        }

                        val tip = state.dailyTip
                        if (tip != null && state.isTipVisible && uz.oybek.ozamiz.ui.utils.DailyTipUtil.isTipsEnabled(
                                requireContext()
                            )
                        ) {
                            bind.cardDailyTip.isVisible = true
                            bind.textTipEmoji.text = tip.emoji
                            bind.textTipTitle.text = tip.title
                            bind.textTipDescription.text = tip.content
                            bind.btnTipDetails.setOnClickListener {
                                DailyTipDetailsBottomSheet.newInstance(tip)
                                    .show(childFragmentManager, DailyTipDetailsBottomSheet.TAG)
                            }
                        } else {
                            bind.cardDailyTip.isVisible = false
                        }

                        val analysis = state.yesterdayAnalysis
                        val isToday = uz.oybek.ozamiz.data.utils.DateUtils.isSameDay(
                            state.selectedDate,
                            java.util.Date()
                        )

                        // Today's forecast condition
                        val showTodayForecast = isToday &&
                                state.todayForecastFatGram > 0 &&
                                !state.isTodayForecastDismissed &&
                                state.caloriesConsumed >= state.caloriesTarget

                        // Yesterday's analysis card logic
                        if (analysis.isAvailable && isToday && !state.isYesterdayCardDismissed && !showTodayForecast) {
                            bind.cardYesterdayAnalysis.root.isVisible = true
                            bind.cardYesterdayAnalysis.btnClose.setOnClickListener { viewModel.onYesterdayCardDismissed() }
                            if (analysis.isSuccess) {
                                bind.cardYesterdayAnalysis.textTitle.text = "Ajoyib natija! 🏆"
                                if (analysis.fatBurnedGram > 0) {
                                    bind.cardYesterdayAnalysis.textDescription.text =
                                        "Siz kecha ${analysis.fatBurnedGram} gram yog' yo'qotdingiz! 1 oylik prognoz - ${
                                            String.format(
                                                "%.1f",
                                                analysis.monthlyPredictionGram
                                            )
                                        } kg"
                                } else {
                                    bind.cardYesterdayAnalysis.textDescription.text =
                                        "Vazningizni ideal holatda saqlab qoldingiz!"
                                }
                            } else {
                                bind.cardYesterdayAnalysis.textTitle.text = "Siz kuchliroqsiz! 💪"
                                bind.cardYesterdayAnalysis.textEmoji.text = "🚀"
                                bind.cardYesterdayAnalysis.textDescription.text =
                                    "Kecha energiya zaxirasi to'ldirildi. Bugun uni faollikka aylantiramiz!"
                            }

                            val openSheet = {
                                YesterdayAnalysisBottomSheet.newInstance(analysis) {
                                    viewModel.onYesterdayAnalysisShown()
                                }.show(childFragmentManager, YesterdayAnalysisBottomSheet.TAG)
                            }

                            bind.cardYesterdayAnalysis.root.setOnClickListener { openSheet() }
                            bind.cardYesterdayAnalysis.btnDetails.setOnClickListener { openSheet() }

                            if (analysis.showSheet && childFragmentManager.findFragmentByTag(
                                    YesterdayAnalysisBottomSheet.TAG
                                ) == null
                            ) {
                                // Delay slightly to avoid overlapping with other potential dialogs
                                lifecycleScope.launch {
                                    delay(800)
                                    if (isResumed && childFragmentManager.findFragmentByTag(YesterdayAnalysisBottomSheet.TAG) == null) {
                                        openSheet()
                                    }
                                }
                            }
                        } else {
                            bind.cardYesterdayAnalysis.root.isVisible = false
                        }

                        // Bugungi maqsad card
                        if (showTodayForecast) {
                            bind.cardTodayForecast.root.isVisible = true
                            bind.cardTodayForecast.btnClose.setOnClickListener { viewModel.onTodayForecastDismissed() }
                            bind.cardTodayForecast.textDescription.text =
                                "Agar bugun boshqa ovqat yemay, faqat suv ichsangiz, tanangiz yana ${state.todayForecastFatGram} gram yog' yo'qotadi! 🚀"
                        } else {
                            bind.cardTodayForecast.root.isVisible = false
                        }
                    }
                }

                launch {
                    viewModel.showRateAppEvent.collect {
                        delay(2000) // Much longer delay to not annoy the user at launch
                        if (isResumed && childFragmentManager.fragments.isEmpty()) {
                            RateAppSheet.newInstance()
                                .show(childFragmentManager, RateAppSheet.TAG)
                        }
                    }
                }

                launch {
                    viewModel.showPremiumEvent.collect {
                        delay(1500) // Staggered delay
                        if (isResumed && childFragmentManager.fragments.isEmpty()) {
                            uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog()
                                .show(
                                    childFragmentManager,
                                    uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog.TAG
                                )
                        }
                    }
                }

                launch {
                    childFragmentManager.setFragmentResultListener(
                        uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog.REQUEST_KEY,
                        viewLifecycleOwner
                    ) { _, bundle ->
                        val result =
                            bundle.getString(uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog.RESULT_ACTION)
                        if (result == "go_to_payment") {
                            navigate(R.id.paymentGetCheckFragment)
                        }
                    }
                }
            }
        }
    }

    private fun updateUi(
        state: HomeUiState,
        animate: Boolean
    ) {
        lastState = state
        binding.toolbarTitle.text = if (DateUtils.isSameDay(state.selectedDate, Date())) {
            getString(R.string.today)
        } else {
            DateUtils.getFormattedDateString(state.selectedDate, "d MMMM")
        }
        dateAdapter?.submitList(state.datesForScroller)
        dateAdapter?.setSelectedDate(state.selectedDate)
        dateAdapter?.updateData(state.dailySummaries, state.caloriesTarget)
        scrollToSelectedDate(state.selectedDate)

        val liters = String.format("%.1f", state.waterConsumed / 1000f)
        val goalLiters = String.format("%.1f", state.waterTarget / 1000f)
        binding.textCurrentWaterLiters.text = "$liters litr ichildi"
        binding.textWaterGoal.text = "Kunlik maqsad: $goalLiters litr"
        binding.textWaterGoal.isVisible = true

        binding.progressWater.max = state.waterTarget
        binding.progressWater.setProgressCompat(state.waterConsumed, animate)

        val streakInfo = state.streakInfo
        if (streakInfo != null) {
            binding.streakDisplayLayout.isVisible = true
            binding.textStreakCount.text = streakInfo.currentStreak.toString()

            if (binding.streakDisplayLayout.scaleX == 1f) {
                binding.streakDisplayLayout.animate()
                    .scaleX(1.1f).scaleY(1.1f)
                    .setDuration(300)
                    .withEndAction {
                        binding.streakDisplayLayout.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(300)
                            .start()
                    }
                    .start()
            }
            binding.streakDisplayLayout.setOnClickListener {
                uz.oybek.ozamiz.ui.feature.streak.StreakInfoSheet
                    .newInstance(streakInfo)
                    .show(
                        childFragmentManager,
                        uz.oybek.ozamiz.ui.feature.streak.StreakInfoSheet.TAG
                    )
            }
        } else {
            binding.streakDisplayLayout.isVisible = false
        }

        val primaryColor = com.google.android.material.color.MaterialColors.getColor(
            requireView(),
            androidx.appcompat.R.attr.colorPrimary
        )
        val redColor = ContextCompat.getColor(requireContext(), R.color.red)
        val orangeColor = ContextCompat.getColor(requireContext(), R.color.orange)
        val yellowColor = com.google.android.material.color.MaterialColors.getColor(
            requireView(),
            R.attr.customYellow
        )
        val greenColor = com.google.android.material.color.MaterialColors.getColor(
            requireView(),
            R.attr.customGreen
        )
        val trackColor = com.google.android.material.color.MaterialColors.getColor(
            requireView(),
            com.google.android.material.R.attr.colorSurfaceContainerHigh
        )
        val onSurfaceColor = com.google.android.material.color.MaterialColors.getColor(
            requireView(),
            com.google.android.material.R.attr.colorOnSurface
        )

        binding.mainProgress.apply {
            textCaloriesRemainingCount.text = state.caloriesRemaining.absoluteValue.toString()

            textEatenCount.text =
                "${state.caloriesConsumed}    Maqsad: ${state.caloriesTarget} oshirmaslik"

            progressCaloriesCircular.max = state.caloriesTarget
            progressCaloriesCircular.setProgressCompat(
                state.caloriesConsumed.absoluteValue,
                animate
            )
            if (state.isCalorieOptimal) {
                layoutNormalProgress.isVisible = false
                layoutLimitState.isVisible = true
                textLimitTitle.text = "Bugungga yetarli yedingiz"
                textLimitSubtitle.text = "Boshqa ovqat yemaslikka harakat qiling!"
                imgLimitState.setImageResource(R.drawable.check_ic)
                imgLimitState.imageTintList = android.content.res.ColorStateList.valueOf(greenColor)
                macrosContainer.isVisible = false
                binding.layoutMacroWarnings.isVisible = false
            }
            progressCaloriesCircular.setIndicatorColor(primaryColor)
            progressCaloriesCircular.trackColor = trackColor
            textCaloriesRemainingCount.setTextColor(onSurfaceColor)


            val proteinExceeded = state.isProteinExceeded
            val proteinOptimal =
                state.proteinConsumed >= state.proteinTarget && !state.isProteinExceeded
            val proteinTargetReached = state.proteinConsumed >= state.proteinTarget
            binding.mainProgress.cardProtein.isVisible = !proteinTargetReached

            val proteinRemaining = state.proteinTarget - state.proteinConsumed
            progressProtein.max =
                if (proteinExceeded) state.proteinConsumed else state.proteinTarget
            progressProtein.setProgressCompat(state.proteinConsumed, animate)
            progressProtein.setIndicatorColor(if (proteinExceeded) redColor else if (proteinOptimal) greenColor else orangeColor)
            progressProtein.trackColor = trackColor
            textProteinValue.setTextColor(if (proteinExceeded) redColor else if (proteinOptimal) greenColor else onSurfaceColor)
            textProteinValue.text = if (proteinExceeded)
                getString(R.string.macro_exceeded_format, -proteinRemaining)
            else if (proteinOptimal)
                "Bugungga yetadi"
            else
                getString(R.string.macro_remaining_format, proteinRemaining)

            val fatExceeded = state.isFatExceeded
            val fatOptimal = state.fatConsumed >= state.fatTarget && !state.isFatExceeded
            val fatTargetReached = state.fatConsumed >= state.fatTarget
            binding.mainProgress.cardFat.isVisible = !fatTargetReached

            val fatRemaining = state.fatTarget - state.fatConsumed
            progressFat.max = if (fatExceeded) state.fatConsumed else state.fatTarget
            progressFat.setProgressCompat(state.fatConsumed, animate)
            progressFat.setIndicatorColor(if (fatExceeded) redColor else if (fatOptimal) greenColor else yellowColor)
            progressFat.trackColor = trackColor
            textFatValue.setTextColor(if (fatExceeded) redColor else if (fatOptimal) greenColor else onSurfaceColor)
            textFatValue.text = if (fatExceeded)
                getString(R.string.macro_exceeded_format, -fatRemaining)
            else if (fatOptimal)
                "Bugungga yetadi"
            else
                getString(R.string.macro_remaining_format, fatRemaining)

            // Carbs
            val carbsExceeded = state.isCarbsExceeded
            val carbsOptimal = state.carbsConsumed >= state.carbsTarget && !state.isCarbsExceeded
            val carbsTargetReached = state.carbsConsumed >= state.carbsTarget
            binding.mainProgress.cardCarbs.isVisible = !carbsTargetReached

            val carbsRemaining = state.carbsTarget - state.carbsConsumed
            progressCarbs.max = if (carbsExceeded) state.carbsConsumed else state.carbsTarget
            progressCarbs.setProgressCompat(state.carbsConsumed, animate)
            progressCarbs.setIndicatorColor(if (carbsExceeded) redColor else if (carbsOptimal) greenColor else greenColor)
            progressCarbs.trackColor = trackColor
            textCarbsValue.setTextColor(if (carbsExceeded) redColor else if (carbsOptimal) greenColor else onSurfaceColor)
            textCarbsValue.text = if (carbsExceeded)
                getString(R.string.macro_exceeded_format, -carbsRemaining)
            else if (carbsOptimal)
                "Bugungga yetadi"
            else
                getString(R.string.macro_remaining_format, carbsRemaining)
        }

        val breakfastCalories = state.dailyFoodLogs.filter { it.mealType == MealTime.Breakfast.id }
            .sumOf { it.calories }
        val lunchCalories =
            state.dailyFoodLogs.filter { it.mealType == MealTime.Lunch.id }.sumOf { it.calories }
        val dinnerCalories =
            state.dailyFoodLogs.filter { it.mealType == MealTime.Dinner.id }.sumOf { it.calories }
        val snackCalories =
            state.dailyFoodLogs.filter { it.mealType == MealTime.Snack.id }.sumOf { it.calories }

        val target = state.caloriesTarget.toFloat()
        val bMax = (target * 0.3f).toInt().takeIf { it > 0 } ?: 600
        val lMax = (target * 0.4f).toInt().takeIf { it > 0 } ?: 800
        val dMax = (target * 0.2f).toInt().takeIf { it > 0 } ?: 400
        val sMax = (target * 0.1f).toInt().takeIf { it > 0 } ?: 200

        val onSurfaceVarColor = com.google.android.material.color.MaterialColors.getColor(
            requireView(),
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )

        fun bindMealCard(
            cardBinding: uz.oybek.ozamiz.databinding.ItemHomeMealCardBinding,
            consumed: Int,
            max: Int
        ) {
            cardBinding.textMealCalories.isVisible = true
            cardBinding.progressMeal.isVisible = true
            if (consumed >= max) {
                cardBinding.textMealCalories.text = "Bugungga yetadi"
                cardBinding.textMealCalories.setTextColor(greenColor)
                cardBinding.progressMeal.max = max
                cardBinding.progressMeal.setProgressCompat(max, animate)
            } else {
                cardBinding.textMealCalories.text = "${max - consumed} kkal qoldi"
                cardBinding.textMealCalories.setTextColor(onSurfaceVarColor)
                cardBinding.progressMeal.max = max
                cardBinding.progressMeal.setProgressCompat(consumed, animate)
            }
        }

        bindMealCard(binding.cardBreakfast, breakfastCalories, bMax)
        bindMealCard(binding.cardLunch, lunchCalories, lMax)
        bindMealCard(binding.cardDinner, dinnerCalories, dMax)
        bindMealCard(binding.cardSnack, snackCalories, sMax)

        // Exercise Progress
        binding.progressExercise.max = state.exerciseTarget
        binding.progressExercise.setProgressCompat(state.exerciseBurnt, animate)
        binding.textExerciseProgress.text = "${state.exerciseBurnt} / ${state.exerciseTarget} kkal"

        val hasExercises = state.exerciseLogs.isNotEmpty()
        binding.recyclerExerciseLogs.isVisible = hasExercises
        if (hasExercises) {
            exerciseAdapter.submitList(state.exerciseLogs)
        }
        binding.recyclerExerciseLogs.scrollToPosition(0)



        binding.warningProtein.root.isVisible = state.isProteinExceeded
        if (state.isProteinExceeded) {
            binding.warningProtein.textWarningTitle.text = "Oqsil oshdi!"
            binding.warningProtein.textWarningSubtitle.text =
                "Cheklang: Go'sht, tuxum, tovuq, baliq, tvorog, mosh, protein."
            binding.warningProtein.root.setOnClickListener {
                DailyLimitFragment.newInstance(DailyLimitFragment.TYPE_PROTEIN)
                    .show(childFragmentManager, DailyLimitFragment.TAG)
            }
        }

        binding.warningCarbs.root.isVisible = state.isCarbsExceeded
        if (state.isCarbsExceeded) {
            binding.warningCarbs.textWarningTitle.text = "Uglevod oshdi!"
            binding.warningCarbs.textWarningSubtitle.text =
                "Cheklang: Non, guruch, makaron, kartoshka, shirinliklar, pishiriqlar, banan, uzum."
            binding.warningCarbs.root.setOnClickListener {
                DailyLimitFragment.newInstance(DailyLimitFragment.TYPE_CARBS)
                    .show(childFragmentManager, DailyLimitFragment.TAG)
            }
        }

        binding.warningFat.root.isVisible = state.isFatExceeded
        if (state.isFatExceeded) {
            binding.warningFat.textWarningTitle.text = "Yog' oshdi!"
            binding.warningFat.textWarningSubtitle.text =
                "Cheklang: Qovurilgan ovqat, yog'li go'sht, sariyog', mayonez, pishloq, yong'oq, fast-fud."
            binding.warningFat.root.setOnClickListener {
                DailyLimitFragment.newInstance(DailyLimitFragment.TYPE_FAT)
                    .show(childFragmentManager, DailyLimitFragment.TAG)
            }
        }

        val hasRecommendedFoods = state.recommendedFoods.isNotEmpty()
        binding.layoutRecommendations.isVisible = hasRecommendedFoods
        if (hasRecommendedFoods) {
            binding.textRecommendationTitle.text =
                "${state.recommendingForMeal}ga tavsiya"
            recommendedFoodAdapter.submitList(state.recommendedFoods)
        }
    }


    private fun scrollToSelectedDate(selectedDate: Date) {
        binding.dateScrollerRecyclerView.post {
            val selectedIndex = viewModel.uiState.value.datesForScroller.indexOfFirst {
                DateUtils.isSameDay(it, selectedDate)
            }
            if (selectedIndex != -1) {
                (binding.dateScrollerRecyclerView.layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset(
                        selectedIndex,
                        (binding.root.width / 2) - 32f.dpToPx()
                    )
            }
        }
    }

    private var animationJob: kotlinx.coroutines.Job? = null
    private fun playBounceAnimation(bind: FragmentHomeBinding, finalPoints: Int, pointsAdded: Int) {
        if (pointsAdded == 0) return
        animationJob?.cancel()
        val startPoints = finalPoints - pointsAdded
        bind.lottieCoin.cancelAnimation()
        bind.lottieCoin.progress = 0f
        bind.lottieCoin.playAnimation()

        // Hide original sync update briefly to avoid conflict
        animationJob = viewLifecycleOwner.lifecycleScope.launch {
            val stepsCount = pointsAdded.absoluteValue.coerceAtMost(10)
            val stepValue = pointsAdded / stepsCount
            val stepDelay =
                if (pointsAdded.absoluteValue <= 5) 150L else if (pointsAdded.absoluteValue <= 10) 100L else 70L

            for (i in 1..stepsCount) {
                val currentVal = if (i == stepsCount) finalPoints else startPoints + (i * stepValue)
                bind.textPointsCount.text = currentVal.toString()

                // Pure code-based animation
                bind.textPointsCount.apply {
                    // Reset to initial for each bounce start
                    translationY = 15f
                    scaleX = 0.9f
                    scaleY = 0.9f
                    alpha = 0.8f

                    animate()
                        .translationY(0f)
                        .scaleX(1.1f)
                        .scaleY(1.1f)
                        .alpha(1f)
                        .setDuration(stepDelay / 2)
                        .setInterpolator(android.view.animation.OvershootInterpolator())
                        .withEndAction {
                            animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(stepDelay / 2)
                                .start()
                        }
                        .start()
                }
                delay(stepDelay)
            }
            bind.textPointsCount.text = finalPoints.toString()
            // Points already updated to final, now synced with state
            viewModel.clearPointsJustAdded()
        }
    }

    private fun showFloatingPointsDelta(bind: FragmentHomeBinding, pointsAdded: Int) {
        bind.textPointsDelta.apply {
            text = if (pointsAdded > 0) "+$pointsAdded" else "$pointsAdded"
            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (pointsAdded > 0) R.color.green else R.color.red
                )
            )
            alpha = 1f
            isVisible = true

            animate()
                .alpha(0f)
                .setDuration(1200)
                .setStartDelay(500)
                .withEndAction {
                    isVisible = false
                }
                .start()
        }
    }

    private fun Float.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    private fun Int.dpToPx(): Int = (this.toFloat() * resources.displayMetrics.density).toInt()

    val messages = listOf(
        "Suv miya faoliyatini tezlashtiradi va diqqatni jamlashga yordam beradi.",
        "Tana haroratini me'yorda ushlab turish bevosita suvga bog'liq.",
        "Suv bo'g'imlar va xaftaga to'qimalarini moylash vazifasini bajaradi.",
        "Ovqat hazm qilish jarayoni suv ishtirokisiz to'liq amalga oshmaydi.",
        "Suv organizmdagi zararli toksinlarni buyrak orqali yuvib chiqaradi.",
        "Teri hujayralari yangilanishi va tarang tortilishi uchun suv kerak.",
        "Qon bosimini bir maromda saqlashda toza suv yordam beradi.",
        "Suv tomirlar orqali kislorodni hujayralarga yetkazib beradi.",
        "Toza suv ichish buyrak toshlari hosil bo'lishining oldini oladi.",
        "Tish va suyaklarni mustahkamlovchi minerallarni suv tashiydi.",
        "Nafas yo'llarini namlab, infeksiyalardan himoya qiluvchi vosita bu - suv.",
        "Suv metabolizmni 24-30 foizgacha tezlashtirishi ilmiy tasdiqlangan.",
        "Ortiqcha vazndan xalos bo'lishda suv tana yog'larini parchalashga yordam beradi.",
        "Suv infarkt va insult xavfini sezilarli darajada kamaytiradi.",
        "Yetarli suv ichish qabziyat (ich qotishi) muammosining oldini oladi.",
        "Bosh og'rig'i ko'pincha suvsizlanishning birinchi belgisidir.",
        "Suv ichish jigarga o'z tozalash funksiyasini bajarishga yordam beradi.",
        "Suvsizlik jismoniy charchoqni 50 foizgacha oshirib yuborishi mumkin.",
        "Gormonlar balansi barqaror bo'lishida qon tarkibidagi suv muhim.",
        "Yurak mushaklari to'liq qon haydashi uchun tana doim namlangan bo'lishi lozim.",
        "Suv sport mashg'ulotlarida mushaklarning shikastlanish xavfini kamaytiradi.",
        "Qonning quyuqlanishiga yo'l qo'ymaslik suv orqali amalga oshadi.",
        "Ko'zlar qurib qolmasligi va yaxshi ko'rishi toza suvga ham bog'liq.",
        "Immunitetni oshiradigan oq qon hujayralari harakati suvni talab qiladi.",
        "Hujayralar ichidagi biokimyoviy jarayonlar faqat suvli muhitda kechadi.",
        "Suv qorin ochish hissini pasaytirishi va ortiqcha ovqatlanishni to'xtatishi mumkin.",
        "Qariyalik jarayonlarini sekinlashtirish hujayrani suv bilan to'yintirishdan boshlanadi.",
        "Siydik yo'li infeksiyalaridan saqlanish uchun ko'proq suv ichish kerak.",
        "To'qimalarning elastikligi aynan ularning tarang suv balansiga bog'liq.",
        "Toza suv og'iz bo'shlig'ida bakteriyalar ko'payishining oldini oladi.",
        "Ovqatdagi barcha foydali vitaminlar qonga o'tishida suv vositachi bo'ladi.",
        "Suv tanadagi tuz va minerallar balansini, ya'ni elektrolitlarni saqlaydi.",
        "Sochlar qalin va yaltiroq o'sishi qon aylanishiga, qon hajmi esa suvga bog'liq.",
        "Kayfiyat keskin tushib ketishi miya suvsizlanishi oqibati bo'lishi mumkin.",
        "Allergiya gormoni gistamin ishlab chiqarilishini suv miqdori biroz pasaytiradi.",
        "Tana shikastlanganda hujayralarni tiklovchi ozuqalarni faqat suv tashiydi.",
        "Asab tizimidagi impulslar uzatilishida elektrolitlar va suv zarur.",
        "Buyraklar sog'lom ishlashi uchun doimiy toza suyuqlik aylanishi shart.",
        "Jismoniy terlash tanani sovutadi, buni esa suv ta'minlaydi.",
        "Suv qon zardobi hajmini ushlab turib, kuchli holsizlikni cheklaydi.",
        "Kundalik charchoq hissini yo'qotishning eng oddiy yechimi bu - suv.",
        "Suvsizlanish odamda asabiylik va vahima hissini kuchaytiradi.",
        "Meditsinada qonni tozalash asosan ko'proq suyuqlik yuborish bilan boshlanadi.",
        "Ichak mikroflorasi sog'lom bo'lishi ham me'yoriy namlik bilan bog'liq.",
        "Qandli diabet rivojlanish xavfini toza suv kamaytirishi isbotlangan.",
        "Suyak iligi barcha hujayralarni ishlab chiqarishida doimiy suv talab qiladi.",
        "Muskul massasini ko'paytirish uchun oqsil bilan birga suv ham juda muhim.",
        "Ovqat tarkibidagi oqsil va uglevodlarni sintez qilish jarayoni suv bilan tezlashadi.",
        "Miyaning kulrang moddasi o'z energiyasini qayta tiklashida namlik talab qiladi.",
        "Yaxshi o'zlashtirilgan suv kasallik pallalarida viruslarni yuvish vazifasini o'taydi."
    )

    private fun showWaterMotivationSnackbar() {
        val message = messages.random()

        val snackbar = Snackbar.make(binding.root, "", 5000)
        val snackbarLayout = snackbar.view as Snackbar.SnackbarLayout
        snackbarLayout.setBackgroundColor(Color.TRANSPARENT)
        snackbarLayout.setPadding(0, 0, 0, 16.dpToPx())

        snackbarLayout.clipChildren = false
        snackbarLayout.clipToPadding = false
        (snackbarLayout.parent as? ViewGroup)?.let {
            it.clipChildren = false
            it.clipToPadding = false
        }

        val customView = layoutInflater.inflate(R.layout.custom_water_snackbar, null)
        val textView = customView.findViewById<TextView>(R.id.text_snackbar_motivation)
        textView.text = message

        val btnClose = customView.findViewById<View>(R.id.btn_close_snackbar)
        btnClose.setOnClickListener {
            snackbar.dismiss()
        }

        snackbarLayout.addView(customView, 0)
        snackbar.show()
    }

    private fun checkWaterGoalReached(state: HomeUiState) {
        val todayStr = Date().dateApiFormat()
        if (state.waterTarget > 0 &&
            state.waterConsumed >= state.waterTarget &&
            getPersistentDate("last_water_goal_shown") != todayStr
        ) {
            showWaterGoalReachedAchievement()
            savePersistentDate("last_water_goal_shown", todayStr)
        }
    }

    private fun showWaterGoalReachedAchievement() {
        if (activity == null || isDetached) return
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(R.layout.dialog_water_achievement)
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnAjoyib = dialog.findViewById<View>(R.id.btn_ajoyib)
        btnAjoyib?.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun getExerciseMotivationTexts(exerciseInfo: String?): List<String> {
        val type = exerciseInfo?.replaceFirstChar { it.uppercase() } ?: "Harakat"
        val lowerType = exerciseInfo?.lowercase() ?: "harakat"
        return listOf(
            "$type organizmdagi ortiqcha yog'larni tabiiy energiya manbaiga aylantiradi.",
            "$type qon aylanishini tezlashtirib, barcha organlarga kislorod yetkazib beradi.",
            "Har bir daqiqalik $lowerType sizni maqsadli vazningizga yaqinlashtiradi.",
            "$type yordamida nafaqat kaloriya, balki stress gormonlari ham yoqiladi.",
            "Muntazam $lowerType bilan shug'ullanish orqali immunitetingiz 30% ga kuchayadi.",
            "$type uyqu sifatini ajoyib darajada yaxshilaydi va asablarni tinchlantiradi.",
            "Faol $lowerType bazal metabolizmni tezlashtiradi, ya'ni dam olganda ham oza boshlaysiz.",
            "$type bo'g'im va mushaklarning elastikligini saqlash uchun eng kuchli vositadir.",
            "$type yurak mushagini chiniqtiradi, natijada yurak kamroq urib, ko'proq qon haydaydi.",
            "$type bilan qon tozalanishi tezlashadi va teringiz sog'lom porlaydi."
        )
    }

    private fun showExerciseMotivationSnackbar(exerciseId: String) {
        val exerciseName = try {
            uz.oybek.ozamiz.data.logs.model.UserExercise.valueOf(exerciseId).exerciseName
        } catch (e: Exception) {
            "Harakat"
        }
        val messages = getExerciseMotivationTexts(exerciseName)
        val message = messages.random()

        val snackbar = Snackbar.make(binding.root, "", 5000)
        val snackbarLayout = snackbar.view as Snackbar.SnackbarLayout
        snackbarLayout.setBackgroundColor(Color.TRANSPARENT)
        snackbarLayout.setPadding(0, 0, 0, 16.dpToPx())

        snackbarLayout.clipChildren = false
        snackbarLayout.clipToPadding = false
        (snackbarLayout.parent as? ViewGroup)?.let {
            it.clipChildren = false
            it.clipToPadding = false
        }

        val customView = layoutInflater.inflate(R.layout.custom_exercise_snackbar, null)
        val textView = customView.findViewById<TextView>(R.id.text_snackbar_motivation)
        textView.text = message

        val btnClose = customView.findViewById<View>(R.id.btn_close_snackbar)
        btnClose.setOnClickListener {
            snackbar.dismiss()
        }

        snackbarLayout.addView(customView, 0)
        snackbar.show()
    }

    private fun check70PercentExercise(state: HomeUiState) {
        val todayStr = Date().dateApiFormat()
        if (state.exerciseTarget > 0 &&
            state.exerciseBurnt >= state.exerciseTarget * 0.7 &&
            getPersistentDate("last_exercise_70_shown") != todayStr
        ) {
            show70PercentExerciseAchievement()
            savePersistentDate("last_exercise_70_shown", todayStr)
        }
    }

    private fun show70PercentExerciseAchievement() {
        if (activity == null || isDetached) return
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(R.layout.dialog_exercise_achievement)
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnAjoyib = dialog.findViewById<View>(R.id.btn_ajoyib)
        btnAjoyib?.setOnClickListener {
            dialog.dismiss()
        }
    }


    private fun checkCalorieOptimalAchievement(state: HomeUiState) {
        val todayStr = Date().dateApiFormat()

        // Let's check if the user is in optimal calorie range and has consumed more than 80% of target
        if (state.caloriesTarget > 0 &&
            state.caloriesConsumed >= state.caloriesTarget * 0.8 &&
            state.isCalorieOptimal &&
            getPersistentDate("last_calorie_goal_shown") != todayStr
        ) {

            showCalorieOptimalAchievement()
            savePersistentDate("last_calorie_goal_shown", todayStr)
        }
    }

    private fun showCalorieOptimalAchievement() {
        if (activity == null || isDetached) return
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(R.layout.dialog_food_achievement)
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnAjoyib = dialog.findViewById<View>(R.id.btn_ajoyib)
        btnAjoyib?.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun checkLevelUp(points: Int) {
        if (lastShownLevelIndex == -1) {
            lastShownLevelIndex = getLastSeenLevelIndex()
        }
        val currentIndex = LevelManager.getCurrentLevelIndex(points)
        if (currentIndex > lastShownLevelIndex) {
            lastShownLevelIndex = currentIndex
            saveLastSeenLevelIndex(currentIndex)
            val newLevel = LevelManager.levels[currentIndex]
            showLevelUpDialog(
                newLevel.emoji,
                newLevel.name,
                newLevel.motivationText,
                newLevel.color
            )
        }
    }

    private fun showLevelUpDialog(emoji: String, name: String, motivation: String, color: Long) {
        if (activity == null || isDetached) return

        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_level_up, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        val parsedColor = Color.parseColor(String.format("#%06X", 0xFFFFFF and color.toInt()))

        dialogView.findViewById<android.widget.TextView>(R.id.text_level_emoji)?.text = emoji
        dialogView.findViewById<android.widget.TextView>(R.id.text_level_name)?.apply {
            text = name
            setTextColor(parsedColor)
        }
        dialogView.findViewById<android.widget.TextView>(R.id.text_level_up_title)
            ?.setTextColor(parsedColor)
        dialogView.findViewById<android.widget.TextView>(R.id.text_level_motivation)?.text =
            motivation
        dialogView.findViewById<View>(R.id.btn_level_up_ok)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun checkStreakMilestone(streak: Int) {
        if (lastShownStreak == -1) {
            lastShownStreak = getLastSeenStreak()
        }

        val milestones = listOf(3, 7, 14, 30, 60, 100, 150, 200, 365)
        val currentMilestone = milestones.lastOrNull { streak >= it } ?: 0

        if (currentMilestone > lastShownStreak) {
            lastShownStreak = currentMilestone
            saveLastSeenStreak(currentMilestone)
            showStreakMilestoneDialog(currentMilestone)
        }
    }

    private fun showStreakMilestoneDialog(days: Int) {
        if (activity == null || isDetached) return

        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_level_up, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        val color = 0xFF4CAF50 // Green for streak
        val emoji = when {
            days >= 30 -> "👑"
            days >= 7 -> "🔥"
            else -> "⚡"
        }

        val title = when (days) {
            3 -> "Ajoyib boshlanish!"
            7 -> "Bir hafta!"
            14 -> "Ikk haftalik intizom!"
            30 -> "Bir oylik g'alaba!"
            else -> "$days kunlik natija!"
        }

        val motivation =
            "Siz ketma-ket $days kundan beri o'z maqsadingiz sari bormoqdasiz. To'xtamang!"

        dialogView.findViewById<android.widget.TextView>(R.id.text_level_emoji)?.text = emoji
        dialogView.findViewById<android.widget.TextView>(R.id.text_level_name)?.apply {
            text = "$days kun"
            setTextColor(color.toInt())
        }
        dialogView.findViewById<android.widget.TextView>(R.id.text_level_up_title)?.apply {
            text = title
            setTextColor(color.toInt())
        }
        dialogView.findViewById<android.widget.TextView>(R.id.text_level_motivation)?.text =
            motivation
        dialogView.findViewById<View>(R.id.btn_level_up_ok)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}