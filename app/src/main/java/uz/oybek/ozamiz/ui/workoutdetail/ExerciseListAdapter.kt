package uz.oybek.ozamiz.ui.workoutdetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.workouts.Exercise
import uz.oybek.ozamiz.databinding.ItemExerciseDetailBinding
import uz.oybek.ozamiz.ui.discover.ExerciseDiffCallback
import uz.oybek.ozamiz.ui.loadPhoto
import java.util.Locale

class ExerciseListAdapter(
    private val onItemClick: (Exercise) -> Unit,
    private val showInfoButton: Boolean = true // Default true
) : ListAdapter<Exercise, ExerciseListAdapter.ViewHolder>(ExerciseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExerciseDetailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemExerciseDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Set click listener on the root item view
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(bindingAdapterPosition))
                }
            }
            // Optionally, keep the info icon click if it should do the same action
            // binding.imageViewInfoIcon.setOnClickListener {
            //     if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
            //         onItemClick(getItem(bindingAdapterPosition))
            //     }
            // }
        }

        fun bind(exercise: Exercise) {
            binding.textViewExerciseName.text = exercise.name
            // Load image or GIF into the ShapeableImageView
            binding.imageViewExerciseIcon.loadPhoto(exercise.mediaUrl ?: R.drawable.placeholder_exercise) // Use placeholder if URL is null

            // Format duration or repetitions text
            val durationText = if (exercise.durationSeconds != null && exercise.durationSeconds!! > 0) {
                val minutes = exercise.durationSeconds!! / 60
                val seconds = exercise.durationSeconds!! % 60
                if (minutes > 0) {
                    String.format(Locale.getDefault(), "%d:%02d min", minutes, seconds)
                } else {
                    String.format(Locale.getDefault(), "00:%02d sek", seconds)
                }
            } else if (exercise.repetitions != null && exercise.repetitions!! > 0) {
                itemView.context.getString(R.string.repetitions_format, exercise.repetitions)
            } else {
                "" // Show nothing if neither is set
            }
            binding.textViewExerciseDurationReps.text = durationText

            // Control visibility of the info icon based on the adapter's parameter
            binding.imageViewInfoIcon.visibility = if(showInfoButton) View.VISIBLE else View.GONE
        }
    }
}