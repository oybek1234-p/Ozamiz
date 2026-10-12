package uz.oybek.ozamiz.ui.feature.notification

import android.content.res.ColorStateList
import android.view.LayoutInflater

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.notification.model.NotificationModel
import uz.oybek.ozamiz.data.notification.model.NotificationType
import uz.oybek.ozamiz.databinding.ItemNotificationBinding
import uz.oybek.ozamiz.ui.utils.DateExtensions
import uz.oybek.ozamiz.ui.utils.appColors
import uz.oybek.ozamiz.ui.utils.loadPhoto
import uz.oybek.ozamiz.ui.utils.loadUserPhoto

class NotificationsAdapter(
    private val onClick: (NotificationModel) -> Unit
) : PagingDataAdapter<NotificationModel, NotificationsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationModel) {
            val context = itemView.context
            val colors = itemView.appColors

            val displayName = item.senderName?.takeIf { it.isNotBlank() }
                ?: item.title.ifBlank { context.getString(R.string.notification_default_title) }

            binding.tvUserName.text = displayName
            binding.tvMessage.text = item.message
            binding.tvTime.text = DateExtensions.formatRelativeTime(context, item.createdAt)

            val isSystem = item.notificationType == NotificationType.GENERAL ||
                    item.notificationType == NotificationType.SYSTEM ||
                    item.notificationType == NotificationType.REMINDER

            if (!item.senderAvatar.isNullOrBlank()) {
                binding.imgAvatar.loadUserPhoto(item.senderAvatar)
            } else if (isSystem) {
                binding.imgAvatar.setImageResource(R.drawable.new_logo)
            } else {
                binding.imgAvatar.setImageResource(R.drawable.user_photo_placeholder)
            }

            val postImage = item.extraData?.get("postImage")
            if (!postImage.isNullOrBlank()) {
                binding.imgPostThumbnail.isVisible = true
                binding.imgPostThumbnail.loadPhoto(postImage)
            } else {
                binding.imgPostThumbnail.isVisible = false
            }

            when (item.notificationType) {
                NotificationType.POST_LIKE -> {
                    binding.imgTypeBadge.isVisible = true
                    binding.imgTypeBadge.setImageResource(R.drawable.ic_heart_filled)
                    binding.imgTypeBadge.imageTintList = ColorStateList.valueOf(colors.colorError)
                    binding.imgTypeBadge.backgroundTintList = ColorStateList.valueOf(colors.colorErrorContainer)
                }
                NotificationType.POST_COMMENT -> {
                    binding.imgTypeBadge.isVisible = true
                    binding.imgTypeBadge.setImageResource(R.drawable.comment_ic)
                    binding.imgTypeBadge.imageTintList = ColorStateList.valueOf(colors.colorPrimary)
                    binding.imgTypeBadge.backgroundTintList = ColorStateList.valueOf(colors.colorPrimaryContainer)
                }
                NotificationType.COMMENT_REPLY -> {
                    binding.imgTypeBadge.isVisible = true
                    binding.imgTypeBadge.setImageResource(R.drawable.chat_ic)
                    binding.imgTypeBadge.imageTintList = ColorStateList.valueOf(colors.colorSecondary)
                    binding.imgTypeBadge.backgroundTintList = ColorStateList.valueOf(colors.colorSecondaryContainer)
                }
                NotificationType.REMINDER,
                NotificationType.SYSTEM,
                NotificationType.GENERAL -> {
                    binding.imgTypeBadge.isVisible = true
                    binding.imgTypeBadge.setImageResource(R.drawable.notify_ic)
                    binding.imgTypeBadge.imageTintList = ColorStateList.valueOf(colors.colorTertiary)
                    binding.imgTypeBadge.backgroundTintList = ColorStateList.valueOf(colors.colorTertiaryContainer)
                }
            }


            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<NotificationModel>() {
            override fun areItemsTheSame(
                oldItem: NotificationModel,
                newItem: NotificationModel
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: NotificationModel,
                newItem: NotificationModel
            ): Boolean = oldItem == newItem
        }
    }
}
