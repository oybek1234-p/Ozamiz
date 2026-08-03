package uz.oybek.ozamiz.ui.feature.community

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.community.model.PostType.FOOD
import uz.oybek.ozamiz.data.community.model.PostType.WEIGHT_LOSS
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.databinding.ItemCommunityPostBinding
import java.util.concurrent.TimeUnit

class CommunityPostAdapter(
    private val onLikeClick: (String) -> Unit,
    private val onUserClick: (String, String) -> Unit,
    private val onDeleteClick: (CommunityPost) -> Unit,
    private val onViewed: (String) -> Unit,
    private val onVerifyClick: ((CommunityPost) -> Unit)? = null,
    private val showProfileLink: Boolean = true,
    private val showPremiumBadge: Boolean = true
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
        private val currentUserId = UserProfileRepository.user.uid.ifEmpty { null }
        private var viewed = false

        fun bind(post: CommunityPost) {
            if (!viewed) {
                viewed = true
                onViewed(post.id)
            }
            binding.tvUserName.text = post.userName

            if (showPremiumBadge) {
                Glide.with(binding.root)
                    .load(post.userAvatarUrl)
                    .placeholder(R.drawable.user_photo_placeholder)
                    .error(R.drawable.user_photo_placeholder)
                    .circleCrop()
                    .into(binding.imgUserAvatar)
            }
            binding.tvPremiumStatus.isVisible = (post.isPremiumUser && showPremiumBadge).also {
                binding.imgUserAvatar.isVisible = showPremiumBadge
                binding.tvUserName.isVisible = showPremiumBadge
            }

            // Hide calories and food name if not a food post
            val isFoodPost = post.type == FOOD || post.foodName.isNotBlank()
            binding.tvFoodCalories.isVisible = isFoodPost && post.calories > 0
            val mealLabel = getMealTimeLabel(post.mealType)
            val foodTitle = post.foodName.ifBlank { post.title }

            if (isFoodPost) {
                binding.tvFoodName.text = if (mealLabel.isNotEmpty()) "$mealLabel • $foodTitle" else foodTitle
                val scoreText = if (post.healthScore > 0) "  •  ⭐ ${post.healthScore}/10" else ""
                binding.tvFoodCalories.text = "${post.calories} kkal$scoreText"
            } else {
                binding.tvFoodName.text = if (mealLabel.isNotEmpty()) "$mealLabel • $foodTitle" else foodTitle
            }

            binding.layoutViews.isVisible = post.viewsCount > 0
            binding.tvViewsCount.text = post.viewsCount.toString()

            var description = if (post.description.isNotEmpty()) post.description else post.comment
            if (isFoodPost && description != null) {
                val chatIndex = description.indexOf("💬")
                if (chatIndex != -1) {
                    description = description.substring(0, chatIndex).trim()
                }
            }

            if (description.isNullOrBlank()) {
                binding.tvComment.isVisible = false
            } else {
                binding.tvComment.isVisible = true
                val formattedDesc = if (isFoodPost && !description.startsWith("💡") && !description.startsWith("✅")) {
                    "💡 Foydalari: $description"
                } else {
                    description
                }
                binding.tvComment.text = formattedDesc
            }
            val timestamp = post.timestamp
            if (timestamp != null) {
                val diff = System.currentTimeMillis() - timestamp.time
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                binding.tvTime.text = when {
                    days > 0 -> "$days ${binding.root.context.getString(R.string.kun_oldin)}"
                    hours > 0 -> "$hours ${binding.root.context.getString(R.string.sohat_oldin)}"
                    minutes > 0 -> "$minutes minut oldin"
                    else -> binding.root.context.getString(R.string.yaqinda)
                }
            } else {
                binding.tvTime.text = ""
            }

            val isWeightLoss = post.type == WEIGHT_LOSS || post.type == "ACHIEVEMENT"
            val photos =
                (if (post.photos.isNotEmpty()) post.photos else if (post.photoUrl.isNotEmpty()) listOf(
                    post.photoUrl
                ) else emptyList()).filter { it.isNotBlank() }

            if (photos.size >= 2 && isWeightLoss) {
                binding.imgFoodPhoto.isVisible = false
                binding.layoutPhotoComparison.isVisible = true

                Glide.with(binding.root).load(photos.first())
                    .placeholder(R.drawable.user_photo_placeholder).into(binding.imgBefore)
                Glide.with(binding.root).load(photos.last())
                    .placeholder(R.drawable.user_photo_placeholder).into(binding.imgAfter)
            } else if (photos.isNotEmpty()) {
                binding.imgFoodPhoto.isVisible = true
                binding.layoutPhotoComparison.isVisible = false
                Glide.with(binding.root).load(photos.last())
                    .placeholder(R.drawable.placeholder_food)
                    .into(binding.imgFoodPhoto)
            } else {
                binding.imgFoodPhoto.isVisible = false
                binding.layoutPhotoComparison.isVisible = false
            }

            var isLiked = currentUserId != null && post.likedBy.contains(currentUserId)
            var currentLikes = post.likesCount

            fun updateLikeUI(liked: Boolean, count: Int) {
                if (liked) {
                    binding.imgLike.setImageResource(R.drawable.like_active_ic)
                    binding.imgLike.setColorFilter(binding.root.context.getColor(R.color.likeColor))
                } else {
                    binding.imgLike.setImageResource(R.drawable.like_ic)
                    binding.imgLike.setColorFilter(binding.root.context.getColor(R.color.grey))
                }
                binding.tvLikesCount.text = count.toString()
                binding.tvLikesCount.isVisible = count > 0
            }

            updateLikeUI(isLiked, currentLikes)

            binding.btnLike.setOnClickListener {
                if (currentUserId == null) return@setOnClickListener
                isLiked = !isLiked
                currentLikes = if (isLiked) currentLikes + 1 else currentLikes - 1
                updateLikeUI(isLiked, currentLikes)

                binding.imgLike.scaleX = 0.8f
                binding.imgLike.scaleY = 0.8f
                binding.imgLike.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(150)
                    .withEndAction {
                        binding.imgLike.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start()
                    }
                    .start()

                onLikeClick(post.id)
            }

            val userClickListener = android.view.View.OnClickListener {
                onUserClick(post.userId, post.userName)
            }
            binding.imgUserAvatar.setOnClickListener(userClickListener)
            binding.tvUserName.setOnClickListener(userClickListener)
            binding.btnViewProfile.setOnClickListener(userClickListener)
            binding.btnViewProfile.isVisible = showProfileLink

            // Admin features
            val isAdmin = UserProfileRepository.user.admin
            val isOwner = currentUserId != null && post.userId == currentUserId

            // Only show more button if user is owner or admin
            binding.btnMore.isVisible = isOwner || isAdmin

            binding.btnMore.setOnClickListener { view ->
                val popup = android.widget.PopupMenu(view.context, view)
                if (isOwner || isAdmin) popup.menu.add("O'chirish")
                if (isAdmin && !post.verified) popup.menu.add("Tasdiqlash")
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

    private fun getMealTimeLabel(mealType: String?): String {
        return when (mealType?.lowercase()?.trim()) {
            "breakfast", "nonushta" -> "Nonushta 🌅"
            "lunch", "tushlik" -> "Tushlik ☀️"
            "dinner", "kechki_ovqat", "kechki", "kechki ovqat" -> "Kechki ovqat 🌙"
            "snack", "tamaddi", "orada" -> "Tamaddi 🍎"
            else -> ""
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
