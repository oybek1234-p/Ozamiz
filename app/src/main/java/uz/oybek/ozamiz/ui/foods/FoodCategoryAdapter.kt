package uz.oybek.ozamiz.ui.foods

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter // Import ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.foods.FoodCategory
import uz.oybek.ozamiz.databinding.ItemFoodCategoryBinding
import uz.oybek.ozamiz.ui.loadPhoto

class FoodCategoryAdapter(
    private val onItemClick: (FoodCategory) -> Unit,
    private val onEditClick: ((FoodCategory) -> Unit)? = null // Admin uchun
) : ListAdapter<FoodCategory, FoodCategoryAdapter.ViewHolder>(FoodCategoryDiffCallback()) { // DiffUtil qo'shamiz

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFoodCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position)) // getItem(position) orqali item olamiz
    }

    // getItemCount kerak emas, ListAdapter o'zi boshqaradi

    inner class ViewHolder(private val binding: ItemFoodCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition // bindingAdapterPosition is preferred
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick?.invoke(getItem(position))
                    true // Indicate event was handled
                } else {
                    false
                }
            }
        }

        fun bind(category: FoodCategory) {
            binding.textViewCategoryName.text = category.name
            // loadPhoto null qiymatni to'g'ri ishlay olishi kerak
            binding.imageViewCategory.loadPhoto(category.imageUrl)
        }
    }

    // DiffUtil.ItemCallback implementatsiyasi
    class FoodCategoryDiffCallback : DiffUtil.ItemCallback<FoodCategory>() {
        override fun areItemsTheSame(oldItem: FoodCategory, newItem: FoodCategory): Boolean {
            return oldItem.id == newItem.id // Unikal ID bo'yicha solishtirish
        }

        override fun areContentsTheSame(oldItem: FoodCategory, newItem: FoodCategory): Boolean {
            return oldItem == newItem // Data class'ning tengligini tekshirish
        }
    }
}