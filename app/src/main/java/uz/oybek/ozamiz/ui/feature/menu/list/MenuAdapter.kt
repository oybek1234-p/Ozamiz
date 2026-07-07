package uz.oybek.ozamiz.ui.feature.menu.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.databinding.ItemMenuBinding
import uz.oybek.ozamiz.ui.utils.loadPhoto
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.ui.feature.premium.isPremium

class MenuAdapter(
    private val onMenuClick: (Menu) -> Unit
) : ListAdapter<Menu, MenuAdapter.ViewHolder>(MenuDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMenuBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Menu) {
            binding.textName.text = item.name
            binding.textCalories.text =
                binding.root.context.getString(R.string.calories_format, item.calories)

            val isPremiumUser = UserProfileRepository.user.isPremium()
            val isPremiumRequired = !isPremiumUser && adapterPosition >= 6

            if (item.imageUrl != null) {
                var glideRequest = Glide.with(binding.imageMenu)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.placeholder_food)
                    .centerCrop()
                
                if (isPremiumRequired) {
                    glideRequest = glideRequest.apply(RequestOptions.bitmapTransform(BlurTransformation(25, 3)))
                }
                glideRequest.into(binding.imageMenu)
            } else {
                binding.imageMenu.setImageResource(R.drawable.placeholder_food)
            }

            binding.premiumOverlay.isVisible = isPremiumRequired

            binding.root.setOnClickListener { 
                onMenuClick(item)
            }
        }
    }

    class MenuDiffCallback : DiffUtil.ItemCallback<Menu>() {
        override fun areItemsTheSame(oldItem: Menu, newItem: Menu): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Menu, newItem: Menu): Boolean {
            return oldItem == newItem
        }
    }
}
