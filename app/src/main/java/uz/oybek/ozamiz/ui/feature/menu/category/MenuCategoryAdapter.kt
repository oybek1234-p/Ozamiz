package uz.oybek.ozamiz.ui.feature.menu.category

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
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
            val containerAttr = if (item.isSecondary) com.google.android.material.R.attr.colorSecondaryContainer else com.google.android.material.R.attr.colorPrimaryContainer
            val onContainerAttr = if (item.isSecondary) com.google.android.material.R.attr.colorOnSecondaryContainer else com.google.android.material.R.attr.colorOnPrimaryContainer
            
            val containerColor = MaterialColors.getColor(binding.root, containerAttr)
            val onContainerColor = MaterialColors.getColor(binding.root, onContainerAttr)

            binding.iconContainer.backgroundTintList = ColorStateList.valueOf(containerColor)
            binding.imgCategoryIcon.imageTintList = ColorStateList.valueOf(onContainerColor)
            
            binding.textCategoryType.text = if (item.isSecondary) "Qo'shimcha" else "Asosiy"
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
