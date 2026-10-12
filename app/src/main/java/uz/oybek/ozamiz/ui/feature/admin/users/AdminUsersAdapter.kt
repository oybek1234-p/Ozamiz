package uz.oybek.ozamiz.ui.feature.admin.users

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.databinding.ItemAdminUserBinding
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.ui.utils.DateExtensions
import uz.oybek.ozamiz.ui.utils.loadPhoto
import uz.oybek.ozamiz.ui.utils.loadUserPhoto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminUsersAdapter(private val onUserClicked: (User) -> Unit) :
    ListAdapter<User, AdminUsersAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemAdminUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemAdminUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.textViewName.text = user.name.ifEmpty { "Noma'lum foydalanuvchi" }
            
            binding.imageViewAvatar.loadUserPhoto(user.avatarUrl)

            binding.textViewLastSeen.text = DateExtensions.formatRelativeTime(itemView.context, user.lastSeenTime)

            binding.textViewPremiumBadge.isVisible = user.premium
            
            if (user.premium && user.premiumDate > 0) {
                binding.textViewPremiumDate.isVisible = true
                binding.textViewPremiumDate.text = "Premium: ${formatDate(user.premiumDate)}"
            } else {
                binding.textViewPremiumDate.isVisible = false
            }

            binding.root.setOnClickListener {
                onUserClicked(user)
            }
        }
        
        private fun formatDate(time: Long): String {
            val date = Date(time)
            val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            return format.format(date)
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
