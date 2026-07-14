package uz.oybek.ozamiz.ui.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.logs.model.ExerciseLog
import uz.oybek.ozamiz.data.logs.model.UserExercise
import uz.oybek.ozamiz.databinding.FragmentHomeBinding
import uz.oybek.ozamiz.databinding.ItemHomeExerciseLogBinding

class HomeExerciseLogAdapter(
    private val onItemClick: (ExerciseLog) -> Unit,
    private val onDeleteClick: (ExerciseLog) -> Unit
) : ListAdapter<ExerciseLog, HomeExerciseLogAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeExerciseLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemHomeExerciseLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(log: ExerciseLog) {
            val exerciseType = try {
                UserExercise.valueOf(log.exerciseType)
            } catch (e: Exception) {
                null
            }

            binding.textEmoji.text = exerciseType?.emoji ?: "💪"
            binding.textExerciseName.text = exerciseType?.exerciseName ?: log.exerciseType
            binding.textBenefit.text = exerciseType?.benefit ?: ""
            binding.textCalories.isVisible = true
            binding.textCalories.text = itemView.context.getString(
                R.string.unit_kcal_burned, log.caloriesBurned
            ) + " · ${log.durationMin} min"
            binding.btnDelete.setOnClickListener {
                onDeleteClick(log)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ExerciseLog>() {
        override fun areItemsTheSame(oldItem: ExerciseLog, newItem: ExerciseLog): Boolean {
            return oldItem.logId == newItem.logId
        }

        override fun areContentsTheSame(oldItem: ExerciseLog, newItem: ExerciseLog): Boolean {
            return oldItem == newItem
        }
    }
}