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
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.config.LocalPreferencesRepository
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.utils.dateApiFormat
import uz.oybek.ozamiz.databinding.FragmentHomeBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.log.food.DailyLimitFragment
import uz.oybek.ozamiz.ui.feature.log.food.LogFoodFragment
import uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import java.util.Calendar
import java.util.Date
import kotlin.math.absoluteValue

class LogFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by activityViewModels()
    private var dateAdapter: DateScrollerAdapter? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
    }

    private var tourOverlay: uz.oybek.ozamiz.ui.utils.GuidedTourOverlay? = null

    private val cachedColors by lazy {
        object {
            val primary = com.google.android.material.color.MaterialColors.getColor(
                requireContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLUE
            )
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
        tourOverlay?.removeImmediately()
        tourOverlay = null
        super.onDestroyView()
        dateAdapter = null
    }

    private fun getPersistentDate(key: String): String? {
        return viewModel.getPersistentDate(key)
    }

    private fun savePersistentDate(key: String, date: String) {
        viewModel.savePersistentDate(key, date)
    }


    init {
        showBottomSheet = true
    }

    override fun viewCreated(bind: FragmentHomeBinding) {
        setFragmentResultListener("dismiss_ai_meal") { _, _ ->
            viewModel.onDismissAiRecommendations()
        }

        setupViews(bind)
        lastState?.let { updateUi(it, false) }
        observeUiState(bind)
        setupFragmentResults()
        viewLifecycleOwner.lifecycleScope.launch {
            delay(2000)
            checkNotificationPermission()
        }

        if (!viewModel.isHomeTourSeen()) {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(1000)
                viewModel.setHomeTourSeen(true)
                runHomeSpotlightSequence()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onHomeEntered()
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
                        checkExerciseGoalReached(it)
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
        if (viewModel.isUserPremium) {
            action()
        } else {
            PremiumBottomSheetDialog().show(childFragmentManager, PremiumBottomSheetDialog.TAG)
        }
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

        fun onCameraClick(meal: String) {
            LogFoodFragment.navigate(
                navController = findNavController(),
                timeStamp = currentDate(),
                meal = meal,
                openCamera = true
            )
        }

        bind.cardBreakfast.root.setOnSafeClickListener { onMealClick(MealTime.Breakfast.id) }
        bind.cardBreakfast.btnAddMeal.setOnSafeClickListener { onMealClick(MealTime.Breakfast.id) }
        bind.cardBreakfast.btnCameraMeal.setOnSafeClickListener { onCameraClick(MealTime.Breakfast.id) }

        bind.cardLunch.root.setOnSafeClickListener { onMealClick(MealTime.Lunch.id) }
        bind.cardLunch.btnAddMeal.setOnSafeClickListener { onMealClick(MealTime.Lunch.id) }
        bind.cardLunch.btnCameraMeal.setOnSafeClickListener { onCameraClick(MealTime.Lunch.id) }

        bind.cardDinner.root.setOnSafeClickListener { onMealClick(MealTime.Dinner.id) }
        bind.cardDinner.btnAddMeal.setOnSafeClickListener { onMealClick(MealTime.Dinner.id) }
        bind.cardDinner.btnCameraMeal.setOnSafeClickListener { onCameraClick(MealTime.Dinner.id) }

        bind.cardSnack.root.setOnSafeClickListener { onMealClick(MealTime.Snack.id) }
        bind.cardSnack.btnAddMeal.setOnSafeClickListener { onMealClick(MealTime.Snack.id) }
        bind.cardSnack.btnCameraMeal.setOnSafeClickListener { onCameraClick(MealTime.Snack.id) }

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

        bind.btnWater250.setOnSafeClickListener { addWaterInline(250) }
        bind.btnWater500.setOnSafeClickListener { addWaterInline(500) }
        bind.btnWater1000.setOnSafeClickListener { addWaterInline(1000) }



        bind.mainProgress.btnDetailedInfo.setOnSafeClickListener {
            val currentState = viewModel.uiState.value
            if (currentState.caloriesRemaining < 0) {
                OverageInfoBottomSheet.newInstance(currentState.caloriesRemaining.absoluteValue)
                    .show(childFragmentManager, OverageInfoBottomSheet.TAG)
            } else {
                val args = Bundle().apply {
                    putString(MacroInfoFragment.ARG_MACRO_TYPE, MacroInfoFragment.MACRO_CALORIES)
                }
                navigate(R.id.action_home_to_macroInfoFragment, args)
            }
        }

        setupDailyTip(bind)

        bind.btnAddWeightHome.setOnSafeClickListener {
            navigate(R.id.photoUploadInfoFragment)
        }


    }

    private fun addWaterInline(amountMl: Int) {
        checkPremium {
            viewModel.addWater(amountMl)
            vibrateDevice()
            viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                lastState?.let {
                    checkWaterGoalReached(it)
                }
            }
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
                    viewModel.showStreakSheetEvent.collect { streakInfo ->
                        uz.oybek.ozamiz.ui.feature.streak.StreakInfoSheet.show(
                            childFragmentManager, true
                        )
                    }
                }
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

                        if (state.pointsJustAdded != 0) {
                            viewModel.clearPointsJustAdded()
                        }
                        bind.cardDailyTip.isVisible = false
                        bind.cardWeightLog.isVisible = state.showWeightLogCard


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
                            val step = LocalPreferencesRepository.getAppOnboardingStep()
                            if (step == 1) {
                                DailyLimitFragment.newInstance(
                                    type = DailyLimitFragment.TYPE_REMAINING,
                                    remainingCalories = state.caloriesRemaining
                                ).show(childFragmentManager, DailyLimitFragment.TAG)
                                viewModel.onAppOnboardingComplete()
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
                    viewModel.showAppOnboardingEvent.collect {
                        delay(500)
                        runHomeSpotlightSequence()
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
        val isWaterCompletedd = state.waterTarget > 0 && state.waterConsumed >= state.waterTarget

        binding.textCurrentWaterLiters.text =
            if (isWaterCompletedd) "Bajarildi" else "$litersStr litr ichildi"
        binding.textCurrentWaterLiters.setTextColor(if (isWaterCompletedd) cachedColors.green else cachedColors.onSurface)

        binding.textWaterGoal.text = "Kunlik maqsad: $goalLitersStr litr"
        binding.textWaterGoal.isVisible = state.waterTarget > 0

        binding.progressWater.max = state.waterTarget
        binding.progressWater.setProgressCompat(state.waterConsumed, animate)

        binding.titleWater.text = getString(R.string.water_title)

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
                if (state.caloriesRemaining < 0) "oshdi" else "qoldi"
            textCaloriesRemainingCount.text = state.caloriesRemaining.absoluteValue.toString()
            textEatenCount.text =
                "Maqsad ${state.caloriesTarget} dan oshirmaslik"
            progressCaloriesCircular.max = state.caloriesTarget
            progressCaloriesCircular.setProgressCompat(
                state.caloriesConsumed.absoluteValue.coerceAtMost(state.caloriesTarget), animate
            )

            btnDetailedInfo.isVisible = true
            if (state.caloriesConsumed >= state.caloriesTarget - 50) {
                layoutCalorieText.isVisible = false
                layoutLimitInfo.isVisible = true
                updateBurningStatus(state)
                binding.layoutMacroWarnings.isVisible = false
                macrosContainer.isVisible = true
            } else {
                layoutCalorieText.isVisible = true
                layoutLimitInfo.isVisible = false
                textLimitSubtitle.visibility = android.view.View.GONE
                binding.layoutMacroWarnings.isVisible = false
                macrosContainer.isVisible = true
            }

            val calorieColor = when {
                state.caloriesRemaining < 0 -> colors.yellow
                state.caloriesConsumed >= state.caloriesTarget - 50 && state.caloriesRemaining >= 0 -> colors.green
                else -> colors.gray
            }
            progressCaloriesCircular.setIndicatorColor(calorieColor)
            progressCaloriesCircular.trackColor = colors.track
            textCaloriesRemainingCount.setTextColor(if (state.caloriesRemaining < -100) colors.yellow else colors.onSurface)
            textLimitTitle.setTextColor(calorieColor)

            fun bindMacro(
                progress: com.google.android.material.progressindicator.LinearProgressIndicator,
                text: android.widget.TextView,
                consumed: Int,
                target: Int,
                isExceededParam: Boolean
            ) {
                val isExceeded = consumed > target
                val isCompleted = consumed == target || (!isExceeded && consumed >= target * 0.95)
                val remaining = target - consumed
                progress.max = if (isExceeded) consumed else target
                progress.setProgressCompat(consumed, animate)

                val macroColor = when {
                    isExceeded -> colors.yellow
                    isCompleted -> colors.green
                    else -> colors.gray
                }

                progress.setIndicatorColor(macroColor)
                progress.trackColor = colors.track
                text.setTextColor(if (isExceeded) colors.yellow else if (isCompleted) colors.green else colors.onSurfaceVariant)
                val diff = (target - consumed).absoluteValue
                text.text = if (consumed <= target) {
                    getString(R.string.macro_remaining_format, diff)
                } else {
                    getString(R.string.macro_exceeded_format, diff)
                }
            }

            bindMacro(
                progressProtein,
                textProteinValue,
                state.proteinConsumed,
                state.proteinTarget,
                state.isProteinExceeded
            )
            bindMacro(
                progressFat,
                textFatValue,
                state.fatConsumed,
                state.fatTarget,
                state.isFatExceeded
            )
            bindMacro(
                progressCarbs,
                textCarbsValue,
                state.carbsConsumed,
                state.carbsTarget,
                state.isCarbsExceeded
            )
        }

        binding.cardAiRecommendations.isVisible =
            !state.isAiMealDismissed && (state.isAiMealLoading || state.aiMealRecommendation != null)
        if (state.nextMealTypeName != null) {
            binding.tvAiRecTitle.text = "${state.nextMealTypeName} tavsiyalari ✨"
        }

        binding.btnCloseAiRec.setOnSafeClickListener {
            viewModel.onDismissAiRecommendations()
        }

        if (state.isAiMealLoading) {
            binding.progressAiRec.isVisible = true

        } else if (state.aiMealRecommendation != null) {
            binding.progressAiRec.isVisible = false

            val item1 = state.aiMealRecommendation
            val formatted = "<b>${item1.description.replace("\n", "<br/>")}"
            binding.recItem.text =
                android.text.Html.fromHtml(formatted, android.text.Html.FROM_HTML_MODE_LEGACY)
        } else {
            binding.recItem.text = ""
        }

        binding.cardDietologAdvice.isVisible = false

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

        val isToday = DateUtils.isSameDay(state.selectedDate, Date())
        val isPastDate = state.selectedDate.time < DateUtils.getStartOfDay(Date()).time
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        fun isMealPassed(mealId: String): Boolean {
            if (isPastDate) return true
            if (!isToday) return false

            val timePassed = when (mealId) {
                MealTime.Breakfast.id -> currentHour >= 12
                MealTime.Lunch.id -> currentHour >= 17
                MealTime.Dinner.id -> currentHour >= 22
                else -> false
            }

            fun hasLogged(targetMeal: String) =
                state.dailyFoodLogs.any { it.mealType == targetMeal }

            val subsequentLogged = when (mealId) {
                MealTime.Breakfast.id -> hasLogged(MealTime.Lunch.id) || hasLogged(MealTime.Dinner.id)
                MealTime.Lunch.id -> hasLogged(MealTime.Dinner.id)
                else -> false
            }
            return timePassed || subsequentLogged
        }

        meals.forEach { (card, id, baseMax) ->
            val consumed = mealCalories[id] ?: 0
            val max = if (baseMax > 0) baseMax else 400
            val latestFood = latestFoodByMeal[id]
            val isPassed = isMealPassed(id)

            val displayConsumed = if (isPassed && consumed < max) max else consumed

            card.progressMeal.max = max
            card.progressMeal.setProgressCompat(displayConsumed.coerceAtMost(max), animate)
            card.progressMeal.setIndicatorColor(if (displayConsumed >= max) colors.green else colors.gray)

            if (displayConsumed >= max || state.caloriesConsumed >= state.caloriesTarget) {
                card.layoutCalories.isVisible = false
                card.textMealLastFood.isVisible = latestFood != null
                card.textMealLastFood.text = latestFood?.description
            } else {
                card.layoutCalories.isVisible = true
                card.textMealLastFood.isVisible = false
                card.textMealCalories.text = "${max - consumed} qoldi"
                card.textMealCalories.setTextColor(colors.onSurfaceVariant)
            }
        }
        binding.progressExercise.max = state.exerciseTarget
        binding.progressExercise.setProgressCompat(state.exerciseBurnt, animate)
        val isExerciseFull = state.exerciseBurnt >= state.exerciseTarget && state.exerciseTarget > 0
        binding.progressExercise.setIndicatorColor(if (isExerciseFull) colors.green else colors.gray)
        binding.progressExercise.trackColor = colors.track
        val exRemaining = maxOf(0, state.exerciseTarget - state.exerciseBurnt)
        binding.textExerciseProgress.text =
            if (exRemaining > 0) "$exRemaining qoldi" else "Bajarildi"
        binding.textExerciseProgress.setTextColor(if (isExerciseFull) colors.green else colors.onSurfaceVariant)
        binding.titleExercise.text = "Yog' yoqish"
        binding.btnTodayExercises.isVisible = state.exerciseLogs.isNotEmpty()

        val fatBurnt = (state.exerciseBurnt / 7.7).toInt()
        binding.textExerciseStatus.text = "Bugun $fatBurnt gr yog' yoqdingiz."
        binding.textExerciseStatus.isVisible = state.exerciseBurnt > 0

        val isWaterCompleted = state.waterTarget > 0 && state.waterConsumed >= state.waterTarget
        binding.textWaterStatus.isVisible = false
        binding.warningProtein.root.isVisible = false
        binding.warningCarbs.root.isVisible = false
        binding.warningFat.root.isVisible = false
    }

    private fun updateBurningStatus(state: HomeUiState) {
        if (state.caloriesConsumed >= state.caloriesTarget - 50) {
            binding.mainProgress.apply {
                if (state.caloriesRemaining in -50..20) {
                    textLimitTitle.text = "Bugunga yetarli yedingiz"
                    textLimitTitle.textSize = 18f
                    textLimitSubtitle.visibility = android.view.View.VISIBLE
                    textLimitSubtitle.text = "ortiqchasi semirtiradi"
                } else if (state.caloriesRemaining < -50) {
                    val count = state.caloriesRemaining.absoluteValue
                    textLimitTitle.text = "$count oshdi"
                    textLimitTitle.textSize = 28f
                    textLimitSubtitle.visibility = android.view.View.VISIBLE
                    textLimitSubtitle.text = "meyordan oshdi"
                } else {
                    val count = state.caloriesRemaining.absoluteValue
                    textLimitTitle.text = "$count qoldi"
                    textLimitTitle.textSize = 28f
                    textLimitSubtitle.visibility = android.view.View.GONE
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


    private fun runHomeSpotlightSequence() {
        if (view == null || isDetached) return

        viewLifecycleOwner.lifecycleScope.launch {
            // STEP 1: Calories
            binding.contentScrollView.smoothScrollTo(0, 0)
            delay(300)
            showOverlaySuspend(
                binding.mainProgress.cardMainCalories,
                "Kaloriya hisobi",
                "Bu yerda kunlik kaloriya me'yoringiz va qancha iste'mol qilganingiz doimiy yozib boriladi. Bu orqali siz ortiqcha vaznni oldini olishingiz va sog'lom turmush tarzini boshqarishingiz mumkin."
            )

            // STEP 2: Macros
            delay(200)
            showOverlaySuspend(
                binding.mainProgress.macrosContainer,
                "Makrolar",
                "Oqsillar, yog'lar va uglevodlar balansi nihoyatda muhim. Oqsillar muskullar, yog'lar salomatlik, uglevodlar esa energiyani ta'minlaydi. Har birini o'z me'yorida ushlash muhim!"
            )

            // STEP 3: Meals
            binding.contentScrollView.smoothScrollTo(0, binding.cardBreakfast.root.top - 100)
            delay(400)
            showOverlaySuspend(
                binding.cardBreakfast.root,
                "Kunlik Ovqatlar",
                "Kun davomida iste'mol qilgan har bir taomingizni shu joyga kiritib kelasiz. Har bir kiritilgan taom kunlik kaloriya hisobingizga qo'shiladi va jarayoningizni nazorat qiladi."
            )

            // STEP 4: Water
            binding.contentScrollView.smoothScrollTo(0, binding.cardWater.top - 100)
            delay(400)
            showOverlaySuspend(
                binding.cardWater,
                "Suv muvozanati",
                "Suv ichish tanani toksinlardan tozalaydi va ozishga yordam beradi. Har bir ichgan stakaningizni erinmasdan belgilang."
            )

            // STEP 5: Exercises
            binding.contentScrollView.smoothScrollTo(0, binding.cardExercise.top - 100)
            delay(400)
            showOverlaySuspend(
                binding.cardExercise,
                "Faollik va Mashqlar",
                "O'z mashqlaringizni va piyoda yurgan qadamlaringizni kiritib qancha kaloriya sarflaganingizni ko'rib borishingiz mumkin. Ko'proq harakat - tezroq natija!",
                "Tugatish"
            )

            viewModel.onAppOnboardingPresented()
        }
    }

    private suspend fun showOverlaySuspend(
        target: android.view.View,
        title: String,
        text: String,
        buttonText: String = "Keyingisi"
    ) = kotlin.coroutines.suspendCoroutine<Unit> { cont ->
        var resumed = false
        tourOverlay = uz.oybek.ozamiz.ui.utils.GuidedTourOverlay(requireContext())
        tourOverlay?.showFor(
            activity = requireActivity(),
            targetView = target,
            title = title,
            text = text,
            buttonText = buttonText,
            onNext = {
                tourOverlay = null
                if (!resumed) {
                    resumed = true
                    cont.resumeWith(Result.success(Unit))
                }
            }
        )
    }

    private fun Float.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()


    private fun checkWaterGoalReached(state: HomeUiState) {}

    private fun checkExerciseGoalReached(state: HomeUiState) {
        val todayStr = Date().dateApiFormat()
        if (state.exerciseTarget > 0 && state.exerciseBurnt >= state.exerciseTarget && getPersistentDate(
                "last_exercise_goal_shown"
            ) != todayStr
        ) {
            val fatBurnt = (state.exerciseBurnt / 7.7).toInt()
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Mashqlar yakunlandi")
                .setMessage("Bugun siz kamida ${state.exerciseBurnt} kkal energiya sarfladingiz va o'rtacha $fatBurnt gr sof yog' yoqdingiz.")
                .setPositiveButton("Tushunarli", null)
                .show()
            savePersistentDate("last_exercise_goal_shown", todayStr)
        }
    }

    private fun showWaterGoalReachedAchievement() {}


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
