package uz.oybek.ozamiz.ui.feature.natija

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.logs.model.WeightLog
import uz.oybek.ozamiz.databinding.ItemWeightHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeightHistoryAdapter(
    private val onDeleteClick: (WeightLog) -> Unit
) : ListAdapter<WeightLog, WeightHistoryAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd MMM", Locale("uz"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale("uz"))

    class ViewHolder(val binding: ItemWeightHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    class DiffCallback : DiffUtil.ItemCallback<WeightLog>() {
        override fun areItemsTheSame(oldItem: WeightLog, newItem: WeightLog): Boolean {
            return oldItem.logId == newItem.logId
        }

        override fun areContentsTheSame(oldItem: WeightLog, newItem: WeightLog): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWeightHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = getItem(position)
        // Calculate change looking at next item (which is previous in time since list is desc)
        val olderLog = if (position < itemCount - 1) getItem(position + 1) else null

        holder.binding.apply {
            val date = Date(log.timestampLong)
            tvHistoryDate.text = dateFormat.format(date)
            tvHistoryTime.text = timeFormat.format(date)
            
            tvHistoryWeight.text = "${String.format("%.1f", log.weightKg)} kg"
            
            // Change
            if (olderLog != null) {
                val change = log.weightKg - olderLog.weightKg
                val changeText = if (change > 0) "+${String.format("%.1f", change)}" else String.format("%.1f", change)
                tvHistoryChange.text = changeText
                
                val color = if (change <= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
                tvHistoryChange.setTextColor(color)
                tvHistoryChange.isVisible = true
            } else {
                tvHistoryChange.isVisible = false
            }
            
            // Photo
            if (log.photoUrl.isNotEmpty()) {
                imgHistoryPhoto.isVisible = true
                com.bumptech.glide.Glide.with(root.context)
                    .load(log.photoUrl)
                    .placeholder(android.R.color.darker_gray)
                    .centerCrop()
                    .into(imgHistoryPhoto)
            } else {
                imgHistoryPhoto.isVisible = false
            }
            
            // Delete
            btnDelete.setOnClickListener {
                onDeleteClick(log)
            }
        }
    }
}
