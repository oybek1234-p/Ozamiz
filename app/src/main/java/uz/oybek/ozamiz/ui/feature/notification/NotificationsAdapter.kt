package uz.oybek.ozamiz.ui.feature.notification

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.notification.model.NotificationModel
import uz.oybek.ozamiz.databinding.ItemNotificationBinding
import uz.oybek.ozamiz.ui.utils.loadPhoto

class NotificationsAdapter(private val onClick: (NotificationModel) -> Unit) :
    ListAdapter<NotificationModel, NotificationsAdapter.ViewHolder>(DiffCallback) {

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
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationModel) {
            binding.tvUserName.text = item.fromUserName
            binding.tvMessage.text = item.message
            binding.imgAvatar.loadPhoto(item.fromUserAvatar)
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<NotificationModel>() {
            override fun areItemsTheSame(
                oldItem: NotificationModel,
                newItem: NotificationModel
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: NotificationModel,
                newItem: NotificationModel
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
