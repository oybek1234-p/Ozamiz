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
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import java.util.Calendar
import java.util.Date
import kotlin.math.absoluteValue

class LogFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by activityViewModels()
    private var dateAdapter: DateScrollerAdapter? = null

    private var lastShownLevelIndex: Int = -1

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
        setupFragmentResults()
        checkNotificationPermission()
    }

    private fun setupFragmentResults() {
        childFragmentManager.setFragmentResultListener(
            "water_added_request",
            viewLifecycleOwner
        ) { _, bundle ->
            val added = bundle.getBoolean("added", false)
            if (added) {
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
                    showWaterMotivationSnackbar()
                    lastState?.let {
                        checkWaterGoalReached(it)
                    }
                }
            }
        }

        parentFragmentManager.setFragmentResultListener(
            "exercise_added_request",
            viewLifecycleOwner
        ) { _, bundle ->
            val exerciseId = bundle.getString("exerciseId")
            if (exerciseId != null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
                    showExerciseMotivationSnackbar(exerciseId)
                    lastState?.let {
                        check70PercentExercise(it)
                    }
                }
            }
        }
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
        bind.btnTodayExercises.setOnSafeClickListener {
            lastState?.let { state ->
                TodayExercisesBottomSheet.newInstance(state.exerciseLogs)
                    .show(childFragmentManager, TodayExercisesBottomSheet.TAG)
            }
        }


        bind.btnQuickWalk.setOnSafeClickListener {
            val args = Bundle().apply {
                putString("exerciseId", "PiyodaYurish")
            }
            navigate(R.id.addExerciseLogFragment, args)
        }

        bind.btnQuickRun.setOnSafeClickListener {
            val args = Bundle().apply {
                putString("exerciseId", "Yugurish")
            }
            navigate(R.id.addExerciseLogFragment, args)
        }

        bind.btnQuickOther.setOnSafeClickListener {
            navigate(R.id.selectExerciseFragment)
        }
        fun onMealClick(meal: String) {
            LogFoodFragment.navigate(
                navController = findNavController(),
                timeStamp = currentDate(),
                meal = meal
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
            navigate(R.id.aiChatFragment)
        }
        bind.mainProgress.btnHowItWorks.setOnSafeClickListener {
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

        bind.btnOpenWaterDialog.setOnSafeClickListener {
            uz.oybek.ozamiz.ui.feature.log.water.LogWaterDialog()
                .show(childFragmentManager, "LogWaterDialog")
        }

        bind.coinDisplayLayout.setOnSafeClickListener {
            navigate(R.id.action_home_to_levelFragment)
        }

        bind.mainProgress.btnDetailedInfo.setOnSafeClickListener {
            DailyLimitFragment.newInstance(DailyLimitFragment.TYPE_CALORIES)
                .show(childFragmentManager, DailyLimitFragment.TAG)
        }

        viewModel.onHomeEntered()
        setupDailyTip(bind)

        bind.btnAddWeightHome.setOnSafeClickListener {
            navigate(R.id.photoUploadInfoFragment)
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

                        bind.cardMotivation.isVisible = false

                        bind.cardWeightLog.isVisible = state.showWeightLogCard
                    }
                }

                launch {
                    viewModel.foodJustAddedEvent.collect {
                        val state = viewModel.uiState.value
                        viewModel.consumeFoodAddedEvent()
                        if (DateUtils.isSameDay(state.selectedDate, Date())) {
                            check50PercentCalorieAchievement(state)
                            if (state.isCalorieExceeded || state.isCalorieOptimal) {
                                DailyLimitFragment.newInstance(DailyLimitFragment.TYPE_CALORIES)
                                    .show(childFragmentManager, DailyLimitFragment.TAG)
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

        val streakInfo = state.streakInfo
        binding.streakDisplayLayout.isVisible = streakInfo != null
        if (streakInfo != null) {
            binding.textStreakCount.text = streakInfo.currentStreak.toString()
            if (binding.streakDisplayLayout.scaleX == 1f) {
                binding.streakDisplayLayout.animate().scaleX(1.1f).scaleY(1.1f).setDuration(300)
                    .withEndAction {
                        binding.streakDisplayLayout.animate().scaleX(1f).scaleY(1f).setDuration(300)
                            .start()
                    }
                    .start()
            }
            binding.streakDisplayLayout.setOnClickListener {
                uz.oybek.ozamiz.ui.feature.streak.StreakInfoSheet.newInstance(streakInfo)
                    .show(
                        childFragmentManager,
                        uz.oybek.ozamiz.ui.feature.streak.StreakInfoSheet.TAG
                    )
            }
        }

        // Cache colors to avoid redundant lookups
        val colors = object {
            val primary = com.google.android.material.color.MaterialColors.getColor(
                requireView(),
                androidx.appcompat.R.attr.colorPrimary
            )
            val red = ContextCompat.getColor(requireContext(), R.color.red)
            val gray = ContextCompat.getColor(requireContext(), R.color.grey)
            val orange = ContextCompat.getColor(requireContext(), R.color.orange)
            val yellow = com.google.android.material.color.MaterialColors.getColor(
                requireView(),
                R.attr.customYellow
            )
            val green = com.google.android.material.color.MaterialColors.getColor(
                requireView(),
                R.attr.customGreen
            )
            val track = com.google.android.material.color.MaterialColors.getColor(
                requireView(),
                com.google.android.material.R.attr.colorSurfaceContainerHigh
            )
            val onSurface = com.google.android.material.color.MaterialColors.getColor(
                requireView(),
                com.google.android.material.R.attr.colorOnSurface
            )
            val onSurfaceVariant = com.google.android.material.color.MaterialColors.getColor(
                requireView(),
                com.google.android.material.R.attr.colorOnSurfaceVariant
            )
        }

        binding.mainProgress.apply {
            textCaloriesRemainingCount.text = state.caloriesRemaining.absoluteValue.toString()
            textEatenCount.text =
                "${state.caloriesConsumed}    Maqsad: ${state.caloriesTarget} oshirmaslik"
            progressCaloriesCircular.max = state.caloriesTarget
            progressCaloriesCircular.setProgressCompat(
                state.caloriesConsumed.absoluteValue,
                animate
            )

            if (state.caloriesConsumed >= state.caloriesTarget) {
                layoutNormalProgress.isVisible = false
                layoutLimitState.isVisible = true
                cardLimitIcon.isVisible = false
                textLimitTitle.text = "Yog' yoqilyapti"
                textLimitSubtitle.text = "Ortiqcha ovqat yog' yoqishni to'xtatadi."
                binding.layoutMacroWarnings.isVisible = false
                macrosContainer.isVisible = false
            } else {
                layoutNormalProgress.isVisible = true
                layoutLimitState.isVisible = false
                binding.layoutMacroWarnings.isVisible = false
                macrosContainer.isVisible = true
            }
            progressCaloriesCircular.setIndicatorColor(colors.primary)
            progressCaloriesCircular.trackColor = colors.track
            textCaloriesRemainingCount.setTextColor(colors.onSurface)

            // Macros Helper
            fun bindMacro(
                progress: com.google.android.material.progressindicator.LinearProgressIndicator,
                text: android.widget.TextView,
                consumed: Int,
                target: Int,
                isExceeded: Boolean,
                exceededColor: Int,
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
                    isExceeded -> getString(R.string.macro_exceeded_format, consumed - target)
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
                colors.gray,
                colors.orange
            )
            bindMacro(
                progressFat,
                textFatValue,
                state.fatConsumed,
                state.fatTarget,
                state.isFatExceeded,
                colors.gray,
                colors.yellow
            )
            bindMacro(
                progressCarbs,
                textCarbsValue,
                state.carbsConsumed,
                state.carbsTarget,
                state.isCarbsExceeded,
                colors.gray,
                colors.green
            )
        }

        // Meal Cards
        val mealCalories = state.dailyFoodLogs.groupBy { it.mealType }
            .mapValues { entry -> entry.value.sumOf { it.calories } }

        val latestFoodByMeal = state.dailyFoodLogs.groupBy { it.mealType }
            .mapValues { entry ->
                entry.value.maxByOrNull { (it.timestamp as? Number)?.toLong() ?: 0L }
            }

        val target = state.caloriesTarget.toFloat()
        val meals = listOf(
            Triple(binding.cardBreakfast, MealTime.Breakfast.id, (target * 0.3f).toInt()),
            Triple(binding.cardLunch, MealTime.Lunch.id, (target * 0.4f).toInt()),
            Triple(binding.cardDinner, MealTime.Dinner.id, (target * 0.2f).toInt()),
            Triple(binding.cardSnack, MealTime.Snack.id, (target * 0.1f).toInt())
        )

        meals.forEach { (card, id, baseMax) ->
            val consumed = mealCalories[id] ?: 0
            val max = baseMax.takeIf { it > 0 } ?: 400
            val latestFood = latestFoodByMeal[id]

            if (consumed >= max || state.caloriesConsumed >= state.caloriesTarget) {
                card.layoutCalories.isVisible = false
                card.textMealLastFood.isVisible = latestFood != null
                card.textMealLastFood.text = latestFood?.description
            } else {
                card.layoutCalories.isVisible = true
                card.textMealLastFood.isVisible = false
                card.textMealCalories.text = "${max - consumed} kkal qoldi"
                card.textMealCalories.setTextColor(colors.onSurfaceVariant)
                card.progressMeal.max = max
                card.progressMeal.setProgressCompat(consumed, animate)
            }
        }

        binding.progressExercise.max = state.exerciseTarget
        binding.progressExercise.setProgressCompat(state.exerciseBurnt, animate)
        binding.textExerciseProgress.text = "${state.exerciseBurnt} / ${state.exerciseTarget} kkal"

        binding.btnTodayExercises.isVisible = state.exerciseLogs.isNotEmpty()

        binding.warningProtein.root.isVisible = false
        binding.warningCarbs.root.isVisible = false
        binding.warningFat.root.isVisible = false
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
        snackbarLayout.setPadding(0, 0, 0, 0)

        snackbarLayout.clipChildren = false
        snackbarLayout.clipToPadding = false
        (snackbarLayout.parent as? ViewGroup)?.let {
            it.clipChildren = false
            it.clipToPadding = false
        }

        // Anchor to bottom nav to avoid being hidden behind it
        val bottomNav = activity?.findViewById<View>(R.id.bottom_nav_view)
        if (bottomNav != null) {
            snackbar.anchorView = bottomNav
        }

        val customView = layoutInflater.inflate(R.layout.custom_water_snackbar, null)
        val textView = customView.findViewById<TextView>(R.id.text_snackbar_motivation)
        textView.text = message

        val btnClose = customView.findViewById<View>(R.id.btn_close_snackbar)
        btnClose.setOnClickListener {
            snackbar.dismiss()
        }

        snackbarLayout.removeAllViews()
        snackbarLayout.addView(customView)
        snackbar.show()
    }

    private fun checkWaterGoalReached(state: HomeUiState) {
        val todayStr = Date().dateApiFormat()
        if (state.waterTarget > 0 &&
            state.waterConsumed >= state.waterTarget * 0.7 &&
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

        val variations = listOf(
            "Ajoyib natija!" to "Bugungi suv maqsadingizning 70% idan o'tdingiz. Davom eting! 💧",
            "Barakalla!" to "Tana namligi a'lo darajada. 70% reja bajarildi! 🌊",
            "Salomatlik sari!" to "Suv ichishda faol ekansiz, 70% marra zabt etildi. 🥤",
            "Suv qiroli/malikasi!" to "Bugun deyarli hamma rejalashtirilgan suvni ichdingiz. 🤴👸",
            "Namlik darajasi yuqori!" to "Siz deyarli marra desiz, 70% suv ichildi! 💎",
            "Muvaffaqiyat!" to "Bugun o'zingizga bo'lgan g'amxo'rligiz 70% marraga yetdi. ✨",
            "Toza energiya!" to "Suv sizga kuch bermoqda, 70% reja ortda qoldi. ⚡",
            "Siz uddaladingiz!" to "Kunlik suv me'yorining 70% i ichildi. Jismingiz quvonmoqda! 😊",
            "Super intizom!" to "Suv tartibingiz hayratlanarli! 70% bajarildi. 👏",
            "Yashang!" to "Har bir qultum salomatlik! 70% marra bosib o'tildi. 🌈",
            "Tetiklik ramzi!" to "Bugungi suv ichish faolligiz 70% ga yetdi. 🚀",
            "A'lo kayfiyat!" to "Suv sizga tetiklik bermoqda, 70% reja tayyor! 🍀",
            "Namlik kuchi!" to "Tana hujayralari bugun 70% ga to'yingan. Davom eting! 🧊",
            "Maqsad yaqin!" to "Suv maqsadingiz sari 70% masofani bosib o'tdingiz. 🎯",
            "Siz haqiqiy namunasiz!" to "Suv ichish tartibiz 70% reja bilan davom etmoqda. 🔥",
            "Zabarast natija!" to "Bugun suv ichishda shaxsiy rekord sari 70% ga keldingiz. 🥇",
            "Hujayralar bayrami!" to "Tanangiz namlikka boy bo'lmoqda (70% reja). 🎉",
            "Suv bilan tetiklashish!" to "Bugun deyarli reja bajardi, 70% ortda qoldi. 🎐",
            "Ideal muvozanat!" to "Suv balansingiz bugun 70% ga tiklandi. 🌀",
            "Ofarin!" to "Bugun suv ichish rejasining 70% ini yakunladingiz. 🎖️"
        )
        val selected = variations.random()

        dialog.findViewById<TextView>(R.id.text_achievement_title)?.text = selected.first
        dialog.findViewById<TextView>(R.id.text_achievement_subtitle)?.text = selected.second

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
            "$type uyqu sifatini ajoyib darajada yaxshilaydi va asablarni tinchlantirishga yordam beradi.",
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
        snackbarLayout.setPadding(0, 0, 0, 0)

        snackbarLayout.clipChildren = false
        snackbarLayout.clipToPadding = false
        (snackbarLayout.parent as? ViewGroup)?.let {
            it.clipChildren = false
            it.clipToPadding = false
        }
        val bottomNav = activity?.findViewById<View>(R.id.bottom_nav_view)
        if (bottomNav != null) {
            snackbar.anchorView = bottomNav
        }
        val customView = layoutInflater.inflate(R.layout.custom_exercise_snackbar, null)
        val textView = customView.findViewById<TextView>(R.id.text_snackbar_motivation)
        textView.text = message
        val btnClose = customView.findViewById<View>(R.id.btn_close_snackbar)
        btnClose.setOnClickListener {
            snackbar.dismiss()
        }
        snackbarLayout.removeAllViews()
        snackbarLayout.addView(customView)
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

        val variations = listOf(
            Triple("🥇", "Mashg'ulot qahramoni!", "Bugungi mashqlar rejasining 70% idan o'tdingiz!"),
            Triple(
                "🔥",
                "Yog'lar \"yig'layapti\"!",
                "70% rejalashtirilgan kaloriya yoqildi. Davom eting!"
            ),
            Triple("⚡", "Super energiya!", "Siz bugun juda faolsiz, marra zabt etildi!"),
            Triple("🏆", "Chempionlik sari!", "Mashqlarga bo'lgan bu g'ayrat marraga yetdi!"),
            Triple("💪", "Kuch va qudrat!", "Tanangiz chiniqmoqda!"),
            Triple("🏃", "Tezkor natija!", "Faolligingiz bugun dan oshdi. Barakalla!"),
            Triple("✨", "Yashang!", "Siz bugun o'zingizni yengdingiz,!"),
            Triple("🚀", "Kosmik tezlik!", "Mashqlar tartibingiz hayratda qoldirmoqda!"),
            Triple("🎯", "Aniq nishon!", "Bugungi faollik rejasining 70% i tayyor!"),
            Triple("🌟", "Yulduzli natija!", "Bugun deyarli hamma mashqlarni uddaladingiz!"),
            Triple(
                "🌈",
                "Siz ajoyibsiz!",
                "Har bir harakat sizni go'zallikka yaqinlashtiradi!"
            ),
            Triple("💎", "Qimmatbaho sog'lik!", "Bugunga mo'ljallangan 70% kaloriya sarflandi!"),
            Triple("🧨", "Portlovchi kuch!", "Siz bugun haqiqiy vulqonsiz! "),
            Triple("🧗", "Cho'qqi sari!", "Faollik masofasining 70% i bosib o'tildi!"),
            Triple("🥊", "Zarbdor natija!", "Siz har qanday dangasalikni yengdingiz!"),
            Triple("🧘", "Garmoniya va kuch!", "Tana va ruh birdamligi!"),
            Triple("🚲", "Tezkor harakat!", "Kaloriya yoqishda bugun to'xtamadingiz!"),
            Triple("🤸", "Elastiklik mukammal!", "Bugungi mashqlar natijasi 70% ga yetdi!"),
            Triple(
                "🛹",
                "Yangi marra!",
                "Siz bugun kaloriya sarflab shaxsiy rekordga yaqinlashdingiz!"
            ),
            Triple(
                "🎖️",
                "Xizmatlaringiz beqiyos!",
                "Bugun faollik bilan o'zingizni xursand qildingiz!"
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

        if (state.caloriesTarget > 0 &&
            state.caloriesConsumed >= state.caloriesTarget * 0.5 &&
            getPersistentDate("last_calorie_50_shown") != todayStr
        ) {
            show50PercentCalorieAchievement()
            savePersistentDate("last_calorie_50_shown", todayStr)
        }
    }


    private fun show50PercentCalorieAchievement() {
        if (activity == null || isDetached) return
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(R.layout.dialog_food_achievement)
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


}