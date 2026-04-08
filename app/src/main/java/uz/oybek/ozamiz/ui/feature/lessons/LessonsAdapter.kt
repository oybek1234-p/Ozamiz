package uz.oybek.ozamiz.ui.feature.lessons

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.ItemLessonSectionBinding
import uz.oybek.ozamiz.databinding.ItemLessonTopicBinding

/**
 * Multi-viewtype adapter for user-facing Lessons list
 */
class LessonsAdapter(
    private val onTopicClick: (LessonListItem.TopicItem) -> Unit
) : ListAdapter<LessonListItem, RecyclerView.ViewHolder>(LessonDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SECTION = 0
        private const val VIEW_TYPE_TOPIC = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is LessonListItem.SectionHeader -> VIEW_TYPE_SECTION
            is LessonListItem.TopicItem -> VIEW_TYPE_TOPIC
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SECTION -> SectionViewHolder(
                ItemLessonSectionBinding.inflate(inflater, parent, false)
            )
            else -> TopicViewHolder(
                ItemLessonTopicBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is LessonListItem.SectionHeader -> (holder as SectionViewHolder).bind(item)
            is LessonListItem.TopicItem -> (holder as TopicViewHolder).bind(item)
        }
    }

    inner class SectionViewHolder(
        private val binding: ItemLessonSectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: LessonListItem.SectionHeader) {
            binding.textSectionTitle.text = item.section.title
        }
    }

    inner class TopicViewHolder(
        private val binding: ItemLessonTopicBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: LessonListItem.TopicItem) {
            binding.textTopicTitle.text = item.topic.title
            
            // Load image
            if (item.topic.imageUrl.isNotEmpty()) {
                Glide.with(binding.imageTopic)
                    .load(item.topic.imageUrl)
                    .placeholder(R.drawable.user_photo_placeholder)
                    .centerCrop()
                    .into(binding.imageTopic)
            } else {
                binding.imageTopic.setImageResource(R.drawable.user_photo_placeholder)
            }
            
            // Status styling
            val context = binding.root.context
            
            // Default "Active" / Filled style setup
            // We use MaterialButton methods to switch look
            
            when {
                item.isCompleted -> {
                    // Hide button completely
                    binding.btnStatus.isVisible = false
                    binding.root.alpha = 1.0f
                }
                item.isLocked -> {
                    binding.btnStatus.isVisible = true
                    // Look like text button: Transparent bg, Grey text/icon
                    binding.btnStatus.text = "Qulflangan"
                    binding.btnStatus.setIconResource(R.drawable.lock_ic)
                    // Reset padding to avoid extra space on left if needed, or set specific padding
                    binding.btnStatus.setPadding(0, 0, 0, 0)
                    
                    val color = context.obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorOnSurfaceVariant)).run {
                        val c = getColor(0, android.graphics.Color.GRAY)
                        recycle()
                        c
                    }
                    binding.btnStatus.setTextColor(color)
                    binding.btnStatus.iconTint = android.content.res.ColorStateList.valueOf(color)
                    binding.btnStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
                    binding.btnStatus.elevation = 0f
                    
                    binding.root.alpha = 0.6f
                }
                else -> { // Active
                    binding.btnStatus.isVisible = true
                    // Filled button: Primary bg, White text/icon
                    binding.btnStatus.text = "Boshlash"
                    binding.btnStatus.setIconResource(R.drawable.arrow_right)
                    
                    // Reset padding to design values
                    binding.btnStatus.setPadding(16.dpToPx(context), 0, 12.dpToPx(context), 0)
                    
                    val colorPrimaryAttr = androidx.appcompat.R.attr.colorPrimary
                    val colorPrimary = context.obtainStyledAttributes(intArrayOf(colorPrimaryAttr)).run {
                        val color = getColor(0, android.graphics.Color.GREEN)
                        recycle()
                        color
                    }
                    val colorOnPrimary = context.obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorOnPrimary)).run {
                        val color = getColor(0, android.graphics.Color.WHITE)
                        recycle()
                        color
                    }                    
                    binding.btnStatus.setTextColor(colorOnPrimary)
                    binding.btnStatus.iconTint = android.content.res.ColorStateList.valueOf(colorOnPrimary)
                    binding.btnStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(colorPrimary)
                    binding.btnStatus.elevation = 4f 
                    
                    binding.root.alpha = 1.0f
                }
            }
            
            // Click listener
            binding.root.setOnClickListener {
                onTopicClick(item)
            }
        }
        
        private fun Int.dpToPx(context: android.content.Context): Int {
            return (this * context.resources.displayMetrics.density).toInt()
        }
    }
}

class LessonDiffCallback : DiffUtil.ItemCallback<LessonListItem>() {
    override fun areItemsTheSame(oldItem: LessonListItem, newItem: LessonListItem): Boolean {
        return when {
            oldItem is LessonListItem.SectionHeader && newItem is LessonListItem.SectionHeader ->
                oldItem.section.id == newItem.section.id
            oldItem is LessonListItem.TopicItem && newItem is LessonListItem.TopicItem ->
                oldItem.topic.id == newItem.topic.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: LessonListItem, newItem: LessonListItem): Boolean {
        return oldItem == newItem
    }
}
