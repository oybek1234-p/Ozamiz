package uz.oybek.ozamiz.ui.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.databinding.ItemHomeRecommendedFoodBinding
import uz.oybek.ozamiz.ui.utils.loadPhoto

class HomeRecommendedFoodAdapter(
    private val onMenuClick: (Menu) -> Unit
) : ListAdapter<Menu, HomeRecommendedFoodAdapter.ViewHolder>(MenuDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeRecommendedFoodBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemHomeRecommendedFoodBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Menu) {
            binding.textFoodName.text = item.name
            binding.textFoodCalories.text =
                binding.root.context.getString(R.string.calories_format, item.calories)

            if (item.imageUrl != null) {
                binding.imageFood.loadPhoto(item.imageUrl!!)
            } else {
                binding.imageFood.setImageResource(R.drawable.placeholder_food)
            }

            binding.root.setOnClickListener { onMenuClick(item) }
        }
    }

    class MenuDiffCallback : DiffUtil.ItemCallback<Menu>() {
        override fun areItemsTheSame(oldItem: Menu, newItem: Menu): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Menu, newItem: Menu): Boolean {
            return oldItem == newItem
        }
    }
}
