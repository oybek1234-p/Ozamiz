package uz.oybek.ozamiz.ui.foods

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.foods.Recipe
import uz.oybek.ozamiz.databinding.ItemFoodBinding
import uz.oybek.ozamiz.ui.loadPhoto
import com.google.android.material.chip.Chip

class FoodListAdapter(
    private val onItemClick: (Recipe) -> Unit // O'ZGARISH: ID o'rniga Recipe obyektini berish
) : ListAdapter<Recipe, FoodListAdapter.ViewHolder>(RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFoodBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemFoodBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(adapterPosition)) // O'ZGARISH: To'liq obyektni berish
                }
            }
        }

        fun bind(recipe: Recipe) {
            binding.textViewFoodName.text = recipe.name
            binding.imageViewFood.loadPhoto(recipe.imageUrl)

            binding.textViewFoodCalories.text = recipe.calories?.let {
                itemView.context.getString(R.string.calories_format, it)
            } ?: ""
            binding.textViewFoodCalories.visibility = if (recipe.calories != null) View.VISIBLE else View.GONE

            binding.textViewPreparationTime.text = recipe.preparationTimeMin?.let {
                itemView.context.getString(R.string.duration_hint) + ": " + it.toString() + " daq"
            } ?: ""
            binding.textViewPreparationTime.visibility = if (recipe.preparationTimeMin != null) View.VISIBLE else View.GONE
        }
    }

    class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem == newItem
        }
    }
}