package uz.oybek.ozamiz.ui.feature.community

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.level.LevelRepository
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.formatLastSeen
import uz.oybek.ozamiz.databinding.ItemCommunityUserBinding

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

        @SuppressLint("SetTextI18n")
        fun bind(user: User) {
            binding.tvName.text = user.name.ifBlank { "Ismsiz foydalanuvchi" }
            binding.imgPremium.isVisible = user.premium

            val level = LevelRepository.getCurrentLevel(user.scorePoints)
            binding.tvLevel.text = "${level.emoji} ${level.name} • ${user.scorePoints} ball"

            val startWeight = user.startWeight ?: 0.0
            val currentWeight = user.weight
            if (startWeight > currentWeight && currentWeight > 0) {
                val lost = startWeight - currentWeight
                binding.tvWeightLoss.isVisible = true
                binding.tvWeightLoss.text = String.format(java.util.Locale.US, "-%.1f kg", lost)
            } else {
                binding.tvWeightLoss.isVisible = false
            }

            binding.tvLastSeen.text = formatLastSeen(user.lastSeenTime)

            Glide.with(binding.imgAvatar)
                .load(user.avatarUrl)
                .placeholder(R.drawable.user_photo_placeholder)
                .circleCrop()
                .into(binding.imgAvatar)
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
