package uz.oybek.ozamiz.ui.feature.log.food

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.food.model.FoodItem
import uz.oybek.ozamiz.databinding.ItemFoodSearchResultBinding
import kotlin.math.roundToInt

class FoodSearchAdapter(
    private val onAddClick: (FoodItem, com.google.android.material.button.MaterialButton, android.view.View) -> Unit,
    private val onItemClick: (FoodItem) -> Unit
) : ListAdapter<FoodItem, FoodSearchAdapter.FoodViewHolder>(FoodDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val binding = ItemFoodSearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FoodViewHolder(private val binding: ItemFoodSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FoodItem) {
            binding.textViewFoodName.text = item.name
            
            val defaultServing = item.servingSizes.firstOrNull()
            binding.textViewFoodServing.text = if (defaultServing != null) {
                "${defaultServing.description} (${defaultServing.grams.roundToInt()} g)"
            } else {
                "100 g"
            }
            
            val weight = defaultServing?.grams ?: 100.0
            val calories = (item.caloriesPer100g * weight / 100).roundToInt()
            val carbs = (item.carbsPer100g * weight / 100)
            val protein = (item.proteinPer100g * weight / 100)
            val fat = (item.fatPer100g * weight / 100)

            binding.textViewCalories.text = "$calories kkal"
            binding.textViewCarbs.text = String.format("C: %.1f g", carbs)
            binding.textViewProtein.text = String.format("P: %.1f g", protein)
            binding.textViewFat.text = String.format("F: %.1f g", fat)

            binding.imageViewVerified.isVisible = item.isVerified

            binding.buttonAddFood.setOnClickListener { onAddClick(item, binding.buttonAddFood, binding.progressAddFood) }
            binding.root.setOnClickListener { onItemClick(item) }
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
