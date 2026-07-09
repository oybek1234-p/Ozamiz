package uz.oybek.ozamiz.ui.feature.profile

import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.food.DietController.DEFICIT_CALORIES
import uz.oybek.ozamiz.data.food.DietController.SURPLUS_CALORIES

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import uz.oybek.ozamiz.databinding.FragmentCalorieSettingsBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.R
import com.google.android.material.color.MaterialColors
import kotlin.math.roundToInt

class CalorieSettingsFragment : BaseFragment<FragmentCalorieSettingsBinding>() {

    private val viewModel: ProfileViewModel by activityViewModels()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCalorieSettingsBinding {
        return FragmentCalorieSettingsBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentCalorieSettingsBinding) {
        bind.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val user = viewModel.user
        val targetCals = if (user.targetCalories > 0) user.targetCalories else DietController.calculateTargetCalories(user)

        bind.editCalories.setText(targetCals.toString())
        bind.editProtein.setText(user.targetProtein.toString())
        bind.editFat.setText(user.targetFat.toString())
        bind.editCarbs.setText(user.targetCarbs.toString())

        val tdee = DietController.calculateTDEE(user).roundToInt()

        if (user.goal == "gain_weight") {
            selectGoal(bind, false, tdee)
        } else {
            selectGoal(bind, true, tdee)
        }

        bind.cardLoseWeight.setOnClickListener {
            selectGoal(bind, true, tdee)
            updateInputs(bind, "lose_weight")
        }

        bind.cardGainWeight.setOnClickListener {
            selectGoal(bind, false, tdee)
            updateInputs(bind, "gain_weight")
        }

        bind.btnSave.setOnClickListener {
            val caloriesText = bind.editCalories.text.toString()
            val proteinText = bind.editProtein.text.toString()
            val fatText = bind.editFat.text.toString()
            val carbsText = bind.editCarbs.text.toString()

            val calories = caloriesText.toIntOrNull() ?: 0
            val protein = proteinText.toIntOrNull() ?: 0
            val fat = fatText.toIntOrNull() ?: 0
            val carbs = carbsText.toIntOrNull() ?: 0

            if (calories <= 0) {
                Toast.makeText(requireContext(), "Kaloriya miqdorini kiriting", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val targetGoal = if (bind.cardLoseWeight.strokeWidth == 3.dpToPx()) "lose_weight" else "gain_weight"

            viewModel.updateCalorieSettings(calories, protein, fat, carbs, targetGoal)

            Toast.makeText(requireContext(), "O'zgarishlar saqlandi", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    private fun updateInputs(bind: FragmentCalorieSettingsBinding, goal: String) {
        val user = viewModel.user.copy(goal = goal)
        DietController.updateTargetValues(user)

        bind.editCalories.setText(user.targetCalories.toString())
        bind.editProtein.setText(user.targetProtein.toString())
        bind.editFat.setText(user.targetFat.toString())
        bind.editCarbs.setText(user.targetCarbs.toString())
    }

    private fun selectGoal(bind: FragmentCalorieSettingsBinding, isLoseWeight: Boolean, tdee: Int) {
        val primaryColor = MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, 0)
        val surfaceColor = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceContainer, 0)
        val outlineColor = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOutlineVariant, 0)
        val primaryContainer = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimaryContainer, 0)

        bind.cardLoseWeight.strokeWidth = if (isLoseWeight) 3.dpToPx() else 1.dpToPx()
        bind.cardLoseWeight.strokeColor = if (isLoseWeight) primaryColor else outlineColor
        bind.cardLoseWeight.setCardBackgroundColor(if (isLoseWeight) primaryContainer else surfaceColor)

        bind.cardGainWeight.strokeWidth = if (!isLoseWeight) 3.dpToPx() else 1.dpToPx()
        bind.cardGainWeight.strokeColor = if (!isLoseWeight) primaryColor else outlineColor
        bind.cardGainWeight.setCardBackgroundColor(if (!isLoseWeight) primaryContainer else surfaceColor)

        bind.cardGoalInfo.isVisible = true
        if (isLoseWeight) {
            bind.textGoalInfo.text = "Sizning normal kaloriyangiz: $tdee kkal. Ozish uchun kunlik -${DEFICIT_CALORIES} kkal defitsit qilindi."
        } else {
            bind.textGoalInfo.text = "Sizning normal kaloriyangiz: $tdee kkal. Semirish uchun kunlik +${SURPLUS_CALORIES} kkal qo'shimcha kaloriya qo'shildi."
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}