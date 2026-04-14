package uz.oybek.ozamiz.ui.feature.log.food.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.databinding.ItemTodaysFoodBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        getItem(position)?.let { holder.bind(it) }
    }

    inner class FoodLogViewHolder(private val binding: ItemTodaysFoodBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        init {
            itemView.setOnClickListener {
                getItem(bindingAdapterPosition)?.let { log -> onItemClick(log) }
            }
        }

        fun bind(foodLog: FoodLog) {
            binding.textViewFoodName.text = foodLog.description
            binding.textViewFoodCalories.text =
                itemView.context.getString(R.string.calories_format, foodLog.calories)

            val timestamp = foodLog.timestamp as? Long
            if (timestamp != null) {
                binding.textViewTimestamp.text = timeFormat.format(Date(timestamp))
                binding.textViewTimestamp.visibility = View.VISIBLE
            } else {
                binding.textViewTimestamp.visibility = View.GONE
            }

            if (!foodLog.photoUrl.isNullOrEmpty() && foodLog.photoUrl != "null") {
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