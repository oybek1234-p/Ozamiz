package uz.oybek.ozamiz.ui

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.MainActivity
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.info.ExerciseLog
import uz.oybek.ozamiz.data.info.FoodLog
import uz.oybek.ozamiz.data.info.WaterLog
import uz.oybek.ozamiz.data.upcomingMeal
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.utils.NutritionCalculator
import uz.oybek.ozamiz.databinding.FragmentHomeBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.dialogs.StreakInfoBottomSheetDialog
import uz.oybek.ozamiz.ui.getinfo.ExerciseType
import uz.oybek.ozamiz.ui.home.DateScrollerAdapter
import uz.oybek.ozamiz.ui.home.HomeUiState
import uz.oybek.ozamiz.ui.home.HomeViewModel
import uz.oybek.ozamiz.ui.home.WarningAdapter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var dateAdapter: DateScrollerAdapter
    private lateinit var warningAdapter: WarningAdapter
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
        setupDateScroller(bind)
        setupRecyclerViews(bind)
        setupClickListeners(bind)
        observeViewModel(bind)

        bind.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
            bind.swipeRefreshLayout.isRefreshing = false
        }
        bind.swipeRefreshLayout.setColorSchemeColors(
            getThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimaryFixed)
        )
    }

    private fun setupRecyclerViews(bind: FragmentHomeBinding) {
        warningAdapter = WarningAdapter()
        bind.recyclerViewWarnings.adapter = warningAdapter
        bind.recyclerViewWarnings.layoutManager = LinearLayoutManager(context)
        bind.recyclerViewWarnings.itemAnimator = null
    }

    private fun setupDateScroller(bind: FragmentHomeBinding) {
        dateAdapter = DateScrollerAdapter { date ->
            viewModel.onDateSelected(date)
        }
        bind.dateScrollerRecyclerView.apply {
            adapter = dateAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            itemAnimator = null
        }
    }

    override fun onResume() {
        super.onResume()
        requestNotificationPermission()
        if (!initialWeightLogCheckDone && LocalUser.user.valid && LocalUser.user.name.isNotEmpty()) {
            view?.post {
                checkAndNavigateForWeightLog()
            }
        }
    }

    private fun checkAndNavigateForWeightLog() {
        if (!isAdded || !isVisible || view == null || findNavController().currentDestination?.id != R.id.home) {
            return
        }
        initialWeightLogCheckDone = true

        val user = LocalUser.user
        val lastLogTime = user.lastWeightLogTimestamp
        val daysSinceLastLog = if (lastLogTime > 0) {
            TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastLogTime)
        } else {
            (MainActivity.getWeightLogIntervalDays() + 1).toLong()
        }

        if (daysSinceLastLog >= MainActivity.getWeightLogIntervalDays()) {
            lifecycleScope.launch {
                if (findNavController().currentDestination?.id == R.id.home) {
                }
            }
        }
    }

    private fun setupToolbar(bind: FragmentHomeBinding) {
        bind.streakDisplayLayout.setOnClickListener {
            viewModel.uiState.value.streakInfo?.let { streak ->
                if (isAdded) {
                    StreakInfoBottomSheetDialog.newInstance(streak)
                        .show(childFragmentManager, StreakInfoBottomSheetDialog.TAG)
                }
            }
        }
    }

    private fun setupClickListeners(bind: FragmentHomeBinding) {
        bind.buttonAddWater.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.home) {
                navigate(R.id.logWaterDialog)
            }
        }
        bind.buttonAddExerciseLog.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.home) {
                val args = Bundle().apply {
                    putLong("selectedDateTimestamp", viewModel.uiState.value.selectedDate.time)
                }
                navigate(R.id.logExerciseFragment, args)
            }
        }
        bind.addFood.setOnClickListener {
            val args = Bundle().apply {
                putLong("selectedDateTimestamp", viewModel.uiState.value.selectedDate.time)
                putString("mealType", LocalUser.user.upcomingMeal())
            }
            navigate(R.id.logFoodFragment, args)
        }

        bind.mainProgress.buttonToggleDetails.setOnClickListener {
            val group = bind.mainProgress.macroDetailsGroup
            val button = it as TextView
            if (group.isVisible) {
                group.isVisible = false
                button.text = "Ko'proq"
            } else {
                group.isVisible = true
                button.text = "Yopish"
            }
        }
    }

    private fun observeViewModel(bind: FragmentHomeBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiState.collectLatest { state ->
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
                        warningAdapter.submitList(state.warnings)
                        bind.recyclerViewWarnings.isVisible = state.warnings.isNotEmpty()
                    }
                }

                launch {
                    viewModel.uiState.map { it.datesForScroller }.distinctUntilChanged()
                        .collectLatest { dates ->
                            if (dates.isNotEmpty()) {
                                dateAdapter.submitList(dates)
                            }
                        }
                }

                launch {
                    viewModel.uiState.map { it.selectedDate }.distinctUntilChanged()
                        .collectLatest { selectedDate ->
                            dateAdapter.setSelectedDate(selectedDate)

                            val today = Calendar.getInstance().time
                            bind.toolbar.title =
                                if (DateUtils.isSameDay(selectedDate.time, today.time)) {
                                    "Bugun"
                                } else {
                                    SimpleDateFormat("d MMMM", Locale("uz", "UZ")).format(
                                        selectedDate
                                    )
                                }

                            bind.dateScrollerRecyclerView.post {
                                val selectedIndex =
                                    viewModel.uiState.value.datesForScroller.indexOfFirst {
                                        DateUtils.isSameDay(
                                            it.time, selectedDate.time
                                        )
                                    }
                                if (selectedIndex != -1) {
                                    (bind.dateScrollerRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                                        selectedIndex, bind.root.width / 2 - 32.dpToPx()
                                    )
                                }
                            }
                        }
                }
            }
        }
    }

    // ... HomeFragment ichidagi boshqa kodlar ...

    private fun updateUiWithState(bind: FragmentHomeBinding, state: HomeUiState) {
        bind.mainProgress.apply {
            val totalCalories = state.targetCalories.takeIf { it > 0 } ?: 1
            val conCalories = state.consumedCalories
            val diff = conCalories - totalCalories
            val context = requireContext()

            consumedCal.text = "$conCalories yeyildi"

            if (diff > 0) {
                textViewCalories.text = "Meyordan oshdi"
                textViewCalories.setTextColor(ContextCompat.getColor(context, R.color.rejected))
                remainingCal.text = "Meyor: $totalCalories"
                overflowCal.text = "$diff ortiqcha"
                overflowCal.isVisible = true

                progressIndicatorCalories.progress = 100
                progressIndicatorCalories.setIndicatorColor(ContextCompat.getColor(context, R.color.rejected))

            } else {
                // Me'yorga yetmaganda
                textViewCalories.text = "${abs(diff)} qoldi"
                textViewCalories.setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, 0))
                remainingCal.text = "Meyor: $totalCalories"
                overflowCal.isVisible = false

                val progressPercent = (conCalories * 100 / totalCalories).coerceIn(0, 100)
                progressIndicatorCalories.setProgressCompat(progressPercent, true)
                progressIndicatorCalories.setIndicatorColor(ContextCompat.getColor(context, R.color.green))
            }

            // Makronutrientlar qismi o'zgarishsiz qoladi
            val macros = NutritionCalculator.calculateMacrosByPercent(totalCalories = totalCalories)
            updateMacroProgress(
                progressIndicatorFats, textViewFatsValue, state.fat.toInt(), macros["fat"], "g"
            )
            updateMacroProgress(
                progressIndicatorCarbs,
                textViewCarbsValue,
                state.carbs.toInt(),
                macros["carbs"],
                "g"
            )
            updateMacroProgress(
                progressIndicatorProtein,
                textViewProteinValue,
                state.protein.toInt(),
                macros["protein"],
                "g"
            )
        }

        // Suv va Mashqlar qismi o'zgarishsiz qoladi
        updateProgress(
            bind.progressWater,
            bind.textConsumedWater,
            state.consumedWaterMl,
            state.targetWaterMl,
            R.string.unit_ml_drunk,
            "%1\$d ml ichildi"
        )
        val leftDrink = state.targetWaterMl - state.consumedWaterMl
        bind.textConsumedWater.text = if (leftDrink <= 0) "Ajoyib! Yetarli suv ichdingiz" else "${state.consumedWaterMl} ml ichildi / ${state.targetWaterMl} ml"

        updateProgress(
            bind.progressExercise,
            bind.textBurnedCalories,
            state.burnedCalories,
            state.targetCaloriesBurned,
            R.string.unit_kcal_burned_exercise,
            "%1\$d kaloriya yoqildi"
        )
        bind.textBurnedCalories.text = "${state.burnedCalories} harakat"

        state.streakInfo?.let { streak ->
            bind.streakDisplayLayout.isVisible = streak.currentStreak > 0
            bind.textStreakCount.text = streak.currentStreak.toString()
            bind.textStreakCount.setTextColor(
                ContextCompat.getColor(
                    requireContext(), R.color.yellow
                )
            )
            bind.imageStreakIcon.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.yellow))
        } ?: run {
            bind.streakDisplayLayout.isVisible = false
        }
    }

    private fun updateMacroProgress(
        indicator: LinearProgressIndicator,
        textView: TextView,
        consumed: Int,
        target: Int?,
        unit: String
    ) {
        val currentTarget = target?.takeIf { it > 0 } ?: 1
        textView.text = "$consumed / $currentTarget $unit"
        val progress = (consumed * 100 / currentTarget).coerceIn(0, 100)
        indicator.setProgressCompat(progress, true)

        if (consumed > currentTarget) {
            indicator.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.rejected))
        } else {
        }
    }


    private fun updateProgress(
        indicator: LinearProgressIndicator,
        textView: TextView,
        consumed: Int,
        target: Int,
        @Suppress("SameParameterValue") unitStringResource: Int,
        format: String
    ) {
        val progressPercent = if (target > 0) (consumed * 100 / target).coerceIn(0, 150) else 0
        indicator.setProgressCompat(progressPercent.coerceAtMost(100), true)
        textView.text = String.format(format, consumed, target)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_DENIED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0
                )
            }
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
                        putLong("selectedDateTimestamp", viewModel.uiState.value.selectedDate.time)
                    }
                    if (findNavController().currentDestination?.id == R.id.home) {
                        navigate(R.id.logExerciseFragment, bundle)
                    }
                }
            }

            else -> return
        }
        val options =
            if (canEdit) arrayOf(getString(R.string.edit), getString(R.string.delete)) else arrayOf(
                getString(R.string.delete)
            )
        AlertDialog.Builder(requireContext()).setTitle(logName).setItems(options) { dialog, which ->
            when (options[which]) {
                getString(R.string.edit) -> editAction.invoke()
                getString(R.string.delete) -> showDeleteConfirmationDialog(logName, deleteAction)
            }
            dialog.dismiss()
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

    private fun navigateToLogFood(mealType: String, logToEdit: FoodLog? = null) {
        val bundle = Bundle().apply {
            putString("mealType", mealType)
            putLong("selectedDateTimestamp", viewModel.uiState.value.selectedDate.time)
            if (logToEdit != null) {
                putString("logToEditJson", Gson().toJson(logToEdit))
            }
        }
        if (findNavController().currentDestination?.id == R.id.home) {
            navigate(R.id.logFoodFragment, bundle)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun getThemeColor(context: Context, attr: Int): Int =
        MaterialColors.getColor(context, attr, 0)
}