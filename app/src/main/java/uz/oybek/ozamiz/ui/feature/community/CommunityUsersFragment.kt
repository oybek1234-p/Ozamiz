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
import uz.oybek.ozamiz.data.user.model.UserSortType
import uz.oybek.ozamiz.databinding.FragmentCommunityUsersBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class CommunityUsersFragment : BaseFragment<FragmentCommunityUsersBinding>() {

    init {
        showBottomSheet = true
    }

    private val viewModel: CommunityUsersViewModel by viewModels()
    private lateinit var adapter: CommunityUsersAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCommunityUsersBinding {
        return FragmentCommunityUsersBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentCommunityUsersBinding) {
        setupRecyclerView()
        setupChips()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = CommunityUsersAdapter { user ->
            val bundle = Bundle().apply {
                putString("userId", user.uid)
                putString("userName", user.name)
            }
            findNavController().navigate(
                R.id.action_communityFragment_to_communityProfileFragment,
                bundle
            )
        }

        binding.rvUsers.adapter = adapter
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())

        binding.rvUsers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (!viewModel.state.value.isLoading && !viewModel.state.value.isPaging && !viewModel.state.value.hasReachedEnd) {
                    if (totalItemCount <= lastVisibleItem + 2) {
                        viewModel.loadUsers()
                    }
                }
            }
        })
    }

    private fun setupChips() {
        binding.chipGroupSort.setOnCheckedStateChangeListener { group, checkedIds ->
            val sortBy = when (checkedIds.firstOrNull()) {
                R.id.chipPoints -> UserSortType.MOST_POINTS
                R.id.chipActive -> UserSortType.MOST_ACTIVE
                R.id.chipRecent -> UserSortType.RECENT_ACTIVITY
                else -> UserSortType.MOST_POINTS
            }
            viewModel.setSortBy(sortBy)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                binding.progressBar.isVisible = state.isLoading
                adapter.submitList(state.users)
                binding.tvEmpty.isVisible = !state.isLoading && state.users.isEmpty()
                
                state.error?.let {
                    android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
