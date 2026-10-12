package uz.oybek.ozamiz.ui.feature.log.food.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.common.util.DateFormatter
import uz.oybek.ozamiz.data.tracker.model.FoodModel
import uz.oybek.ozamiz.databinding.ItemTodaysFoodBinding
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter
import uz.oybek.ozamiz.ui.utils.loadPhoto

class RecentLogsAdapter(
    private val onItemClick: (FoodModel) -> Unit,
    private val onDeleteClick: (FoodModel) -> Unit
) : ListAdapter<FoodModel, RecentLogsAdapter.FoodLogViewHolder>(FOOD_LOG_DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodLogViewHolder {
        val binding = ItemTodaysFoodBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FoodLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodLogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FoodLogViewHolder(private val binding: ItemTodaysFoodBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { log -> onItemClick(log) }
                }
            }
            binding.btnEditFood.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { log -> onItemClick(log) }
                }
            }
            binding.btnDeleteFood.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { log -> onDeleteClick(log) }
                }
            }
        }

        fun bind(foodLog: FoodModel) {
            binding.textViewFoodName.text = foodLog.description
            binding.textViewFoodCalories.text = NutritionFormatter.kcal(foodLog.calories)

            val timestamp = foodLog.timestamp
            if (timestamp > 0) {
                binding.textViewTimestamp.text = DateFormatter.format(timestamp, DateFormatter.PATTERN_HOUR_MINUTE)
                binding.textViewTimestamp.isVisible = true
                binding.textViewDotSeparator.isVisible = true
            } else {
                binding.textViewTimestamp.isVisible = false
                binding.textViewDotSeparator.isVisible = false
            }

            if (!foodLog.photoUrl.isNullOrEmpty()) {
                binding.imageViewFood.isVisible = true
                binding.imageViewFood.loadPhoto(foodLog.photoUrl)
            } else {
                binding.imageViewFood.isVisible = false
            }
        }
    }

    companion object {
        private val FOOD_LOG_DIFF_CALLBACK = object : DiffUtil.ItemCallback<FoodModel>() {
            override fun areItemsTheSame(oldItem: FoodModel, newItem: FoodModel): Boolean =
                oldItem.logId == newItem.logId

            override fun areContentsTheSame(oldItem: FoodModel, newItem: FoodModel): Boolean =
                oldItem == newItem
        }
    }
}

