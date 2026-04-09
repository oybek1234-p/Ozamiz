package uz.oybek.ozamiz.ui.feature.admin.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAdminUsersListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class AdminUsersListFragment : BaseFragment<FragmentAdminUsersListBinding>() {

    private val viewModel: AdminUsersListViewModel by viewModels()
    private lateinit var adapter: AdminUsersAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAdminUsersListBinding {
        return FragmentAdminUsersListBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAdminUsersListBinding) {
        setupRecyclerView(bind)
        setupToolbar(bind)
        observeViewModel(bind)
    }

    private fun setupToolbar(bind: FragmentAdminUsersListBinding) {
        bind.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView(bind: FragmentAdminUsersListBinding) {
        adapter = AdminUsersAdapter { selectedUser ->
            val bundle = bundleOf("userId" to selectedUser.uid)
            findNavController().navigate(R.id.action_adminUsersListFragment_to_adminUserDetailsFragment, bundle)
        }
        bind.recyclerUsers.adapter = adapter
    }

    private fun observeViewModel(bind: FragmentAdminUsersListBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.users.collect { users ->
                        adapter.submitList(users)
                    }
                }
                launch {
                    viewModel.isLoading.collect { loading ->
                        bind.progressBar.isVisible = loading
                    }
                }
            }
        }
    }
}
