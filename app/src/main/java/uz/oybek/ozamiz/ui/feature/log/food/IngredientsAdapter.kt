package uz.oybek.ozamiz.ui.feature.log.food

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.ai.analyzer.AnalyzedIngredient
import uz.oybek.ozamiz.databinding.ItemAnalyzedIngredientBinding

class IngredientsAdapter(
    private var ingredients: List<AnalyzedIngredient> = emptyList(),
    private val onItemClick: ((AnalyzedIngredient, Int) -> Unit)? = null
) : RecyclerView.Adapter<IngredientsAdapter.ViewHolder>() {

    fun updateData(newIngredients: List<AnalyzedIngredient>) {
        ingredients = newIngredients
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnalyzedIngredientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(ingredients[position], position)
    }

    override fun getItemCount(): Int = ingredients.size

    inner class ViewHolder(private val binding: ItemAnalyzedIngredientBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AnalyzedIngredient, position: Int) {
            binding.textViewIngredientName.text = item.name

            val calText = if (item.calories != null && item.calories > 0) {
                "• ${item.calories} kkal"
            } else ""
            binding.textViewIngredientCalories.text = calText
            binding.textViewIngredientCalories.isVisible = calText.isNotEmpty()

            val amountStr = buildString {
                if (item.amount != null && item.amount > 0) {
                    val formattedAmount = if (item.amount % 1.0 == 0.0) item.amount.toInt().toString() else item.amount.toString()
                    append(formattedAmount)
                    if (!item.unit.isNullOrBlank()) {
                        append(" ")
                        append(item.unit)
                    }
                } else if (!item.unit.isNullOrBlank()) {
                    append(item.unit)
                }
            }
            binding.textViewIngredientAmount.text = amountStr
            binding.textViewIngredientAmount.isVisible = amountStr.isNotEmpty()

            binding.root.setOnClickListener {
                onItemClick?.invoke(item, position)
            }
        }
    }
}