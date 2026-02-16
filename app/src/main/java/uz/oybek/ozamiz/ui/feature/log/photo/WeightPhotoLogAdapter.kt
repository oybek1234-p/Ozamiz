package uz.oybek.ozamiz.ui.feature.log.photo

import uz.oybek.ozamiz.data.logs.model.WeightLog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.ItemWeightPhotoLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeightPhotoLogAdapter(
    private val onItemClick: (WeightLog) -> Unit
) : ListAdapter<WeightLog, WeightPhotoLogAdapter.ViewHolder>(WeightPhotoLogDiffCallback()) {

    private val dateFormat =
        SimpleDateFormat("dd-MMMM, yyyy HH:mm", Locale("uz", "UZ")) // Adjust locale if needed

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWeightPhotoLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemWeightPhotoLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(log: WeightLog) {
            binding.textViewLogWeight.text = String.format(Locale.US, "%.1f kg", log.weightKg)
            binding.textViewLogDate.text = dateFormat.format(Date(log.timestampLong))

            Glide.with(itemView.context)
                .load(log.photoUrl)
                .placeholder(R.drawable.woman_placeholder) // Placeholder
                .error(R.drawable.woman_placeholder)       // Error placeholder
                .into(binding.imageViewLogPhoto)
        }
    }

    class WeightPhotoLogDiffCallback : DiffUtil.ItemCallback<WeightLog>() {
        override fun areItemsTheSame(oldItem: WeightLog, newItem: WeightLog): Boolean {
            return oldItem.logId == newItem.logId
        }

        override fun areContentsTheSame(oldItem: WeightLog, newItem: WeightLog): Boolean {
            return oldItem == newItem
        }
    }
}