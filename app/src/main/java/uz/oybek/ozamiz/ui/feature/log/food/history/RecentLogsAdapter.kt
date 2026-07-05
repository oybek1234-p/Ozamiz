package uz.oybek.ozamiz.ui.feature.log.food.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.utils.formatToHhMm
import uz.oybek.ozamiz.databinding.ItemTodaysFoodBinding

class RecentLogsAdapter(
    private val onItemClick: (FoodLog) -> Unit,
    private val onDeleteClick: (FoodLog) -> Unit,
    private val onLikeClick: (FoodLog) -> Unit
) : ListAdapter<FoodLog, RecentLogsAdapter.FoodLogViewHolder>(FOOD_LOG_DIFF_CALLBACK) {

    private var savedFoodIds: Set<String> = emptySet()

    fun setSavedFoodIds(ids: Set<String>) {
        savedFoodIds = ids
        notifyDataSetChanged()
    }

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
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { log -> onItemClick(log) }
                }
            }
            binding.btnDeleteFood.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { log -> onDeleteClick(log) }
                }
            }
            binding.btnLikeFood.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    getItem(bindingAdapterPosition)?.let { log -> onLikeClick(log) }
                }
            }
        }

        fun bind(foodLog: FoodLog) {
            binding.textViewFoodName.text = foodLog.description
            binding.textViewFoodCalories.text =
                itemView.context.getString(R.string.calories_format, foodLog.calories)

            val timestamp = (foodLog.timestamp as? Number)?.toLong()
            if (timestamp != null) {
                binding.textViewTimestamp.text = timestamp.formatToHhMm()
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

            val isLiked = savedFoodIds.contains(foodLog.description.lowercase().trim())
            binding.btnLikeFood.setImageResource(
                if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )
            binding.btnLikeFood.imageTintList = android.content.res.ColorStateList.valueOf(
                if (isLiked) itemView.context.getColor(R.color.likeColor)
                else itemView.context.getColor(R.color.iconColor)
            )
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
