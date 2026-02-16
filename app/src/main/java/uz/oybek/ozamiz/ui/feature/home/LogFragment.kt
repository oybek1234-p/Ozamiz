package uz.oybek.ozamiz.ui.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.databinding.DialogLogWaterBinding
import uz.oybek.ozamiz.databinding.FragmentHomeBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.log.exercise.HomeEmptyExerciseAdapter
import uz.oybek.ozamiz.ui.feature.log.food.LogFoodFragment
import java.util.Calendar
import java.util.Date
import kotlin.math.absoluteValue

class LogFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by activityViewModels()
    private var dateAdapter: DateScrollerAdapter? = null

    private lateinit var exerciseAdapter: HomeExerciseLogAdapter


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


        exerciseAdapter = HomeExerciseLogAdapter { log ->

        }
        bind.recyclerExerciseLogs.apply {
            adapter = exerciseAdapter
            layoutManager = LinearLayoutManager(context)
            isNestedScrollingEnabled = false
        }

        // Quick Add Exercise Buttons
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
        val prefs = requireContext().getSharedPreferences("promo_prefs", android.content.Context.MODE_PRIVATE)
        val clickCount = prefs.getInt("add_food_click_count", 0)
        val showPromo = clickCount < 3

        bind.cardAddFoodPromo.isVisible = showPromo
        bind.buttonAddFood.isVisible = !showPromo

        fun onAddFoodClick() {
            if (showPromo) {
                prefs.edit().putInt("add_food_click_count", clickCount + 1).apply()
            }
            LogFoodFragment.navigate(
                navController = findNavController(),
                timeStamp = currentDate(),
                meal = getCurrentMealType()
            )
        }

        bind.cardAddFoodPromo.setOnClickListener { onAddFoodClick() }
        bind.btnPromoAddFood.setOnClickListener { onAddFoodClick() }
        bind.buttonAddFood.setOnClickListener { onAddFoodClick() }

        bind.cardMenus.setOnClickListener {
            navigate(R.id.action_home_to_menuListFragment)
        }

        bind.mainProgress.btnHowItWorks.setOnClickListener {
            navigate(R.id.action_home_to_calorieInfoFragment)
        }

        // Macro info buttons
        // Macro info click listeners
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
            showAddWaterDialog()
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
                            updateUi(state, true)
                        }
                    }
                }
                
                launch {
                     viewModel.showRateAppEvent.collect {
                         RateAppSheet.newInstance()
                             .show(childFragmentManager, RateAppSheet.TAG)
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
        binding.toolbar.title = if (DateUtils.isSameDay(state.selectedDate, Date())) {
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

        // Streak UI Binding
        val streakInfo = state.streakInfo
        if (streakInfo != null) {
            binding.streakDisplayLayout.isVisible = true
            binding.textStreakCount.text = streakInfo.currentStreak.toString()
            
            // Basic pulse animation if not animated yet (could use flag)
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
                     .show(childFragmentManager, uz.oybek.ozamiz.ui.feature.streak.StreakInfoSheet.TAG)
            }
        } else {
            binding.streakDisplayLayout.isVisible = false
        }
        
        // Goal text is now part of the header or hidden, so we can remove/comment out this line if the view is gone
        // binding.textWaterGoal.text = "Kunlik me'yor: $goalLiters litr"


        binding.mainProgress.apply {
            textCaloriesRemainingCount.text = state.caloriesRemaining.absoluteValue.toString()
            textEatenCount.text = state.caloriesConsumed.toString()
            textBurnedCount.text = state.exerciseBurnt.toString()

            progressCaloriesCircular.max = state.caloriesTarget
            val isMoreConsume = state.caloriesRemaining < 0
            textCaloriesRemainingLabel.setText(
                if (isMoreConsume) R.string.oshdi else R.string.calories_remaining
            )
            progressCaloriesCircular.setProgressCompat(
                state.caloriesConsumed.absoluteValue,
                animate
            )
            val primaryColor = com.google.android.material.color.MaterialColors.getColor(requireView(), androidx.appcompat.R.attr.colorPrimary)
            val redColor = ContextCompat.getColor(requireContext(), R.color.red)
            val trackColor = com.google.android.material.color.MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorSurfaceContainerHigh)
            val onSurfaceColor = com.google.android.material.color.MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorOnSurface)

            progressCaloriesCircular.setIndicatorColor(if (isMoreConsume) redColor else primaryColor)
            progressCaloriesCircular.trackColor = trackColor
            textCaloriesRemainingCount.setTextColor(if (isMoreConsume) redColor else onSurfaceColor)

            val orangeColor = ContextCompat.getColor(requireContext(), R.color.orange)
            val yellowColor = ContextCompat.getColor(requireContext(), R.color.yellow)
            val greenColor = ContextCompat.getColor(requireContext(), R.color.green)

            val proteinExceeded = state.proteinConsumed > state.proteinTarget
            val proteinRemaining = state.proteinTarget - state.proteinConsumed
            progressProtein.max = if (proteinExceeded) state.proteinConsumed else state.proteinTarget
            progressProtein.setProgressCompat(state.proteinConsumed, animate)
            progressProtein.setIndicatorColor(if (proteinExceeded) redColor else orangeColor)
            progressProtein.trackColor = trackColor
            textProteinValue.setTextColor(if (proteinExceeded) redColor else onSurfaceColor)
            textProteinValue.text = if (proteinExceeded)
                getString(R.string.macro_exceeded_format, -proteinRemaining)
            else
                getString(R.string.macro_remaining_format, proteinRemaining)

            val fatExceeded = state.fatConsumed > state.fatTarget
            val fatRemaining = state.fatTarget - state.fatConsumed
            progressFat.max = if (fatExceeded) state.fatConsumed else state.fatTarget
            progressFat.setProgressCompat(state.fatConsumed, animate)
            progressFat.setIndicatorColor(if (fatExceeded) redColor else yellowColor)
            progressFat.trackColor = trackColor
            textFatValue.setTextColor(if (fatExceeded) redColor else onSurfaceColor)
            textFatValue.text = if (fatExceeded)
                getString(R.string.macro_exceeded_format, -fatRemaining)
            else
                getString(R.string.macro_remaining_format, fatRemaining)

            textFatValue.text = if (fatExceeded)
                getString(R.string.macro_exceeded_format, -fatRemaining)
            else
                getString(R.string.macro_remaining_format, fatRemaining)

            // Carbs
            val carbsExceeded = state.carbsConsumed > state.carbsTarget
            val carbsRemaining = state.carbsTarget - state.carbsConsumed
            progressCarbs.max = if (carbsExceeded) state.carbsConsumed else state.carbsTarget
            progressCarbs.setProgressCompat(state.carbsConsumed, animate)
            progressCarbs.setIndicatorColor(if (carbsExceeded) redColor else greenColor)
            progressCarbs.trackColor = trackColor
            textCarbsValue.setTextColor(if (carbsExceeded) redColor else onSurfaceColor)
            textCarbsValue.text = if (carbsExceeded)
                getString(R.string.macro_exceeded_format, -carbsRemaining)
            else
                getString(R.string.macro_remaining_format, carbsRemaining)
        }
        
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
    }



    private fun showAddWaterDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogLogWaterBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        fun addWater(amount: Int) {
            viewModel.addWater(amount)
            dialog.dismiss()
        }
        dialogBinding.cardCup100.setOnClickListener { addWater(100) }
        dialogBinding.cardGlass250.setOnClickListener { addWater(250) }
        dialogBinding.cardBottle500.setOnClickListener { addWater(500) }
        dialogBinding.cardBottle1000.setOnClickListener { addWater(1000) }

        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
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



    private fun Float.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}