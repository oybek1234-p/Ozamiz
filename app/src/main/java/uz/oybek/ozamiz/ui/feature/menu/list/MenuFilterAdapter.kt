package uz.oybek.ozamiz.ui.feature.menu.list

import android.view.LayoutInflater
import android.view.ViewGroup
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
            val containerColor = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorSurfaceContainerHigh,
                0
            )

            binding.root.setBackgroundResource(R.drawable.pill_background)

            if (isSelected) {
                binding.root.background.setTint(selectedColor)
                binding.textName.setTextColor(
                    MaterialColors.getColor(
                        context,
                        com.google.android.material.R.attr.colorOnPrimary,
                        0
                    )
                )
                binding.textName.typeface = context.resources.getFont(R.font.roboto_bold)
            } else {
                binding.root.background.setTint(containerColor)
                binding.textName.setTextColor(
                    MaterialColors.getColor(
                        context,
                        com.google.android.material.R.attr.colorOnSurfaceVariant,
                        0
                    )
                )
                binding.textName.typeface = context.resources.getFont(R.font.roboto_medium)
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
