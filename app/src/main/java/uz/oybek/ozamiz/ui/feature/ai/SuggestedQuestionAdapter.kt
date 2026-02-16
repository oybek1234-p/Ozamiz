package uz.oybek.ozamiz.ui.feature.ai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.databinding.ItemSuggestedQuestionBinding

class SuggestedQuestionAdapter(
    private val onQuestionClick: (String) -> Unit
) : ListAdapter<String, SuggestedQuestionAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSuggestedQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemSuggestedQuestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(question: String) {
            binding.questionText.text = question
            binding.questionCard.setOnClickListener {
                onQuestionClick(question)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
}
