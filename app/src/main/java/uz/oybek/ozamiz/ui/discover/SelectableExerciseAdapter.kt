package uz.oybek.ozamiz.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.workouts.Exercise
import uz.oybek.ozamiz.databinding.ItemExerciseSelectableBinding
import uz.oybek.ozamiz.ui.loadPhoto
import java.util.*

class SelectableExerciseAdapter(
    private val onSelectionChanged: (List<String>) -> Unit // Tanlangan ID'lar listini qaytaradi
) : ListAdapter<Exercise, SelectableExerciseAdapter.ViewHolder>(ExerciseDiffCallback()) {

    private val selectedIds = mutableSetOf<String>()
    private var originalList: List<Exercise> = emptyList() // Qidiruv uchun to'liq list

    fun setInitialSelection(ids: List<String>) {
        selectedIds.clear()
        selectedIds.addAll(ids)
        notifyDataSetChanged() // Boshlang'ich tanlovni ko'rsatish uchun
    }

    fun submitOriginalList(list: List<Exercise>) {
        originalList = list
        submitList(list) // Adapterga ham berish
    }

    fun filter(query: String?) {
        val filteredList = if (query.isNullOrBlank()) {
            originalList
        } else {
            originalList.filter {
                it.name.contains(query, ignoreCase = true)
                // TODO: Tavsif bo'yicha ham qidirish qo'shilishi mumkin
            }
        }
        submitList(filteredList)
    }

    fun getSelectedIds(): List<String> {
        // Tanlangan ID'larni original listdagi tartibda qaytarish (agar tartib muhim bo'lsa)
        // Hozircha oddiy set ni listga aylantiramiz
        return selectedIds.toList()
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
        }

        fun bind(exercise: Exercise) {
            binding.textViewExerciseSelectableName.text = exercise.name
            binding.imageViewExerciseSelectableIcon.loadPhoto(exercise.mediaUrl ?: R.drawable.placeholder_exercise)

            val details = if (exercise.durationSeconds != null && exercise.durationSeconds!! > 0) {
                val minutes = exercise.durationSeconds!! / 60
                val seconds = exercise.durationSeconds!! % 60
                if (minutes > 0) String.format(Locale.getDefault(), "%d:%02d min", minutes, seconds)
                else String.format(Locale.getDefault(), "00:%02d sek", seconds)
            } else if (exercise.repetitions != null && exercise.repetitions!! > 0) {
                 itemView.context.getString(R.string.repetitions_format, exercise.repetitions)
            } else { "" }
            binding.textViewExerciseSelectableDetails.text = details

            binding.checkBoxExerciseSelected.isChecked = selectedIds.contains(exercise.id)
        }
    }

    private fun toggleSelection(exercise: Exercise) {
        if (selectedIds.contains(exercise.id)) {
            selectedIds.remove(exercise.id)
        } else {
            selectedIds.add(exercise.id)
        }
        notifyItemChanged(currentList.indexOf(exercise)) // Faqat o'zgargan itemni yangilash
        onSelectionChanged(selectedIds.toList()) // Listenerga xabar berish
    }

    // DiffUtil (ExerciseDiffCallback oldingi javobda berilgan, qayta ishlating)
     class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean {
            return oldItem == newItem // Agar Exercise data class bo'lsa
        }
    }
}