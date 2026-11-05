package uz.oybek.ozamiz.ui.feature.community

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.community.model.PostStatus
import uz.oybek.ozamiz.data.community.model.PostType
import uz.oybek.ozamiz.databinding.ItemCommunityPostBinding
import uz.oybek.ozamiz.ui.utils.DateExtensions
import uz.oybek.ozamiz.ui.utils.getMaterialColor
import uz.oybek.ozamiz.ui.utils.loadPhoto
import uz.oybek.ozamiz.ui.utils.loadUserPhoto

class CommunityPostAdapter(
    private var currentUserId: String,
    private val onLikeClick: (CommunityPost) -> Unit,
    private val onCommentClick: (CommunityPost) -> Unit,
    private val onUserClick: (userId: String, userName: String) -> Unit,
    private val onDeleteClick: (CommunityPost) -> Unit
) : ListAdapter<CommunityPost, CommunityPostAdapter.PostViewHolder>(PostDiffCallback()) {

    private var lastSeenTimestamp: Long = 0L

    fun updateCurrentUserId(userId: String) {
        if (this.currentUserId != userId) {
            this.currentUserId = userId
            notifyItemRangeChanged(0, itemCount, PAYLOAD_LIKE)
        }
    }

    fun updateLastSeenTimestamp(timestamp: Long) {
        if (this.lastSeenTimestamp != timestamp) {
            this.lastSeenTimestamp = timestamp
            notifyDataSetChanged()
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
            binding.layoutPremiumBadge.isVisible = post.isPremiumUser

            // New post badge
            val isNewPost = lastSeenTimestamp > 0L && post.createdAt > lastSeenTimestamp
            binding.tvNewBadge.isVisible = isNewPost

            // Pending badge
            val isPending = post.status == PostStatus.PENDING.name
            binding.tvPendingBadge.isVisible = isPending

            // Time formatting via DateExtensions / DateFormatter
            binding.tvPostTime.text = DateExtensions.formatRelativeTime(context, post.createdAt)

            // Author Stats (Weight loss & days active)
            val statsList = mutableListOf<String>()
            val weightLoss = post.weightLossKg
            if (weightLoss != null && weightLoss > 0.0) {
                val formattedLoss = if (weightLoss % 1.0 == 0.0) weightLoss.toInt().toString() else weightLoss.toString()
                statsList.add("🔥 $formattedLoss kg ozgan")
            }
            val days = post.daysActive
            if (days != null && days > 0) {
                statsList.add("⚡ $days kundan beri shug'ullanmoqda")
            }
            if (statsList.isNotEmpty()) {
                binding.tvAuthorStats.isVisible = true
                binding.tvAuthorStats.text = statsList.joinToString(" • ")
            } else {
                binding.tvAuthorStats.isVisible = false
            }

            // Post Title & Description
            binding.tvPostTitle.isVisible = post.title.isNotBlank()
            binding.tvPostTitle.text = post.title

            val hasDescription = post.description.isNotBlank()
            binding.layoutPostDescription.isVisible = hasDescription
            if (hasDescription) {
                binding.tvPostDescription.text = post.description
                val isExpanded = expandedPostIds.contains(post.id)
                if (isExpanded) {
                    binding.tvPostDescription.maxLines = Int.MAX_VALUE
                    binding.tvReadMore.setText(R.string.community_post_read_less)
                    binding.tvReadMore.isVisible = true
                } else {
                    binding.tvPostDescription.maxLines = 2
                    binding.tvReadMore.setText(R.string.community_post_read_more)
                    binding.tvReadMore.isVisible = false

                    binding.tvPostDescription.post {
                        if (!expandedPostIds.contains(post.id)) {
                            val layout = binding.tvPostDescription.layout
                            if (layout != null) {
                                val lineCount = layout.lineCount
                                val isEllipsized = lineCount > 2 || (lineCount > 0 && layout.getEllipsisCount(lineCount - 1) > 0)
                                binding.tvReadMore.isVisible = isEllipsized
                            } else if (post.description.length > 70 || post.description.contains("\n")) {
                                binding.tvReadMore.isVisible = true
                            }
                        }
                    }
                }

                val toggleExpand = {
                    if (expandedPostIds.contains(post.id)) {
                        expandedPostIds.remove(post.id)
                        binding.tvPostDescription.maxLines = 2
                        binding.tvReadMore.setText(R.string.community_post_read_more)
                    } else {
                        expandedPostIds.add(post.id)
                        binding.tvPostDescription.maxLines = Int.MAX_VALUE
                        binding.tvReadMore.setText(R.string.community_post_read_less)
                        binding.tvReadMore.isVisible = true
                    }
                }

                binding.tvReadMore.setOnClickListener { toggleExpand() }
                binding.tvPostDescription.setOnClickListener {
                    if (!expandedPostIds.contains(post.id) && binding.tvReadMore.isVisible) {
                        toggleExpand()
                    }
                }
            }

            // Photos
            val photos = post.photos.filter { it.isNotBlank() }

            if (photos.size >= 2) {
                binding.layoutMediaContainer.isVisible = true
                binding.layoutSinglePhotoContainer.isVisible = false
                binding.layoutPhotoComparisonContainer.isVisible = true

                binding.imgBefore.loadPhoto(photos.first())
                binding.imgAfter.loadPhoto(photos[1])
            } else if (photos.isNotEmpty()) {
                binding.layoutMediaContainer.isVisible = true
                binding.layoutSinglePhotoContainer.isVisible = true
                binding.layoutPhotoComparisonContainer.isVisible = false

                bindSinglePhoto(photos.first())
            } else {
                binding.layoutMediaContainer.isVisible = false
                binding.layoutSinglePhotoContainer.isVisible = false
                binding.layoutPhotoComparisonContainer.isVisible = false
            }

            // Likes
            updateLikes(post)

            binding.btnLike.setOnClickListener {
                val target = currentPost ?: return@setOnClickListener
                val newLikedState = !isItemLiked
                val newCount = if (newLikedState) itemLikesCount + 1 else (itemLikesCount - 1).coerceAtLeast(0)

                isItemLiked = newLikedState
                itemLikesCount = newCount
                applyLikeUi(newLikedState, newCount, target.type)

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
            applyLikeUi(isItemLiked, itemLikesCount, post.type)
        }

        fun updateComments(post: CommunityPost) {
            currentPost = post
            val context = binding.root.context
            val commentsWord = context.getString(R.string.community_post_action_comments)
            binding.tvCommentLabel.text = if (post.commentsCount > 0) {
                "${post.commentsCount} $commentsWord"
            } else {
                commentsWord
            }
            val normalColor = context.getMaterialColor(com.google.android.material.R.attr.colorOnSurface)
            binding.imgComment.setColorFilter(normalColor)
            binding.tvCommentLabel.setTextColor(normalColor)
        }

        private fun applyLikeUi(liked: Boolean, count: Int, postType: String? = currentPost?.type) {
            val context = binding.root.context
            val normalColor = context.getMaterialColor(com.google.android.material.R.attr.colorOnSurface)
            val activeColor = ContextCompat.getColor(context, R.color.likeColor)

            val actionLabel = when (postType) {
                PostType.FOOD.name -> context.getString(R.string.community_post_like_useful)
                PostType.WEIGHT_LOSS.name -> context.getString(R.string.community_post_like_congrats)
                else -> context.getString(R.string.community_post_like_general)
            }

            // Format: "1 Yoqdi", "14 Foydali", "5 Tabriklayman"
            val textToDisplay = if (count > 0) "$count $actionLabel" else actionLabel
            binding.tvLikeAction.text = textToDisplay

            if (liked) {
                binding.imgLike.setImageResource(R.drawable.like_active_ic)
                binding.imgLike.setColorFilter(activeColor)
                binding.tvLikeAction.setTextColor(activeColor)
            } else {
                binding.imgLike.setImageResource(R.drawable.like_ic)
                binding.imgLike.setColorFilter(normalColor)
                binding.tvLikeAction.setTextColor(normalColor)
            }
        }

        private fun bindSinglePhoto(photoUrl: String) {
            val context = binding.root.context
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val marginPx = (28 * displayMetrics.density).roundToInt()
            val containerWidth = (screenWidth - marginPx).coerceAtLeast(100)
            val maxHeightPx = (270 * displayMetrics.density).roundToInt()
            val minHeightPx = (160 * displayMetrics.density).roundToInt()

            fun applyDimensions(imageWidth: Int, imageHeight: Int) {
                if (imageWidth <= 0 || imageHeight <= 0) return
                val ratio = imageWidth.toFloat() / imageHeight.toFloat()
                val isVertical = ratio < 0.95f

                val targetHeight = if (isVertical) {
                    maxHeightPx
                } else {
                    (containerWidth / ratio).roundToInt().coerceIn(minHeightPx, maxHeightPx)
                }

                val targetWidth = if (isVertical) {
                    (maxHeightPx * ratio).roundToInt().coerceAtMost(containerWidth)
                } else {
                    containerWidth
                }

                val containerLp = binding.layoutSinglePhotoContainer.layoutParams
                if (containerLp.height != targetHeight) {
                    containerLp.height = targetHeight
                    containerLp.width = ViewGroup.LayoutParams.MATCH_PARENT
                    binding.layoutSinglePhotoContainer.layoutParams = containerLp
                }

                val imgLp = binding.imgSinglePhoto.layoutParams
                if (imgLp.height != targetHeight || imgLp.width != targetWidth) {
                    imgLp.height = targetHeight
                    imgLp.width = targetWidth
                    binding.imgSinglePhoto.layoutParams = imgLp
                }

                if (isVertical) {
                    binding.imgSinglePhotoBlur.isVisible = true
                    binding.imgSinglePhotoBlur.loadPhoto(photoUrl, blur = true)
                } else {
                    binding.imgSinglePhotoBlur.isVisible = false
                    binding.imgSinglePhotoBlur.setImageDrawable(null)
                }
            }

            val cached = imageDimensionsCache[photoUrl]
            if (cached != null) {
                applyDimensions(cached.first, cached.second)
            } else {
                val defaultHeight = (containerWidth * 3 / 4).coerceIn(minHeightPx, maxHeightPx)
                val containerLp = binding.layoutSinglePhotoContainer.layoutParams
                if (containerLp.height != defaultHeight) {
                    containerLp.height = defaultHeight
                    containerLp.width = ViewGroup.LayoutParams.MATCH_PARENT
                    binding.layoutSinglePhotoContainer.layoutParams = containerLp
                }
                val imgLp = binding.imgSinglePhoto.layoutParams
                if (imgLp.height != defaultHeight || imgLp.width != containerWidth) {
                    imgLp.height = defaultHeight
                    imgLp.width = containerWidth
                    binding.imgSinglePhoto.layoutParams = imgLp
                }
                binding.imgSinglePhotoBlur.isVisible = false
                binding.imgSinglePhotoBlur.tag = null
                binding.imgSinglePhotoBlur.setImageDrawable(null)
            }

            binding.imgSinglePhoto.loadPhoto(
                photo = photoUrl,
                onResourceReady = { width, height ->
                    val existing = imageDimensionsCache[photoUrl]
                    if (existing == null || existing.first != width || existing.second != height) {
                        imageDimensionsCache[photoUrl] = Pair(width, height)
                        applyDimensions(width, height)
                    }
                }
            )
        }
    }

    companion object {
        private val imageDimensionsCache = mutableMapOf<String, Pair<Int, Int>>()
        private val expandedPostIds = mutableSetOf<String>()
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
