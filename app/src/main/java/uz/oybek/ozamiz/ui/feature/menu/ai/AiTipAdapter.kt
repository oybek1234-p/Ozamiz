package uz.oybek.ozamiz.ui.feature.menu.ai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.databinding.ItemAiMenuTipBinding

class AiTipAdapter : ListAdapter<AiTip, AiTipAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAiMenuTipBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemAiMenuTipBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tip: AiTip) {
            binding.textTipTitle.text = tip.title
            binding.textTipDesc.text = tip.description
            
            val emoji = when (tip.icon.lowercase()) {
                "water" -> "💧"
                "time", "clock" -> "🕒"
                "move", "walk" -> "🚶"
                "food", "plate" -> "🧣"
                "moon", "night" -> "🌙"
                "ruler", "portion" -> "📏"
                else -> "💡"
            }
            binding.textTipEmoji.text = emoji
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AiTip>() {
        override fun areItemsTheSame(oldItem: AiTip, newItem: AiTip): Boolean = oldItem.title == newItem.title
        override fun areContentsTheSame(oldItem: AiTip, newItem: AiTip): Boolean = oldItem == newItem
    }
}
