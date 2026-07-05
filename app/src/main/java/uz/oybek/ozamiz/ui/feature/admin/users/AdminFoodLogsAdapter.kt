package uz.oybek.ozamiz.ui.feature.admin.users

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.logs.model.AdminGenericLog
import uz.oybek.ozamiz.databinding.ItemAdminFoodLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminFoodLogsAdapter(
    private val onItemClick: (AdminGenericLog) -> Unit
) : ListAdapter<AdminGenericLog, AdminFoodLogsAdapter.FoodLogViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodLogViewHolder {
        val binding = ItemAdminFoodLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FoodLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodLogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FoodLogViewHolder(private val binding: ItemAdminFoodLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(log: AdminGenericLog) {
            binding.textViewFoodName.text = log.title
            
            // formatting time
            val timeFmt = SimpleDateFormat("dd-MMM, HH:mm", Locale.getDefault())
            val timeStr = timeFmt.format(Date(log.timestamp))
            binding.textViewFoodCalories.text = "${log.subtitle} • $timeStr"
            
            if (log.extraInfo.isNotEmpty()) {
                binding.textViewFoodMacros.isVisible = true
                binding.textViewFoodMacros.text = log.extraInfo
            } else {
                binding.textViewFoodMacros.isVisible = false
            }
            
            if (log.photoUrl != null) {
                binding.imageViewFood.isVisible = true
                Glide.with(itemView.context)
                    .load(log.photoUrl)
                    .error(R.drawable.placeholder_food)
                    .placeholder(R.drawable.placeholder_food)
                    .into(binding.imageViewFood)
            } else {
                val icon = when (log.type) {
                    "water" -> R.drawable.water_ic
                    "exercise" -> R.drawable.sport_ic
                    else -> R.drawable.food_ic
                }
                binding.imageViewFood.isVisible = true
                Glide.with(itemView.context)
                    .load(icon)
                    .into(binding.imageViewFood)
            }
            
            binding.root.setOnClickListener {
                onItemClick(log)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AdminGenericLog>() {
        override fun areItemsTheSame(oldItem: AdminGenericLog, newItem: AdminGenericLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AdminGenericLog, newItem: AdminGenericLog): Boolean {
            return oldItem == newItem
        }
    }
}
