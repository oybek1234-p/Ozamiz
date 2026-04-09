package uz.oybek.ozamiz.ui.feature.level

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.level.LevelInfo
import uz.oybek.ozamiz.databinding.ItemLevelBinding

class LevelAdapter(
    private val levels: List<LevelInfo>,
    private val currentPoints: Int
) : RecyclerView.Adapter<LevelAdapter.LevelViewHolder>() {

    inner class LevelViewHolder(val binding: ItemLevelBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
        val binding = ItemLevelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LevelViewHolder(binding)
    }

    override fun getItemCount() = levels.size

    override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
        val level = levels[position]
        val isUnlocked = currentPoints >= level.requiredPoints
        val isCurrentLevel = run {
            val next = levels.getOrNull(position + 1)
            isUnlocked && (next == null || currentPoints < next.requiredPoints)
        }

        with(holder.binding) {
            textItemEmoji.text = level.emoji
            textItemLevelName.text = level.name

            val color = Color.parseColor(String.format("#%06X", 0xFFFFFF and level.color.toInt()))

            if (level.requiredPoints == 0) {
                textItemRequiredPoints.text = "Boshlang'ich"
                textItemRequiredPoints.setTextColor(color)
            } else {
                textItemRequiredPoints.text = "${level.requiredPoints} bal"
                textItemRequiredPoints.setTextColor(color)
            }

            if (isUnlocked) {
                imgLevelLock.visibility = android.view.View.GONE
                val next = levels.getOrNull(position + 1)
                if (next != null && isCurrentLevel) {
                    val range = next.requiredPoints - level.requiredPoints
                    val earned = currentPoints - level.requiredPoints
                    val progress = ((earned.toFloat() / range.toFloat()) * 100).toInt()
                    progressItemLevel.max = 100
                    progressItemLevel.setProgressCompat(progress, true)
                    progressItemLevel.setIndicatorColor(color)
                } else if (!isCurrentLevel) {
                    progressItemLevel.max = 100
                    progressItemLevel.setProgressCompat(100, false)
                    progressItemLevel.setIndicatorColor(color)
                } else {
                    progressItemLevel.max = 100
                    progressItemLevel.setProgressCompat(100, false)
                    progressItemLevel.setIndicatorColor(color)
                }
                root.alpha = 1f
                if (isCurrentLevel) {
                    root.strokeWidth = 4
                    root.strokeColor = color
                } else {
                    root.strokeWidth = 3
                    root.strokeColor = android.graphics.Color.parseColor("#22000000")
                }
            } else {
                imgLevelLock.visibility = android.view.View.VISIBLE
                progressItemLevel.max = 100
                progressItemLevel.setProgressCompat(0, false)
                progressItemLevel.setIndicatorColor(Color.GRAY)
                root.alpha = 0.5f
                root.strokeWidth = 1
            }
        }
    }
}
