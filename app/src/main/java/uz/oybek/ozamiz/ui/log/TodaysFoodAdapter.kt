package uz.oybek.ozamiz.ui.log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.info.FoodLog
import uz.oybek.ozamiz.databinding.ItemTodaysFoodBinding

class TodaysFoodAdapter(
    private val onItemClick: (FoodLog) -> Unit
) : PagingDataAdapter<FoodLog, TodaysFoodAdapter.FoodLogViewHolder>(FOOD_LOG_DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodLogViewHolder {
        val binding = ItemTodaysFoodBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FoodLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodLogViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item)
        }
    }

    inner class FoodLogViewHolder(private val binding: ItemTodaysFoodBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { log ->
                        onItemClick(log)
                    }
                }
            }
        }

        fun bind(foodLog: FoodLog) {
            binding.textViewFoodName.text = foodLog.description
            binding.textViewFoodName.visibility = if (foodLog.description.isNotBlank()) View.VISIBLE else View.GONE

            binding.textViewFoodCalories.text =
                itemView.context.getString(R.string.calories_format, foodLog.calories)
            binding.textViewFoodCalories.visibility = if (foodLog.calories > 0) View.VISIBLE else View.GONE

            if (!foodLog.photoUrl.isNullOrEmpty()) {
                binding.imageViewFood.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(foodLog.photoUrl)
                    .placeholder(R.drawable.placeholder_food)
                    .into(binding.imageViewFood)
            } else {
                binding.imageViewFood.visibility = View.GONE
            }
        }
    }

    companion object {
        private val FOOD_LOG_DIFF_CALLBACK = object : DiffUtil.ItemCallback<FoodLog>() {
            override fun areItemsTheSame(oldItem: FoodLog, newItem: FoodLog): Boolean =
                oldItem.logId == newItem.logId

            override fun areContentsTheSame(oldItem: FoodLog, newItem: FoodLog): Boolean =
                oldItem == newItem
        }
    }
}