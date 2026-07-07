package uz.oybek.ozamiz.ui.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.utils.dateApiFormat
import uz.oybek.ozamiz.data.utils.toCalendar
import uz.oybek.ozamiz.databinding.ItemDateScrollerBinding
import uz.oybek.ozamiz.ui.utils.VaznimColors
import java.util.Calendar
import java.util.Date

class DateScrollerAdapter(
    private val onDateSelected: (Date) -> Unit
) : ListAdapter<Date, DateScrollerAdapter.DateViewHolder>(DateDiffCallback()) {

    private var selectedDate: Date = DateUtils.getStartOfDay(Date())

    var dailySummaries: Map<String, DailySummary> = emptyMap()
    var targetCalories: Int = 2000
    
    companion object {
        private const val PAYLOAD_PROGRESS = "PAYLOAD_PROGRESS"
        private const val PAYLOAD_PROGRESS_SILENT = "PAYLOAD_PROGRESS_SILENT"
    }

    fun updateData(summaries: Map<String, DailySummary>, target: Int, animate: Boolean = true) {
        val oldSummaries = this.dailySummaries
        val oldTarget = this.targetCalories

        this.dailySummaries = summaries
        this.targetCalories = target

        val payload = if (animate) PAYLOAD_PROGRESS else PAYLOAD_PROGRESS_SILENT

        if (oldTarget != target) {
            notifyItemRangeChanged(0, itemCount, payload)
            return
        }

        for (i in 0 until itemCount) {
            val date = getItem(i)
            val dateKey = date.dateApiFormat()
            val oldSum = oldSummaries[dateKey]
            val newSum = summaries[dateKey]

            if (oldSum != newSum) {
                notifyItemChanged(i, payload)
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
        when {
            payloads.contains(PAYLOAD_PROGRESS) -> holder.updateProgress(getItem(position), true)
            payloads.contains(PAYLOAD_PROGRESS_SILENT) -> holder.updateProgress(getItem(position), false)
            else -> super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun setSelectedDate(date: Date) {
        val normalizedNewDate = DateUtils.getStartOfDay(date)
        if (!DateUtils.isSameDay(selectedDate, normalizedNewDate)) {
            val oldPosition = currentList.indexOfFirst { DateUtils.isSameDay(it, selectedDate) }
            val newPosition = currentList.indexOfFirst { DateUtils.isSameDay(it, normalizedNewDate) }
            selectedDate = normalizedNewDate
            if (oldPosition != -1) notifyItemChanged(oldPosition)
            if (newPosition != -1) notifyItemChanged(newPosition)
        }
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

        private val primaryColor = VaznimColors.getPrimary(binding.root.context)
        private val onPrimary = VaznimColors.getOnPrimary(binding.root.context)
        private val surfaceLow = VaznimColors.getSurfaceLow(binding.root.context)
        private val onSurfaceVariant = VaznimColors.getOnSurfaceVariant(binding.root.context)
        
        private val greenColor = VaznimColors.getGreen(binding.root.context)
        private val yellowColor = VaznimColors.getYellow(binding.root.context)

        fun bind(date: Date) {
            val isSelected = DateUtils.isSameDay(date, selectedDate)
            val isToday = DateUtils.isSameDay(date, Date())

            binding.textViewDayOfMonth.text = date.toCalendar().get(Calendar.DAY_OF_MONTH).toString()

            updateProgress(date, false)

            if (isSelected) {
                binding.cardDate.setCardBackgroundColor(primaryColor)
                binding.textViewDayOfMonth.setTextColor(onPrimary)
                binding.cardDate.strokeWidth = 0
            } else {
                binding.cardDate.setCardBackgroundColor(surfaceLow)
                binding.textViewDayOfMonth.setTextColor(onSurfaceVariant)
                if (isToday) {
                    binding.cardDate.strokeWidth = 2
                    binding.cardDate.strokeColor = primaryColor
                } else {
                    binding.cardDate.strokeWidth = 0
                }
            }
        }

        fun updateProgress(date: Date, animate: Boolean) {
            val summary = dailySummaries[date.dateApiFormat()]
            val consumed = summary?.consumedCalories ?: 0
            val target = targetCalories
            
            val progress = if (target > 0) (consumed * 100 / target) else 0
            val isExceeded = consumed > target + 100
            val isCompleted = consumed >= target - 100 && consumed <= target + 100

            binding.progressRing.max = 100
            binding.progressRing.setProgressCompat(if (isExceeded) 100 else progress, animate)
            
            val ringColor = when {
                isExceeded -> yellowColor
                isCompleted -> greenColor
                progress > 0 -> primaryColor
                else -> VaznimColors.getOutline(binding.root.context)
            }
            
            binding.progressRing.setIndicatorColor(ringColor)
            binding.progressRing.trackColor = VaznimColors.getSurfaceContainerHigh(binding.root.context)
        }
    }

    class DateDiffCallback : DiffUtil.ItemCallback<Date>() {
        override fun areItemsTheSame(oldItem: Date, newItem: Date): Boolean =
            DateUtils.isSameDay(oldItem, newItem)

        override fun areContentsTheSame(oldItem: Date, newItem: Date): Boolean = oldItem == newItem
    }
}