package uz.oybek.ozamiz.ui.feature.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentCommunityBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.data.user.UserRepository

class CommunityFragment : BaseFragment<FragmentCommunityBinding>() {

    init {
        showBottomSheet = true
    }

    private val viewModel: CommunityViewModel by viewModels()
    private lateinit var adapter: CommunityPostAdapter

    override fun viewCreated(bind: FragmentCommunityBinding) {
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        observeNotifications()
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCommunityBinding {
        return FragmentCommunityBinding.inflate(inflater, container, false)
    }

    private fun setupToolbar() {
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_communityFragment_to_notificationsFragment)
        }
        binding.fabAddPost.setOnClickListener {
            findNavController().navigate(R.id.action_communityFragment_to_addPostFragment)
        }
    }

    private fun setupRecyclerView() {
        adapter = CommunityPostAdapter(
            onLikeClick = { postId -> viewModel.toggleLike(postId) },
            onUserClick = { userId, userName ->
                val bundle = Bundle().apply {
                    putString("userId", userId)
                    putString("userName", userName)
                }
                findNavController().navigate(
                    R.id.action_communityFragment_to_communityProfileFragment,
                    bundle
                )
            },
            onViewed = { postId -> viewModel.markViewed(postId) },
            onDeleteClick = { post ->
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("O'chirish")
                    .setMessage("Haqiqatan ham ushbu postni o'chirmoqchimisiz?")
                    .setPositiveButton("Ha") { _, _ ->
                        viewModel.deletePost(post.id)
                    }
                    .setNegativeButton("Yo'q", null)
                    .show()
            }
        )

        binding.rvPosts.adapter = adapter
        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())

        binding.rvPosts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (!viewModel.state.value.isLoading && !viewModel.state.value.isPaging && !viewModel.state.value.hasReachedEnd) {
                    if (totalItemCount <= lastVisibleItem + 2) {
                        viewModel.loadPosts()
                    }
                }
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                binding.swipeRefresh.isRefreshing = state.isLoading
                binding.progressBar.isVisible = state.isPaging

                adapter.submitList(state.posts)

                binding.tvEmpty.isVisible = !state.isLoading && state.posts.isEmpty()
            }
        }
    }

    private fun observeNotifications() {
        UserRepository.observeUnReadMessages { count ->
            binding.tvNotificationBadge.isVisible = count > 0
            binding.tvNotificationBadge.text = count.toString()
        }
    }
}
