package uz.oybek.ozamiz.ui.feature.lessons.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.lessons.model.Quiz
import uz.oybek.ozamiz.data.lessons.model.Screen
import uz.oybek.ozamiz.databinding.ItemAdminQuizBinding
import uz.oybek.ozamiz.databinding.ItemAdminScreenBinding

/**
 * Adapter for Screens list in admin topic detail
 */
class ScreensAdapter(
    private val onEdit: (Screen) -> Unit,
    private val onDelete: (Screen) -> Unit
) : ListAdapter<Screen, ScreensAdapter.ScreenViewHolder>(ScreenDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScreenViewHolder {
        val binding = ItemAdminScreenBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ScreenViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScreenViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ScreenViewHolder(
        private val binding: ItemAdminScreenBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(screen: Screen) {
            binding.textScreenTitle.text = screen.title
            binding.textScreenOrder.text = "Tartib: ${screen.order}"
            
            if (screen.imageUrl.isNotEmpty()) {
                Glide.with(binding.imageScreen)
                    .load(screen.imageUrl)
                    .placeholder(R.drawable.user_photo_placeholder)
                    .centerCrop()
                    .into(binding.imageScreen)
            } else {
                binding.imageScreen.setImageResource(R.drawable.user_photo_placeholder)
            }
            
            binding.btnEditScreen.setOnClickListener { onEdit(screen) }
            binding.btnDeleteScreen.setOnClickListener { onDelete(screen) }
        }
    }

    class ScreenDiffCallback : DiffUtil.ItemCallback<Screen>() {
        override fun areItemsTheSame(oldItem: Screen, newItem: Screen) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Screen, newItem: Screen) = oldItem == newItem
    }
}

/**
 * Adapter for Quizzes list in admin topic detail
 */
class QuizzesAdapter(
    private val onEdit: (Quiz) -> Unit,
    private val onDelete: (Quiz) -> Unit
) : ListAdapter<Quiz, QuizzesAdapter.QuizViewHolder>(QuizDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
        val binding = ItemAdminQuizBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return QuizViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class QuizViewHolder(
        private val binding: ItemAdminQuizBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(quiz: Quiz, number: Int) {
            binding.textQuizNumber.text = number.toString()
            binding.textQuizQuestion.text = quiz.question
            binding.textQuizOptions.text = "${quiz.options.size} ta variant"
            
            binding.btnEditQuiz.setOnClickListener { onEdit(quiz) }
            binding.btnDeleteQuiz.setOnClickListener { onDelete(quiz) }
        }
    }

    class QuizDiffCallback : DiffUtil.ItemCallback<Quiz>() {
        override fun areItemsTheSame(oldItem: Quiz, newItem: Quiz) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Quiz, newItem: Quiz) = oldItem == newItem
    }
}
