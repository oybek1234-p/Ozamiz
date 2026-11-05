package uz.oybek.ozamiz.ui.feature.admin.users

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAdminUsersListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

@AndroidEntryPoint
class AdminUsersListFragment : BaseFragment<FragmentAdminUsersListBinding>() {

    private val viewModel: AdminUsersListViewModel by viewModels()
    private var adapter: AdminUsersAdapter? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAdminUsersListBinding {
        return FragmentAdminUsersListBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAdminUsersListBinding) {
        setupRecyclerView(bind)
        setupToolbar(bind)
        setupFilters(bind)
        observeViewModel(bind)
    }

    private fun setupFilters(bind: FragmentAdminUsersListBinding) {
        bind.chipPremiumOnly.setOnCheckedChangeListener { _, isChecked ->
            viewModel.togglePremiumFilter(isChecked)
        }
        bind.editTextSearch.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }
    }

    private fun setupToolbar(bind: FragmentAdminUsersListBinding) {
        bind.toolbar.setNavigationOnClickListener {
            navigateUp()
        }
    }

    private fun setupRecyclerView(bind: FragmentAdminUsersListBinding) {
        val usersAdapter = AdminUsersAdapter { selectedUser ->
            val bundle = bundleOf("userId" to selectedUser.uid)
            navigate(R.id.adminUserDetailsFragment, bundle)
        }
        adapter = usersAdapter
        bind.recyclerUsers.adapter = usersAdapter

        bind.recyclerUsers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                if (lastVisibleItemPosition >= totalItemCount - 3 && totalItemCount >= 20) {
                    viewModel.loadNextPage()
                }
            }
        })
    }

    private fun observeViewModel(bind: FragmentAdminUsersListBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter?.submitList(state.users)
                    bind.progressBar.isVisible = state.isLoading && state.users.isEmpty()
                    bind.progressBarBottom.isVisible = state.isLoading && state.users.isNotEmpty()
                    bind.textViewEmpty.isVisible = !state.isLoading && state.users.isEmpty() && state.searchQuery.isNotBlank()
                    if (bind.chipPremiumOnly.isChecked != state.isPremiumFilterActive) {
                        bind.chipPremiumOnly.isChecked = state.isPremiumFilterActive
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        bindingT?.recyclerUsers?.adapter = null
        adapter = null
        super.onDestroyView()
    }
}
