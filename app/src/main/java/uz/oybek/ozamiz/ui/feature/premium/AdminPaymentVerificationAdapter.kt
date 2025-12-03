package uz.oybek.ozamiz.ui.feature.premium

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.databinding.ItemAdminPaymentVerificationBinding
import java.text.SimpleDateFormat
import java.util.*

class AdminPaymentVerificationAdapter(
    private val onItemClick: (User) -> Unit
) : ListAdapter<User, AdminPaymentVerificationAdapter.ViewHolder>(UserDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminPaymentVerificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAdminPaymentVerificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(user: User) {
            binding.textViewUserNameAdmin.text = user.name.ifEmpty { user.uid.take(8) }
            binding.textViewUserPhoneAdmin.text = user.phoneNumber.ifEmpty { "Raqam kiritilmagan" }
            binding.textViewSubmissionTimeAdmin.text =
                if (user.premiumDate > 0) "Chek yuborilgan: ${dateFormat.format(Date(user.premiumDate))}" else "Yuborilgan vaqti noma'lum"

            Glide.with(itemView.context)
                .load(user.paymentCheckPhotoUrl)
                .placeholder(R.drawable.placeholder_food) // Generic placeholder
                .error(R.drawable.placeholder_food)
                .override(100,100) // Smaller image for list
                .into(binding.imageViewCheckMini)
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