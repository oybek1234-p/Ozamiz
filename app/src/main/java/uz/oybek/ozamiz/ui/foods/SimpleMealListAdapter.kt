package uz.oybek.ozamiz.ui.foods

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.foods.SimpleMealIdea
import uz.oybek.ozamiz.databinding.ItemSimpleMealListBinding
import uz.oybek.ozamiz.ui.loadPhoto

class SimpleMealListAdapter(
    private val onItemClick: (SimpleMealIdea) -> Unit
) : ListAdapter<SimpleMealIdea, SimpleMealListAdapter.ViewHolder>(SimpleMealIdeaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSimpleMealListBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemSimpleMealListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(idea: SimpleMealIdea) {
            binding.textViewSimpleMealName.text = idea.name
            binding.textViewSimpleMealDescription.text = idea.description

            if (idea.estimatedCalories != null && idea.estimatedCalories!! > 0) {
                binding.textViewSimpleMealCalories.text =
                    itemView.context.getString(R.string.calories_format_approx, idea.estimatedCalories)
                binding.textViewSimpleMealCalories.isVisible = true
            } else {
                binding.textViewSimpleMealCalories.isVisible = false
            }
        }
    }

    class SimpleMealIdeaDiffCallback : DiffUtil.ItemCallback<SimpleMealIdea>() {
        override fun areItemsTheSame(oldItem: SimpleMealIdea, newItem: SimpleMealIdea): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: SimpleMealIdea, newItem: SimpleMealIdea): Boolean {
            return oldItem == newItem
        }
    }
}