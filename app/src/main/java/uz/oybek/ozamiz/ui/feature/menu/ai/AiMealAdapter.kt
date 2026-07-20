package uz.oybek.ozamiz.ui.feature.menu.ai

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import uz.oybek.ozamiz.databinding.ItemAiMenuMealBinding

class AiMealAdapter(
    private val onAddItemClick: (AiMeal, AiMealItem) -> Unit
) : ListAdapter<AiMeal, AiMealAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAiMenuMealBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onAddItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemAiMenuMealBinding,
        private val onAddItemClick: (AiMeal, AiMealItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(meal: AiMeal) {
            binding.textMealTitle.text = meal.title
            binding.textMealTime.text = meal.time
            binding.textCaloriesVal.text = meal.totalCalories.toString()
            binding.textMealTip.text = meal.tip
            
            val isLunch = meal.type.lowercase().contains("tush")
            val colorStr = if (isLunch) "#228B22" else "#FFA500"
            val colorInt = android.graphics.Color.parseColor(colorStr)
            
            binding.viewIndicator.setBackgroundColor(colorInt)
            binding.textCaloriesVal.setTextColor(colorInt)
 
            binding.containerItems.removeAllViews()
            
            meal.items.forEach { mealItem ->
                val nameView = TextView(binding.root.context).apply {
                    text = "• ${mealItem.name}"
                    textSize = 15f
                    setTextColor(android.graphics.Color.DKGRAY)
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 8)
                    }
                }
                binding.containerItems.addView(nameView)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AiMeal>() {
        override fun areItemsTheSame(oldItem: AiMeal, newItem: AiMeal): Boolean = oldItem.type == newItem.type
        override fun areContentsTheSame(oldItem: AiMeal, newItem: AiMeal): Boolean = oldItem == newItem
    }
}
