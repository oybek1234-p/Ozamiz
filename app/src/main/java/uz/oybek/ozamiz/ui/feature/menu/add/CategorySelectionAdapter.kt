package uz.oybek.ozamiz.ui.feature.menu.add

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.menu.FilterItem
import uz.oybek.ozamiz.databinding.ItemCategorySelectBinding

class CategorySelectionAdapter(
    private val onCategorySelected: (FilterItem, Boolean) -> Unit
) : ListAdapter<Pair<FilterItem, Boolean>, CategorySelectionAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemCategorySelectBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (category, isSelected) = getItem(position)
        holder.bind(category, isSelected)
    }

    inner class ViewHolder(private val binding: ItemCategorySelectBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(category: FilterItem, isSelected: Boolean) {
            binding.tvCategoryName.text = category.name
            binding.tvEmoji.text = category.emoji
            binding.checkbox.isChecked = isSelected
            binding.root.setOnClickListener {
                onCategorySelected(category, !binding.checkbox.isChecked)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Pair<FilterItem, Boolean>>() {
        override fun areItemsTheSame(
            oldItem: Pair<FilterItem, Boolean>,
            newItem: Pair<FilterItem, Boolean>
        ): Boolean = oldItem.first.id == newItem.first.id

        override fun areContentsTheSame(
            oldItem: Pair<FilterItem, Boolean>,
            newItem: Pair<FilterItem, Boolean>
        ): Boolean = oldItem == newItem
    }
}
