package uz.oybek.ozamiz.ui.discover // Yoki mos paket

import androidx.recyclerview.widget.DiffUtil
import uz.oybek.ozamiz.data.workouts.Exercise
import uz.oybek.ozamiz.data.workouts.Workout

// Workout uchun DiffUtil
class WorkoutDiffCallback : DiffUtil.ItemCallback<Workout>() {
    override fun areItemsTheSame(oldItem: Workout, newItem: Workout): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Workout, newItem: Workout): Boolean {
        return oldItem == newItem
    }
}

// Exercise uchun DiffUtil
class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
    override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
        return oldItem == newItem
    }
}