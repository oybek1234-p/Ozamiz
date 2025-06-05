package uz.oybek.ozamiz.ui

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.color.MaterialColors
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.MainActivity
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.info.DailyLogData
import uz.oybek.ozamiz.data.info.ExerciseLog
import uz.oybek.ozamiz.data.info.FoodLog
import uz.oybek.ozamiz.data.info.WaterLog
import uz.oybek.ozamiz.databinding.FragmentHomeBinding
import uz.oybek.ozamiz.databinding.ItemHomeMealBinding
import uz.oybek.ozamiz.databinding.ItemHomeReminderBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.dialogs.StreakInfoBottomSheetDialog
import uz.oybek.ozamiz.ui.getinfo.ExerciseType
import uz.oybek.ozamiz.ui.getinfo.ReminderItem
import uz.oybek.ozamiz.ui.getinfo.ReminderType
import uz.oybek.ozamiz.ui.home.HomeUiState
import uz.oybek.ozamiz.ui.home.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by viewModels()
    private val logTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private var initialWeightLogCheckDone = false


    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentHomeBinding) {
        setupToolbar(bind)
        setupMealSections(bind)
        setupClickListeners(bind)
        observeUiState(bind)

        bind.swipeRefreshLayout.setOnRefreshListener {
            viewModel.clearErrorMessage()
            bind.swipeRefreshLayout.isRefreshing = false
        }
        bind.swipeRefreshLayout.setColorSchemeColors(
            getThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimary),
            getThemeColor(requireContext(), com.google.android.material.R.attr.colorSecondary),
            getThemeColor(requireContext(), com.google.android.material.R.attr.colorTertiary)
        )
    }

    companion object {
        private var miniInfoShown = false
    }


    override fun onResume() {
        super.onResume()
        if (miniInfoShown.not()) {
            navigate(R.id.miniInfoFragment)
            miniInfoShown = true
        }
        requestNotificationPermission()
        if (!initialWeightLogCheckDone && LocalUser.user.valid && LocalUser.user.name.isNotEmpty()) {
            view?.post {
                checkAndNavigateForWeightLog()
            }
        }
    }


    private fun shouldPromptForWeightPhotoLog(): Boolean {
        val user = LocalUser.user
        if (!user.valid || user.name.isEmpty() || user.gender == -1) return false

        val lastLogTime = user.lastWeightLogTimestamp
        if (lastLogTime == 0L && user.name.isNotEmpty()) {
            return true
        }
        val currentTime = System.currentTimeMillis()
        val daysSinceLastLog = TimeUnit.MILLISECONDS.toDays(currentTime - lastLogTime)
        return (daysSinceLastLog >= MainActivity.getWeightLogIntervalDays()) && (LocalUser.user.streakInfo?.currentStreak ?: 0) > 2
    }

    private fun checkAndNavigateForWeightLog() {
        if (!isAdded || !isVisible || view == null || findNavController().currentDestination?.id != R.id.home) {
            return // Do nothing if not in a valid state to navigate
        }
        initialWeightLogCheckDone = true // Mark as done to avoid multiple checks

        if (shouldPromptForWeightPhotoLog()) {
            lifecycleScope.launch { // Use lifecycleScope for safety
                // delay(100) // Small delay can sometimes help ensure nav controller is ready
                if (findNavController().currentDestination?.id == R.id.home) { // Double check
                    if (!LocalUser.user.hasSeenWeightPhotoInfo) {
                        navigate(R.id.photoUploadInfoFragment)
                    } else {
                        navigate(R.id.addWeightPhotoFragment)
                    }
                }
            }
        }
    }

    private fun setupToolbar(bind: FragmentHomeBinding) {
        bind.streakDisplayLayout.setOnClickListener {
            viewModel.uiState.value.streakInfo?.let { streak ->
                if (isAdded) { // Check if fragment is added
                    val dialog = StreakInfoBottomSheetDialog.newInstance(streak)
                    // Use childFragmentManager if showing from within this fragment,
                    // or parentFragmentManager if the dialog is managed by the activity or parent fragment.
                    // For BottomSheetDialogFragment shown from a Fragment, childFragmentManager is usually correct.
                    dialog.show(childFragmentManager, StreakInfoBottomSheetDialog.TAG)
                }
            }
        }
    }


    private fun setupMealSections(bind: FragmentHomeBinding) {
        setupSingleMealSection(
            bind.layoutBreakfast,
            R.drawable.breakfast_ic,
            getString(R.string.breakfast),
            "breakfast"
        )
        setupSingleMealSection(
            bind.layoutLunch, R.drawable.lunch_ic, getString(R.string.lunch), "lunch"
        )
        setupSingleMealSection(
            bind.layoutDinner, R.drawable.dinner_ic, getString(R.string.dinner), "dinner"
        )
        setupSingleMealSection(
            bind.layoutSnack, R.drawable.snack_ic, getString(R.string.snack), "snack"
        )
    }

    private fun setupSingleMealSection(
        mealBinding: ItemHomeMealBinding, iconRes: Int, title: String, mealType: String
    ) {
        mealBinding.imageMealIcon.setImageResource(iconRes)
        mealBinding.textMealName.text = title
        mealBinding.buttonAddMeal.setOnClickListener { navigateToLogFood(mealType) }
        mealBinding.mealRootLayout.setOnClickListener { navigateToLogFood(mealType) }
    }

    private fun setupClickListeners(bind: FragmentHomeBinding) {
        bind.buttonAddWater.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.home) {
                navigate(R.id.logWaterDialog)
            }
        }
        bind.buttonAddExerciseLog.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.home) {
                navigate(R.id.logExerciseFragment)
            }
        }
        bind.buttonFoodList.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.home) {
                navigate(R.id.foods)
            }
        }
        bind.buttonAddReminder.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.home) {
                navigate(R.id.setupRemindersFragment)
            }
        }
        // Weight goal card click listener
        bind.cardWeightGoalProgressSection.setOnClickListener {
            if (isAdded && findNavController().currentDestination?.id == R.id.home) {
                navigate(R.id.weightPhotoHistoryFragment)
            }
        }
    }

    private fun observeUiState(bind: FragmentHomeBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.progressBarOverall.isVisible =
                        state.isLoading && state.dailyLogData == null && state.user == null
                    bind.contentScrollView.isVisible =
                        !state.isLoading || state.dailyLogData != null || state.user != null
                    bind.swipeRefreshLayout.isRefreshing =
                        state.isLoading && (state.dailyLogData != null || state.user != null)


                    state.errorMessage?.let {
                        Snackbar.make(bind.root, it, Snackbar.LENGTH_LONG).show()
                        viewModel.clearErrorMessage()
                    }
                    updateUiWithState(bind, state)

                    // Moved the weight log check here to ensure data is loaded
                    if (!initialWeightLogCheckDone && !state.isLoading && state.user != null && state.user.name.isNotEmpty()) {
                        checkAndNavigateForWeightLog()
                    }
                }
            }
        }
    }

    private fun displayEnabledExerciseReminders(
        bind: FragmentHomeBinding, reminders: List<ReminderItem>
    ) {
        bind.layoutReminders.removeAllViews()
        if (reminders.isEmpty()) {
            bind.textNoReminders.isVisible = true
            bind.layoutReminders.isVisible = false
        } else {
            bind.textNoReminders.isVisible = false
            bind.layoutReminders.isVisible = true
            reminders.forEach { reminder ->
                val reminderBinding = ItemHomeReminderBinding.inflate(
                    LayoutInflater.from(requireContext()), bind.layoutReminders, false
                )
                reminderBinding.iconReminderItem.setImageResource(reminder.iconRes)
                reminderBinding.textReminderItemDetails.text = String.format(
                    getString(R.string.reminder_display_format),
                    reminder.name,
                    reminder.getTimeAsString()
                )
                reminderBinding.textReminderItemDetails.setTextColor(
                    getThemeColor(
                        requireContext(), com.google.android.material.R.attr.colorOnSurface
                    )
                )
                if (reminder.type == ReminderType.EXERCISE) {
                    reminderBinding.buttonStartReminder.setOnClickListener {
                        val bundle = Bundle().apply {
                            putString("preselectedExerciseId", reminder.id)
                        }
                        if (findNavController().currentDestination?.id == R.id.home) {
                            findNavController().navigate(R.id.logExerciseFragment, bundle)
                        }
                    }
                    reminderBinding.buttonStartReminder.isVisible = true
                } else {
                    reminderBinding.buttonStartReminder.isVisible = false
                }
                bind.layoutReminders.addView(reminderBinding.root)
            }
        }
    }

    private fun updateSingleMealTime(mealBinding: ItemHomeMealBinding, time: String?) {
        return
        if (!time.isNullOrEmpty()) {
            mealBinding.textMealReminderTime.text = time
            mealBinding.textMealReminderTime.isVisible = true
        } else {
            mealBinding.textMealReminderTime.isVisible = false
        }
    }

    private fun setMainProgress(
        textView: TextView, indicator: LinearProgressIndicator, consumed: Int, target: Int
    ) {
        val consumedVal = consumed
        val targetVal = target

        val progressPercent =
            if (targetVal > 0) (consumedVal * 100 / targetVal).coerceIn(0, 100) else 0
        indicator.setProgressCompat(progressPercent, true)


        val remaining = targetVal - consumedVal
        val exceededBy = consumedVal - targetVal

        when {
            targetVal <= 0 -> {
                textView.text = getString(R.string.meal_calories_target_not_set)
                textView.setTextColor(
                    getThemeColor(
                        requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant
                    )
                )
                indicator.setIndicatorColor(
                    getThemeColor(
                        requireContext(), com.google.android.material.R.attr.colorOutlineVariant
                    )
                )

            }

            consumedVal == 0 -> {
                textView.text = getString(R.string.meal_calories_target_only_format, targetVal)
                textView.setTextColor(
                    getThemeColor(
                        requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant
                    )
                )
                indicator.setIndicatorColor(
                    getThemeColor(
                        requireContext(), com.google.android.material.R.attr.colorPrimary
                    )
                )
            }

            remaining in 1..50 -> { // Slightly under or met
                textView.text = getString(R.string.yetarli)
                textView.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.green)
                )
                indicator.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.green))
            }

            remaining > 50 -> { // Still some way to go
                textView.text = getString(R.string.meal_calories_remaining_format, remaining)
                textView.setTextColor(
                    getThemeColor(
                        requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant
                    )
                )
                indicator.setIndicatorColor(
                    getThemeColor(
                        requireContext(), com.google.android.material.R.attr.colorPrimary
                    )
                )
            }

            exceededBy in 1..100 -> { // Slightly over
                textView.text =
                    getString(R.string.bugunga_yetarli) // "Today is enough" or similar, perhaps a bit too much
                textView.setTextColor(
                    ContextCompat.getColor(
                        requireContext(), R.color.green
                    )
                ) // Warning color
                indicator.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.green))
            }

            exceededBy > 100 -> { // Significantly over
                textView.text = "Bugun ko'proq ovqat yedingiz"
                textView.setTextColor(
                    getThemeColor(
                        requireContext(), com.google.android.material.R.attr.colorError
                    )
                )
                indicator.setIndicatorColor(
                    getThemeColor(
                        requireContext(), com.google.android.material.R.attr.colorError
                    )
                )
            }

            else -> { // Exactly met or very slightly under (covered by remaining >= 0)
                textView.text = getString(R.string.meal_calories_format, consumedVal, targetVal)
                textView.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.green)
                )
                indicator.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.green))
            }
        }
    }

    private fun updateUiWithState(bind: FragmentHomeBinding, state: HomeUiState) {

        setMainProgress(
            bind.textConsumedCalories,
            bind.progressCalories,
            state.consumedCalories,
            state.targetCalories
        )
        bind.textConsumed.text =
            getString(R.string.meal_calories_format, state.consumedCalories, state.targetCalories)

        bind.textTargetWater.text =
            "${state.targetWaterMl / 1000f} litr = ${state.targetWaterMl / 250} stakan = ${state.targetWaterMl / 180} piyola suv iching"

        updateSingleMealTime(bind.layoutBreakfast, state.mealReminderTimes["breakfast"])
        updateSingleMealTime(bind.layoutLunch, state.mealReminderTimes["lunch"])
        updateSingleMealTime(bind.layoutDinner, state.mealReminderTimes["dinner"])
        updateSingleMealTime(bind.layoutSnack, state.mealReminderTimes["snack"])

        updateProgress(
            indicator = bind.progressWater,
            textView = bind.textConsumedWater,
            consumed = state.consumedWaterMl,
            target = state.targetWaterMl,
            unitStringResource = R.string.unit_ml_drunk,
            treatExceedingAsError = false
        )
        updateProgress(
            indicator = bind.progressExercise,
            textView = bind.textBurnedCalories,
            consumed = state.burnedCalories,
            target = state.targetCaloriesBurned,
            unitStringResource = R.string.unit_kcal_burned_exercise,
            treatExceedingAsError = false
        )
        bind.textBurnedCalories.text = getString(
            R.string.meal_calories_format, state.burnedCalories, state.targetCaloriesBurned
        )
        updateMealProgress(
            bind.layoutBreakfast.textMealCalories,
            state.mealConsumedCalories["breakfast"],
            state.mealCalorieDistribution["breakfast"]
        )
        updateMealProgress(
            bind.layoutLunch.textMealCalories,
            state.mealConsumedCalories["lunch"],
            state.mealCalorieDistribution["lunch"]
        )
        updateMealProgress(
            bind.layoutDinner.textMealCalories,
            state.mealConsumedCalories["dinner"],
            state.mealCalorieDistribution["dinner"]
        )
        updateMealProgress(
            bind.layoutSnack.textMealCalories,
            state.mealConsumedCalories["snack"],
            state.mealCalorieDistribution["snack"]
        )
        displayLoggedItems(bind, state.dailyLogData)
        displayEnabledExerciseReminders(bind, state.enabledExerciseReminders)

        state.streakInfo?.let { streak ->
            if (streak.currentStreak > 0) {
                bind.streakDisplayLayout.isVisible = true
                bind.textStreakCount.text = streak.currentStreak.toString()
                bind.textStreakCount.setTextColor(
                    ContextCompat.getColor(
                        requireContext(), R.color.yellow
                    )
                )
                bind.imageStreakIcon.imageTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.yellow))
            } else {
                bind.streakDisplayLayout.isVisible = false
            }
        } ?: run {
            bind.streakDisplayLayout.isVisible = false
        }

        // Weight Goal Section UI Update
        bind.cardWeightGoalProgressSection.isVisible = state.showWeightGoalSection
        if (state.showWeightGoalSection) {
            bind.textCurrentWeightOnGoalCard.text =
                String.format(Locale.US, "%.1f kg", state.currentWeightDisplay)
            bind.textInitialWeightGoalLabel.text =
                String.format(Locale.US, "%.1f kg", state.initialWeightForGoalDisplay)
            bind.textTargetWeightGoalLabel.text =
                String.format(Locale.US, "%.1f kg", state.targetWeightDisplay)
            bind.progressWeightGoalBar.progress = state.goalProgressPercent
            bind.textWeightLostOnGoal.text =
                String.format(Locale.US, "%.1f kg", state.weightLostDisplay)
            bind.textWeightRemainingOnGoal.text =
                String.format(Locale.US, "%.1f kg", state.weightRemainingDisplay)

            val sdf = SimpleDateFormat("dd MMMM, yyyy", Locale("uz", "UZ"))
            bind.textGoalStartDateOnGoal.text =
                "Boshlangan sana: ${sdf.format(Date(state.goalStartDateDisplay))}"
            bind.textActiveDaysOnGoal.text = "Aktiv kunlar: ${state.activeDays} kun"

            bind.textTimeline1OnGoal.text = state.estimatedTimelineStrings.getOrNull(0) ?: ""
            bind.textTimeline3OnGoal.text = state.estimatedTimelineStrings.getOrNull(2) ?: ""

            bind.textTimeline1OnGoal.isVisible = state.estimatedTimelineStrings.isNotEmpty()
            bind.textTimeline3OnGoal.isVisible = state.estimatedTimelineStrings.size > 2
        }
    }

    private fun updateProgress(
        indicator: LinearProgressIndicator,
        textView: TextView,
        consumed: Int,
        target: Int,
        unitStringResource: Int,
        treatExceedingAsError: Boolean
    ) {
        val progressPercent = if (target > 0) (consumed * 100 / target).coerceIn(0, 150) else 0
        indicator.setProgressCompat(progressPercent.coerceAtMost(100), true)
        if (unitStringResource == R.string.unit_ml_drunk) {
            textView.text = getString(unitStringResource, consumed / 250)
        } else {
            textView.text = getString(unitStringResource, consumed)
        }

        val colorAttr: Int
        val colorRes: Int?

        if (target <= 0) {
            colorAttr = com.google.android.material.R.attr.colorOnSurface
            colorRes = null
        } else if (consumed >= target) {
            colorAttr =
                com.google.android.material.R.attr.colorPrimary // Keep primary for met or exceeded
            colorRes = R.color.green // For text
        } else {
            colorAttr = com.google.android.material.R.attr.colorPrimary
            colorRes = null
        }

        indicator.setIndicatorColor(getThemeColor(requireContext(), colorAttr))
        if (colorRes != null) {
            textView.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        } else {
            textView.setTextColor(
                getThemeColor(
                    requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant
                )
            )
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionState = ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionState == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    0 // Use a defined request code
                )
            }
        }
    }

    private fun updateMealProgress(textView: TextView, consumed: Int?, target: Int?) {
        val consumedVal = consumed ?: 0
        val targetVal = target ?: 0

        val remaining = targetVal - consumedVal
        val exceededBy = consumedVal - targetVal

        when {
            targetVal <= 0 -> {
                textView.text = getString(R.string.meal_calories_target_not_set)
                textView.setTextColor(
                    getThemeColor(
                        requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant
                    )
                )
            }

            consumedVal == 0 -> {
                textView.text = getString(R.string.meal_calories_target_only_format, targetVal)
                textView.setTextColor(
                    getThemeColor(
                        requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant
                    )
                )
            }

            remaining in 1..50 -> {
                textView.text = getString(R.string.yetarli)
                textView.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.green)
                )
            }

            remaining > 50 -> {
                textView.text = getString(R.string.meal_calories_remaining_format, remaining)
                textView.setTextColor(
                    getThemeColor(
                        requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant
                    )
                )
            }

            exceededBy in 1..100 -> {
                textView.text = getString(R.string.bugunga_yetarli)
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
            }

            exceededBy > 100 -> {
                textView.text = getString(R.string.meal_calories_greatly_exceeded_format)
                textView.setTextColor(
                    getThemeColor(
                        requireContext(), com.google.android.material.R.attr.colorError
                    )
                )
            }

            else -> { // Exactly met
                textView.text = getString(R.string.meal_calories_format, consumedVal, targetVal)
                textView.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.green)
                )
            }
        }
    }


    private fun displayLoggedItems(bind: FragmentHomeBinding, logs: DailyLogData?) {
        // Nonushta loglari
        return
        bind.layoutBreakfast.layoutMealLogs.removeAllViews()
        val breakfastLogs = logs?.foodLogs?.values?.filter { it.mealType == "breakfast" }
            ?.sortedByDescending { it.timestamp as? Long ?: 0L }
        if (!breakfastLogs.isNullOrEmpty()) {
            breakfastLogs.forEach { log ->
                bind.layoutBreakfast.layoutMealLogs.addView(
                    createLogTextView(
                        formatFoodLogForDisplay(log), log
                    )
                )
            }
            bind.layoutBreakfast.layoutMealLogs.isVisible = true
        } else {
            bind.layoutBreakfast.layoutMealLogs.isVisible = false
        }

        // Tushlik loglari
        bind.layoutLunch.layoutMealLogs.removeAllViews()
        val lunchLogs = logs?.foodLogs?.values?.filter { it.mealType == "lunch" }
            ?.sortedByDescending { it.timestamp as? Long ?: 0L }
        if (!lunchLogs.isNullOrEmpty()) {
            lunchLogs.forEach { log ->
                bind.layoutLunch.layoutMealLogs.addView(
                    createLogTextView(
                        formatFoodLogForDisplay(
                            log
                        ), log
                    )
                )
            }
            bind.layoutLunch.layoutMealLogs.isVisible = true
        } else {
            bind.layoutLunch.layoutMealLogs.isVisible = false
        }

        // Kechki ovqat loglari
        bind.layoutDinner.layoutMealLogs.removeAllViews()
        val dinnerLogs = logs?.foodLogs?.values?.filter { it.mealType == "dinner" }
            ?.sortedByDescending { it.timestamp as? Long ?: 0L }
        if (!dinnerLogs.isNullOrEmpty()) {
            dinnerLogs.forEach { log ->
                bind.layoutDinner.layoutMealLogs.addView(
                    createLogTextView(
                        formatFoodLogForDisplay(
                            log
                        ), log
                    )
                )
            }
            bind.layoutDinner.layoutMealLogs.isVisible = true
        } else {
            bind.layoutDinner.layoutMealLogs.isVisible = false
        }

        // Tamaddi loglari
        bind.layoutSnack.layoutMealLogs.removeAllViews()
        val snackLogs = logs?.foodLogs?.values?.filter { it.mealType == "snack" }
            ?.sortedByDescending { it.timestamp as? Long ?: 0L }
        if (!snackLogs.isNullOrEmpty()) {
            snackLogs.forEach { log ->
                bind.layoutSnack.layoutMealLogs.addView(
                    createLogTextView(
                        formatFoodLogForDisplay(
                            log
                        ), log
                    )
                )
            }
            bind.layoutSnack.layoutMealLogs.isVisible = true
        } else {
            bind.layoutSnack.layoutMealLogs.isVisible = false
        }

        // Mashq loglari
        bind.layoutExerciseLogs.removeAllViews()
        val exerciseLogsList =
            logs?.exerciseLogs?.values?.sortedByDescending { it.timestamp as? Long ?: 0L }
        if (!exerciseLogsList.isNullOrEmpty()) {
            exerciseLogsList.forEach { log ->
                bind.layoutExerciseLogs.addView(
                    createLogTextView(
                        formatExerciseLogForDisplay(log), log
                    )
                )
            }
            bind.layoutExerciseLogs.isVisible = true
            bind.textNoExerciseLogs.isVisible = false
        } else {
            bind.layoutExerciseLogs.isVisible = false
            bind.textNoExerciseLogs.isVisible = true
        }
    }

    private fun formatFoodLogForDisplay(log: FoodLog): String {
        val time = (log.timestamp as? Long)?.let { logTimeFormat.format(java.util.Date(it)) } ?: ""
        return "$time - ${log.description.take(25)}${if (log.description.length > 25) "..." else ""} (${log.calories} kal)"
    }

    private fun formatExerciseLogForDisplay(log: ExerciseLog): String {
        val time = (log.timestamp as? Long)?.let { logTimeFormat.format(java.util.Date(it)) } ?: ""
        val typeName = ExerciseType.getExerciseTypeById(log.activityType).name
        return "$time - $typeName (${log.durationMin} daq, ${log.caloriesBurned} kal)"
    }

    private fun createLogTextView(text: String, logData: Any): TextView {
        return TextView(requireContext(), null, 0, R.style.Text1_14).apply {
            this.text = text
            setTextColor(
                getThemeColor(
                    context, com.google.android.material.R.attr.colorOnSurfaceVariant
                )
            )
            setPadding(0, 4.dpToPx(), 0, 4.dpToPx())
            isClickable = true
            isFocusable = true
            setOnClickListener { showEditDeleteDialog(logData) }
        }
    }


    private fun navigateToLogFood(mealType: String, logToEdit: FoodLog? = null) {
        val bundle = Bundle().apply {
            putString("mealType", mealType)
            if (logToEdit != null) {
                putString("logToEditJson", Gson().toJson(logToEdit))
            }
        }
        if (findNavController().currentDestination?.id == R.id.home) {
            findNavController().navigate(R.id.logFoodFragment, bundle)
        }
    }

    private fun showEditDeleteDialog(logData: Any) {
        val logName: String
        val deleteAction: () -> Unit
        var canEdit = false
        var editAction: () -> Unit = {}

        when (logData) {
            is FoodLog -> {
                logName =
                    logData.description.take(30) + if (logData.description.length > 30) "..." else ""
                deleteAction = { viewModel.deleteFoodLog(logData) }
                canEdit = true
                editAction = { navigateToLogFood(logData.mealType, logData) }
            }

            is WaterLog -> {
                logName = "${logData.amountMl} ml ${getString(R.string.unit_water)}"
                deleteAction = { viewModel.deleteWaterLog(logData) }
            }

            is ExerciseLog -> {
                val exerciseType = ExerciseType.getExerciseTypeById(logData.activityType)
                logName =
                    "${exerciseType.name} (${logData.durationMin} ${getString(R.string.unit_minute)})"
                deleteAction = { viewModel.deleteExerciseLog(logData) }
                canEdit = true
                editAction = {
                    val bundle = Bundle().apply {
                        putString("logToEditJson", Gson().toJson(logData))
                    }
                    if (findNavController().currentDestination?.id == R.id.home) {
                        findNavController().navigate(R.id.logExerciseFragment, bundle)
                    }
                }
            }

            else -> return
        }
        val options = if (canEdit) {
            arrayOf(getString(R.string.edit), getString(R.string.delete))
        } else {
            arrayOf(getString(R.string.delete))
        }
        AlertDialog.Builder(requireContext()).setTitle(logName).setItems(options) { dialog, which ->
            when (options[which]) {
                getString(R.string.edit) -> {
                    editAction.invoke(); dialog.dismiss()
                }

                getString(R.string.delete) -> {
                    showDeleteConfirmationDialog(logName, deleteAction); dialog.dismiss()
                }
            }
        }.setNegativeButton(getString(R.string.cancel), null).show()
    }

    private fun showDeleteConfirmationDialog(itemName: String, deleteAction: () -> Unit) {
        AlertDialog.Builder(requireContext()).setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_message_generic, itemName))
            .setPositiveButton(R.string.yes_delete) { dialog, _ ->
                deleteAction.invoke(); dialog.dismiss()
            }.setNegativeButton(R.string.no, null).show()
    }

    private fun getThemeColor(context: Context, attr: Int): Int {
        return MaterialColors.getColor(
            context, attr, ContextCompat.getColor(context, R.color.grey)
        )
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}