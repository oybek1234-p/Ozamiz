package uz.oybek.ozamiz.ui.foods

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.foods.FoodItem
import uz.oybek.ozamiz.databinding.ItemFoodAlternativeBinding

class FoodAlternativesAdapter(
    private val onChooseClick: (FoodItem) -> Unit
) : ListAdapter<FoodItem, FoodAlternativesAdapter.ViewHolder>(FoodItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFoodAlternativeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemFoodAlternativeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.buttonChooseAlternative.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onChooseClick(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(foodItem: FoodItem) {
            binding.textViewAlternativeName.text = foodItem.description ?: foodItem.name
            binding.textViewAlternativeCalories.text = foodItem.calories?.let { "~${it} kkal" } ?: "N/A"
        }
    }

    class FoodItemDiffCallback : DiffUtil.ItemCallback<FoodItem>() {
        override fun areItemsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean {
            return oldItem.id == newItem.id && oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean {
            return oldItem == newItem
        }
    }
}