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
    private val onViewed: (String) -> Unit
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
            binding.tvFoodName.text = post.foodName
            binding.tvFoodCalories.text = "${post.calories} kkal"

            val level = LevelSystem.getLevel(post.userScore)
            binding.tvUserLevel.text = level.name

            binding.tvHealthScore.isVisible = false
            binding.tvLikesCount.text = post.likesCount.toString()
            binding.tvViewsCount.text = "${post.viewsCount} ko'rdi"

            if (post.comment.isNullOrBlank()) {
                binding.tvComment.isVisible = false
            } else {
                binding.tvComment.isVisible = true
                binding.tvComment.text = post.comment
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
            Glide.with(binding.root).load(post.photoUrl).placeholder(R.drawable.placeholder_food)
                .into(binding.imgFoodPhoto)
            val isLiked = currentUserId != null && post.likedBy.contains(currentUserId)
            if (isLiked) {
                binding.imgLike.setColorFilter(binding.root.context.getColor(R.color.red))
            } else {
                binding.imgLike.setColorFilter(binding.root.context.getColor(R.color.grey))
            }
            binding.btnLike.setOnClickListener {
                onLikeClick(post.id)
            }
            val userClickListener = android.view.View.OnClickListener {
                onUserClick(post.userId, post.userName)
            }
            binding.imgUserAvatar.setOnClickListener(userClickListener)
            binding.tvUserName.setOnClickListener(userClickListener)
            binding.btnViewProfile.setOnClickListener(userClickListener)

            // More Menu (Delete)
            val isOwner = currentUserId != null && post.userId == currentUserId
            binding.btnMore.isVisible = isOwner
            if (isOwner) {
                binding.btnMore.setOnClickListener { view ->
                    val popup = android.widget.PopupMenu(view.context, view)
                    popup.menu.add("O'chirish")
                    popup.setOnMenuItemClickListener { item ->
                        if (item.title == "O'chirish") {
                            onDeleteClick(post)
                        }
                        true
                    }
                    popup.show()
                }
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
