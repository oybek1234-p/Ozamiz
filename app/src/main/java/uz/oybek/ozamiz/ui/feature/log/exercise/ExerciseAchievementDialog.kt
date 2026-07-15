package uz.oybek.ozamiz.ui.feature.log.exercise

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import uz.oybek.ozamiz.databinding.DialogExerciseAchievementBinding
import uz.oybek.ozamiz.ui.utils.ExerciseCalorieConverter

class ExerciseAchievementDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val binding = DialogExerciseAchievementBinding.inflate(inflater, container, false)
        val calories = arguments?.getInt("calories") ?: 0

        binding.textBurnedCalories.text = "$calories kkal"
        binding.textFoodEquivalent.text = ExerciseCalorieConverter.getEquivalentFood(requireContext(), calories)
        binding.textFatBurned.text = "${ExerciseCalorieConverter.getFatBurnedGram(calories)} g"

        binding.btnAjoyib.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    companion object {
        fun newInstance(calories: Int): ExerciseAchievementDialog {
            val args = Bundle().apply {
                putInt("calories", calories)
            }
            val fragment = ExerciseAchievementDialog()
            fragment.arguments = args
            return fragment
        }
    }
}
