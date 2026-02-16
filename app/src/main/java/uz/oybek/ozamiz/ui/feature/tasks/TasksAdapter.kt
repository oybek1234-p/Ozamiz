package uz.oybek.ozamiz.ui.feature.tasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.databinding.ItemDailyTaskBinding
import uz.oybek.ozamiz.data.points.model.Task
import uz.oybek.ozamiz.data.points.model.TimeOfDay

class TasksAdapter : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

    private var tasks: List<Task> = emptyList()
    private var completedIds: List<String> = emptyList()
    private var listener: OnTaskClickListener? = null

    interface OnTaskClickListener {
        fun onCheck(task: Task)
        fun onInfo(task: Task)
    }

    fun setOnTaskClickListener(listener: OnTaskClickListener) {
        this.listener = listener
    }

    fun submitList(newTasks: List<Task>, newCompletedIds: List<String>) {
        tasks = newTasks
        completedIds = newCompletedIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemDailyTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    inner class TaskViewHolder(private val binding: ItemDailyTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(task: Task) {
            binding.tvTaskTitle.text = task.title
            binding.tvPoints.text = "+${task.points} 💎"
            
            // Translate/Format Time
            binding.tvTimeOfDay.text = when(task.timeOfDay) {
                TimeOfDay.MORNING -> "TONG"
                TimeOfDay.AFTERNOON -> "TUSHLIK"
                TimeOfDay.EVENING -> "KECH"
            }
            
            val isCompleted = completedIds.contains(task.id)
            
            if (isCompleted) {
                binding.btnTaskDone.setImageResource(uz.oybek.ozamiz.R.drawable.check_circle_ic)
                binding.btnTaskDone.setColorFilter(android.graphics.Color.parseColor("#4CAF50")) // Green
                binding.root.alpha = 0.6f
            } else {
                binding.btnTaskDone.setImageResource(uz.oybek.ozamiz.R.drawable.radio_unchecked)
                binding.btnTaskDone.clearColorFilter() // Reset tint (or set to default)
                binding.root.alpha = 1.0f
            }

            binding.btnTaskDone.setOnClickListener {
                if (!isCompleted) {
                    it.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100).withEndAction {
                        it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                        listener?.onCheck(task)
                    }
                }
            }

            binding.btnInfo.setOnClickListener {
                listener?.onInfo(task)
            }
        }
    }
}
