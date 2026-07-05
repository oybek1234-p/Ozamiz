package uz.oybek.ozamiz.ui.feature.level

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.level.Level
import uz.oybek.ozamiz.databinding.ItemLevelBinding

class LevelAdapter(
    private val levels: List<Level>, private val currentPoints: Int
) : RecyclerView.Adapter<LevelAdapter.LevelViewHolder>() {

    class LevelViewHolder(val binding: ItemLevelBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LevelViewHolder {
        val binding =
            ItemLevelBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
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
        val isCompleted = isUnlocked && !isCurrentLevel

        with(holder.binding) {
            textItemEmoji.text = level.emoji
            textItemLevelName.text = level.name
            textItemRequiredPoints.text = level.requiredPoints.toString() + " ball"
            textItemStatus.text = level.motivationText
            val primaryColor = com.google.android.material.color.MaterialColors.getColor(
                root, androidx.appcompat.R.attr.colorPrimary
            )
            val colorOnSurface = com.google.android.material.color.MaterialColors.getColor(
                root, com.google.android.material.R.attr.colorOnSurface
            )
            val colorOutline = com.google.android.material.color.MaterialColors.getColor(
                root, com.google.android.material.R.attr.colorOutlineVariant
            )
            val colorSurfaceVariant = com.google.android.material.color.MaterialColors.getColor(
                root, com.google.android.material.R.attr.colorSurfaceContainerHigh
            )
            cardEmojiContainer.setCardBackgroundColor(colorSurfaceVariant)

            if (isUnlocked) {
                imgLevelLock.visibility = android.view.View.GONE
                imgLevelCheck.visibility =
                    if (isCompleted) android.view.View.VISIBLE else android.view.View.GONE
                textItemRequiredPoints.setTextColor(primaryColor)

                    textItemStatus.setTextColor(
                        com.google.android.material.color.MaterialColors.getColor(
                            root, com.google.android.material.R.attr.colorOnSurfaceVariant
                        )
                    )
                    cardItemLevel.strokeWidth = 1
                    cardItemLevel.strokeColor = colorOutline
                    cardItemLevel.setCardBackgroundColor(
                        com.google.android.material.color.MaterialColors.getColor(
                            root, com.google.android.material.R.attr.colorSurfaceContainerLowest
                        )
                    )

            } else {
                imgLevelLock.visibility = android.view.View.VISIBLE
                imgLevelCheck.visibility = android.view.View.GONE

                textItemRequiredPoints.setTextColor(colorOnSurface)

                cardItemLevel.strokeWidth = 1
                cardItemLevel.strokeColor = colorOutline
                cardItemLevel.setCardBackgroundColor(
                    com.google.android.material.color.MaterialColors.getColor(
                        root, com.google.android.material.R.attr.colorSurface
                    )
                )
            }
        }
    }
}
