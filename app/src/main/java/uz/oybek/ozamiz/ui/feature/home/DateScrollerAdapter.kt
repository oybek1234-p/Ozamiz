package uz.oybek.ozamiz.ui.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.utils.dateApiFormat
import uz.oybek.ozamiz.databinding.ItemDateScrollerBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DateScrollerAdapter(
    private val onDateSelected: (Date) -> Unit
) : ListAdapter<Date, DateScrollerAdapter.DateViewHolder>(DateDiffCallback()) {

    private var selectedDate: Date = getMidnight(Date())
    private val dayFormat = SimpleDateFormat("E", Locale("uz", "UZ"))
    private val dateFormat = SimpleDateFormat("d", Locale.getDefault())
    
    var dailySummaries: Map<String, DailySummary> = emptyMap()
    var targetCalories: Int = 2000

    companion object {
        private const val PAYLOAD_PROGRESS = "PAYLOAD_PROGRESS"
    }

    fun updateData(summaries: Map<String, DailySummary>, target: Int) {
        val oldSummaries = this.dailySummaries
        val oldTarget = this.targetCalories

        this.dailySummaries = summaries
        this.targetCalories = target

        if (oldTarget != target) {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_PROGRESS)
            return
        }

        for (i in 0 until itemCount) {
            val date = getItem(i)
            val dateKey = date.dateApiFormat()
            val oldSum = oldSummaries[dateKey]
            val newSum = summaries[dateKey]

            // We only care if the consumed calories or target changed effectively
            // But since target check is above, just check summary equality or specific field
            if (oldSum != newSum) {
                notifyItemChanged(i, PAYLOAD_PROGRESS)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): DateViewHolder {
        val binding =
            ItemDateScrollerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return DateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_PROGRESS)) {
            holder.updateProgress(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun setSelectedDate(date: Date) {
        val normalizedNewDate = getMidnight(date)
        if (!isSameDay(selectedDate, normalizedNewDate)) {
            val oldPosition = currentList.indexOfFirst { isSameDay(it, selectedDate) }
            val newPosition = currentList.indexOfFirst { isSameDay(it, normalizedNewDate) }
            selectedDate = normalizedNewDate
            if (oldPosition != -1) notifyItemChanged(oldPosition)
            if (newPosition != -1) notifyItemChanged(newPosition)
        }
    }

    private fun getMidnight(date: Date): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0);
            set(Calendar.MINUTE, 0);
            set(Calendar.SECOND, 0);
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        return getMidnight(date1).time == getMidnight(date2).time
    }

    inner class DateViewHolder(private val binding: ItemDateScrollerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.cardDate.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onDateSelected(getItem(bindingAdapterPosition))
                }
            }
        }

        private val context = binding.root.context
        private val primaryColor = MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary, 0)
        private val onPrimary = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimary, 0)
        private val surfaceLow = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceContainerLow, 0)
        private val onSurface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, 0)
        private val outlineVariant = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutlineVariant, 0)
        
        private val greenColor = ContextCompat.getColor(context, R.color.green)
        private val redColor = ContextCompat.getColor(context, R.color.red)

        fun bind(date: Date) {
            val isSelected = isSameDay(date, selectedDate)
            val isToday = isSameDay(date, Date())

            binding.textViewDayOfWeek.text =
                dayFormat.format(date).replaceFirstChar { it.uppercase() }
            binding.textViewDayOfMonth.text = dateFormat.format(date)

            updateProgress(date)
            
            // Selection Logic
            if (isSelected) {
                binding.cardDate.setCardBackgroundColor(primaryColor)
                binding.textViewDayOfWeek.setTextColor(primaryColor)
                binding.textViewDayOfMonth.setTextColor(onPrimary)
                binding.cardDate.strokeWidth = 0
            } else {
                binding.cardDate.setCardBackgroundColor(surfaceLow)
                binding.textViewDayOfWeek.setTextColor(onSurface)
                binding.textViewDayOfMonth.setTextColor(onSurface)
                // If today, maybe a border? Or just the progress ring is enough distinction
                if (isToday) {
                    binding.cardDate.strokeWidth = 2
                    binding.cardDate.strokeColor = primaryColor
                } else {
                    binding.cardDate.strokeWidth = 0
                }
            }
        }

        fun updateProgress(date: Date) {
            val dateKey = date.dateApiFormat()
            val summary = dailySummaries[dateKey]
            val consumed = summary?.consumedCalories ?: 0
            val max = if (targetCalories > 0) targetCalories else 1
            val progress = (consumed.toFloat() / max * 100).toInt()
            val isExceeded = consumed > targetCalories

            binding.progressRing.max = 100
            // Animate progress change
            binding.progressRing.setProgressCompat(if (isExceeded) 100 else progress, true)
            binding.progressRing.setIndicatorColor(if (isExceeded) redColor else greenColor)
        }
    }

    class DateDiffCallback : DiffUtil.ItemCallback<Date>() {
        override fun areItemsTheSame(oldItem: Date, newItem: Date): Boolean =
            getMidnight(oldItem) == getMidnight(newItem)

        override fun areContentsTheSame(oldItem: Date, newItem: Date): Boolean = oldItem == newItem
        private fun getMidnight(date: Date): Date = Calendar.getInstance().apply {
            time = date; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(
            Calendar.SECOND,
            0
        ); set(Calendar.MILLISECOND, 0)
        }.time
    }
}