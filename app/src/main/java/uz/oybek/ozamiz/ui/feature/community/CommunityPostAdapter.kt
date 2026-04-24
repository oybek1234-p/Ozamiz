package uz.oybek.ozamiz.ui.feature.community

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.points.LevelSystem
import uz.oybek.ozamiz.databinding.ItemCommunityPostBinding
import java.util.concurrent.TimeUnit

class CommunityPostAdapter(
    private val onLikeClick: (String) -> Unit,
    private val onUserClick: (String, String) -> Unit,
    private val onDeleteClick: (CommunityPost) -> Unit,
    private val onViewed: (String) -> Unit,
    private val onVerifyClick: ((CommunityPost) -> Unit)? = null
) : ListAdapter<CommunityPost, CommunityPostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemCommunityPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(private val binding: ItemCommunityPostBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val currentUserId = Firebase.auth.currentUser?.uid
        private var viewed = false

        fun bind(post: CommunityPost) {
            if (!viewed) {
                viewed = true
                onViewed(post.id)
            }
            binding.tvUserName.text = post.userName
            Glide.with(binding.root)
                .load(post.userAvatarUrl)
                .placeholder(R.drawable.user_photo_placeholder)
                .error(R.drawable.user_photo_placeholder)
                .circleCrop()
                .into(binding.imgUserAvatar)
            binding.tvFoodName.text = if (post.title.isNotEmpty()) post.title else post.foodName
            binding.tvFoodCalories.text = "${post.calories} kkal"
            binding.tvFoodCalories.isVisible = post.calories > 0

            binding.tvLikesCount.text = post.likesCount.toString()
            binding.tvViewsCount.text = "${post.viewsCount} ko'rdi"

            val description = if (post.description.isNotEmpty()) post.description else post.comment
            if (description.isNullOrBlank()) {
                binding.tvComment.isVisible = false
            } else {
                binding.tvComment.isVisible = true
                binding.tvComment.text = description
            }
            val timestamp = post.timestamp
            if (timestamp != null) {
                val diff = System.currentTimeMillis() - timestamp.time
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                binding.tvTime.text = when {
                    days > 0 -> "$days kun oldin"
                    hours > 0 -> "$hours soat oldin"
                    else -> "Yaqinda"
                }
            } else {
                binding.tvTime.text = ""
            }
            
            val photoUrl = if (post.photos.isNotEmpty()) post.photos.first() else post.photoUrl
            if (photoUrl.isNotEmpty()) {
                binding.imgFoodPhoto.isVisible = true
                Glide.with(binding.root).load(photoUrl).placeholder(R.drawable.placeholder_food)
                    .into(binding.imgFoodPhoto)
            } else {
                binding.imgFoodPhoto.isVisible = false
            }

            var isLiked = currentUserId != null && post.likedBy.contains(currentUserId)
            var currentLikes = post.likesCount

            fun updateLikeUI(liked: Boolean, count: Int) {
                if (liked) {
                    binding.imgLike.setImageResource(R.drawable.like_active_ic)
                    binding.imgLike.setColorFilter(binding.root.context.getColor(R.color.red))
                } else {
                    binding.imgLike.setImageResource(R.drawable.like_ic)
                    binding.imgLike.setColorFilter(binding.root.context.getColor(R.color.grey))
                }
                binding.tvLikesCount.text = count.toString()
            }

            updateLikeUI(isLiked, currentLikes)

            binding.btnLike.setOnClickListener {
                if (currentUserId == null) return@setOnClickListener
                isLiked = !isLiked
                currentLikes = if (isLiked) currentLikes + 1 else currentLikes - 1
                updateLikeUI(isLiked, currentLikes)
                onLikeClick(post.id)
            }

            val userClickListener = android.view.View.OnClickListener {
                onUserClick(post.userId, post.userName)
            }
            binding.imgUserAvatar.setOnClickListener(userClickListener)
            binding.tvUserName.setOnClickListener(userClickListener)
            binding.btnViewProfile.setOnClickListener(userClickListener)

            // Admin features
            val isOwner = currentUserId != null && post.userId == currentUserId
            binding.btnMore.isVisible = isOwner || onVerifyClick != null
            binding.btnMore.setOnClickListener { view ->
                val popup = android.widget.PopupMenu(view.context, view)
                if (isOwner) popup.menu.add("O'chirish")
                if (onVerifyClick != null && !post.verified) popup.menu.add("Tasdiqlash")
                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "O'chirish" -> onDeleteClick(post)
                        "Tasdiqlash" -> onVerifyClick?.invoke(post)
                    }
                    true
                }
                popup.show()
            }
        }
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<CommunityPost>() {
    override fun areItemsTheSame(oldItem: CommunityPost, newItem: CommunityPost): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CommunityPost, newItem: CommunityPost): Boolean {
        return oldItem == newItem
    }
}
