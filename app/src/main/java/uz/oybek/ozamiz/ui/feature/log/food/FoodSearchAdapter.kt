package uz.oybek.ozamiz.ui.feature.log.food

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.model.FoodItem
import uz.oybek.ozamiz.databinding.ItemFoodSearchResultBinding
import kotlin.math.roundToInt

class FoodSearchAdapter(
    private val onAddClick: (FoodItem, MaterialButton, CircularProgressIndicator) -> Unit,
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
            val calories = item.caloriesForGrams(weight)
            val weightInt = weight.roundToInt()
            val context = binding.root.context

            val servingDescription = if (defaultServing == null) {
                context.getString(R.string.food_detail_default_grams)
            } else {
                context.getString(
                    R.string.food_detail_serving_item_format,
                    defaultServing.description,
                    weightInt
                )
            }

            binding.textViewFoodServing.text = context.getString(
                R.string.search_food_item_serving_format,
                servingDescription,
                calories
            )

            // Reset recycled state
            binding.buttonAddFood.setIconResource(R.drawable.add_ic)
            binding.buttonAddFood.isEnabled = true
            binding.buttonAddFood.scaleX = 1f
            binding.buttonAddFood.scaleY = 1f
            binding.progressAddFood.visibility = View.INVISIBLE
            binding.progressAddFood.progress = 0
            binding.progressAddFood.alpha = 1f

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
