package uz.oybek.ozamiz.ui.feature.community

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.common.util.orZero
import uz.oybek.ozamiz.data.level.LevelRepository
import uz.oybek.ozamiz.data.profile.model.User
import uz.oybek.ozamiz.databinding.ItemCommunityUserBinding
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter
import uz.oybek.ozamiz.ui.utils.DateExtensions
import uz.oybek.ozamiz.ui.utils.loadPhoto
import uz.oybek.ozamiz.ui.utils.loadUserPhoto

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
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onUserClick(getItem(position))
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(user: User) {
            binding.tvName.text = user.name.ifBlank { binding.root.context.getString(R.string.default_user_name) }
            binding.imgPremium.isVisible = user.premium

            val level = LevelRepository.getCurrentLevel(user.scorePoints)
            binding.tvLevel.text = "${level.emoji} ${level.name} • ${user.scorePoints} ball"

            val startWeight = user.startWeight.orZero()
            val currentWeight = user.weight
            if (startWeight > currentWeight && currentWeight > 0) {
                val lost = currentWeight - startWeight
                binding.tvWeightLoss.isVisible = true
                binding.tvWeightLoss.text = NutritionFormatter.kgChange(lost)
            } else {
                binding.tvWeightLoss.isVisible = false
            }

            binding.tvLastSeen.text = DateExtensions.formatLastSeen(binding.root.context, user.lastSeenTime)

            binding.imgAvatar.loadUserPhoto(user.avatarUrl)
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
