package uz.oybek.ozamiz.ui.feature.menu.category

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.menu.model.MenuCategory
import uz.oybek.ozamiz.databinding.ItemMenuCategoryBinding

class MenuCategoryAdapter(
    private val onEditClick: (MenuCategory) -> Unit,
    private val onDeleteClick: (MenuCategory) -> Unit
) : ListAdapter<MenuCategory, MenuCategoryAdapter.ViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMenuCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemMenuCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MenuCategory) {
            binding.textCategoryName.text = item.name
            
            // Apply dynamic colors based on category type
            val containerAttr = if (!item.meal) com.google.android.material.R.attr.colorSecondaryContainer else com.google.android.material.R.attr.colorPrimaryContainer
            val onContainerAttr = if (!item.meal) com.google.android.material.R.attr.colorOnSecondaryContainer else com.google.android.material.R.attr.colorOnPrimaryContainer
            
            val containerColor = MaterialColors.getColor(binding.root, containerAttr)
            val onContainerColor = MaterialColors.getColor(binding.root, onContainerAttr)

            binding.iconContainer.backgroundTintList = ColorStateList.valueOf(containerColor)
            
            if (item.imageUrl.isNotEmpty()) {
                binding.imgCategoryPhoto.visibility = android.view.View.VISIBLE
                binding.imgCategoryIcon.visibility = android.view.View.GONE
                binding.iconContainer.clipToOutline = true
                com.bumptech.glide.Glide.with(binding.imgCategoryPhoto)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.placeholder_food)
                    .centerCrop()
                    .into(binding.imgCategoryPhoto)
            } else {
                binding.imgCategoryPhoto.visibility = android.view.View.GONE
                binding.imgCategoryIcon.visibility = android.view.View.VISIBLE
                binding.imgCategoryIcon.imageTintList = ColorStateList.valueOf(onContainerColor)
                binding.imgCategoryIcon.setImageResource(R.drawable.food_ic)
            }
            
            binding.textCategoryType.text = if (!item.meal) "Qo'shimcha" else "Asosiy"
            binding.textCategoryType.backgroundTintList = ColorStateList.valueOf(containerColor)
            binding.textCategoryType.setTextColor(onContainerColor)
            
            binding.btnEdit.setOnClickListener { onEditClick(item) }
            binding.btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<MenuCategory>() {
        override fun areItemsTheSame(oldItem: MenuCategory, newItem: MenuCategory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MenuCategory, newItem: MenuCategory): Boolean {
            return oldItem == newItem
        }
    }
}
