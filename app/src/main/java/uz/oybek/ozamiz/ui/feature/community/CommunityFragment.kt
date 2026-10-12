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
            viewModel.refresh()
        }
    }

    private fun setupListeners() {
        binding.fabAddPost.setOnClickListener {
            navigate(R.id.addPostFragment)
        }

        val openFilter = {
            PostFilterBottomSheet(
                currentFilter = viewModel.uiState.value.filter
            ) { selectedFilter ->
                viewModel.setFilter(selectedFilter)
            }.show(childFragmentManager, PostFilterBottomSheet.TAG)
        }

        binding.layoutFilterSelector.setOnClickListener { openFilter() }
        binding.btnFilter.setOnClickListener { openFilter() }
    }

    private fun openCommentsSheet(post: CommunityPost) {
        val sheet = PostCommentsBottomSheet(post.id)
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

    private fun renderState(state: CommunityUiState) {
        // Title update based on filter
        val titleRes = when (state.filter) {
            CommunityPostFilter.ALL -> R.string.community_main_title
            CommunityPostFilter.WEIGHT_LOSS -> R.string.community_filter_weight_loss
            CommunityPostFilter.FOOD -> R.string.community_filter_food
            CommunityPostFilter.ACTIVE_USERS -> R.string.community_filter_active_users
        }
        binding.tvHeaderTitle.text = getString(titleRes)

        val isUsers = state.filter == CommunityPostFilter.ACTIVE_USERS
        binding.rvPosts.isVisible = !isUsers
        binding.rvUsers.isVisible = isUsers

        binding.swipeRefresh.isRefreshing = state.isLoading
        binding.progressBar.isVisible = false

        // Pagination loading item at bottom
        postsLoadingAdapter?.isVisible = !isUsers && state.isPaging
        usersLoadingAdapter?.isVisible = isUsers && state.isPaging

        postsAdapter?.updateCurrentUserId(state.currentUserId)

        if (isUsers) {
            usersAdapter?.submitList(state.users)
            val isEmpty = !state.isLoading && state.users.isEmpty()
            binding.layoutEmpty.isVisible = isEmpty
            binding.tvEmpty.text = getString(R.string.community_users_empty)
        } else {
            postsAdapter?.submitList(state.posts)
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
