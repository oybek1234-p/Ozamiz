package uz.oybek.ozamiz.ui.feature.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentCommunityBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.data.community.model.CommunityPostFilter.ALL
import uz.oybek.ozamiz.data.community.model.CommunityPostFilter.WEIGHT_LOSS
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.data.user.model.UserSortType
import uz.oybek.ozamiz.ui.feature.premium.isPremium

class CommunityFragment : BaseFragment<FragmentCommunityBinding>() {

    private val postsViewModel: CommunityViewModel by viewModels()
    private val usersViewModel: CommunityUsersViewModel by viewModels()

    private lateinit var postsAdapter: CommunityPostAdapter
    private lateinit var usersAdapter: CommunityUsersAdapter
    private var lastSelectedChipId: Int = R.id.chipOzganlar

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
        setupFilters()

        val selectWeightLoss = arguments?.getBoolean("selectWeightLoss", false) ?: false
        if (selectWeightLoss) {
            arguments?.remove("selectWeightLoss")
            binding.chipGroupFilter.check(R.id.chipOzganlar)
        } else {
            restoreFilterState()
        }

        observeViewModel()
        observeNotifications()
        loadBanner()
    }

    private fun loadBanner() {
        if (user.isPremium()) {
            binding.bannerContainer.isVisible = false
            return
        }
        try {
            val bannerView = uz.oybek.ozamiz.data.ads.AdsManager.createBannerView(
                requireContext(),
                uz.oybek.ozamiz.data.ads.AdsConfig.BANNER_HOME
            )
            binding.bannerContainer.removeAllViews()
            binding.bannerContainer.addView(bannerView)
            binding.bannerContainer.isVisible = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun restoreFilterState() {
        val chipToCheck = lastSelectedChipId
        if (chipToCheck != binding.chipGroupFilter.checkedChipId) {
            binding.chipGroupFilter.check(chipToCheck)
        }
        val isUsers = chipToCheck == R.id.chipUsers
        binding.rvPosts.isVisible = !isUsers
        binding.rvUsers.isVisible = isUsers
        if (isUsers) {
            usersAdapter.submitList(usersViewModel.state.value.users)
        } else {
            postsAdapter.submitList(postsViewModel.state.value.posts)
        }
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
            binding.toolbar.navigationIcon = null
        }
        binding.btnNotifications.setOnClickListener {
            navigate(R.id.action_communityFragment_to_notificationsFragment)
        }
        binding.fabAddPost.setOnClickListener {
            navigate(R.id.action_communityFragment_to_addPostFragment)
        }
    }

    private fun setupRecyclerViews() {
        postsAdapter = CommunityPostAdapter(
            onLikeClick = { postId ->
                postsViewModel.toggleLike(postId)
            },
            onUserClick = { userId, userName ->
                val bundle = Bundle().apply {
                    putString("userId", userId)
                    putString("userName", userName)
                }
                navigate(
                    R.id.action_communityFragment_to_communityProfileFragment,
                    bundle
                )
            },
            onViewed = { postId -> postsViewModel.markViewed(postId) },
            onDeleteClick = { post ->
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("O'chirish")
                    .setMessage("Haqiqatan ham ushbu postni o'chirmoqchimisiz?")
                    .setPositiveButton("Ha") { _, _ ->
                        postsViewModel.deletePost(post.id)
                    }
                    .setNegativeButton("Yo'q", null)
                    .show()
            }
        )
        binding.rvPosts.adapter = postsAdapter
        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())

        binding.rvPosts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (!postsViewModel.state.value.isLoading && !postsViewModel.state.value.isPaging && !postsViewModel.state.value.hasReachedEnd) {
                    if (lastVisibleItem + 5 >= totalItemCount) {
                        postsViewModel.loadPosts()
                    }
                }
            }
        })

        usersAdapter = CommunityUsersAdapter { user ->
            val bundle = Bundle().apply {
                putString("userId", user.uid)
                putString("userName", user.name)
            }
            navigate(
                R.id.action_communityFragment_to_communityProfileFragment,
                bundle
            )
        }
        binding.rvUsers.adapter = usersAdapter
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())

        binding.rvUsers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (!usersViewModel.state.value.isLoading && !usersViewModel.state.value.isPaging && !usersViewModel.state.value.hasReachedEnd) {
                    if (totalItemCount <= lastVisibleItem + 2) {
                        usersViewModel.loadUsers()
                    }
                }
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            val checkedId = binding.chipGroupFilter.checkedChipId
            if (checkedId == R.id.chipUsers) {
                usersViewModel.refresh()
            } else {
                postsViewModel.refresh()
            }
        }
    }

    private fun setupFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            lastSelectedChipId = checkedId
            
            val isUsers = checkedId == R.id.chipUsers
            binding.rvPosts.isVisible = !isUsers
            binding.rvUsers.isVisible = isUsers

            when (checkedId) {
                R.id.chipOzganlar -> {
                    postsViewModel.setFilter(WEIGHT_LOSS)
                    val postsState = postsViewModel.state.value
                    binding.swipeRefresh.isRefreshing = postsState.isLoading
                    binding.progressBar.isVisible = postsState.isPaging
                    binding.tvEmpty.isVisible = !postsState.isLoading && postsState.posts.isEmpty()
                    binding.tvEmpty.text = "Xatolik yuz berdi"
                }
                R.id.chipUsers -> {
                    usersViewModel.setSortBy(UserSortType.RECENT_ACTIVITY)
                    val usersState = usersViewModel.state.value
                    binding.swipeRefresh.isRefreshing = usersState.isLoading
                    binding.progressBar.isVisible = usersState.isPaging
                    binding.tvEmpty.isVisible = !usersState.isLoading && usersState.users.isEmpty()
                    binding.tvEmpty.text = getString(R.string.community_empty_users)
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            postsViewModel.state.collect { state ->
                val isPostFilter = binding.chipGroupFilter.checkedChipId != R.id.chipUsers
                if (isPostFilter) {
                    binding.swipeRefresh.isRefreshing = state.isLoading
                    binding.progressBar.isVisible = state.isPaging
                    postsAdapter.submitList(state.posts)
                    binding.tvEmpty.isVisible = !state.isLoading && state.posts.isEmpty()
                    val emptyText = if (binding.chipGroupFilter.checkedChipId == R.id.chipOzganlar) 
                        "Xatolik yuz berdi" else getString(R.string.community_empty_posts)
                    binding.tvEmpty.text = emptyText
                }
                state.error?.let {
                    android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            usersViewModel.state.collect { state ->
                val isUserFilter = binding.chipGroupFilter.checkedChipId == R.id.chipUsers
                if (isUserFilter) {
                    binding.swipeRefresh.isRefreshing = state.isLoading
                    binding.progressBar.isVisible = state.isPaging
                    usersAdapter.submitList(state.users)
                    binding.tvEmpty.isVisible = !state.isLoading && state.users.isEmpty()
                    binding.tvEmpty.text = getString(R.string.community_empty_users)
                }
                state.error?.let {
                    android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeNotifications() {
        postsViewModel.observeUnReadMessages { count ->
            binding.tvNotificationBadge.isVisible = count > 0
            binding.tvNotificationBadge.text = count.toString()
        }
    }
}
