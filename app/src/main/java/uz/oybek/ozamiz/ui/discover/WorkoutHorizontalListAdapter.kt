package uz.oybek.ozamiz.ui.discover // Yoki mos paket

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.workouts.Workout
import uz.oybek.ozamiz.databinding.ItemWorkoutCardBinding
import uz.oybek.ozamiz.ui.loadPhoto

// Gorizontal ro'yxat uchun adapter (item_workout_card.xml ishlatadi)
class WorkoutHorizontalListAdapter(
    private val onItemClick: (Workout) -> Unit
) : ListAdapter<Workout, WorkoutHorizontalListAdapter.ViewHolder>(WorkoutDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWorkoutCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemWorkoutCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(workout: Workout) {
            binding.textViewWorkoutCardTitle.text = workout.name
            binding.imageViewWorkoutCard.loadPhoto(workout.imageUrl) // Placeholder kerak bo'lsa qo'shing
        }
    }
}