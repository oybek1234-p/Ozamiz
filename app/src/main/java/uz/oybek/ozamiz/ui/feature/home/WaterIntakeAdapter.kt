package uz.oybek.ozamiz.ui.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat.animate
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.databinding.ItemWaterGlassBinding

class WaterIntakeAdapter(
    private val onQuickAddClick: () -> Unit
) : ListAdapter<WaterGlassState, WaterIntakeAdapter.GlassViewHolder>(GlassDiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GlassViewHolder {
        return GlassViewHolder(
            ItemWaterGlassBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: GlassViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GlassViewHolder(private val binding: ItemWaterGlassBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(state: WaterGlassState) {
            val targetLevel = if (state.isFilled) 1f else 0f
            binding.viewGlass.setLevel(
                targetLevel = targetLevel,
                animate = false
            )
            binding.iconPlus.isVisible = state.isNextToAdd
            binding.viewGlass.alpha = 1.0f
            binding.root.setOnClickListener {
                if (!state.isFilled) {
                    onQuickAddClick()
                    binding.viewGlass.setLevel(
                        targetLevel = 1f,
                        animate = true
                    )
                }
            }
        }
    }
}

data class WaterGlassState(
    val id: Int,
    val isFilled: Boolean,
    val isNextToAdd: Boolean,
    val shouldAnimateFill: Boolean = false
)

class GlassDiffCallback : DiffUtil.ItemCallback<WaterGlassState>() {
    override fun areItemsTheSame(oldItem: WaterGlassState, newItem: WaterGlassState) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: WaterGlassState, newItem: WaterGlassState) =
        oldItem == newItem
}