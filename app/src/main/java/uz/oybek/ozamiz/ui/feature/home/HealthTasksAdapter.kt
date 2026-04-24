package uz.oybek.ozamiz.ui.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.databinding.ItemHealthTaskBinding
import uz.oybek.ozamiz.data.health.model.HealthTaskStatus

class HealthTasksAdapter(
    private val onToggle: (String, Boolean) -> Unit
) : ListAdapter<HealthTaskStatus, HealthTasksAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHealthTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemHealthTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(status: HealthTaskStatus) {
            val task = status.task
            binding.textTaskEmoji.text = task.emoji
            binding.textTaskTitle.text = task.title
            binding.textTaskBenefit.text = task.benefit
            binding.textTaskDescription.text = task.description
            
            val count = status.totalCompletions ?: 0
            if (count > 0) {
                binding.textTaskCount.isVisible = true
                binding.textTaskCount.text = "\uD83D\uDD25 $count marta bajarilgan"
            } else {
                binding.textTaskCount.isVisible = false
            }

            binding.btnCompleteTask.isVisible = !status.isCompleted
            binding.layoutCompleted.isVisible = status.isCompleted

            binding.btnCompleteTask.setOnClickListener {
                onToggle(task.id, true)
            }
            
            binding.layoutCompleted.setOnClickListener {
                onToggle(task.id, false)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<HealthTaskStatus>() {
        override fun areItemsTheSame(oldItem: HealthTaskStatus, newItem: HealthTaskStatus): Boolean {
            return oldItem.task.id == newItem.task.id
        }

        override fun areContentsTheSame(oldItem: HealthTaskStatus, newItem: HealthTaskStatus): Boolean {
            return oldItem == newItem
        }
    }
}
