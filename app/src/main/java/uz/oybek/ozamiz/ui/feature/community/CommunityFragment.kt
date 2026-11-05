package uz.oybek.ozamiz.ui.feature.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.community.model.CommunityPostFilter
import uz.oybek.ozamiz.databinding.FragmentCommunityBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.community.comment.PostCommentsBottomSheet
import uz.oybek.ozamiz.ui.utils.toBundle
import uz.oybek.ozamiz.ui.utils.toast

@AndroidEntryPoint
class CommunityFragment : BaseFragment<FragmentCommunityBinding>() {

    private val viewModel: CommunityViewModel by viewModels()

    private var postsAdapter: CommunityPostAdapter? = null
    private var postsLoadingAdapter: PagingLoadItemAdapter? = null

    private var usersAdapter: CommunityUsersAdapter? = null
    private var usersLoadingAdapter: PagingLoadItemAdapter? = null

    private var isManualRefresh = false
    private var shouldScrollToTopOnNextList = true

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCommunityBinding {
        return FragmentCommunityBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentCommunityBinding) {
        setupToolbar()
        setupRecyclerViews()
        setupSwipeRefresh()
        setupListeners()
        observeState()
    }

    private fun setupToolbar() {
        val showBackButton = arguments?.getBoolean("showBackButton", false) ?: false
        if (showBackButton) {
            showBottomSheet = false
            binding.toolbar.setNavigationIcon(R.drawable.arrow_back)
            binding.toolbar.setNavigationOnClickListener {
                popBackStack()
            }
        } else {
            showBottomSheet = true
            binding.toolbar.navigationIcon = null
        }
    }

    private fun setupRecyclerViews() {
        postsAdapter = CommunityPostAdapter(
            currentUserId = viewModel.uiState.value.currentUserId,
            onLikeClick = { post -> viewModel.toggleLike(post) },
            onCommentClick = { post -> openCommentsSheet(post) },
            onUserClick = { userId, userName -> navigateToUserProfile(userId, userName) },
            onDeleteClick = { post -> showDeleteDialog(post) }
        )
        postsLoadingAdapter = PagingLoadItemAdapter {
            viewModel.loadNextPage()
        }
        val concatPostsAdapter = ConcatAdapter(postsAdapter!!, postsLoadingAdapter!!)
        binding.rvPosts.setHasFixedSize(true)
        binding.rvPosts.adapter = concatPostsAdapter
        binding.rvPosts.addOnScrollListener(createPaginationScrollListener())

        usersAdapter = CommunityUsersAdapter { user ->
            navigateToUserProfile(user.uid, user.name)
        }
        usersLoadingAdapter = PagingLoadItemAdapter {
            viewModel.loadNextPage()
        }
        val concatUsersAdapter = ConcatAdapter(usersAdapter!!, usersLoadingAdapter!!)
        binding.rvUsers.setHasFixedSize(true)
        binding.rvUsers.adapter = concatUsersAdapter
        binding.rvUsers.addOnScrollListener(createPaginationScrollListener())
    }

