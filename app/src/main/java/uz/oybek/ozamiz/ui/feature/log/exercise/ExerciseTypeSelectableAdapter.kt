package uz.oybek.ozamiz.ui.feature.log.exercise

import android.content.Context // Import Context
import android.graphics.Color // Import Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors // Import MaterialColors
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.databinding.ItemExerciseTypeSelectableBinding
import uz.oybek.ozamiz.ui.feature.onboarding.exercise.ExerciseType

class ExerciseTypeSelectableAdapter(
    private val listener: (ExerciseType) -> Unit
) : ListAdapter<ExerciseType,
        ExerciseTypeSelectableAdapter.ViewHolder>(ExerciseTypeDiffCallback()) {

    private var selectedPosition = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExerciseTypeSelectableBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position == selectedPosition)
    }

    fun setSelectedPosition(position: Int) {
        if (position == selectedPosition || position < 0 || position >= itemCount) return

        val previousSelectedPosition = selectedPosition
        selectedPosition = position

        if (previousSelectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousSelectedPosition)
        }
        notifyItemChanged(selectedPosition)
    }

    // Helper function to resolve theme color attributes
    private fun getThemeColor(context: Context, attr: Int): Int {
        // Use MaterialColors library for compatibility
        return MaterialColors.getColor(context, attr, Color.GRAY) // Provide a fallback color
    }

    inner class ViewHolder(private val binding: ItemExerciseTypeSelectableBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val previouslySelected = selectedPosition
                    selectedPosition = position

                    if (previouslySelected != RecyclerView.NO_POSITION) {
                        notifyItemChanged(previouslySelected)
                    }
                    notifyItemChanged(selectedPosition)
                    listener(getItem(position))
                }
            }
        }

        fun bind(item: ExerciseType, isSelected: Boolean) {
            binding.ivExerciseIconSelectable.setImageResource(item.iconRes)
            binding.tvExerciseNameSelectable.text = item.name
            binding.radioButtonSelected.isChecked = isSelected

            val context = itemView.context

            // Resolve theme attributes for colors
            val strokeColorAttr = if (isSelected) com.google.android.material.R.attr.colorPrimarySurface else com.google.android.material.R.attr.colorOutlineVariant
            val backgroundColorAttr = if (isSelected) com.google.android.material.R.attr.colorPrimaryContainer else android.R.attr.colorBackground // Use standard background for non-selected

            val resolvedStrokeColor = getThemeColor(context, strokeColorAttr)
            val resolvedBackgroundColor = getThemeColor(context, backgroundColorAttr)

            binding.cardExerciseType.strokeColor = resolvedStrokeColor
            binding.constraintLayoutExerciseType.setBackgroundColor(resolvedBackgroundColor) // Set background color

            // Adjust text color for better contrast on selection
            val textColorAttr = if (isSelected) com.google.android.material.R.attr.colorOnPrimaryContainer else com.google.android.material.R.attr.colorOnSurface
            binding.tvExerciseNameSelectable.setTextColor(getThemeColor(context, textColorAttr))

            binding.cardExerciseType.cardElevation = if (isSelected) 2.dpToPx() else 0.dpToPx() // Optional elevation
        }

        private fun Float.dpToPx(): Float = this * itemView.context.resources.displayMetrics.density
    }

    class ExerciseTypeDiffCallback : DiffUtil.ItemCallback<ExerciseType>() {
        override fun areItemsTheSame(oldItem: ExerciseType, newItem: ExerciseType): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExerciseType, newItem: ExerciseType): Boolean {
            return oldItem == newItem
        }
    }
}

private fun Int.dpToPx(): Float {
    val context = appContext
    val density = context.resources.displayMetrics.density
    return this * density
}
