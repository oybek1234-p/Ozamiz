package uz.oybek.ozamiz.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
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
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.info.DailyLogData
import uz.oybek.ozamiz.data.info.ExerciseLog
import uz.oybek.ozamiz.data.info.FoodLog
import uz.oybek.ozamiz.data.info.WaterLog
import uz.oybek.ozamiz.databinding.FragmentHomeBinding
import uz.oybek.ozamiz.databinding.ItemHomeMealBinding
import uz.oybek.ozamiz.databinding.ItemHomeReminderBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.getinfo.ExerciseType
import uz.oybek.ozamiz.ui.getinfo.ReminderItem
import uz.oybek.ozamiz.ui.getinfo.ReminderType
import uz.oybek.ozamiz.ui.home.HomeUiState
import uz.oybek.ozamiz.ui.home.HomeViewModel

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by viewModels()

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
            // Trigger data refresh in ViewModel if necessary,
            // otherwise rely on existing observers.
            // For now, just stop the indicator.
            bind.swipeRefreshLayout.isRefreshing = false
        }
        // Set swipe refresh colors using theme attributes
        bind.swipeRefreshLayout.setColorSchemeColors(
            getThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimary),
            getThemeColor(requireContext(), com.google.android.material.R.attr.colorSecondary),
            getThemeColor(requireContext(), com.google.android.material.R.attr.colorTertiary)
        )
    }

    private fun setupToolbar(bind: FragmentHomeBinding) {
        // Toolbar settings if needed
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
        mealBinding: ItemHomeMealBinding, // Receive the specific binding
        iconRes: Int, title: String, mealType: String
    ) {
        mealBinding.imageMealIcon.setImageResource(iconRes)
        mealBinding.textMealName.text = title
        // Click listeners
        mealBinding.buttonAddMeal.setOnClickListener { navigateToLogFood(mealType) }
        mealBinding.mealRootLayout.setOnClickListener { navigateToLogFood(mealType) }
        // Time will be set in updateUiWithState
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

        bind.textWaterInfo.isVisible = false
        bind.textExerciseInfo.isVisible = false

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
    }

    private fun observeUiState(bind: FragmentHomeBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.progressBarOverall.isVisible =
                        state.isLoading && state.dailyLogData == null
                    bind.contentScrollView.isVisible =
                        !state.isLoading || state.dailyLogData != null
                    bind.swipeRefreshLayout.isRefreshing =
                        state.isLoading && state.dailyLogData != null

                    state.errorMessage?.let {
                        Snackbar.make(bind.root, it, Snackbar.LENGTH_LONG).show()
                        viewModel.clearErrorMessage()
                    }

                    updateUiWithState(bind, state)
                }
            }
        }
    }

    private fun displayEnabledExerciseReminders(
        bind: FragmentHomeBinding, reminders: List<ReminderItem>
    ) {
        bind.layoutReminders.removeAllViews() // Clear previous reminders
        if (reminders.isEmpty()) {
            bind.textNoReminders.isVisible = true // Show "No reminders" text
            bind.layoutReminders.isVisible = false
        } else {
            bind.textNoReminders.isVisible = false // Hide "No reminders" text
            bind.layoutReminders.isVisible = true
            reminders.forEach { reminder -> // Iterate through only enabled exercise reminders
                val reminderBinding = ItemHomeReminderBinding.inflate(
                    LayoutInflater.from(requireContext()), bind.layoutReminders, false
                )

                reminderBinding.iconReminderItem.setImageResource(reminder.iconRes)

                reminderBinding.textReminderItemDetails.text = String.format(
                    getString(R.string.reminder_display_format), // "Name: HH:MM"
                    reminder.name, reminder.getTimeAsString() // Should format HH:MM
                )
                reminderBinding.textReminderItemDetails.setTextColor(
                    getThemeColor(
                        requireContext(), com.google.android.material.R.attr.colorOnSurface
                    )
                )

                // Set "Start" button listener
                reminderBinding.buttonStartReminder.setOnClickListener {
                    // Navigate to LogExerciseFragment with preselected ID
                    val bundle = Bundle().apply {
                        putString("preselectedExerciseId", reminder.id)
                    }
                    if (findNavController().currentDestination?.id == R.id.home) {
                        findNavController().navigate(R.id.logExerciseFragment, bundle)
                    }
                }

                bind.layoutReminders.addView(reminderBinding.root)
            }
        }
    }

    private fun updateSingleMealTime(mealBinding: ItemHomeMealBinding, time: String?) {
        if (!time.isNullOrEmpty()) {
            mealBinding.textMealReminderTime.text = time
            mealBinding.textMealReminderTime.isVisible = true
        } else {
            mealBinding.textMealReminderTime.isVisible = false
        }
    }


    private fun updateUiWithState(bind: FragmentHomeBinding, state: HomeUiState) {
        bind.textTargetCalories.text =
            getString(R.string.target_calories_format, state.targetCalories)
        bind.textTargetWater.text = "Kunlik maqsad: ${state.targetWaterMl} ml"
        bind.textTargetExercise.text =
            getString(R.string.target_exercise_format, state.targetCaloriesBurned)

        updateSingleMealTime(bind.layoutBreakfast, state.mealReminderTimes["breakfast"])
        updateSingleMealTime(bind.layoutLunch, state.mealReminderTimes["lunch"])
        updateSingleMealTime(bind.layoutDinner, state.mealReminderTimes["dinner"])
        updateSingleMealTime(bind.layoutSnack, state.mealReminderTimes["snack"])

        updateProgress(
            indicator = bind.progressCalories,
            textView = bind.textConsumedCalories,
            consumed = state.consumedCalories,
            target = state.targetCalories,
            unit = "kal yeyildi",
            treatExceedingAsError = true
        )

        updateProgress(
            indicator = bind.progressWater,
            textView = bind.textConsumedWater,
            consumed = state.consumedWaterMl,
            target = state.targetWaterMl,
            unit = "ml ichildi",
            treatExceedingAsError = false
        )
        updateProgress(
            indicator = bind.progressExercise,
            textView = bind.textBurnedCalories,
            consumed = state.burnedCalories,
            target = state.targetCaloriesBurned,
            unit = "kal energiya yoqildi",
            treatExceedingAsError = false
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
    }


    private fun updateProgress(
        indicator: LinearProgressIndicator,
        textView: TextView,
        consumed: Int,
        target: Int,
        unit: String,
        treatExceedingAsError: Boolean
    ) {
        val progressPercent = if (target > 0) (consumed * 100 / target).coerceIn(0, 150) else 0
        indicator.setProgressCompat(progressPercent.coerceAtMost(100), true)

        textView.text = "$consumed $unit"

        val colorAttr = when {
            target <= 0 -> com.google.android.material.R.attr.colorOnSurface
            consumed > target && treatExceedingAsError -> com.google.android.material.R.attr.colorError
            consumed >= target && !treatExceedingAsError -> com.google.android.material.R.attr.colorPrimary
            else -> com.google.android.material.R.attr.colorPrimary
        }
        indicator.setIndicatorColor(getThemeColor(requireContext(), colorAttr))
    }


    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionState = ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionState == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0
                )
            }
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        val packageName = requireContext().packageName
        val pm = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            AlertDialog.Builder(requireContext()).setTitle("Eslatmalar uchun Muhim Sozlama")
                .setMessage("Eslatmalarning o'z vaqtida va ishonchli ishlashi uchun, iltimos, ilovamiz uchun batareya optimallashtirishni o'chirib qo'ying. Bu sozlamani o'zgartirmasangiz, ba'zi eslatmalar kechikishi yoki umuman ishlamasligi mumkin.")
                .setPositiveButton("Sozlamalarga o'tish") { dialog, _ ->
                    try {
                        val intent = Intent().apply {
                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val intent = Intent().apply {
                                action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                            }
                            startActivity(intent)
                        } catch (e2: Exception) {
                            showToast("Batareya sozlamalarini ochishda xatolik. Iltimos, qo'lda sozlang.")
                        }
                    }
                    dialog.dismiss()
                }.setNegativeButton("Keyinroq") { dialog, _ ->
                    dialog.dismiss()
                    // Foydalanuvchiga bu sozlama muhimligini yana bir bor eslatish mumkin
                    showToast("Eslatmalar to'liq ishlamasligi mumkin.")
                }.setCancelable(false).show()
        } else {
            // Optimallashtirish allaqachon o'chirilgan
            // showToast("Batareya optimallashtirish allaqachon o'chirilgan.")
        }
    }

    private fun updateMealProgress(textView: TextView, consumed: Int?, target: Int?) {
        val consumedVal = consumed ?: 0
        val targetVal = target ?: 0
        textView.text = getString(R.string.meal_calories_format, consumedVal, targetVal)
        val textColorAttr = when {
            targetVal <= 0 -> com.google.android.material.R.attr.colorOnSurfaceVariant
            consumedVal - 50 > targetVal -> com.google.android.material.R.attr.colorError
            else -> com.google.android.material.R.attr.colorOnSurfaceVariant
        }
        textView.setTextColor(getThemeColor(requireContext(), textColorAttr))
    }

    private fun displayLoggedItems(bind: FragmentHomeBinding, logs: DailyLogData?) {
        return
        displayFoodLogs(bind.layoutBreakfast.layoutMealLogs,
            logs?.foodLogs?.values?.filter { it.mealType == "breakfast" })
        displayFoodLogs(bind.layoutLunch.layoutMealLogs,
            logs?.foodLogs?.values?.filter { it.mealType == "lunch" })
        displayFoodLogs(bind.layoutDinner.layoutMealLogs,
            logs?.foodLogs?.values?.filter { it.mealType == "dinner" })
        displayFoodLogs(bind.layoutSnack.layoutMealLogs,
            logs?.foodLogs?.values?.filter { it.mealType == "snack" })
        displayExerciseLogs(bind.layoutExerciseLogs, logs?.exerciseLogs?.values)
    }

    private fun displayFoodLogs(layout: LinearLayout, foodLogs: Collection<FoodLog>?) {
        layout.removeAllViews()
        val sortedLogs = foodLogs?.sortedBy { it.timestamp as? Long } ?: emptyList()
        sortedLogs.forEach { log ->
            val textView = createLogTextView(
                String.format(
                    getString(R.string.logged_item_food_format), log.description, log.calories
                ), log
            )
            layout.addView(textView)
        }
        layout.isVisible = sortedLogs.isNotEmpty()
    }

    // Updated displayExerciseLogs to show names instead of IDs
    private fun displayExerciseLogs(layout: LinearLayout, exerciseLogs: Collection<ExerciseLog>?) {
        layout.removeAllViews()
        val sortedLogs = exerciseLogs?.sortedBy { it.timestamp as? Long } ?: emptyList()
        sortedLogs.forEach { log ->
            // Get the display name from ExerciseType
            val exerciseType =
                ExerciseType.getExerciseTypeById(log.activityType) // Fetch type object
            val displayName = exerciseType.name // Use the name property

            val textView = createLogTextView(
                String.format(
                    getString(R.string.logged_item_exercise_format),
                    displayName, // Use display name here
                    log.durationMin,
                    log.caloriesBurned
                ), log
            )
            layout.addView(textView)
        }
        layout.isVisible = sortedLogs.isNotEmpty()
        binding.textNoExerciseLogs.isVisible = sortedLogs.isEmpty()
    }

    private fun createLogTextView(text: String, logData: Any): TextView {
        // Use a base style if defined, otherwise set attributes programmatically
        return TextView(requireContext(), null, 0, R.style.Text1_14).apply {
            this.text = text
            // Set text color using theme attribute
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

    override fun onResume() {
        super.onResume()
        requestNotificationPermission()
    }

    // Updated displayReminders to use ItemHomeReminderBinding
    private fun displayReminders(bind: FragmentHomeBinding, reminders: List<ReminderItem>) {
        bind.layoutReminders.removeAllViews() // Clear previous reminders
        if (reminders.isEmpty()) {
            bind.textNoReminders.isVisible = true
            bind.layoutReminders.isVisible = false
        } else {
            bind.textNoReminders.isVisible = false
            bind.layoutReminders.isVisible = true
            reminders.forEach { reminder ->
                // Inflate the item layout using binding
                val reminderBinding = ItemHomeReminderBinding.inflate(
                    LayoutInflater.from(requireContext()),
                    bind.layoutReminders,
                    false // Pass false to attachToRoot
                )

                reminderBinding.iconReminderItem.setImageResource(reminder.iconRes)
                // Use theme color for icon tint
                reminderBinding.iconReminderItem.setColorFilter(
                    getThemeColor(
                        requireContext(), com.google.android.material.R.attr.colorPrimary
                    )
                )

                reminderBinding.textReminderItemDetails.text = String.format(
                    getString(R.string.reminder_display_format),
                    reminder.name,
                    reminder.getTimeAsString() // Assumes ReminderItem has this helper method
                )
                // Set text color using theme attribute
                reminderBinding.textReminderItemDetails.setTextColor(
                    getThemeColor(
                        requireContext(), com.google.android.material.R.attr.colorOnSurface
                    )
                )


                reminderBinding.buttonStartReminder.setOnClickListener {
                    if (reminder.type == ReminderType.EXERCISE) {
                        val bundle = Bundle().apply {
                            putString("preselectedExerciseId", reminder.id)
                        }
                        if (findNavController().currentDestination?.id == R.id.home) {
                            findNavController().navigate(R.id.logExerciseFragment, bundle)
                        }
                    } else if (reminder.type == ReminderType.MEAL) {
                        // Navigate to log food for the specific meal type
                        navigateToLogFood(reminder.id)
                    } else {
                        showToast("${reminder.name} ${getString(R.string.reminder_started_placeholder)}")
                    }
                }

                // Add the inflated root view to the layout
                bind.layoutReminders.addView(reminderBinding.root)
            }
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
                    "${exerciseType.name} (${logData.durationMin} ${getString(R.string.unit_minute)})" // Use display name
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
                    editAction.invoke()
                    dialog.dismiss()
                }

                getString(R.string.delete) -> {
                    showDeleteConfirmationDialog(logName, deleteAction)
                    dialog.dismiss()
                }
            }
        }.setNegativeButton(getString(R.string.cancel), null).show()
    }

    private fun showDeleteConfirmationDialog(itemName: String, deleteAction: () -> Unit) {
        AlertDialog.Builder(requireContext()).setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_message_generic, itemName))
            .setPositiveButton(R.string.yes_delete) { dialog, _ ->
                deleteAction.invoke()
                dialog.dismiss()
            }.setNegativeButton(R.string.no, null).show()
    }

    // Helper to get color from theme attribute
    private fun getThemeColor(context: Context, attr: Int): Int {
        return MaterialColors.getColor(
            context, attr, ContextCompat.getColor(context, R.color.grey)
        ) // Fallback grey
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}