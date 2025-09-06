package uz.oybek.ozamiz.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.databinding.ItemDateScrollerBinding
import java.text.SimpleDateFormat
import java.util.*

class DateScrollerAdapter(
    private val onDateSelected: (Date) -> Unit
) : ListAdapter<Date, DateScrollerAdapter.DateViewHolder>(DateDiffCallback()) {

    private var selectedDate: Date = getMidnight(Date())
    private val dayFormat = SimpleDateFormat("E", Locale("uz", "UZ"))
    private val dateFormat = SimpleDateFormat("d", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val binding = ItemDateScrollerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        holder.bind(getItem(position))
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
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        return getMidnight(date1).time == getMidnight(date2).time
    }

    inner class DateViewHolder(private val binding: ItemDateScrollerBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onDateSelected(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(date: Date) {
            val isSelected = isSameDay(date, selectedDate)
            val isToday = isSameDay(date, Date())
            val context = binding.root.context

            binding.textViewDayOfWeek.text = dayFormat.format(date).replaceFirstChar { it.uppercase() }
            binding.textViewDayOfMonth.text = dateFormat.format(date)

            when {
                isSelected -> {
                    // Tanlangan kun
                    binding.cardDate.strokeWidth = 0
                    binding.dateContainer.setBackgroundColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimaryFixed, 0))
                    binding.textViewDayOfWeek.setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimary, 0))
                    binding.textViewDayOfMonth.setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimary, 0))
                }
                isToday -> {
                    // Bugungi kun (lekin tanlanmagan)
                    binding.cardDate.strokeWidth = (1.5f * context.resources.displayMetrics.density).toInt()
                    binding.cardDate.strokeColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimaryFixed, 0)
                    binding.dateContainer.background = null
                    binding.textViewDayOfWeek.setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimaryFixed, 0))
                    binding.textViewDayOfMonth.setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimaryFixed, 0))
                }
                else -> {
                    // Boshqa kunlar
                    binding.cardDate.strokeWidth = 0
                    binding.dateContainer.background = null
                    binding.textViewDayOfWeek.setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, 0))
                    binding.textViewDayOfMonth.setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, 0))
                }
            }
        }
    }

    class DateDiffCallback : DiffUtil.ItemCallback<Date>() {
        override fun areItemsTheSame(oldItem: Date, newItem: Date): Boolean = getMidnight(oldItem) == getMidnight(newItem)
        override fun areContentsTheSame(oldItem: Date, newItem: Date): Boolean = oldItem == newItem
        private fun getMidnight(date: Date): Date = Calendar.getInstance().apply {
            time = date; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
    }
}