package uz.oybek.ozamiz.ui.feature.log.exercise

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.logs.model.UserExercise
import uz.oybek.ozamiz.databinding.ItemHomeExerciseLogBinding

class HomeEmptyExerciseAdapter(
    private val onItemClick: (UserExercise) -> Unit
) : ListAdapter<UserExercise, HomeEmptyExerciseAdapter.ViewHolder>(DiffCallback) {

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

        fun bind(exercise: UserExercise) {
            binding.textEmoji.text = exercise.emoji
            binding.textExerciseName.text = exercise.exerciseName
            binding.textBenefit.text = exercise.benefit
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<UserExercise>() {
        override fun areItemsTheSame(oldItem: UserExercise, newItem: UserExercise): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: UserExercise, newItem: UserExercise): Boolean {
            return oldItem == newItem
        }
    }
}