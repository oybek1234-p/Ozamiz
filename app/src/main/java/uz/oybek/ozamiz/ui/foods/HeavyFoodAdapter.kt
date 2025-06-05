package uz.oybek.ozamiz.ui.foods

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.foods.HeavyFoodItem
import uz.oybek.ozamiz.databinding.HeavyFoodItemBinding
import uz.oybek.ozamiz.databinding.ItemHeavyFoodBinding
import uz.oybek.ozamiz.ui.loadPhoto

class HeavyFoodAdapter : ListAdapter<HeavyFoodItem, HeavyFoodAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = HeavyFoodItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: HeavyFoodItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HeavyFoodItem) {
            binding.nameView.text = item.name
            binding.`when`.text = item.qachon
            binding.amount.text = item.qancha
            binding.mealtime.text = item.mahal
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<HeavyFoodItem>() {
        override fun areItemsTheSame(oldItem: HeavyFoodItem, newItem: HeavyFoodItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HeavyFoodItem, newItem: HeavyFoodItem): Boolean {
            return oldItem == newItem
        }
    }
}