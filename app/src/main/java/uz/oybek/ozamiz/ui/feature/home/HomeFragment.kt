package uz.oybek.ozamiz.ui.feature.home

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.config.LocalPreferencesRepository
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.databinding.FragmentHomeBinding
import uz.oybek.ozamiz.ui.MainActivity
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.log.food.LogFoodFragment
import uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog
import uz.oybek.ozamiz.ui.feature.streak.StreakInfoSheet
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import java.util.Date
import kotlin.math.absoluteValue

class LogFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by activityViewModels()
    private var dateAdapter: DateScrollerAdapter? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
    }

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
        val startTime = System.currentTimeMillis()
        Log.d("HomeFragment", "viewCreated started")
        setFragmentResultListener("dismiss_ai_meal") { _, _ ->
            Log.d("HomeFragment", "dismiss_ai_meal fragment result triggered")
            viewModel.onDismissAiRecommendations()
        }
        setupViews(bind)
        lastState?.let {
            Log.d("HomeFragment", "viewCreated: updating UI with lastState")
            updateUi(it, false)
        }
        observeUiState(bind)
        setupFragmentResults()
        checkNotificationPermission()
        val duration = (System.currentTimeMillis() - startTime) / 1000.0
        Log.d("HomeFragment", "viewCreated completed in $duration seconds")
    }

    private var homeEntered = false

    override fun onResume() {
        val startTime = System.currentTimeMillis()
        Log.d("HomeFragment", "onResume started")
        super.onResume()
        if (!homeEntered) {
            Log.d("HomeFragment", "onResume: triggering viewModel.onHomeEntered()")
            viewModel.onHomeEntered()
            homeEntered = true
        }
        checkOnboarding()
        val duration = (System.currentTimeMillis() - startTime) / 1000.0
        Log.d("HomeFragment", "onResume completed in $duration seconds")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val startTime = System.currentTimeMillis()
        Log.d("HomeFragment", "onCreate started")
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            delay(2000)
            val needShowWeightLog =
                viewModel.uiState.value.showWeightLogCard && (user.streakInfo?.currentStreak
                    ?: 0) > 1
            Log.d("HomeFragment", "onCreate: checking needShowWeightLog = $needShowWeightLog")
            if (needShowWeightLog) {
                Log.d("HomeFragment", "onCreate: navigating to photoUploadInfoFragment")
                navigate(R.id.photoUploadInfoFragment)
            }
        }
        val duration = (System.currentTimeMillis() - startTime) / 1000.0
        Log.d("HomeFragment", "onCreate completed in $duration seconds")
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    private fun setupFragmentResults() {
        val startTime = System.currentTimeMillis()
        Log.d("HomeFragment", "setupFragmentResults started")
        childFragmentManager.setFragmentResultListener(
            "water_added_request", viewLifecycleOwner
        ) { _, bundle ->
            val added = bundle.getBoolean("added", false)
            Log.d("HomeFragment", "FragmentResult: water_added_request received, added = $added")
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
            Log.d(
                "HomeFragment",
                "FragmentResult: exercise_added_request received, exerciseId = $exerciseId"
            )
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
            Log.d(
                "HomeFragment",
                "FragmentResult: navigate_to_ai_chat received, navigating to aiChatFragment"
            )
            navigate(R.id.aiChatFragment, bundle)
        }
        val duration = (System.currentTimeMillis() - startTime) / 1000.0
        Log.d("HomeFragment", "setupFragmentResults completed in $duration seconds")
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
        if (LocalPreferencesRepository.isSecondOnboardingSheetSeen().not()) return
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
        val startTime = System.currentTimeMillis()
        Log.d("HomeFragment", "setupViews started")
        if (dateAdapter == null) {
            dateAdapter = DateScrollerAdapter { date ->
                Log.d("HomeFragment", "Date selected from scroller: $date")
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
            alpha = 0f // Start completely invisible to prevent visual scroller jump!
        }
        bind.swipeRefreshLayout.setOnRefreshListener {
            Log.d("HomeFragment", "swipeRefreshLayout triggered refresh")
            viewModel.refresh()
        }
        bind.btnQuickWalk.setOnSafeClickListener {
            Log.d("HomeFragment", "btnQuickWalk clicked")
            val args = Bundle().apply {
                putString("exerciseId", "PiyodaYurish")
            }
            navigate(R.id.addExerciseLogFragment, args)
        }

        bind.btnQuickRun.setOnSafeClickListener {
            Log.d("HomeFragment", "btnQuickRun clicked")
            val args = Bundle().apply {
                putString("exerciseId", "Yugurish")
            }
            navigate(R.id.addExerciseLogFragment, args)
        }

        bind.btnQuickOther.setOnSafeClickListener {
            Log.d("HomeFragment", "btnQuickOther clicked")
            navigate(R.id.selectExerciseFragment)
        }
        fun onMealClick(meal: String) {
            Log.d("HomeFragment", "onMealClick: meal = $meal")
            LogFoodFragment.navigate(
                navController = findNavController(),
                timeStamp = currentDate(),
                meal = meal,
                openCamera = false
            )
        }

        fun onCameraClick(meal: String) {
            Log.d("HomeFragment", "onCameraClick: meal = $meal")
            LogFoodFragment.navigate(
                navController = findNavController(),
                timeStamp = currentDate(),
                meal = meal,
                openCamera = true
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

        bind.cardBreakfast.imgMealIcon.setImageResource(R.drawable.breakfast_simple)
        bind.cardBreakfast.textMealName.text = getString(R.string.breakfast)
        bind.cardLunch.imgMealIcon.setImageResource(R.drawable.lunch_simple)
        bind.cardLunch.textMealName.text = getString(R.string.lunch)
        bind.cardDinner.imgMealIcon.setImageResource(R.drawable.dinner_simple)
        bind.cardDinner.textMealName.text = getString(R.string.dinner)
        bind.cardSnack.imgMealIcon.setImageResource(R.drawable.apple_simple)
        bind.cardSnack.textMealName.text = getString(R.string.snack)

        bind.cardDietolog.setOnSafeClickListener {
            Log.d("HomeFragment", "cardDietolog clicked")
            checkPremium {
                navigate(R.id.aiChatFragment)
            }
        }
        bind.mainProgress.cardProtein.setOnClickListener {
            val state = viewModel.uiState.value
            Log.d(
                "HomeFragment",
                "cardProtein clicked. consumed = ${state.proteinConsumed}, target = ${state.proteinTarget}"
            )
            val args = Bundle().apply {
                putString(MacroInfoFragment.ARG_MACRO_TYPE, MacroInfoFragment.MACRO_PROTEIN)
                putInt(MacroInfoFragment.ARG_CONSUMED, state.proteinConsumed)
                putInt(MacroInfoFragment.ARG_TARGET, state.proteinTarget)
            }
            navigate(R.id.action_home_to_macroInfoFragment, args)
        }

        bind.mainProgress.cardFat.setOnClickListener {
            val state = viewModel.uiState.value
            Log.d(
                "HomeFragment",
                "cardFat clicked. consumed = ${state.fatConsumed}, target = ${state.fatTarget}"
            )
            val args = Bundle().apply {
                putString(MacroInfoFragment.ARG_MACRO_TYPE, MacroInfoFragment.MACRO_FAT)
                putInt(MacroInfoFragment.ARG_CONSUMED, state.fatConsumed)
                putInt(MacroInfoFragment.ARG_TARGET, state.fatTarget)
            }
            navigate(R.id.action_home_to_macroInfoFragment, args)
        }

        bind.mainProgress.cardCarbs.setOnClickListener {
            val state = viewModel.uiState.value
            Log.d(
                "HomeFragment",
                "cardCarbs clicked. consumed = ${state.carbsConsumed}, target = ${state.carbsTarget}"
            )
            val args = Bundle().apply {
                putString(MacroInfoFragment.ARG_MACRO_TYPE, MacroInfoFragment.MACRO_CARBS)
                putInt(MacroInfoFragment.ARG_CONSUMED, state.carbsConsumed)
                putInt(MacroInfoFragment.ARG_TARGET, state.carbsTarget)
            }
            navigate(R.id.action_home_to_macroInfoFragment, args)
        }

        bind.btnWater250.setOnSafeClickListener { addWaterInline(250) }
        bind.btnWater500.setOnSafeClickListener { addWaterInline(500) }
        bind.btnWater1000.setOnSafeClickListener { addWaterInline(1000) }



        bind.mainProgress.btnDetailedInfo.setOnSafeClickListener {
            val currentState = viewModel.uiState.value
            Log.d(
                "HomeFragment",
                "btnDetailedInfo clicked. caloriesRemaining = ${currentState.caloriesRemaining}"
            )
            if (currentState.caloriesRemaining < 0) {
                OverageInfoBottomSheet.newInstance(currentState.caloriesRemaining.absoluteValue)
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

        setupDailyTip(bind)

        bind.btnAddWeightHome.setOnSafeClickListener {
            Log.d("HomeFragment", "btnAddWeightHome clicked")
            navigate(R.id.addWeightPhotoFragment)
        }

        val duration = (System.currentTimeMillis() - startTime) / 1000.0
        Log.d("HomeFragment", "setupViews completed in $duration seconds")
    }

    private fun addWaterInline(amountMl: Int) {
        viewModel.addWater(amountMl)
        vibrateDevice()
        viewLifecycleOwner.lifecycleScope.launch {
            delay(300)
            lastState?.let {
                checkWaterGoalReached(it)
            }
        }
    }

    private fun setupDailyTip(bind: FragmentHomeBinding) {
        bind.btnCloseTip.setOnClickListener {

        }
    }

    companion object {
        var currentDate: Date = Date()
    }

    private fun observeUiState(bind: FragmentHomeBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                    viewModel.uiState.collect { state ->
                        val collectStartTime = System.currentTimeMillis()
                        Log.d(
                            "HomeFragment",
                            "UI State collected. isInitialLoading = ${state.isInitialLoading}, isRefreshing = ${state.isRefreshing}"
                        )
                        bind.progressBarOverall.isVisible = false
                        bind.contentScrollView.isVisible = true
                        bind.swipeRefreshLayout.isRefreshing = state.isRefreshing

                        val shouldAnimate = !state.isInitialLoading
                        updateUi(state, shouldAnimate)

                        MainActivity.isAppReady = true

                        if (state.pointsJustAdded != 0) {
                            Log.d(
                                "HomeFragment",
                                "Points just added: ${state.pointsJustAdded}, clearing..."
                            )
                            viewModel.clearPointsJustAdded()
                        }
                        bind.cardDailyTip.isVisible = false
                        bind.cardWeightLog.isVisible = state.showWeightLogCard
                        val duration = (System.currentTimeMillis() - collectStartTime) / 1000.0
                        Log.d("HomeFragment", "UI State processing completed in $duration seconds")
                    }
                }
                launch {
                    viewModel.showRateAppEvent.collect {
                        delay(2000)
                        if (isResumed && childFragmentManager.fragments.isEmpty()) {
                            (activity as? MainActivity)?.requestAppReview()
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
            }
        }
    }

    private fun checkOnboarding() {
        val isFirstDay = true
        if (isFirstDay) {
            if (!LocalPreferencesRepository.isFirstLogAdded()) {
                showAppOnboarding()
            } else if (!LocalPreferencesRepository.isSecondOnboardingSheetSeen()) {
                showSecondOnboardingSheet()
            }
        }
    }

    private fun updateUi(state: HomeUiState, animate: Boolean) {
        val startTime = System.currentTimeMillis()
        Log.d(
            "HomeFragment",
            "updateUi started: date = ${state.selectedDate}, caloriesTarget = ${state.caloriesTarget}, caloriesConsumed = ${state.caloriesConsumed}, waterConsumed = ${state.waterConsumed}, waterTarget = ${state.waterTarget}, proteinConsumed = ${state.proteinConsumed}, fatConsumed = ${state.fatConsumed}, carbsConsumed = ${state.carbsConsumed}"
        )
        val oldState = lastState
        lastState = state

        // Only animate if we are updating from an existing non-null state!
        // This prevents the progress bars from starting at 0 on first load or recreate.
        val useAnimation = oldState != null && animate

        binding.toolbarTitle.text = if (DateUtils.isSameDay(state.selectedDate, Date())) {
            getString(R.string.today) + ", " + DateUtils.getFormattedDateString(state.selectedDate, "d-MMMM")
        } else {
            DateUtils.getFormattedDateString(state.selectedDate, "d-MMMM")
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

        if (isWaterCompletedd) {
            val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.check_ic)
            drawable?.setTint(cachedColors.green)
            binding.textCurrentWaterLiters.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
            binding.textCurrentWaterLiters.compoundDrawablePadding = 4f.dpToPx()
        } else {
            binding.textCurrentWaterLiters.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
        }

        binding.textWaterGoal.text = "Kunlik maqsad: $goalLitersStr litr"
        binding.textWaterGoal.isVisible = state.waterTarget > 0

        binding.progressWater.max = state.waterTarget
        binding.progressWater.setProgressCompat(state.waterConsumed, useAnimation)

        binding.titleWater.text = getString(R.string.water_title)

        val streakInfo = state.streakInfo
        binding.streakDisplayLayout.isVisible = streakInfo != null
        if (streakInfo != null) {
            binding.textStreakCount.text = streakInfo.currentStreak.toString()
            
            val isActive = uz.oybek.ozamiz.data.streak.StreakManager.isStreakActive(streakInfo)
            if (isActive) {
                binding.imageStreakIcon.alpha = 1.0f
                binding.textStreakCount.alpha = 1.0f
            } else {
                binding.imageStreakIcon.alpha = 0.4f
                binding.textStreakCount.alpha = 0.4f
            }

            if (binding.streakDisplayLayout.scaleX == 1f) {
                binding.streakDisplayLayout.animate().scaleX(1.1f).scaleY(1.1f).setDuration(300)
                    .withEndAction {
                        binding.streakDisplayLayout.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300)
                            .start()
                    }.start()
            }
            binding.streakDisplayLayout.setOnClickListener {
                uz.oybek.ozamiz.ui.feature.streak.StreakInfoSheet.newInstance(streakInfo, isNewAction = false, returnedAfterManyDays = false).show(
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
                "${state.caloriesConsumed} yeyildi"
            progressCaloriesCircular.max = state.caloriesTarget
            progressCaloriesCircular.setProgressCompat(
                state.caloriesConsumed.absoluteValue.coerceAtMost(state.caloriesTarget),
                useAnimation
            )

            btnDetailedInfo.isVisible = true
            if (state.caloriesConsumed >= state.caloriesTarget - 100) {
                layoutCalorieText.isVisible = false
                layoutLimitInfo.isVisible = true
                updateBurningStatus(state)
                macrosContainer.isVisible = true
            } else {
                layoutCalorieText.isVisible = true
                layoutLimitInfo.isVisible = false
                textLimitSubtitle.visibility = android.view.View.GONE
                macrosContainer.isVisible = true
            }

            val calorieColor = when {
                state.caloriesConsumed > state.caloriesTarget + 100 -> colors.yellow
                state.caloriesConsumed >= state.caloriesTarget - 100 && state.caloriesConsumed <= state.caloriesTarget + 100 -> colors.green
                else -> colors.primary
            }
            progressCaloriesCircular.setIndicatorColor(calorieColor)
            progressCaloriesCircular.trackColor = colors.track
            textCaloriesRemainingCount.setTextColor(if (state.caloriesConsumed > state.caloriesTarget + 100) colors.yellow else colors.onSurface)

            fun bindMacro(
                progress: com.google.android.material.progressindicator.LinearProgressIndicator,
                text: android.widget.TextView,
                consumed: Int,
                target: Int,
                isExceededParam: Boolean
            ) {
                val isExceededByMoreThan10 = consumed > target + 10
                val isExceeded = consumed > target
                val isCompleted = consumed >= target * 0.95 && consumed <= target + 10

                progress.max = if (isExceeded) consumed else target
                progress.setProgressCompat(consumed, useAnimation)

                val macroColor = when {
                    isExceededByMoreThan10 -> colors.yellow
                    isCompleted -> colors.green
                    else -> colors.primary
                }

                progress.setIndicatorColor(macroColor)
                progress.trackColor = colors.track

                text.setTextColor(if (isExceededByMoreThan10) colors.yellow else colors.onSurface)

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

        val isDietologEligible = viewModel.isUserPremium
        if (isDietologEligible) {
            val hasMealRec = state.aiMealRecommendation != null && !state.isAiMealDismissed
            binding.cardAiRecommendations.isVisible = hasMealRec
            binding.btnCloseAiRec.isVisible = true
            binding.cardAiRecommendations.setOnClickListener(null)
            binding.cardAiRecommendations.isClickable = false

            binding.btnCloseAiRec.setOnSafeClickListener {
                if (hasMealRec) {
                    viewModel.onDismissAiRecommendations()
                }
            }

            binding.progressAiRec.isVisible =
                state.isAiMealLoading && state.aiMealRecommendation == null

            if (hasMealRec) {
                if (state.nextMealTypeName != null) {
                    binding.tvAiRecTitle.text = "${state.nextMealTypeName} tavsiyalari ✨"
                } else {
                    binding.tvAiRecTitle.text = "Tavsiyalar ✨"
                }
                val item1 = state.aiMealRecommendation
                val formatted = item1?.description?.replace("\n", "<br/>") ?: ""
                binding.recItem.text =
                    android.text.Html.fromHtml(formatted, android.text.Html.FROM_HTML_MODE_LEGACY)
            } else {
                binding.recItem.text = ""
            }
        } else {
            binding.cardAiRecommendations.isVisible = !state.isAiMealDismissed
            binding.progressAiRec.isVisible = false
            binding.btnCloseAiRec.isVisible = false
            binding.tvAiRecTitle.text = getString(R.string.today_recommendations)
            binding.recItem.text = getString(R.string.premium_rec_request)
            binding.cardAiRecommendations.setOnSafeClickListener {
                PremiumBottomSheetDialog().show(childFragmentManager, PremiumBottomSheetDialog.TAG)
            }
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
            card.progressMeal.setProgressCompat(consumed.coerceAtMost(max), useAnimation)
            card.progressMeal.setIndicatorColor(colors.primary)

            if (consumed >= max || state.caloriesConsumed >= state.caloriesTarget) {
                card.layoutCalories.isVisible = false
            } else {
                card.layoutCalories.isVisible = true
                card.textMealCalories.text = "${max - consumed} qoldi"
                card.textMealCalories.setTextColor(colors.onSurfaceVariant)
            }
        }
        binding.progressExercise.max = state.exerciseTarget
        binding.progressExercise.setProgressCompat(state.exerciseBurnt, useAnimation)
        val isExerciseFull = state.exerciseBurnt >= state.exerciseTarget && state.exerciseTarget > 0
        binding.progressExercise.setIndicatorColor(if (isExerciseFull) colors.green else colors.primary)
        binding.progressExercise.trackColor = colors.track
        val exRemaining = maxOf(0, state.exerciseTarget - state.exerciseBurnt)
        binding.textExerciseProgress.text =
            if (exRemaining > 0) "$exRemaining qoldi" else "Bajarildi"
        binding.textExerciseProgress.setTextColor(if (isExerciseFull) colors.green else colors.onSurface)

        if (isExerciseFull) {
            val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.check_ic)
            drawable?.setTint(colors.green)
            binding.textExerciseProgress.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
            binding.textExerciseProgress.compoundDrawablePadding = 4f.dpToPx()
        } else {
            binding.textExerciseProgress.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
        }
        binding.titleExercise.text = "Yog' yoqish"

        val fatBurnt = (state.exerciseBurnt / 7.7).toInt()
        binding.textExerciseStatus.text = "Bugun $fatBurnt gr yog' yoqdingiz."
        binding.textExerciseStatus.isVisible = state.exerciseBurnt > 0

        binding.warningProtein.root.isVisible = false
        binding.warningCarbs.root.isVisible = false
        binding.warningFat.root.isVisible = false
        val duration = (System.currentTimeMillis() - startTime) / 1000.0
        Log.d("HomeFragment", "updateUi completed in $duration seconds")
    }

    private fun updateBurningStatus(state: HomeUiState) {
        val target = state.caloriesTarget
        val consumed = state.caloriesConsumed
        if (target in 1..consumed) {
            binding.mainProgress.apply {
                val diff = consumed - target
                val diffText = if (diff > 0) "$diff kal oshdi." else "limit to'ldi."
                textLimitTitle.text = "Bugunga yetarli, ortiqcha ovqat semirtiradi"
                textLimitSubtitle.visibility = View.VISIBLE
                textLimitSubtitle.text =
                    "Suv, choy, yengil sabzavotli salat yeyishingiz mumkin, $diffText"
            }
        } else if (target > 0) {
            binding.mainProgress.apply {
                val count = state.caloriesRemaining.absoluteValue
                textLimitTitle.text = "$count qoldi"
                textLimitSubtitle.visibility = View.GONE
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
                // Smoothly fade in the RecyclerView after centering the selected item!
                if (binding.dateScrollerRecyclerView.alpha == 0f) {
                    binding.dateScrollerRecyclerView.animate().alpha(1f).setDuration(150).start()
                }
            }
        }
    }

    private var onboardingSheet: AppOnboardingBottomSheet? = null

    private fun showAppOnboarding() {
        if (activity == null || isDetached) return
        runCatching {
            onboardingSheet?.dismiss()
        }
        viewModel.setHomeTourSeen(true)
        LocalPreferencesRepository.setSecondOnboardingSheetSeen(false)
        val name = user.name.ifBlank { "Nargiza" }
        val greetingText =
            "Assalomu alaykum $name! Ozish uchun har bir ovqatingizni menga kiritib borishingiz kerak, ozishning siri bu tog'ri ovqatlanishda, birinchi ovqatingizni kiriting"

        onboardingSheet = AppOnboardingBottomSheet(
            text = greetingText,
            buttonText = "Qo'shish",
            onButtonClick = {
                LogFoodFragment.navigate(
                    navController = findNavController(),
                    timeStamp = currentDate(),
                    meal = MealTime.Breakfast.id,
                    openCamera = false,
                    openEditText = true
                )
                LocalPreferencesRepository.setFirstLogAdded(true)
            },
            onLaterClick = {
                // Dismisses dialog on user cancel / later click
            }
        ).apply {
            show(mainActivity()!!.supportFragmentManager, AppOnboardingBottomSheet.TAG)
        }
    }

    private fun showSecondOnboardingSheet() {
        if (activity == null || isDetached) return
        LocalPreferencesRepository.setSecondOnboardingSheetSeen(true)

        val name = user.name.ifBlank { "Oybek" }
        val cal = viewModel.uiState.value.caloriesRemaining
        if (cal <= 0) return
        val remainingText =
            "${name}, sizda yana ${cal} kaloriya ovqat yesangiz bo'ladi, har bir yeganingizni qo'shib boring, meyordan oshirmaslikka harakat qiling."

        AppOnboardingBottomSheet(
            text = remainingText,
            buttonText = "Tushundim",
            title = "Ajoyib!",
            onButtonClick = {
                // Done!
            }
        ).show(childFragmentManager, AppOnboardingBottomSheet.TAG)
    }

    private fun Float.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()


    private fun checkWaterGoalReached(state: HomeUiState) {}

    private fun checkExerciseGoalReached(state: HomeUiState) {}


    private fun check70PercentExercise(state: HomeUiState) {}

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
