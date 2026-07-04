package uz.oybek.ozamiz.ui.feature.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.points.LevelManager
import uz.oybek.ozamiz.data.utils.formatLastSeen
import uz.oybek.ozamiz.databinding.FragmentCommunityProfileBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class CommunityProfileFragment : BaseFragment<FragmentCommunityProfileBinding>() {

    private val viewModel: CommunityProfileViewModel by viewModels()
    private lateinit var communityAdapter: CommunityPostAdapter
    private var isLoading = false
    private var isLastPage = false
    private var lastVisiblePostId: String? = null
    private val postsList = mutableListOf<CommunityPost>()

    init {
        showBottomSheet = false
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCommunityProfileBinding {
        return FragmentCommunityProfileBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentCommunityProfileBinding) {
        bind.toolbar.setNavigationOnClickListener {
            closeFragment()
        }

        val userId = arguments?.getString("userId") ?: return
        val userName = arguments?.getString("userName") ?: "Foydalanuvchi"

        bind.tvUserNameProfile.text = userName

        setupAdapter(bind, userId)
        setupScrollListener(bind, userId)
        loadInitialData(bind, userId, userName)
    }

    private fun setupAdapter(bind: FragmentCommunityProfileBinding, userId: String) {
        communityAdapter = CommunityPostAdapter(
            onLikeClick = { postId ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val currentUserId = viewModel.currentUserId
                    if (currentUserId.isNotEmpty()) {
                        viewModel.toggleLike(postId, currentUserId)
                    }
                }
            },
            onUserClick = { clickedUserId, clickedUserName ->
                if (clickedUserId != userId) {
                    val bundle = Bundle().apply {
                        putString("userId", clickedUserId)
                        putString("userName", clickedUserName)
                    }
                    // Navigation logic would go here if needed
                }
            },
            onViewed = { postId ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val currentUserId = viewModel.currentUserId
                    if (currentUserId.isNotEmpty()) {
                        viewModel.markViewed(postId, currentUserId)
                    }
                }
            },
            onDeleteClick = {

            },
            showProfileLink = false,
            showPremiumBadge = false
        )
        bind.rvProfilePosts.adapter = communityAdapter
    }

    private fun setupScrollListener(bind: FragmentCommunityProfileBinding, userId: String) {
        // The layout has a NestedScrollView that contains the ConstraintLayout
        bind.root.findViewById<androidx.core.widget.NestedScrollView>(R.id.nestedScrollView)
            ?.let { scrollView ->
                scrollView.setOnScrollChangeListener { v: androidx.core.widget.NestedScrollView, _, scrollY, _, _ ->
                    if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                        loadMorePosts(userId)
                    }
                }
            } ?: run {
            // Fallback if ID is different or not found
            (bind.rvProfilePosts.parent?.parent as? androidx.core.widget.NestedScrollView)?.setOnScrollChangeListener { v: androidx.core.widget.NestedScrollView, _, scrollY, _, _ ->
                if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                    loadMorePosts(userId)
                }
            }
        }
    }

    private fun loadInitialData(
        bind: FragmentCommunityProfileBinding,
        userId: String,
        initialUserName: String
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            bind.progressLoading.isVisible = true
            bind.layoutStats.isVisible = false

            try {
                val user = viewModel.getPublicUser(userId)

                if (user != null) {
                    val name = user.name
                    val avatarUrl = user.avatarUrl
                    val points = user.scorePoints
                    val weight = user.weight
                    val startWeight = user.startWeight ?: weight
                    val targetWeight = user.targetWeight
                    val streakInfo = user.streakInfo
                    val lastSeenTime = user.lastSeenTime

                    bind.tvUserNameProfile.text = name.ifEmpty { initialUserName }
                    bind.tvStreak.text = "${streakInfo?.activeDays ?: 0} kun"
                    bind.tvCardStreak.text = "${streakInfo?.activeDays ?: 0} kun"
                    bind.tvPoints.text = "$points ball"

                    bind.tvLastActive.text = formatLastSeen(lastSeenTime)
                    fun Double.fmt(): String = if (this % 1.0 == 0.0) this.toInt()
                        .toString() else String.format(java.util.Locale.US, "%.1f", this)

                    bind.tvStartWeight.text =
                        if (startWeight > 0) "${startWeight.fmt()} kg" else "--"
                    bind.tvCurrentWeight.text = if (weight > 0) "${weight.fmt()} kg" else "--"
                    bind.tvTargetWeight.text =
                        if (targetWeight != null) "${targetWeight.fmt()} kg" else "--"

                    if (startWeight > weight && weight > 0) {
                        val lost = startWeight - weight
                        bind.tvOuterWeightLoss.text = "${lost.fmt()} kilogram ozildi"
                        bind.tvOuterWeightLoss.isVisible = true
                    } else {
                        bind.tvOuterWeightLoss.isVisible = false
                    }

                    val level = LevelManager.getCurrentLevel(points)
                    bind.tvProfileLevel.text = "${level.emoji} ${level.name}"

                    com.bumptech.glide.Glide.with(this@CommunityProfileFragment)
                        .load(avatarUrl)
                        .placeholder(R.drawable.user_photo_placeholder)
                        .circleCrop()
                        .into(bind.imgProfileAvatar)
                }

                // 2. Postlarni yuklash
                fetchPosts(userId)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                bind.progressLoading.isVisible = false
                bind.layoutStats.isVisible = true
            }
        }
    }

    private fun loadMorePosts(userId: String) {
        if (isLoading || isLastPage) return
        viewLifecycleOwner.lifecycleScope.launch {
            fetchPosts(userId)
        }
    }

    private suspend fun fetchPosts(userId: String) {
        isLoading = true
        val result = viewModel.getUserPosts(userId, 8, lastVisiblePostId)
        result.onSuccess { newPosts ->
            if (newPosts.isEmpty()) {
                isLastPage = true
            } else {
                postsList.addAll(newPosts)
                communityAdapter.submitList(postsList.toList())
                lastVisiblePostId = newPosts.lastOrNull()?.id
                if (newPosts.size < 8) isLastPage = true
            }
        }
        isLoading = false
    }
}
