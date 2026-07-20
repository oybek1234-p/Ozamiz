package uz.oybek.ozamiz.ui.feature.exercises

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.ItemWeightLossExerciseBinding
import uz.oybek.ozamiz.ui.base.BaseAdapter
import uz.oybek.ozamiz.ui.utils.loadPhoto
import uz.oybek.ozamiz.ui.utils.visibleOrGone
import android.content.res.ColorStateList
import android.graphics.Color

class WeightLossExercisesAdapter : BaseAdapter<WeightLossExerciseUiModel, ItemWeightLossExerciseBinding>(
    R.layout.item_weight_loss_exercise,
    WeightLossExerciseDiffCallback()
) {

    override fun inflate(inflater: LayoutInflater, parent: ViewGroup): ItemWeightLossExerciseBinding {
        return ItemWeightLossExerciseBinding.inflate(inflater, parent, false)
    }

    override fun bind(holder: ViewHolder<*>, model: WeightLossExerciseUiModel, pos: Int) {
        val binding = holder.binding as ItemWeightLossExerciseBinding
        val context = binding.root.context
        val exercise = model.exercise

        binding.textExerciseName.text = exercise.name
        binding.textExerciseCategory.text = exercise.category
        binding.textCalories.text = context.getString(R.string.exerc_cal_format, exercise.calories)
        binding.textDuration.text = context.getString(R.string.exerc_sec_format, exercise.durationSeconds)

        val (diffText, diffColor) = when (exercise.difficulty) {
            "beginner" -> Pair(context.getString(R.string.boshlangich), Color.parseColor("#4CAF50"))
            "medium" -> Pair(context.getString(R.string.ortacha), Color.parseColor("#FF9800"))
            "hard" -> Pair(context.getString(R.string.qiyin), Color.parseColor("#BA1A1A"))
            else -> Pair(exercise.difficulty, Color.parseColor("#808080"))
        }

        binding.textDifficultyBadge.text = diffText
        binding.textDifficultyBadge.backgroundTintList = ColorStateList.valueOf(diffColor)

        binding.layoutLockOverlay.visibleOrGone(model.isLocked)

        binding.imageExercise.setImageResource(exercise.imageRes)
    }

    class WeightLossExerciseDiffCallback : DiffUtil.ItemCallback<WeightLossExerciseUiModel>() {
        override fun areItemsTheSame(oldItem: WeightLossExerciseUiModel, newItem: WeightLossExerciseUiModel): Boolean {
            return oldItem.exercise.id == newItem.exercise.id
        }

        override fun areContentsTheSame(oldItem: WeightLossExerciseUiModel, newItem: WeightLossExerciseUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
