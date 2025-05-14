package uz.oybek.ozamiz.ui.foods

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter // Import
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.foods.Recipe
import uz.oybek.ozamiz.databinding.ItemRecipeHorizontalBinding
import uz.oybek.ozamiz.ui.loadPhoto

class RecipeHorizontalAdapter(
    private val onItemClick: (Recipe) -> Unit
) : ListAdapter<Recipe, RecipeHorizontalAdapter.ViewHolder>(RecipeDiffCallback()) { // ListAdapter va DiffUtil

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecipeHorizontalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position)) // getItem()
    }

    // getItemCount kerak emas

    inner class ViewHolder(private val binding: ItemRecipeHorizontalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(recipe: Recipe) {
            binding.textViewRecipeName.text = recipe.name
            binding.imageViewRecipe.loadPhoto(recipe.imageUrl)
            binding.textViewRecipeCalories.text = recipe.calories?.let { itemView.context.getString(R.string.calories_format, it) } ?: ""
        }
    }

    // DiffUtil implementatsiyasi (FoodListAdapter'dan ko'chirish mumkin yoki alohida yozish)
    class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem == newItem
        }
    }
}