package uz.oybek.ozamiz.ui.feature.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.community.CommunityPostRepository
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.points.LevelSystem
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.databinding.FragmentCommunityProfileBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class CommunityProfileFragment : BaseFragment<FragmentCommunityProfileBinding>() {

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
        loadInitialData(bind, userId)
    }

    private fun setupAdapter(bind: FragmentCommunityProfileBinding, userId: String) {
        communityAdapter = CommunityPostAdapter(
            onLikeClick = { postId ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val currentUserId = Firebase.auth.currentUser?.uid ?: return@launch
                    CommunityPostRepository.toggleLike(postId, currentUserId)
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
                    val currentUserId = Firebase.auth.currentUser?.uid ?: return@launch
                    CommunityPostRepository.markViewed(postId, currentUserId)
                }
            },
            onDeleteClick = {

            }
        )
        bind.rvProfilePosts.adapter = communityAdapter
    }

    private fun setupScrollListener(bind: FragmentCommunityProfileBinding, userId: String) {
        // The layout has a NestedScrollView that contains the ConstraintLayout
        bind.root.findViewById<androidx.core.widget.NestedScrollView>(R.id.nestedScrollView)?.let { scrollView ->
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

    private fun loadInitialData(bind: FragmentCommunityProfileBinding, userId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            bind.progressLoading.isVisible = true
            bind.layoutStats.isVisible = false

            try {
                // 1. Fetch user data
                val dbRef = FirebasePaths.userReference(userId)
                val snapshot = dbRef.get().await()
                val user = snapshot.getValue(User::class.java)

                if (user != null) {
                    bind.tvStreak.text = "${user.streakInfo?.activeDays ?: 0} kun"
                    bind.tvPoints.text = "${user.scorePoints} ball"
                    bind.tvCurrentWeight.text = if (user.weight > 0) "${user.weight} kg" else "--"
                    bind.tvTargetWeight.text = if (user.targetWeight != null) "${user.targetWeight} kg" else "--"

                    val level = LevelSystem.getLevel(user.scorePoints)
                    bind.tvProfileLevel.text = level.name
                }

                // 2. Fetch first page of posts
                fetchPosts(userId)

            } catch (e: Exception) {
                // Handle error
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
        val result = CommunityPostRepository.getUserPosts(userId, 8, lastVisiblePostId)
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
