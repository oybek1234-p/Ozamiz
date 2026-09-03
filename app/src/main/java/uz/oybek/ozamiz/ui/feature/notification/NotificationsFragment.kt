package uz.oybek.ozamiz.ui.feature.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.databinding.FragmentNotificationsBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class NotificationsFragment : BaseFragment<FragmentNotificationsBinding>() {

    private val viewModel: NotificationsViewModel by viewModels()
    private lateinit var adapter: NotificationsAdapter

    override fun viewCreated(bind: FragmentNotificationsBinding) {
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        
        viewModel.loadNotifications()
        viewModel.markAllAsRead()
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNotificationsBinding {
        return FragmentNotificationsBinding.inflate(inflater, container, false)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { navigateUp() }
    }

    private fun setupRecyclerView() {
        adapter = NotificationsAdapter { notification ->
            if (notification.postId != null) {
                // Navigate to post detail if implemented
            }
        }
        binding.rvNotifications.adapter = adapter
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notifications.collect { list ->
                adapter.submitList(list)
                binding.tvEmpty.isVisible = list.isEmpty()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                binding.progressBar.isVisible = loading
            }
        }
    }
}
