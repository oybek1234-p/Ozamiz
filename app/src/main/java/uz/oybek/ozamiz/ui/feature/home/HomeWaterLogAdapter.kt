package uz.oybek.ozamiz.ui.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.logs.model.WaterLog
import uz.oybek.ozamiz.data.utils.formatToHhMm
import uz.oybek.ozamiz.databinding.ItemHomeWaterLogBinding

class HomeWaterLogAdapter(
    private val onDeleteClick: (WaterLog) -> Unit
) : ListAdapter<WaterLog, HomeWaterLogAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeWaterLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemHomeWaterLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: WaterLog) {
            binding.textWaterAmount.text = "${log.amountMl} ml"
            binding.textTime.text = log.timestampLong.formatToHhMm()
            binding.btnDelete.setOnClickListener {
                onDeleteClick(log)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<WaterLog>() {
        override fun areItemsTheSame(oldItem: WaterLog, newItem: WaterLog): Boolean {
            return oldItem.logId == newItem.logId
        }

        override fun areContentsTheSame(oldItem: WaterLog, newItem: WaterLog): Boolean {
            return oldItem == newItem
        }
    }
}
