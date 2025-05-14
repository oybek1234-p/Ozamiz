package uz.oybek.ozamiz.ui.discover // Yoki mos paket

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.workouts.Workout
import uz.oybek.ozamiz.databinding.ItemWorkoutSearchBinding
import uz.oybek.ozamiz.ui.loadPhoto

class WorkoutSearchAdapter(
    private val onItemClick: (Workout) -> Unit
) : ListAdapter<Workout, WorkoutSearchAdapter.ViewHolder>(WorkoutDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWorkoutSearchBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemWorkoutSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(workout: Workout) {
            binding.textViewWorkoutSearchName.text = workout.name
            binding.imageViewWorkoutSearch.loadPhoto(workout.imageUrl)

            val details = mutableListOf<String>()
            workout.durationMinutes?.let { details.add("$it min") }
            workout.difficultyLevel?.let { details.add(it) }
            binding.textViewWorkoutSearchDetails.text = details.joinToString(" • ")
            binding.textViewWorkoutSearchDetails.visibility = if (details.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        }
    }
}