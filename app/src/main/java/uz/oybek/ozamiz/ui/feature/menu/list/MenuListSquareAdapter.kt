package uz.oybek.ozamiz.ui.feature.menu.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.menu.FilterId
import uz.oybek.ozamiz.data.menu.FilterItem
import uz.oybek.ozamiz.databinding.ItemMenuCategoryFilterBinding

class MenuListSquareAdapter(
    private val isHorizontal: Boolean = false,
    private val selectionColorAttr: Int = androidx.appcompat.R.attr.colorPrimary,
    private val onItemClick: (FilterItem) -> Unit
) : ListAdapter<FilterItem, MenuListSquareAdapter.ViewHolder>(FilterDiffCallback()) {

    private var selectedId: FilterId? = null

    fun setSelectedId(id: FilterId?) {
        selectedId = id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMenuCategoryFilterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        if (isHorizontal) {
            val density = parent.context.resources.displayMetrics.density
            val lp = binding.root.layoutParams ?: ViewGroup.MarginLayoutParams(
                (120 * density).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.width = (120 * density).toInt()
            binding.root.layoutParams = lp
        }
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemMenuCategoryFilterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FilterItem) {
            binding.textName.text = item.name

            val isSelected = item.id == selectedId
            val context = binding.root.context
            val selectedColor = MaterialColors.getColor(context, selectionColorAttr, 0)
            val containerColor = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorSurface, 0
            )
            val outlineVariant = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOutlineVariant, 0
            )

            binding.imgCategory.visibility = View.GONE
            binding.textEmoji.visibility = View.VISIBLE
            binding.textEmoji.text = item.emoji.ifEmpty { "🍽️" }

            if (isSelected) {
                binding.root.setCardBackgroundColor(selectedColor)
                binding.root.strokeColor = selectedColor
                binding.root.strokeWidth = 4
                binding.textName.setTextColor(
                    MaterialColors.getColor(
                        context,
                        com.google.android.material.R.attr.colorOnPrimary, 0
                    )
                )
                binding.textName.typeface = context.resources.getFont(R.font.roboto_bold)
            } else {
                binding.root.setCardBackgroundColor(containerColor)
                binding.root.strokeColor = outlineVariant
                binding.root.strokeWidth = 2
                binding.textName.setTextColor(
                    MaterialColors.getColor(
                        context,
                        com.google.android.material.R.attr.colorOnSurfaceVariant, 0
                    )
                )
                binding.textName.typeface = context.resources.getFont(R.font.roboto_medium)
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    private class FilterDiffCallback : DiffUtil.ItemCallback<FilterItem>() {
        override fun areItemsTheSame(oldItem: FilterItem, newItem: FilterItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FilterItem, newItem: FilterItem): Boolean {
            return oldItem == newItem
        }
    }
}
