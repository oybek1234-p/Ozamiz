package uz.oybek.ozamiz.ui.premium

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAdminPaymentVerificationListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.showToast

class AdminPaymentVerificationListFragment : BaseFragment<FragmentAdminPaymentVerificationListBinding>() {

    private val viewModel: AdminPaymentVerificationListViewModel by viewModels()
    private lateinit var adapter: AdminPaymentVerificationAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAdminPaymentVerificationListBinding {
        return FragmentAdminPaymentVerificationListBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAdminPaymentVerificationListBinding) {
        bind.toolbarAdminPayments.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        setupRecyclerView(bind)
        observeViewModel(bind)

        bind.swipeRefreshLayoutAdminPayments.setOnRefreshListener {
            viewModel.loadPendingVerifications()
        }
    }

    private fun setupRecyclerView(bind: FragmentAdminPaymentVerificationListBinding) {
        adapter = AdminPaymentVerificationAdapter { user ->
            // Navigate to detail screen
            val action = AdminPaymentVerificationListFragmentDirections
                .actionAdminPaymentVerificationListFragmentToAdminPaymentVerificationDetailFragment(user.uid)
            findNavController().navigate(action)
        }
        bind.recyclerViewAdminPayments.layoutManager = LinearLayoutManager(requireContext())
        bind.recyclerViewAdminPayments.adapter = adapter
    }

    private fun observeViewModel(bind: FragmentAdminPaymentVerificationListBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.progressBarAdminPayments.isVisible = state.isLoading
                    bind.swipeRefreshLayoutAdminPayments.isRefreshing = state.isLoading && state.pendingUsers.isNotEmpty()
                    bind.textViewNoPendingPayments.isVisible = !state.isLoading && state.pendingUsers.isEmpty() && state.errorMessage == null
                    bind.recyclerViewAdminPayments.isVisible = !state.isLoading && state.pendingUsers.isNotEmpty()

                    adapter.submitList(state.pendingUsers)

                    state.errorMessage?.let {
                        showToast(it)
                        bind.textViewNoPendingPayments.text = it
                        bind.textViewNoPendingPayments.isVisible = true
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload data when returning to this screen, in case a verification was processed
        viewModel.loadPendingVerifications()
    }

    override fun onDestroyView() {
        binding.recyclerViewAdminPayments.adapter = null
        super.onDestroyView()
    }
}