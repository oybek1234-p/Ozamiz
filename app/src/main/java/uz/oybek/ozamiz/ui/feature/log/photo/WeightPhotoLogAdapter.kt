package uz.oybek.ozamiz.ui.feature.log.photo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.logs.model.WeightLog
import uz.oybek.ozamiz.common.util.DateFormatter
import uz.oybek.ozamiz.databinding.ItemWeightPhotoLogBinding
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter
import uz.oybek.ozamiz.ui.utils.loadPhoto

class WeightPhotoLogAdapter(
    private val onItemClick: (WeightLog) -> Unit
) : ListAdapter<WeightLog, WeightPhotoLogAdapter.ViewHolder>(WeightPhotoLogDiffCallback()) {

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
            binding.textViewLogWeight.text = NutritionFormatter.kg(log.weightKg)
            binding.textViewLogDate.text =
                DateFormatter.format(log.timestampLong, DateFormatter.PATTERN_DATE_TIME)

            binding.imageViewLogPhoto.loadPhoto(log.photoUrl)
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