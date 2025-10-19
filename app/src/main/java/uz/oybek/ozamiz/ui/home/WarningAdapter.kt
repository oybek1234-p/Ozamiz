package uz.oybek.ozamiz.ui.home

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as RMaterial
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.ItemDietaryWarningBinding
import uz.oybek.ozamiz.logic.DietaryWarning
import uz.oybek.ozamiz.logic.Severity

class WarningAdapter : ListAdapter<DietaryWarning, WarningAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDietaryWarningBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemDietaryWarningBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(warning: DietaryWarning) {
            binding.textViewWarningTitle.text = warning.title
            binding.textViewWarningMessage.text = warning.suggestion

            val context = itemView.context
            val (cardStrokeColor, cardBackgroundColor, iconAndTitleColor) = when (warning.severity) {
                Severity.INFO -> Triple(
                    ContextCompat.getColor(context, R.color.blue),
                    ContextCompat.getColor(context, R.color.blue_alpha_10),
                    ContextCompat.getColor(context, R.color.blue)
                )
                Severity.WARNING -> Triple(
                    ContextCompat.getColor(context, R.color.yellowDark),
                    ContextCompat.getColor(context, R.color.yellow_alpha_10),
                    ContextCompat.getColor(context, R.color.yellowDark)
                )
                Severity.ALERT -> Triple(
                    ContextCompat.getColor(context, R.color.rejected),
                    ContextCompat.getColor(context, R.color.red_alpha_10),
                    ContextCompat.getColor(context, R.color.rejected)
                )
            }
//            binding.cardWarning.strokeColor = cardStrokeColor
//            binding.cardWarning.setCardBackgroundColor(cardBackgroundColor)
           // binding.iconWarning.imageTintList = ColorStateList.valueOf(iconAndTitleColor)
         //   binding.textViewWarningTitle.setTextColor(iconAndTitleColor)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DietaryWarning>() {
        override fun areItemsTheSame(oldItem: DietaryWarning, newItem: DietaryWarning): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DietaryWarning, newItem: DietaryWarning): Boolean {
            return oldItem == newItem
        }
    }
}