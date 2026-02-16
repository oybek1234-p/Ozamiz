package uz.oybek.ozamiz.ui.feature.log.food

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.databinding.ItemAnalyzedIngredientBinding

class IngredientsAdapter(
    private val ingredients: List<String>
) : RecyclerView.Adapter<IngredientsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnalyzedIngredientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(ingredients[position])
    }

    override fun getItemCount(): Int = ingredients.size

    inner class ViewHolder(private val binding: ItemAnalyzedIngredientBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: String) {
            binding.textViewIngredientName.text = item
            // Hide calories and amount since we only have ingredient names now
            binding.textViewIngredientCalories.text = ""
            binding.textViewIngredientAmount.text = ""
        }
    }
}