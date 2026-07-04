package uz.oybek.ozamiz.ui.feature.admin.users

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.ItemAdminUserBinding
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.formatLastSeen
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
            
            Glide.with(itemView.context)
                .load(user.avatarUrl)
                .error(R.drawable.user_photo_placeholder)
                .placeholder(R.drawable.user_photo_placeholder)
                .apply(RequestOptions.circleCropTransform())
                .into(binding.imageViewAvatar)

            binding.textViewLastSeen.text = formatLastSeen(user.lastSeenTime)

            binding.textViewPremiumBadge.visibility = if (user.premium) android.view.View.VISIBLE else android.view.View.GONE
            
            if (user.premium && user.premiumDate > 0) {
                binding.textViewPremiumDate.visibility = android.view.View.VISIBLE
                binding.textViewPremiumDate.text = "Premium: ${formatDate(user.premiumDate)}"
            } else {
                binding.textViewPremiumDate.visibility = android.view.View.GONE
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
