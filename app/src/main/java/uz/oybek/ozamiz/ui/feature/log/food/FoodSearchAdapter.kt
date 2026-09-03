package uz.oybek.ozamiz.ui.feature.log.food

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.model.FoodItem
import uz.oybek.ozamiz.databinding.ItemFoodSearchResultBinding
import kotlin.math.roundToInt

class FoodSearchAdapter(
    private val onAddClick: (FoodItem, com.google.android.material.button.MaterialButton, android.view.View) -> Unit,
    private val onItemClick: (FoodItem) -> Unit
) : PagingDataAdapter<FoodItem, FoodSearchAdapter.FoodViewHolder>(FoodDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val binding = ItemFoodSearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item)
        }
    }

    inner class FoodViewHolder(private val binding: ItemFoodSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FoodItem) {
            binding.textViewFoodName.text = item.name

            val defaultServing = item.servingSizes.firstOrNull()
            val weight = defaultServing?.grams ?: 100.0
            val calories = (item.caloriesPer100g * weight / 100).roundToInt()
            val weightInt = weight.roundToInt()

            val servingDescription = when {
                defaultServing == null -> "100 g"
                defaultServing.description.contains("g)", ignoreCase = true) || defaultServing.description.equals("100 g", ignoreCase = true) -> defaultServing.description
                defaultServing.description.equals("gramm", ignoreCase = true) -> "100 g"
                else -> "${defaultServing.description} (${weightInt} g)"
            }

            binding.textViewFoodServing.text = "$servingDescription • $calories kkal"

            binding.buttonAddFood.setOnClickListener {
                onAddClick(item, binding.buttonAddFood, binding.progressAddFood)
            }
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    class FoodDiffCallback : DiffUtil.ItemCallback<FoodItem>() {
        override fun areItemsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean {
            return oldItem == newItem
        }
    }
}
