package uz.oybek.ozamiz.ui.feature.log.food

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.analyzer.model.AnalyzedIngredient
import uz.oybek.ozamiz.databinding.ItemAnalyzedIngredientBinding
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter

class IngredientsAdapter(
    private var ingredients: List<AnalyzedIngredient> = emptyList(),
    private val onItemClick: ((AnalyzedIngredient, Int) -> Unit)? = null
) : RecyclerView.Adapter<IngredientsAdapter.ViewHolder>() {

    fun updateData(newIngredients: List<AnalyzedIngredient>) {
        ingredients = newIngredients
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnalyzedIngredientBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(ingredients[position], position)
    }

    override fun getItemCount(): Int = ingredients.size

    inner class ViewHolder(private val binding: ItemAnalyzedIngredientBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AnalyzedIngredient, position: Int) {
            binding.textViewIngredientName.text = item.name

            val calText = NutritionFormatter.kcal(item.calories)
            binding.textViewIngredientCalories.text = calText
            binding.textViewIngredientCalories.isVisible = calText.isNotEmpty()

            val amountStr = NutritionFormatter.ingredientAmount(item.amount, item.unit)
            binding.textViewIngredientAmount.text = amountStr
            binding.textViewIngredientAmount.isVisible = amountStr.isNotEmpty()

            binding.textViewIngredientMacros.isVisible = false

            binding.viewDivider.isVisible = position < ingredients.size - 1

            binding.root.setOnClickListener {
                onItemClick?.invoke(item, position)
            }
        }
    }
}