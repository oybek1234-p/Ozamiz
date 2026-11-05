package uz.oybek.ozamiz.ui.feature.natija

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.common.util.DateFormatter
import uz.oybek.ozamiz.data.tracker.weight.model.WeightLog
import uz.oybek.ozamiz.databinding.ItemWeightHistoryBinding
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter

class WeightHistoryAdapter(
    private val onDeleteClick: (WeightLog) -> Unit,
    private val onPhotoClick: (String) -> Unit = {}
) : ListAdapter<WeightLog, WeightHistoryAdapter.ViewHolder>(DiffCallback()) {

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
        val olderLog = if (position < itemCount - 1) getItem(position + 1) else null

        holder.binding.apply {
            val context = root.context
            val dateLong = log.timestampLong
            tvHistoryDate.text = DateFormatter.format(dateLong, DateFormatter.PATTERN_DAY_MONTH)
            tvHistoryTime.text = DateFormatter.format(dateLong, DateFormatter.PATTERN_HOUR_MINUTE)

            tvHistoryWeight.text = NutritionFormatter.kg(log.weightKg)

            // Weight difference relative to previous entry
            if (olderLog != null) {
                val change = log.weightKg - olderLog.weightKg
                tvHistoryChange.text = NutritionFormatter.kgChange(change)

                when {
                    change < -0.01 -> {
                        tvHistoryChange.setTextColor(Color.parseColor("#2E7D32"))
                        tvHistoryChange.setBackgroundResource(R.drawable.bg_weight_change_pill_loss)
                    }
                    change > 0.01 -> {
                        tvHistoryChange.setTextColor(Color.parseColor("#C62828"))
                        tvHistoryChange.setBackgroundResource(R.drawable.bg_weight_change_pill_gain)
                    }
                    else -> {
                        tvHistoryChange.setTextColor(Color.parseColor("#757575"))
                        tvHistoryChange.setBackgroundResource(R.drawable.bg_weight_change_pill_neutral)
                    }
                }
                tvHistoryChange.isVisible = true
            } else {
                tvHistoryChange.isVisible = false
            }

            // Photo thumbnail
            if (log.photoUrl.isNotEmpty()) {
                imgHistoryPhoto.isVisible = true
                Glide.with(context)
                    .load(log.photoUrl)
                    .placeholder(android.R.color.darker_gray)
                    .centerCrop()
                    .into(imgHistoryPhoto)

                imgHistoryPhoto.setOnClickListener {
                    onPhotoClick(log.photoUrl)
                }
            } else {
                imgHistoryPhoto.isVisible = false
                imgHistoryPhoto.setOnClickListener(null)
            }

            // Body Measurements (Bel, Bo'ksa, Qo'l)
            val hasBelly = (log.bellyCm ?: 0.0) > 0
            val hasHips = (log.hipsCm ?: 0.0) > 0
            val hasArm = (log.armCm ?: 0.0) > 0
            val hasMeasurements = hasBelly || hasHips || hasArm

            dividerMeasurements.isVisible = hasMeasurements
            scrollMeasurements.isVisible = hasMeasurements

            if (hasBelly) {
                chipBelly.isVisible = true
                tvHistoryBelly.text = context.getString(
                    R.string.weight_history_metric_format,
                    context.getString(R.string.weight_history_metric_belly),
                    NutritionFormatter.cm(log.bellyCm)
                )
            } else {
                chipBelly.isVisible = false
            }

            if (hasHips) {
                chipHips.isVisible = true
                tvHistoryHips.text = context.getString(
                    R.string.weight_history_metric_format,
                    context.getString(R.string.weight_history_metric_hips),
                    NutritionFormatter.cm(log.hipsCm)
                )
            } else {
                chipHips.isVisible = false
            }

            if (hasArm) {
                chipArm.isVisible = true
                tvHistoryArm.text = context.getString(
                    R.string.weight_history_metric_format,
                    context.getString(R.string.weight_history_metric_arm),
                    NutritionFormatter.cm(log.armCm)
                )
            } else {
                chipArm.isVisible = false
            }

            // Delete action
            btnDelete.setOnClickListener {
                onDeleteClick(log)
            }
        }
    }
}
