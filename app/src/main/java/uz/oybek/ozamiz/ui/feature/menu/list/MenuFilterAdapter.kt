package uz.oybek.ozamiz.ui.feature.menu.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.menu.model.MenuCategory
import uz.oybek.ozamiz.databinding.ItemMenuCategoryFilterBinding

class MenuFilterAdapter(
    private val selectionColorAttr: Int = androidx.appcompat.R.attr.colorPrimary,
    private val onCategoryClick: (MenuCategory) -> Unit
) : ListAdapter<MenuCategory, MenuFilterAdapter.ViewHolder>(CategoryDiffCallback()) {

    private var selectedCategoryId: String = ""

    fun setSelectedCategory(id: String) {
        selectedCategoryId = id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMenuCategoryFilterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemMenuCategoryFilterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MenuCategory) {
            binding.textName.text = item.name
            
            val isSelected = item.id == selectedCategoryId
            val context = binding.root.context
            val selectedColor = MaterialColors.getColor(context, selectionColorAttr, 0)
            val primaryContainerColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceContainerLow, 0)

            if (isSelected) {
                binding.root.setBackgroundResource(R.drawable.round_lay)
                binding.root.background.setTint(selectedColor)
                binding.textName.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                binding.root.setBackgroundResource(R.drawable.round_lay)
                binding.root.background.setTint(primaryContainerColor)
                binding.textName.setTextColor(ContextCompat.getColor(context, R.color.black))
            }

            binding.root.setOnClickListener { onCategoryClick(item) }
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
