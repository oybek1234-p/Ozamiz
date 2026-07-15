package uz.oybek.ozamiz.ui.feature.menu.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.menu.FilterItem
import uz.oybek.ozamiz.databinding.ItemMenuCircularFilterBinding

class MenuCircularFilterAdapter(
    private val onItemClick: (FilterItem) -> Unit
) : ListAdapter<FilterItem, MenuCircularFilterAdapter.ViewHolder>(FilterDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMenuCircularFilterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMenuCircularFilterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FilterItem) {
            binding.textCategoryTitle.text = item.name
            binding.imgCategoryPhoto.visibility = View.GONE
            binding.textEmoji.visibility = View.VISIBLE
            binding.textEmoji.text = item.emoji.ifEmpty { "🍽️" }
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
