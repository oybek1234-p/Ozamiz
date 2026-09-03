package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.color.MaterialColors
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.user.model.info.Gender
import uz.oybek.ozamiz.databinding.FragmentGoalPaceOnboardBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment
import kotlin.math.roundToInt

class GoalPaceOnboardFragment : OnBoardingChildFragment<FragmentGoalPaceOnboardBinding>() {

    private var selectedPace: Int = 500

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentGoalPaceOnboardBinding {
        return FragmentGoalPaceOnboardBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentGoalPaceOnboardBinding) {
        val isGain = user.goal == "gain_weight"
        selectedPace = if (isGain) 400 else 500

        bind.cardPaceNormal.setOnClickListener {
            selectedPace = if (user.goal == "gain_weight") 400 else 500
            updateUI(bind)
        }

        bind.cardPaceFast.setOnClickListener {
            selectedPace = if (user.goal == "gain_weight") 600 else 700
            updateUI(bind)
        }

        bind.buttonContinue.setOnClickListener {
            savePaceAndTargetCalories()
            moveToNextScreen()
        }

        updateUI(bind)
    }

    private fun updateUI(bind: FragmentGoalPaceOnboardBinding) {
        val selectedBg = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceContainerHigh, 0)
        val unselectedBg = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceContainerLowest, 0)
        val selectedStroke = MaterialColors.getColor(requireContext(), androidx.appcompat.R.attr.colorPrimary, 0)
        val unselectedStroke = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOutlineVariant, 0)
        val onSurfaceColor = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurface, 0)
        val onSurfaceVariant = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
        val surfaceContainerHighest = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceContainerHighest, 0)

        val isGain = user.goal == "gain_weight"
        val isNormalSelected = selectedPace == 500 || selectedPace == 400

        bind.textSpeedTitle.text = if (isGain) "Semirish tezligini tanlang" else "Ozish tezligini tanlang"

        // Normal card
        val normalBg = if (isNormalSelected) selectedBg else unselectedBg
        bind.cardPaceNormal.setCardBackgroundColor(ColorStateList.valueOf(normalBg))
        bind.cardPaceNormal.strokeWidth = if (isNormalSelected) 2.dpToPx() else 1.dpToPx()
        bind.cardPaceNormal.strokeColor = if (isNormalSelected) selectedStroke else unselectedStroke
        bind.cardPaceNormal.cardElevation = if (isNormalSelected) 2.dpToPx().toFloat() else 0f
        bind.framePaceNormalIcon.backgroundTintList = ColorStateList.valueOf(if (isNormalSelected) surfaceContainerHighest else unselectedBg)
        bind.iconPaceNormal.imageTintList = ColorStateList.valueOf(if (isNormalSelected) onSurfaceColor else onSurfaceVariant)
        bind.textPaceNormalLabel.setTextColor(if (isNormalSelected) onSurfaceColor else onSurfaceVariant)
        bind.textPaceNormalRate.setTextColor(if (isNormalSelected) onSurfaceColor else onSurfaceVariant)

        // Fast card
        val fastBg = if (!isNormalSelected) selectedBg else unselectedBg
        bind.cardPaceFast.setCardBackgroundColor(ColorStateList.valueOf(fastBg))
        bind.cardPaceFast.strokeWidth = if (!isNormalSelected) 2.dpToPx() else 1.dpToPx()
        bind.cardPaceFast.strokeColor = if (!isNormalSelected) selectedStroke else unselectedStroke
        bind.cardPaceFast.cardElevation = if (!isNormalSelected) 2.dpToPx().toFloat() else 0f
        bind.framePaceFastIcon.backgroundTintList = ColorStateList.valueOf(if (!isNormalSelected) surfaceContainerHighest else unselectedBg)
        bind.iconPaceFast.imageTintList = ColorStateList.valueOf(if (!isNormalSelected) onSurfaceColor else onSurfaceVariant)
        bind.textPaceFastLabel.setTextColor(if (!isNormalSelected) onSurfaceColor else onSurfaceVariant)
        bind.textPaceFastRate.setTextColor(if (!isNormalSelected) onSurfaceColor else onSurfaceVariant)

        bind.textPaceNormalRate.text = if (isGain) "+1.6 kg / oy" else "-1.8 kg / oy"
        bind.textPaceFastRate.text = if (isGain) "+2.4 kg / oy" else "-2.8 kg / oy"

        // Calculations
        val tdee = DietController.calculateTDEE(user).roundToInt()
        val deltaCals = if (selectedPace > 0) selectedPace else (if (isGain) 400 else 500)
        val minCals = if (user.gender == Gender.MALE) 1200 else 1000

        val targetCals = if (isGain) {
            tdee + deltaCals
        } else {
            maxOf(tdee - deltaCals, minCals)
        }

        val macros = if (isGain) {
            DietController.calculateMacrosByPercent(targetCals, proteinPercent = 0.25, fatPercent = 0.25, carbPercent = 0.50)
        } else {
            DietController.calculateMacrosByPercent(targetCals, proteinPercent = 0.35, fatPercent = 0.25, carbPercent = 0.40)
        }

        bind.editTargetCalories.setText(targetCals.toString())
        bind.editTargetProtein.setText(macros.protein.toString())
        bind.editTargetCarbs.setText(macros.carbs.toString())
        bind.editTargetFat.setText(macros.fat.toString())

        val actualDeficit = if (isGain) deltaCals else maxOf(0, tdee - targetCals)
        val weeklyKg = if (isGain) {
            if (deltaCals == 600) 0.6 else 0.4
        } else {
            val calcW = (actualDeficit * 7.0) / 7700.0
            if (calcW > 0) calcW else 0.5
        }

        val (weeklyStr, monthlyStr) = if (isGain) {
            if (deltaCals == 600) Pair("0.6", "2.4") else Pair("0.4", "1.6")
        } else {
            val calcW = (actualDeficit * 7.0) / 7700.0
            val calcM = (actualDeficit * 30.0) / 7700.0
            Pair(
                String.format(java.util.Locale.US, "%.1f", calcW),
                String.format(java.util.Locale.US, "%.1f", calcM)
            )
        }

        val durationStr = getDurationText(user.weight, user.targetWeight ?: user.weight, weeklyKg)

        if (isGain) {
            bind.textEnergySummary.text =
                "• Sizning tanangiz kuniga taxminan $tdee kkal energiya sarflaydi,\n\n• $deltaCals kkal ortiqcha yesak vazn yig'ish boshlanadi.\n\n• Natijada haftasiga $weeklyStr kg, oyiga $monthlyStr kg semirasiz.\n\n• Maqsadga esa $durationStr erishasiz."
        } else {
            val deficitAmount = tdee - targetCals
            val defVal = if (deficitAmount > 0) deficitAmount else deltaCals
            bind.textEnergySummary.text =
                "• Sizning tanangiz kuniga taxminan $tdee kkal energiya sarflaydi,\n\n• $defVal kkal kamroq ovqat yeymiz natijada haftasiga $weeklyStr kg, oyiga $monthlyStr kg ozasiz.\n\n• Maqsadga esa $durationStr erishasiz."
        }
    }

    private fun savePaceAndTargetCalories() {
        val bind = bindingT ?: return
        val isGain = user.goal == "gain_weight"
        val tdee = DietController.calculateTDEE(user).roundToInt()
        val deltaCals = if (selectedPace > 0) selectedPace else (if (isGain) 400 else 500)
        val minCals = if (user.gender == Gender.MALE) 1200 else 1000

        val calcCals = if (isGain) (tdee + deltaCals) else maxOf(tdee - deltaCals, minCals)

        val customCals = bind.editTargetCalories.text.toString().toIntOrNull() ?: calcCals
        val customProtein = bind.editTargetProtein.text.toString().toIntOrNull()
        val customCarbs = bind.editTargetCarbs.text.toString().toIntOrNull()
        val customFat = bind.editTargetFat.text.toString().toIntOrNull()

        LocalUser.update {
            this.targetCalories = customCals
            DietController.updateTargetValues(this)

            if (customProtein != null && customProtein > 0) this.targetProtein = customProtein
            if (customCarbs != null && customCarbs > 0) this.targetCarbs = customCarbs
            if (customFat != null && customFat > 0) this.targetFat = customFat
        }
    }

    private fun getDurationText(currentWeight: Double, targetWeight: Double, weeklyKg: Double): String {
        val diff = kotlin.math.abs(currentWeight - targetWeight)
        if (diff <= 0.1 || weeklyKg <= 0.0) return "erishgansiz"
        val weeksNeeded = diff / weeklyKg
        val daysNeeded = (weeksNeeded * 7).roundToInt()

        val months = daysNeeded / 30
        val remainingDays = daysNeeded % 30
        return when {
            months > 0 && remainingDays > 0 -> "$months oy $remainingDays kunda"
            months > 0 -> "$months oyda"
            else -> "$daysNeeded kunda"
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
