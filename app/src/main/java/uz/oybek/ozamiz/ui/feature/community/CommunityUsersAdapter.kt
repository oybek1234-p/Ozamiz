package uz.oybek.ozamiz.ui.feature.community

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.points.LevelManager
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.databinding.ItemCommunityUserBinding
import java.text.SimpleDateFormat
import java.util.Locale

class CommunityUsersAdapter(
    private val onUserClick: (User) -> Unit
) : ListAdapter<User, CommunityUsersAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemCommunityUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemCommunityUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onUserClick(getItem(position))
                }
            }
        }

        fun bind(user: User) {
            binding.tvName.text = user.name.ifBlank { "Ismsiz foydalanuvchi" }
            binding.imgPremium.isVisible = user.premium

            val level = LevelManager.getCurrentLevel(user.scorePoints)
            binding.tvLevel.text = "${level.emoji} ${level.name} • ${user.scorePoints} ball"

            binding.tvLastSeen.text = formatLastSeen(user.lastSeenTime)

            Glide.with(binding.imgAvatar)
                .load(user.avatarUrl)
                .placeholder(R.drawable.user_photo_placeholder)
                .circleCrop()
                .into(binding.imgAvatar)
        }

        private fun formatLastSeen(timestamp: Long): String {
            if (timestamp == 0L) return ""
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60000 -> "yaqinda"
                diff < 3600000 -> "${diff / 60000}m oldin"
                diff < 86400000 -> "${diff / 3600000}s oldin"
                else -> {
                    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    sdf.format(timestamp)
                }
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
