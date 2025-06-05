package uz.oybek.ozamiz.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.admin
import uz.oybek.ozamiz.data.workouts.Exercise
import uz.oybek.ozamiz.databinding.ItemExerciseSelectableBinding
import uz.oybek.ozamiz.ui.loadPhoto
import java.util.*

interface ExerciseItemAdminActions {
    fun onEditExercise(exercise: Exercise)
    fun onDeleteExercise(exercise: Exercise)
}

class SelectableExerciseAdapter(
    private val onSelectionChanged: (List<String>) -> Unit,
    private val adminActionsListener: ExerciseItemAdminActions?
) : ListAdapter<Exercise, SelectableExerciseAdapter.ViewHolder>(ExerciseDiffCallback()) {

    private val selectedIds = mutableSetOf<String>()
    private var originalList: List<Exercise> = emptyList()
    private val isAdmin = LocalUser.user.admin()

    fun setInitialSelection(ids: List<String>) {
        selectedIds.clear()
        selectedIds.addAll(ids)
        notifyDataSetChanged()
    }

    fun submitOriginalList(list: List<Exercise>) {
        originalList = list.sortedBy { it.name } // Ensure consistent order
        submitList(originalList)
    }

    fun filter(query: String?) {
        val filteredList = if (query.isNullOrBlank()) {
            originalList
        } else {
            originalList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.description?.contains(query, ignoreCase = true) == true
            }
        }
        submitList(filteredList)
    }

    fun getSelectedIds(): List<String> {
        // Return selected IDs in the order they appear in the original (sorted) list
        return originalList.filter { selectedIds.contains(it.id) }.map { it.id }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExerciseSelectableBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemExerciseSelectableBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    toggleSelection(getItem(bindingAdapterPosition))
                }
            }
            binding.buttonEditExercise.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    adminActionsListener?.onEditExercise(getItem(bindingAdapterPosition))
                }
            }
            binding.buttonDeleteExercise.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    adminActionsListener?.onDeleteExercise(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(exercise: Exercise) {
            binding.textViewExerciseSelectableName.text = exercise.name
            binding.imageViewExerciseSelectableIcon.loadPhoto(exercise.mediaUrl ?: R.drawable.placeholder_exercise)

            val details = exercise.getDurationOrRepsText()
            binding.textViewExerciseSelectableDetails.text = details
            binding.textViewExerciseSelectableDetails.isVisible = details.isNotEmpty()

            binding.checkBoxExerciseSelected.isChecked = selectedIds.contains(exercise.id)

            binding.layoutAdminActions.isVisible = isAdmin
            binding.buttonEditExercise.isVisible = isAdmin
            binding.buttonDeleteExercise.isVisible = isAdmin
        }
    }

    private fun toggleSelection(exercise: Exercise) {
        if (selectedIds.contains(exercise.id)) {
            selectedIds.remove(exercise.id)
        } else {
            selectedIds.add(exercise.id)
        }
        notifyItemChanged(currentList.indexOf(exercise))
        onSelectionChanged(getSelectedIds()) // Use sorted list of IDs
    }

    class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem == newItem
        }
    }
}