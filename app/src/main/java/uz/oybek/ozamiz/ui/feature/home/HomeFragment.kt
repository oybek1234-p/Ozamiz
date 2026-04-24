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
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.level.LevelManager
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.utils.dateApiFormat
import uz.oybek.ozamiz.databinding.FragmentHomeBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.log.food.DailyLimitFragment
import uz.oybek.ozamiz.ui.feature.log.food.LogFoodFragment
import uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog
import uz.oybek.ozamiz.ui.feature.premium.isPremiumOrTrial
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import java.util.Calendar
import java.util.Date
import kotlin.math.absoluteValue

class LogFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by activityViewModels()
    private var dateAdapter: DateScrollerAdapter? = null
    private var lastShownLevelIndex: Int = -1

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
    }

    private val cachedColors by lazy {
        object {
            val primary = com.google.android.material.color.MaterialColors.getColor(
                requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLUE
            )
            val red = ContextCompat.getColor(requireContext(), R.color.red)
            val gray = ContextCompat.getColor(requireContext(), R.color.grey)
            val orange = ContextCompat.getColor(requireContext(), R.color.orange)
            val yellow = com.google.android.material.color.MaterialColors.getColor(
                requireContext(), R.attr.customYellow, Color.YELLOW
            )
            val green = com.google.android.material.color.MaterialColors.getColor(
                requireContext(), R.attr.customGreen, Color.GREEN
            )
            val track = com.google.android.material.color.MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorSurfaceContainerHigh,
                Color.LTGRAY
            )
            val onSurface = com.google.android.material.color.MaterialColors.getColor(
                requireContext(), com.google.android.material.R.attr.colorOnSurface, Color.BLACK
            )
            val onSurfaceVariant = com.google.android.material.color.MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                Color.DKGRAY
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dateAdapter = null
    }

    private fun getLastSeenLevelIndex(): Int {
        val prefs = requireContext().getSharedPreferences(
            "level_prefs", android.content.Context.MODE_PRIVATE
        )
        return prefs.getInt("last_seen_level_index", 0)
    }

    private fun saveLastSeenLevelIndex(index: Int) {
        val prefs = requireContext().getSharedPreferences(
            "level_prefs", android.content.Context.MODE_PRIVATE
        )
        prefs.edit().putInt("last_seen_level_index", index).apply()
    }

    private fun getPersistentDate(key: String): String? {
        val prefs = requireContext().getSharedPreferences(
            "achievement_prefs", android.content.Context.MODE_PRIVATE
        )
        return prefs.getString(key, null)
    }

    private fun savePersistentDate(key: String, date: String) {
        val prefs = requireContext().getSharedPreferences(
            "achievement_prefs", android.content.Context.MODE_PRIVATE
        )
        prefs.edit().putString(key, date).apply()
    }


    init {
        showBottomSheet = true
    }

    override fun viewCreated(bind: FragmentHomeBinding) {
        setupViews(bind)
        lastState?.let { updateUi(it, false) }
        observeUiState(bind)
        setupFragmentResults()
        viewLifecycleOwner.lifecycleScope.launch {
            delay(2000)
            checkNotificationPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onHomeEntered()
        checkSpotlightOnResume()
    }

    private fun checkSpotlightOnResume() {
        val prefs =
            requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val step = prefs.getInt("app_onboarding_step", 0)
        if (step == 1) {
            startBreakfastSpotlight()
        } else {
            resetBreakfastSpotlight()
        }
    }

    private fun resetBreakfastSpotlight() {
        if (bindingT == null) return
        val btn = binding.cardBreakfast.btnAddMeal
        btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
            com.google.android.material.color.MaterialColors.getColor(
                btn.context,
                com.google.android.material.R.attr.colorSurfaceContainerHighest,
                Color.GRAY
            )
        )
        btn.imageTintList = android.content.res.ColorStateList.valueOf(
            com.google.android.material.color.MaterialColors.getColor(
                btn.context,
                com.google.android.material.R.attr.colorOnSurface,
                Color.BLACK
            )
        )
        btn.scaleX = 1f
        btn.scaleY = 1f
        btn.animate().cancel()
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    private fun setupFragmentResults() {
        childFragmentManager.setFragmentResultListener(
            "water_added_request", viewLifecycleOwner
        ) { _, bundle ->
            val added = bundle.getBoolean("added", false)
            if (added) {
                vibrateDevice()
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
                    lastState?.let {
                        checkWaterGoalReached(it)
                    }
                }
            }
        }
        parentFragmentManager.setFragmentResultListener(
            "exercise_added_request", viewLifecycleOwner
        ) { _, bundle ->
            val exerciseId = bundle.getString("exerciseId")
            if (exerciseId != null) {
                vibrateDevice()
                viewModel.onExerciseAdded()
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
                    lastState?.let {
                        check70PercentExercise(it)
                    }
                }
            }
        }

        parentFragmentManager.setFragmentResultListener(
            "navigate_to_ai_chat", viewLifecycleOwner
        ) { _, bundle ->
            navigate(R.id.aiChatFragment, bundle)
        }
    }


    private fun checkPremium(action: () -> Unit) {
        if (UserRepository.user.isPremiumOrTrial()) {
            action()
        } else {
            PremiumBottomSheetDialog().show(childFragmentManager, PremiumBottomSheetDialog.TAG)
        }
    }

    private fun checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), android.Manifest.permission.POST_NOTIFICATIONS
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
                context, LinearLayoutManager.HORIZONTAL, false
            )
            itemAnimator = null
        }
        bind.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
        bind.btnTodayExercises.setOnSafeClickListener {
            lastState?.let { state ->
                TodayExercisesBottomSheet.newInstance(state.exerciseLogs)
                    .show(childFragmentManager, TodayExercisesBottomSheet.TAG)
            }
        }


        bind.btnQuickWalk.setOnSafeClickListener {
            checkPremium {
                val args = Bundle().apply {
                    putString("exerciseId", "PiyodaYurish")
                }
                navigate(R.id.addExerciseLogFragment, args)
            }
        }

        bind.btnQuickRun.setOnSafeClickListener {
            checkPremium {
                val args = Bundle().apply {
                    putString("exerciseId", "Yugurish")
                }
                navigate(R.id.addExerciseLogFragment, args)
            }
        }

        bind.btnQuickOther.setOnSafeClickListener {
            checkPremium {
                navigate(R.id.selectExerciseFragment)
            }
        }
        fun onMealClick(meal: String) {
            LogFoodFragment.navigate(
                navController = findNavController(),
                timeStamp = currentDate(),
                meal = meal,
                openCamera = false
            )
        }
        bind.cardBreakfast.root.setOnSafeClickListener { onMealClick(MealTime.Breakfast.id) }
        bind.cardBreakfast.btnAddMeal.setOnSafeClickListener { onMealClick(MealTime.Breakfast.id) }
        bind.cardLunch.root.setOnSafeClickListener { onMealClick(MealTime.Lunch.id) }
        bind.cardLunch.btnAddMeal.setOnSafeClickListener { onMealClick(MealTime.Lunch.id) }
        bind.cardDinner.root.setOnSafeClickListener { onMealClick(MealTime.Dinner.id) }
        bind.cardDinner.btnAddMeal.setOnSafeClickListener { onMealClick(MealTime.Dinner.id) }
        bind.cardSnack.root.setOnSafeClickListener { onMealClick(MealTime.Snack.id) }
        bind.cardSnack.btnAddMeal.setOnSafeClickListener { onMealClick(MealTime.Snack.id) }

        bind.cardBreakfast.imgMealIcon.setImageResource(R.drawable.breakfast_ic)
        bind.cardBreakfast.textMealName.text = getString(R.string.breakfast)
        bind.cardLunch.imgMealIcon.setImageResource(R.drawable.lunch_ic)
        bind.cardLunch.textMealName.text = getString(R.string.lunch)
        bind.cardDinner.imgMealIcon.setImageResource(R.drawable.dinner_ic)
        bind.cardDinner.textMealName.text = getString(R.string.dinner)
        bind.cardSnack.imgMealIcon.setImageResource(R.drawable.snack_ic)
        bind.cardSnack.textMealName.text = getString(R.string.snack)

        bind.cardMenus.setOnSafeClickListener {
            navigate(R.id.action_home_to_menuListFragment)
        }
        bind.cardDietolog.setOnSafeClickListener {
            checkPremium {
                navigate(R.id.aiChatFragment)
            }
        }
        bind.mainProgress.btnHowItWorks.setOnSafeClickListener {
            if (viewModel.uiState.value.isCalorieExceeded || viewModel.uiState.value.isCalorieOptimal) {
                DailyLimitFragment.newInstance(
                    DailyLimitFragment.TYPE_CALORIES, viewModel.uiState.value.lastFoodTimestamp
                ).show(childFragmentManager, DailyLimitFragment.TAG)
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

        bind.btnOpenWaterDialog.setOnSafeClickListener {
            checkPremium {
                uz.oybek.ozamiz.ui.feature.log.water.LogWaterDialog()
                    .show(childFragmentManager, "LogWaterDialog")
            }
        }

        bind.coinDisplayLayout.setOnSafeClickListener {
            navigate(R.id.action_home_to_levelFragment)
        }

        bind.mainProgress.btnDetailedInfo.setOnSafeClickListener {
            DailyLimitFragment.newInstance(
                DailyLimitFragment.TYPE_CALORIES, viewModel.uiState.value.lastFoodTimestamp
            ).show(childFragmentManager, DailyLimitFragment.TAG)
        }

        setupDailyTip(bind)

        bind.btnAddWeightHome.setOnSafeClickListener {
            navigate(R.id.photoUploadInfoFragment)
        }




        bind.cardDailyHealthTasks.setOnSafeClickListener {
            findNavController().navigate(R.id.action_home_to_dailyTasksFragment)
        }
        bind.btnTasksDone.setOnSafeClickListener {
            findNavController().navigate(R.id.action_home_to_dailyTasksFragment)
        }
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

                        if (state.pointsJustAdded == 0) {
                            bind.textPointsCount.text = state.userPoints.toString()
                        }

                        if (state.pointsJustAdded != 0) {
                            playBounceAnimation(bind, state.userPoints, state.pointsJustAdded)
                            showFloatingPointsDelta(bind, state.pointsJustAdded)
                        }

                        if (state.pointsJustAdded > 0) {
                            checkLevelUp(state.userPoints)
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
                        bind.cardWeightLog.isVisible = state.showWeightLogCard

                        bind.cardDailyHealthTasks.isVisible = state.dailyHealthTasks.isNotEmpty()
                        bind.textHealthTasksStatus.text = state.healthTasksSummary
                    }
                }
                launch {
                    viewModel.foodJustAddedEvent.collect {
                        delay(500)

                        while (!isResumed || childFragmentManager.fragments.any { it is androidx.fragment.app.DialogFragment }) {
                            delay(500)
                        }

                        val state = viewModel.uiState.value
                        viewModel.consumeFoodAddedEvent()

                        if (DateUtils.isSameDay(state.selectedDate, Date())) {
                            // Check for 50% / 100% achievements
                            check50PercentCalorieAchievement(state)
                            if (state.isCalorieExceeded || state.isCalorieOptimal) {
                                DailyLimitFragment.newInstance(
                                    DailyLimitFragment.TYPE_CALORIES, state.lastFoodTimestamp
                                ).show(childFragmentManager, DailyLimitFragment.TAG)
                            } else {
                                // If within onboarding, show remaining calories message
                                val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                                val step = prefs.getInt("app_onboarding_step", 0)
                                if (step == 1) {
                                    DailyLimitFragment.newInstance(
                                        type = DailyLimitFragment.TYPE_REMAINING,
                                        remainingCalories = state.caloriesRemaining
                                    ).show(childFragmentManager, DailyLimitFragment.TAG)
                                    // Move to next step or keep at 1 if you want it to appear only once
                                    // Let's mark onboarding as complete (step 2) after the first food addition message
                                    viewModel.onAppOnboardingComplete()
                                }
                            }
                        }
                    }
                }
                launch {
                    viewModel.showRateAppEvent.collect {
                        delay(2000)
                        if (isResumed && childFragmentManager.fragments.isEmpty()) {
                            (activity as? uz.oybek.ozamiz.ui.MainActivity)?.requestAppReview()
                        }
                    }
                }
                launch {
                    childFragmentManager.setFragmentResultListener(
                        PremiumBottomSheetDialog.REQUEST_KEY, viewLifecycleOwner
                    ) { _, bundle ->
                        val result = bundle.getString(PremiumBottomSheetDialog.RESULT_ACTION)
                        if (result == "go_to_payment") {
                            val price = bundle.getLong("selected_price", 29000L)
                            val isYearly = bundle.getBoolean("is_yearly", false)
                            val args = bundleOf("price" to price, "is_yearly" to isYearly)
                            navigate(R.id.paymentGetCheckFragment, args)
                        }
                    }
                }
                launch {
                    viewModel.showHealthTaskOnboardingEvent.collect { taskId ->
                        SingleTaskBottomSheet.newInstance(taskId) {
                            viewModel.showNextOnboardingTask()
                        }.show(childFragmentManager, SingleTaskBottomSheet.TAG)
                        viewModel.consumeOnboardingEvent()
                    }
                }
                launch {
                    viewModel.showAppOnboardingEvent.collect {
                        AppOnboardingBottomSheet {
                            viewModel.onAppOnboardingPresented()
                            startBreakfastSpotlight()
                        }.show(childFragmentManager, AppOnboardingBottomSheet.TAG)
                    }
                }
                launch {
                    while (true) {
                        delay(30000)
                        lastState?.let { updateBurningStatus(it) }
                    }
                }
            }
        }
    }

    private fun updateUi(state: HomeUiState, animate: Boolean) {
        lastState = state
        binding.toolbarTitle.text = if (DateUtils.isSameDay(state.selectedDate, Date())) {
            getString(R.string.today)
        } else {
            DateUtils.getFormattedDateString(state.selectedDate, "d MMMM")
        }

        dateAdapter?.apply {
            submitList(state.datesForScroller)
            setSelectedDate(state.selectedDate)
            updateData(state.dailySummaries, state.caloriesTarget)
        }
        scrollToSelectedDate(state.selectedDate)

        val litersStr = String.format("%.1f", state.waterConsumed / 1000f)
        val goalLitersStr = String.format("%.1f", state.waterTarget / 1000f)
        binding.textCurrentWaterLiters.text = "$litersStr litr ichildi"
        binding.textWaterGoal.text = "Kunlik maqsad: $goalLitersStr litr"
        binding.textWaterGoal.isVisible = state.waterTarget > 0

        binding.progressWater.max = state.waterTarget
        binding.progressWater.setProgressCompat(state.waterConsumed, animate)

        val waterProgressPercent =
            if (state.waterTarget > 0) state.waterConsumed.toFloat() / state.waterTarget else 0f
        val isWater70Percent = waterProgressPercent >= 0.7f

        binding.titleWater.text =
            if (isWater70Percent) getString(R.string.water_title) + " 🥳" else getString(R.string.water_title)

        val streakInfo = state.streakInfo
        binding.streakDisplayLayout.isVisible = streakInfo != null
        if (streakInfo != null) {
            binding.textStreakCount.text = streakInfo.currentStreak.toString()
            if (binding.streakDisplayLayout.scaleX == 1f) {
                binding.streakDisplayLayout.animate().scaleX(1.1f).scaleY(1.1f).setDuration(300)
                    .withEndAction {
                        binding.streakDisplayLayout.animate().scaleX(1f).scaleY(1f).setDuration(300)
                            .start()
                    }.start()
            }
            binding.streakDisplayLayout.setOnClickListener {
                uz.oybek.ozamiz.ui.feature.streak.StreakInfoSheet.newInstance(streakInfo).show(
                    childFragmentManager, uz.oybek.ozamiz.ui.feature.streak.StreakInfoSheet.TAG
                )
            }
        }
        val colors = cachedColors

        binding.mainProgress.apply {
            textCaloriesRemainingLabel.text =
                if (state.caloriesRemaining < 0) "kkal oshdi" else "kkal qoldi"
            textCaloriesRemainingCount.text = state.caloriesRemaining.absoluteValue.toString()
            textEatenCount.text =
                "${state.caloriesConsumed} yeyildi   ${state.caloriesTarget} maqsad"
            progressCaloriesCircular.max = state.caloriesTarget
            progressCaloriesCircular.setProgressCompat(
                state.caloriesConsumed.absoluteValue.coerceAtMost(state.caloriesTarget), animate
            )

            if (state.caloriesConsumed >= state.caloriesTarget - 50) {
                layoutCalorieText.isVisible = false
                layoutLimitInfo.isVisible = true
                btnDetailedInfo.isVisible = true
                updateBurningStatus(state)
                binding.layoutMacroWarnings.isVisible = false
                macrosContainer.isVisible = true
            } else {
                layoutCalorieText.isVisible = true
                layoutLimitInfo.isVisible = false
                btnDetailedInfo.isVisible = false
                binding.layoutMacroWarnings.isVisible = false
                macrosContainer.isVisible = true
            }

            progressCaloriesCircular.setIndicatorColor(colors.primary)
            progressCaloriesCircular.trackColor = colors.track
            textCaloriesRemainingCount.setTextColor(colors.onSurface)

            fun bindMacro(
                progress: com.google.android.material.progressindicator.LinearProgressIndicator,
                text: android.widget.TextView,
                consumed: Int,
                target: Int,
                isExceeded: Boolean,
                normalColor: Int
            ) {
                val isOptimal = consumed >= target && !isExceeded
                val remaining = target - consumed
                progress.max = if (isExceeded) consumed else target
                progress.setProgressCompat(consumed, animate)
                progress.setIndicatorColor(if (isExceeded) colors.gray else if (isOptimal) colors.green else normalColor)
                progress.trackColor = colors.track
                text.setTextColor(if (isExceeded) colors.gray else if (isOptimal) colors.green else colors.onSurface)
                text.text = when {
                    isExceeded -> getString(
                        R.string.macro_exceeded_format, (consumed - target).absoluteValue
                    )

                    isOptimal -> "To'ldi"
                    else -> getString(R.string.macro_remaining_format, remaining)
                }
            }

            bindMacro(
                progressProtein,
                textProteinValue,
                state.proteinConsumed,
                state.proteinTarget,
                state.isProteinExceeded,
                colors.orange
            )
            bindMacro(
                progressFat,
                textFatValue,
                state.fatConsumed,
                state.fatTarget,
                state.isFatExceeded,
                colors.yellow
            )
            bindMacro(
                progressCarbs,
                textCarbsValue,
                state.carbsConsumed,
                state.carbsTarget,
                state.isCarbsExceeded,
                colors.green
            )
        }

        val mealCalories = state.dailyFoodLogs.groupBy { it.mealType }
            .mapValues { it.value.sumOf { f -> f.calories } }
        val latestFoodByMeal = state.dailyFoodLogs.groupBy { it.mealType }
            .mapValues { it.value.maxByOrNull { f -> (f.timestamp as? Number)?.toLong() ?: 0L } }
        val target = state.caloriesTarget.toFloat()
        val meals = listOf(
            Triple(binding.cardBreakfast, MealTime.Breakfast.id, (target * 0.3f).toInt()),
            Triple(binding.cardLunch, MealTime.Lunch.id, (target * 0.4f).toInt()),
            Triple(binding.cardDinner, MealTime.Dinner.id, (target * 0.2f).toInt()),
            Triple(binding.cardSnack, MealTime.Snack.id, (target * 0.1f).toInt())
        )

        meals.forEach { (card, id, baseMax) ->
            val consumed = mealCalories[id] ?: 0
            val max = if (baseMax > 0) baseMax else 400
            val latestFood = latestFoodByMeal[id]
            card.progressMeal.max = max
            card.progressMeal.setProgressCompat(consumed.coerceAtMost(max), animate)

            if (consumed >= max || state.caloriesConsumed >= state.caloriesTarget) {
                card.layoutCalories.isVisible = false
                card.textMealLastFood.isVisible = latestFood != null
                card.textMealLastFood.text = latestFood?.description
            } else {
                card.layoutCalories.isVisible = true
                card.textMealLastFood.isVisible = false
                card.textMealCalories.text = "${max - consumed} kkal qoldi"
                card.textMealCalories.setTextColor(colors.onSurfaceVariant)
            }
        }
        binding.progressExercise.max = state.exerciseTarget
        binding.progressExercise.setProgressCompat(state.exerciseBurnt, animate)
        binding.textExerciseProgress.text = "${state.exerciseBurnt} / ${state.exerciseTarget} kkal"
        val isExercise70 =
            (if (state.exerciseTarget > 0) state.exerciseBurnt.toFloat() / state.exerciseTarget else 0f) >= 0.7f
        binding.titleExercise.text = if (isExercise70) "Yog' yoqish 🔥" else "Yog' yoqish"
        binding.btnTodayExercises.isVisible = state.exerciseLogs.isNotEmpty()
        binding.textWaterStatus.text = state.waterStatusMessage
        binding.textExerciseStatus.text = state.exerciseStatusMessage
        binding.warningProtein.root.isVisible = false
        binding.warningCarbs.root.isVisible = false
        binding.warningFat.root.isVisible = false
    }

    private fun updateBurningStatus(state: HomeUiState) {
        if (state.caloriesConsumed >= state.caloriesTarget - 50) {
            binding.mainProgress.apply {
                val timeSinceLastFood =
                    state.lastFoodTimestamp?.let { System.currentTimeMillis() - it }
                        ?: Long.MAX_VALUE
                val twoHoursInMs = 2 * 60 * 60 * 1000L
                val isTwoHoursPassed = timeSinceLastFood >= twoHoursInMs

                if (state.caloriesConsumed <= state.caloriesTarget + 100 && isTwoHoursPassed) {
                    textLimitTitle.text = "🔥 Yog' yoqilyapti"
                    textLimitSubtitle.text =
                        "Boshqa ovqat yemay turing, ortiqchasi yog' yoqishni to'xtatadi"
                } else {
                    textLimitTitle.text = "Limitga yetdingiz"
                    textLimitSubtitle.text =
                        "Boshqa ovqat yemay turing, ortiqchasi yog' yoqishni to'xtatadi"
                }
            }
        }
    }

    private fun scrollToSelectedDate(selectedDate: Date) {
        binding.dateScrollerRecyclerView.post {
            val selectedIndex = viewModel.uiState.value.datesForScroller.indexOfFirst {
                DateUtils.isSameDay(it, selectedDate)
            }
            if (selectedIndex != -1) {
                (binding.dateScrollerRecyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                    selectedIndex, (binding.root.width / 2) - 32f.dpToPx()
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

        animationJob = viewLifecycleOwner.lifecycleScope.launch {
            val stepsCount = pointsAdded.absoluteValue.coerceAtMost(10)
            val stepValue = pointsAdded / stepsCount
            val stepDelay =
                if (pointsAdded.absoluteValue <= 5) 150L else if (pointsAdded.absoluteValue <= 10) 100L else 70L

            for (i in 1..stepsCount) {
                val currentVal = if (i == stepsCount) finalPoints else startPoints + (i * stepValue)
                bind.textPointsCount.text = currentVal.toString()
                bind.textPointsCount.apply {
                    translationY = 15f
                    scaleX = 0.9f
                    scaleY = 0.9f
                    alpha = 0.8f

                    animate().translationY(0f).scaleX(1.1f).scaleY(1.1f).alpha(1f)
                        .setDuration(stepDelay / 2)
                        .setInterpolator(android.view.animation.OvershootInterpolator())
                        .withEndAction {
                            animate().scaleX(1.0f).scaleY(1.0f).setDuration(stepDelay / 2).start()
                        }.start()
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
                    requireContext(), if (pointsAdded > 0) R.color.green else R.color.red
                )
            )
            alpha = 1f
            isVisible = true

            animate().alpha(0f).setDuration(1200).setStartDelay(500).withEndAction {
                isVisible = false
            }.start()
        }
    }

    private fun startBreakfastSpotlight() {
        if (view == null) return
        val lifecycleOwner = try {
            viewLifecycleOwner
        } catch (e: IllegalStateException) {
            null
        } ?: return

        lifecycleOwner.lifecycleScope.launch {
            if (bindingT == null) return@launch
            val btn = binding.cardBreakfast.btnAddMeal

            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                com.google.android.material.color.MaterialColors.getColor(
                    btn.context,
                    androidx.appcompat.R.attr.colorPrimary,
                    Color.BLUE
                )
            )
            btn.imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)

            // Faster bounce
            btn.animate().scaleX(0.75f).scaleY(0.75f).setDuration(250).withEndAction {
                if (bindingT == null) return@withEndAction
                btn.animate().scaleX(1.1f).scaleY(1.1f).setDuration(250).withEndAction {
                    if (bindingT == null) return@withEndAction
                    btn.animate().scaleX(1f).scaleY(1f).setDuration(250).withEndAction {
                        lifecycleOwner.lifecycleScope.launch {
                            delay(400)
                            startBreakfastSpotlight()
                        }
                    }.start()
                }.start()
            }.start()
        }
    }

    private fun Float.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()


    private fun checkWaterGoalReached(state: HomeUiState) {
        val todayStr = Date().dateApiFormat()
        if (state.waterTarget > 0 && state.waterConsumed >= state.waterTarget * 0.7 && getPersistentDate(
                "last_water_goal_shown"
            ) != todayStr
        ) {
            showWaterGoalReachedAchievement()
            savePersistentDate("last_water_goal_shown", todayStr)
        }
    }

    private fun showWaterGoalReachedAchievement() {
        if (activity == null || isDetached) return
        val dialog =
            MaterialAlertDialogBuilder(requireContext()).setView(R.layout.dialog_water_achievement)
                .create()

        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val variations = listOf(
            "Ajoyib natija!" to "Bugungi suv maqsadingizga juda qattiq yaqinlashdingiz. Davom eting! 💧",
            "Barakalla!" to "Tana namligi a'lo darajada. Bugungi reja deyarli bajarildi! 🌊",
            "Salomatlik sari!" to "Suv ichishda faol ekansiz, katta marra zabt etildi. 🥤",
            "Suv qiroli/malikasi!" to "Bugun deyarli hamma rejalashtirilgan suvni ichdingiz. 🤴👸",
            "Namlik darajasi yuqori!" to "Siz deyarli marra desiz, suvning katta qismi ichildi! 💎",
            "Muvaffaqiyat!" to "Bugun o'zingizga bo'lgan g'amxo'rligiz yangi marraga yetdi. ✨",
            "Toza energiya!" to "Suv sizga kuch bermoqda, katta reja ortda qoldi. ⚡",
            "Siz uddaladingiz!" to "Kunlik suv me'yorining katta qismi ichildi. Jismingiz quvonmoqda! 😊",
            "Super intizom!" to "Suv tartibingiz hayratlanarli! Reja deyarli bajarildi. 👏",
            "Yashang!" to "Har bir qultum salomatlik! Katta marra bosib o'tildi. 🌈",
            "Tetiklik ramzi!" to "Bugungi suv ichish faolligiz yuqori darajaga yetdi. 🚀",
            "A'lo kayfiyat!" to "Suv sizga tetiklik bermoqda, reja tayyor! 🍀",
            "Namlik kuchi!" to "Tana hujayralari bugun suvga to'yingan. Davom eting! 🧊",
            "Maqsad yaqin!" to "Suv maqsadingiz sari katta masofani bosib o'tdingiz. 🎯",
            "Siz haqiqiy namunasiz!" to "Suv ichish tartibiz a'lo darajada davom etmoqda. 🔥",
            "Zabarast natija!" to "Bugun suv ichishda shaxsiy rekord sari yaqin keldingiz. 🥇",
            "Hujayralar bayrami!" to "Tanangiz namlikka boy bo'lmoqda. 🎉",
            "Suv bilan tetiklashish!" to "Bugun deyarli reja bajardi, katta qism ortda qoldi. 🎐",
            "Ideal muvozanat!" to "Suv balansingiz bugun sezilarli tiklandi. 🌀",
            "Ofarin!" to "Bugun suv ichish rejasini deyarli yakunladingiz. 🎖️"
        )
        val selected = variations.random()

        dialog.findViewById<TextView>(R.id.text_achievement_title)?.text = selected.first
        dialog.findViewById<TextView>(R.id.text_achievement_subtitle)?.text = selected.second

        val btnAjoyib = dialog.findViewById<View>(R.id.btn_ajoyib)
        btnAjoyib?.setOnClickListener {
            dialog.dismiss()
        }
    }


    private fun check70PercentExercise(state: HomeUiState) {
        val todayStr = Date().dateApiFormat()
        if (state.exerciseTarget > 0 && state.exerciseBurnt >= state.exerciseTarget * 0.7 && getPersistentDate(
                "last_exercise_70_shown"
            ) != todayStr
        ) {
            show70PercentExerciseAchievement()
            savePersistentDate("last_exercise_70_shown", todayStr)
        }
    }

    private fun show70PercentExerciseAchievement() {
        if (activity == null || isDetached) return
        val dialog =
            MaterialAlertDialogBuilder(requireContext()).setView(R.layout.dialog_exercise_achievement)
                .create()

        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val variations = listOf(
            Triple(
                "🥇",
                "Mashg'ulot qahramoni!",
                "Bugungi mashqlar rejasining katta qismidan o'tdingiz!"
            ),
            Triple(
                "🔥",
                "Yog'lar \"yig'layapti\"!",
                "Rejalashtirilgan kaloriyalarning ko'p qismi yoqildi. Davom eting!"
            ),
            Triple("⚡", "Super energiya!", "Siz bugun juda faolsiz, marra zabt etildi!"),
            Triple("🏆", "Chempionlik sari!", "Mashqlarga bo'lgan bu g'ayrat marraga yetdi!"),
            Triple("💪", "Kuch va qudrat!", "Tanangiz chiniqmoqda!"),
            Triple("🏃", "Tezkor natija!", "Faolligingiz bugun ancha oshdi. Barakalla!"),
            Triple("✨", "Yashang!", "Siz bugun o'zingizni yengdingiz!"),
            Triple("🚀", "Kosmik tezlik!", "Mashqlar tartibingiz hayratda qoldirmoqda!"),
            Triple("🎯", "Aniq nishon!", "Bugungi faollik rejasining katta qismi tayyor!"),
            Triple("🌟", "Yulduzli natija!", "Bugun deyarli hamma mashqlarni uddaladingiz!"),
            Triple(
                "🌈", "Siz ajoyibsiz!", "Har bir harakat sizni go'zallikka yaqinlashtiradi!"
            ),
            Triple("💎", "Qimmatbaho sog'lik!", "Bugunga mo'ljallangan ko'plab kaloriya sarflandi!"),
            Triple("🧨", "Portlovchi kuch!", "Siz bugun haqiqiy vulqonsiz! "),
            Triple("🧗", "Cho'qqi sari!", "Faollik masofasining katta qismi bosib o'tildi!"),
            Triple("🥊", "Zarbdor natija!", "Siz har qanday dangasalikni yengdingiz!"),
            Triple("🧘", "Garmoniya va kuch!", "Tana va ruh birdamligi!"),
            Triple("🚲", "Tezkor harakat!", "Kaloriya yoqishda bugun to'xtamadingiz!"),
            Triple("🤸", "Elastiklik mukammal!", "Bugungi mashqlar natijasi yuqori darajaga yetdi!"),
            Triple(
                "🛹", "Yangi marra!", "Siz bugun kaloriya sarflab shaxsiy rekordga yaqinlashdingiz!"
            ),
            Triple(
                "🎖️", "Xizmatlaringiz beqiyos!", "Bugun faollik bilan o'zingizni xursand qildingiz!"
            )
        )
        val selected = variations.random()

        dialog.findViewById<TextView>(R.id.text_achievement_emoji)?.text = selected.first
        dialog.findViewById<TextView>(R.id.text_achievement_title)?.text = selected.second
        dialog.findViewById<TextView>(R.id.text_achievement_subtitle)?.text = selected.third

        val btnAjoyib = dialog.findViewById<View>(R.id.btn_ajoyib)
        btnAjoyib?.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun check50PercentCalorieAchievement(state: HomeUiState) {
        val todayStr = Date().dateApiFormat()

        if (state.caloriesTarget > 0 && state.caloriesConsumed >= state.caloriesTarget * 0.5 && getPersistentDate(
                "last_calorie_50_shown"
            ) != todayStr
        ) {
            show50PercentCalorieAchievement()
            savePersistentDate("last_calorie_50_shown", todayStr)
        }
    }


    private fun show50PercentCalorieAchievement() {
        if (activity == null || isDetached) return
        val dialog =
            MaterialAlertDialogBuilder(requireContext()).setView(R.layout.dialog_food_achievement)
                .create()

        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val variations = listOf(
            "Ajoyib ketyapsiz!" to "Shu tarzda davom eting, natija uzoq kuttirmaydi!",
            "Zo'r, to'xtamang!" to "Sizdagi bu shijoat ko'pchilikni hayratda qoldiradi.",
            "Ofarin, davom eting!" to "Maqsad sari tashlangan har bir qadam muhim.",
            "Hammasi a'lo!" to "Siz bugun juda tartiblisiz, barakalla!",
            "Barakalla, olg'a!" to "O'zingizni asrashda chiroyli natija ko'rsatyapsiz.",
            "Shijoatni so'ndirmang!" to "Intizomingiz sizni go'zallik sari yetaklaydi.",
            "Siz haqiqiy namunali odamsiz!" to "Sog'lom hayot sari yo'lda sizga tengi yo'q.",
            "A'lo darajada!" to "Bugun ovqatlanish tartibingiz shunchaki super!",
            "Davom eting!" to "Har bir to'g'ri tanlov - bu sizning g'alabangiz.",
            "Zo'r natija!" to "Taslim bo'lmasligingiz bizni quvontirmoqda.",
            "Intizomingizga qoyil!" to "Vaziyatni to'liq nazorat qilishingiz tahsinga loyiq.",
            "Ajoyib, taslim bo'lmang!" to "Siz bugun o'z maqsadingizga juda yaqinsiz.",
            "Ofarin, super!" to "Sizni bunday g'ayrat bilan har qanday marra nishon!",
            "Yashang, shunday davom eting!" to "Tanangiz sizdan chin dildan minnatdor.",
            "Zo'r, shijoat bu - kuch!" to "Sizdagi bu kuchli iroda har qanday to'siqni yengadi.",
            "Ajoyib intizom!" to "Tartibli bo'lish sizga juda yarashyapti.",
            "Omad, to'xtab qolmang!" to "G'alaba sari yo'lingiz nurli bo'lsin.",
            "Ofarin!" to "Charchamang, go'zal qomat sizni kutmoqda.",
            "Zo'r ketyapsiz!" to "Bugun siz haqiqatan ham ajoyib ketyapsiz.",
            "Ajoyib, olg'a!" to "Bugungi har bir harakatingiz - ertangi sog'ligingiz!"
        )
        val selected = variations.random()

        dialog.findViewById<TextView>(R.id.text_achievement_title)?.text = selected.first
        dialog.findViewById<TextView>(R.id.text_achievement_subtitle)?.text = selected.second

        val btnAjoyib = dialog.findViewById<View>(R.id.btn_ajoyib)
        btnAjoyib?.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun checkLevelUp(points: Int) {
        val currentIndex = LevelManager.getCurrentLevelIndex(points)
        if (lastShownLevelIndex == -1) {
            lastShownLevelIndex = getLastSeenLevelIndex()
            if (lastShownLevelIndex == 0 && currentIndex > 0) {
                lastShownLevelIndex = currentIndex
                saveLastSeenLevelIndex(currentIndex)
            }
        }

        if (currentIndex > lastShownLevelIndex) {
            lastShownLevelIndex = currentIndex
            saveLastSeenLevelIndex(currentIndex)
            val newLevel = LevelManager.levels[currentIndex]
            showLevelUpDialog(
                newLevel.emoji, newLevel.name, newLevel.motivationText, newLevel.color
            )
        }
    }

    private fun showLevelUpDialog(emoji: String, name: String, motivation: String, color: Long) {
        if (activity == null || isDetached) return

        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_level_up, null)

        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(dialogView).create()

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

    private fun vibrateDevice() {
        try {
            val vibrator =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val vibratorManager =
                        requireContext().getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION") requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                }

            if (vibrator?.hasVibrator() == true) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        android.os.VibrationEffect.createOneShot(
                            50, android.os.VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION") vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
