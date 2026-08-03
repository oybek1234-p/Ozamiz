package uz.oybek.ozamiz.ui.feature.community

import uz.oybek.ozamiz.data.community.model.CommunityPostFilter.ALL
import uz.oybek.ozamiz.data.community.model.CommunityPostFilter.FOOD
import uz.oybek.ozamiz.data.community.model.CommunityPostFilter.WEIGHT_LOSS

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
import uz.oybek.ozamiz.databinding.FragmentCommunityPostsBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class CommunityPostsFragment : BaseFragment<FragmentCommunityPostsBinding>() {
    
    init {
        showBottomSheet = true
    }

    private val viewModel: CommunityViewModel by viewModels()
    private lateinit var adapter: CommunityPostAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCommunityPostsBinding {
        return FragmentCommunityPostsBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentCommunityPostsBinding) {
        setupRecyclerView()
        setupSwipeRefresh()
        setupFilters()
        observeViewModel()
    }

    private fun setupFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chipAll -> ALL
                R.id.chipOzganlar -> WEIGHT_LOSS
                else -> ALL
            }
            viewModel.setFilter(filter)
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
                    if (lastVisibleItem + 5 >= totalItemCount) {
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
                
                state.error?.let {
                    android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
