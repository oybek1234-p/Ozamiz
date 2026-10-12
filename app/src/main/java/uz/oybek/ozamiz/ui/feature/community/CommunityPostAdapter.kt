package uz.oybek.ozamiz.ui.feature.community

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.community.model.PostStatus
import uz.oybek.ozamiz.data.community.model.PostType
import uz.oybek.ozamiz.databinding.ItemCommunityPostBinding
import uz.oybek.ozamiz.ui.utils.DateExtensions
import uz.oybek.ozamiz.ui.utils.loadPhoto
import uz.oybek.ozamiz.ui.utils.loadUserPhoto

class CommunityPostAdapter(
    private var currentUserId: String,
    private val onLikeClick: (CommunityPost) -> Unit,
    private val onCommentClick: (CommunityPost) -> Unit,
    private val onUserClick: (userId: String, userName: String) -> Unit,
    private val onDeleteClick: (CommunityPost) -> Unit
) : ListAdapter<CommunityPost, CommunityPostAdapter.PostViewHolder>(PostDiffCallback()) {

    fun updateCurrentUserId(userId: String) {
        if (this.currentUserId != userId) {
            this.currentUserId = userId
            notifyItemRangeChanged(0, itemCount, PAYLOAD_LIKE)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemCommunityPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: PostViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val item = getItem(position)
            for (payload in payloads) {
                if (payload is Set<*>) {
                    if (payload.contains(PAYLOAD_LIKE)) {
                        holder.updateLikes(item)
                    }
                    if (payload.contains(PAYLOAD_COMMENTS)) {
                        holder.updateComments(item)
                    }
                } else if (payload == PAYLOAD_LIKE) {
                    holder.updateLikes(item)
                } else if (payload == PAYLOAD_COMMENTS) {
                    holder.updateComments(item)
                }
            }
        }
    }

    inner class PostViewHolder(
        private val binding: ItemCommunityPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentPost: CommunityPost? = null
        private var isItemLiked: Boolean = false
        private var itemLikesCount: Int = 0

        fun bind(post: CommunityPost) {
            currentPost = post
            val context = binding.root.context

            // User info
            binding.tvUserName.text = post.userName.ifBlank { context.getString(R.string.default_user_name) }
            binding.imgUserAvatar.loadUserPhoto(post.userAvatarUrl)
            binding.imgPremium.isVisible = post.isPremiumUser

            // Pending badge
            val isPending = post.status == PostStatus.PENDING.name
            binding.tvPendingBadge.isVisible = isPending

            // Time formatting via DateExtensions / DateFormatter
            binding.tvPostTime.text = DateExtensions.formatRelativeTime(context, post.createdAt)

            // Post Title & Description
            binding.tvPostTitle.isVisible = post.title.isNotBlank()
            binding.tvPostTitle.text = post.title
            binding.tvPostDescription.isVisible = post.description.isNotBlank()
            binding.tvPostDescription.text = post.description

            // Photos
            val isWeightLoss = post.type == PostType.WEIGHT_LOSS.name
            val photos = post.photos.filter { it.isNotBlank() }

            if (isWeightLoss && photos.size >= 2) {
                binding.layoutMediaContainer.isVisible = true
                binding.layoutPhotoComparison.isVisible = true
                binding.imgSinglePhoto.isVisible = false

                binding.imgBefore.loadPhoto(photos.first())
                binding.imgAfter.loadPhoto(photos.last())
            } else if (photos.isNotEmpty()) {
                binding.layoutMediaContainer.isVisible = true
                binding.layoutPhotoComparison.isVisible = false
                binding.imgSinglePhoto.isVisible = true

                binding.imgSinglePhoto.loadPhoto(photos.first())
            } else {
                binding.layoutMediaContainer.isVisible = false
                binding.layoutPhotoComparison.isVisible = false
                binding.imgSinglePhoto.isVisible = false
            }

            // Likes
            updateLikes(post)

            binding.btnLike.setOnClickListener {
                val target = currentPost ?: return@setOnClickListener
                val newLikedState = !isItemLiked
                val newCount = if (newLikedState) itemLikesCount + 1 else (itemLikesCount - 1).coerceAtLeast(0)

                isItemLiked = newLikedState
                itemLikesCount = newCount
                applyLikeUi(newLikedState, newCount)

                // Heart bounce animation
                binding.imgLike.animate()
                    .scaleX(1.35f)
                    .scaleY(1.35f)
                    .setDuration(120)
                    .withEndAction {
                        binding.imgLike.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(120)
                            .start()
                    }
                    .start()

                onLikeClick(target)
            }

            // Comments
            updateComments(post)
            binding.btnComments.setOnClickListener {
                currentPost?.let { onCommentClick(it) }
            }

            // More Options (Author delete)
            val isAuthor = currentUserId.isNotBlank() && post.userId == currentUserId
            binding.btnPostMore.isVisible = isAuthor
            binding.btnPostMore.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add(context.getString(R.string.delete))
                popup.setOnMenuItemClickListener {
                    currentPost?.let { onDeleteClick(it) }
                    true
                }
                popup.show()
            }

            binding.layoutUserInfo.setOnClickListener {
                currentPost?.let { onUserClick(it.userId, it.userName) }
            }
        }

        fun updateLikes(post: CommunityPost) {
            currentPost = post
            isItemLiked = currentUserId.isNotBlank() && post.likedBy.contains(currentUserId)
            itemLikesCount = post.likesCount
            applyLikeUi(isItemLiked, itemLikesCount)
        }

        fun updateComments(post: CommunityPost) {
            currentPost = post
            binding.tvCommentsCount.text = post.commentsCount.toString()
            binding.tvCommentsCount.isVisible = post.commentsCount > 0
        }

        private fun applyLikeUi(liked: Boolean, count: Int) {
            val context = binding.root.context
            if (liked) {
                binding.imgLike.setImageResource(R.drawable.like_active_ic)
                binding.imgLike.setColorFilter(ContextCompat.getColor(context, R.color.likeColor))
            } else {
                binding.imgLike.setImageResource(R.drawable.like_ic)
                binding.imgLike.setColorFilter(ContextCompat.getColor(context, R.color.grey))
            }
            binding.tvLikesCount.text = count.toString()
            binding.tvLikesCount.isVisible = count > 0
        }
    }

    companion object {
        const val PAYLOAD_LIKE = "PAYLOAD_LIKE"
        const val PAYLOAD_COMMENTS = "PAYLOAD_COMMENTS"
    }
}

private class PostDiffCallback : DiffUtil.ItemCallback<CommunityPost>() {
    override fun areItemsTheSame(oldItem: CommunityPost, newItem: CommunityPost): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CommunityPost, newItem: CommunityPost): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: CommunityPost, newItem: CommunityPost): Any? {
        val bundle = mutableSetOf<String>()
        if (oldItem.likesCount != newItem.likesCount || oldItem.likedBy != newItem.likedBy) {
            bundle.add(CommunityPostAdapter.PAYLOAD_LIKE)
        }
        if (oldItem.commentsCount != newItem.commentsCount) {
            bundle.add(CommunityPostAdapter.PAYLOAD_COMMENTS)
        }
        return if (bundle.isNotEmpty()) bundle else null
    }
}
