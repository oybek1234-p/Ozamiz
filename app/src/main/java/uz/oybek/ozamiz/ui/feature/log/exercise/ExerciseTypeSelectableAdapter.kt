package uz.oybek.ozamiz.ui.feature.log.exercise

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.logs.model.UserExercise
import uz.oybek.ozamiz.databinding.ItemExerciseTypeSelectableBinding

class ExerciseTypeSelectableAdapter(
    private val listener: (UserExercise) -> Unit
) : ListAdapter<UserExercise, ExerciseTypeSelectableAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExerciseTypeSelectableBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemExerciseTypeSelectableBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    listener(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(item: UserExercise) {
            binding.tvExerciseNameSelectable.text = item.exerciseName
            binding.ivExerciseIconSelectable.text = item.emoji
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