    private fun createPaginationScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (lastVisibleItem + 4 >= totalItemCount) {
                    viewModel.loadNextPage()
                }
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            isManualRefresh = true
            viewModel.refresh()
        }
    }

    private fun setupListeners() {
        binding.fabAddPost.setOnClickListener {
            navigate(R.id.addPostFragment)
        }

        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val targetFilter = when {
                checkedIds.contains(binding.chipOzganlar.id) -> CommunityPostFilter.WEIGHT_LOSS
                checkedIds.contains(binding.chipFood.id) -> CommunityPostFilter.FOOD
                checkedIds.contains(binding.chipUsers.id) -> CommunityPostFilter.ACTIVE_USERS
                else -> CommunityPostFilter.ALL
            }
            if (viewModel.uiState.value.filter != targetFilter) {
                shouldScrollToTopOnNextList = true
                viewModel.setFilter(targetFilter)
            }
        }
    }

    private fun openCommentsSheet(post: CommunityPost) {
        val sheet = PostCommentsBottomSheet.newInstance(post.id)
        sheet.onCommentCountChanged = { targetPostId, delta ->
            viewModel.updateCommentCount(targetPostId, delta)
        }
        sheet.show(childFragmentManager, PostCommentsBottomSheet.TAG)
    }

    private fun navigateToUserProfile(userId: String, userName: String) {
        navigate(
            R.id.communityProfileFragment,
            CommunityProfileArgs(userId = userId, userName = userName).toBundle()
        )
    }

    private fun showDeleteDialog(post: CommunityPost) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.community_post_delete_confirm_title))
            .setMessage(getString(R.string.community_post_delete_confirm_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deletePost(post)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderState(state)
                    }
                }

                launch {
                    viewModel.effect.collect { effect ->
                        when (effect) {
                            is CommunityUiEffect.PostDeletedSuccess -> {
                                toast(getString(R.string.community_post_delete_success))
                            }
                            is CommunityUiEffect.GenericError -> {
                                toast(getString(R.string.community_posts_error))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? uz.oybek.ozamiz.ui.MainActivity)?.clearCommunityBadge()
        shouldScrollToTopOnNextList = true
        binding.rvPosts.scrollToPosition(0)
        isManualRefresh = false
        viewModel.refresh()
    }

    private fun renderState(state: CommunityUiState) {
        // Sync filter chip selection
        when (state.filter) {
            CommunityPostFilter.WEIGHT_LOSS -> if (!binding.chipOzganlar.isChecked) binding.chipOzganlar.isChecked = true
            CommunityPostFilter.FOOD -> if (!binding.chipFood.isChecked) binding.chipFood.isChecked = true
            CommunityPostFilter.ACTIVE_USERS -> if (!binding.chipUsers.isChecked) binding.chipUsers.isChecked = true
            CommunityPostFilter.ALL -> if (!binding.chipAll.isChecked) binding.chipAll.isChecked = true
        }

        binding.tvHeaderTitle.text = getString(R.string.community_main_title)

        val isUsers = state.filter == CommunityPostFilter.ACTIVE_USERS
        binding.rvPosts.isVisible = !isUsers
        binding.rvUsers.isVisible = isUsers

        if (state.isLoading) {
            if (isManualRefresh) {
                binding.swipeRefresh.isRefreshing = true
                binding.progressTopSmall.isVisible = false
                binding.progressBar.isVisible = false
            } else if (state.posts.isNotEmpty()) {
                binding.swipeRefresh.isRefreshing = false
                binding.progressTopSmall.isVisible = true
                binding.progressBar.isVisible = false
            } else {
                binding.swipeRefresh.isRefreshing = false
                binding.progressTopSmall.isVisible = false
                binding.progressBar.isVisible = true
            }
        } else {
            isManualRefresh = false
            binding.swipeRefresh.isRefreshing = false
            binding.progressTopSmall.isVisible = false
            binding.progressBar.isVisible = false
        }

        // Pagination loading item at bottom
        postsLoadingAdapter?.isVisible = !isUsers && state.isPaging
        usersLoadingAdapter?.isVisible = isUsers && state.isPaging

        if (isUsers) {
            usersAdapter?.submitList(state.users) {
                if (shouldScrollToTopOnNextList) {
                    binding.rvUsers.post {
                        binding.rvUsers.scrollToPosition(0)
                    }
                    shouldScrollToTopOnNextList = false
                }
            }
            val isEmpty = !state.isLoading && state.users.isEmpty()
            binding.layoutEmpty.isVisible = isEmpty
            binding.tvEmpty.text = getString(R.string.community_posts_empty)
        } else {
            postsAdapter?.updateCurrentUserId(state.currentUserId)
            postsAdapter?.updateLastSeenTimestamp(state.lastSeenTimestamp)

            postsAdapter?.submitList(state.posts) {
                if (shouldScrollToTopOnNextList) {
                    binding.rvPosts.post {
                        binding.rvPosts.scrollToPosition(0)
                    }
                    shouldScrollToTopOnNextList = false
                }
            }
            val isEmpty = !state.isLoading && state.posts.isEmpty()
            binding.layoutEmpty.isVisible = isEmpty
            binding.tvEmpty.text = getString(R.string.community_posts_empty)
        }
    }

    override fun onDestroyView() {
        postsAdapter = null
        postsLoadingAdapter = null
        usersAdapter = null
        usersLoadingAdapter = null
        super.onDestroyView()
    }
}
