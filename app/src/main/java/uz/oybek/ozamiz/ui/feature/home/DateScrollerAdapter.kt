
package uz.oybek.ozamiz.ui.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.databinding.ItemDateScrollerBinding
import uz.oybek.ozamiz.ui.utils.appColors
import java.util.Date

class DateScrollerAdapter(
    private val onDateSelected: (Date) -> Unit
) : ListAdapter<DateScrollerItem,
        DateScrollerAdapter.DateViewHolder>(
    DateDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val binding = ItemDateScrollerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DateViewHolder(private val binding: ItemDateScrollerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val colors get() = binding.root.context.appColors

        init {
            binding.cardDate.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDateSelected(getItem(position).date)
                }
            }
        }

        fun bind(item: DateScrollerItem) {
            binding.textViewDayOfMonth.text = item.dayOfMonth

            binding.progressRing.max = 100
            val progressVal = if (item.isExceeded) 100 else item.progressPercent
            binding.progressRing.setProgressCompat(progressVal, false)

            val ringColor = when {
                item.isExceeded -> colors.colorCalorieExceeded
                item.isCompleted || item.progressPercent > 0 -> colors.colorCalorieProgress
                else -> colors.colorSurfaceContainerHigh
            }

            binding.progressRing.setIndicatorColor(ringColor)
            binding.progressRing.trackColor = colors.colorSurfaceContainerHigh

            binding.cardDate.strokeWidth = 0

            when {
                item.isSelected -> {
                    val selectedBg = if (item.isExceeded) colors.colorCalorieExceeded else colors.colorSecondary
                    binding.cardDate.setCardBackgroundColor(selectedBg)
                    val textColor = if (item.isExceeded) colors.colorOnPrimary else colors.colorSurfaceContainerLowest
                    binding.textViewDayOfMonth.setTextColor(textColor)
                }
                item.isExceeded -> {
                    binding.cardDate.setCardBackgroundColor(colors.colorCalorieExceeded)
                    binding.textViewDayOfMonth.setTextColor(colors.colorOnPrimary)
                }
                else -> {
                    binding.cardDate.setCardBackgroundColor(colors.colorSurfaceContainerLowest)
                    if (item.isToday) {
                        binding.textViewDayOfMonth.setTextColor(colors.colorPrimary)
                    } else {
                        binding.textViewDayOfMonth.setTextColor(colors.colorOnSurface)
                    }
                }
            }
        }
    }

    class DateDiffCallback : DiffUtil.ItemCallback<DateScrollerItem>() {
        override fun areItemsTheSame(
            oldItem: DateScrollerItem, newItem: DateScrollerItem
        ): Boolean = oldItem.date.time == newItem.date.time

        override fun areContentsTheSame(
            oldItem: DateScrollerItem, newItem: DateScrollerItem
        ): Boolean = oldItem == newItem
    }
}
