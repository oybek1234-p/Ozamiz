package uz.oybek.ozamiz.ui.feature.lessons.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.lessons.model.Section
import uz.oybek.ozamiz.data.lessons.model.Topic
import uz.oybek.ozamiz.databinding.ItemAdminLessonSectionBinding
import uz.oybek.ozamiz.databinding.ItemAdminLessonTopicBinding

/**
 * Multi-viewtype adapter for Sections and Topics in admin panel
 */
class AdminLessonsAdapter(
    private val onSectionEdit: (Section) -> Unit,
    private val onSectionDelete: (Section) -> Unit,
    private val onSectionAddTopic: (Section) -> Unit,
    private val onTopicClick: (Topic) -> Unit,
    private val onTopicEdit: (Topic) -> Unit,
    private val onTopicDelete: (Topic) -> Unit
) : ListAdapter<AdminListItem, ViewHolder>(AdminDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SECTION = 0
        private const val VIEW_TYPE_TOPIC = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is AdminListItem.SectionItem -> VIEW_TYPE_SECTION
            is AdminListItem.TopicItem -> VIEW_TYPE_TOPIC
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SECTION -> SectionViewHolder(
                ItemAdminLessonSectionBinding.inflate(inflater, parent, false)
            )
            else -> TopicViewHolder(
                ItemAdminLessonTopicBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is AdminListItem.SectionItem -> (holder as SectionViewHolder).bind(item.section)
            is AdminListItem.TopicItem -> (holder as TopicViewHolder).bind(item.topic)
        }
    }

    inner class SectionViewHolder(
        private val binding: ItemAdminLessonSectionBinding
    ) : ViewHolder(binding.root) {
        
        fun bind(section: Section) {
            binding.textSectionTitle.text = section.title
            binding.btnEditSection.setOnClickListener { onSectionEdit(section) }
            binding.btnDeleteSection.setOnClickListener { onSectionDelete(section) }
            binding.btnAddTopic.setOnClickListener { onSectionAddTopic(section) }
        }
    }

    inner class TopicViewHolder(
        private val binding: ItemAdminLessonTopicBinding
    ) : ViewHolder(binding.root) {
        
        fun bind(topic: Topic) {
            binding.textTopicTitle.text = topic.title
            binding.textTopicInfo.text = "Order: ${topic.order}"
            
            if (topic.imageUrl.isNotEmpty()) {
                Glide.with(binding.imageTopic)
                    .load(topic.imageUrl)
                    .placeholder(R.drawable.user_photo_placeholder)
                    .centerCrop()
                    .into(binding.imageTopic)
            } else {
                binding.imageTopic.setImageResource(R.drawable.user_photo_placeholder)
            }
            
            binding.root.setOnClickListener { onTopicClick(topic) }
            binding.btnEditTopic.setOnClickListener { onTopicEdit(topic) }
            binding.btnDeleteTopic.setOnClickListener { onTopicDelete(topic) }
        }
    }
}

class AdminDiffCallback : DiffUtil.ItemCallback<AdminListItem>() {
    override fun areItemsTheSame(oldItem: AdminListItem, newItem: AdminListItem): Boolean {
        return when {
            oldItem is AdminListItem.SectionItem && newItem is AdminListItem.SectionItem -> 
                oldItem.section.id == newItem.section.id
            oldItem is AdminListItem.TopicItem && newItem is AdminListItem.TopicItem -> 
                oldItem.topic.id == newItem.topic.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: AdminListItem, newItem: AdminListItem): Boolean {
        return oldItem == newItem
    }
}
