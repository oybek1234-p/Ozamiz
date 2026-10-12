package uz.oybek.ozamiz.ui.feature.notification

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentNotificationsBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.community.detail.PostDetailArgs
import uz.oybek.ozamiz.ui.feature.menu.list.MenuLoadStateAdapter
import uz.oybek.ozamiz.ui.utils.collectFlowOnStart
import uz.oybek.ozamiz.ui.utils.toBundle

@AndroidEntryPoint
class NotificationsFragment : BaseFragment<FragmentNotificationsBinding>() {

    private val viewModel: NotificationsViewModel by viewModels()
    private lateinit var adapter: NotificationsAdapter

    override fun viewCreated(bind: FragmentNotificationsBinding) {
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()

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
            val postId = notification.targetId?.takeIf { it.isNotBlank() }
                ?: notification.extraData?.get("postId")?.takeIf { it.isNotBlank() }

            if (!postId.isNullOrBlank()) {
                val args = PostDetailArgs(postId, notification.notificationType)
                navigate(R.id.postDetailFragment, args.toBundle())
            }
        }



        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NotificationsFragment.adapter.withLoadStateFooter(
                footer = MenuLoadStateAdapter { this@NotificationsFragment.adapter.retry() }
            )
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            adapter.refresh()
        }

        binding.btnRetry.setOnClickListener {
            adapter.retry()
        }

        adapter.addLoadStateListener { loadState ->
            val isRefreshing = loadState.refresh is LoadState.Loading
            val isError = loadState.refresh is LoadState.Error
            val isEmpty = loadState.refresh is LoadState.NotLoading && adapter.itemCount == 0

            binding.progressBar.isVisible = isRefreshing && !binding.swipeRefresh.isRefreshing
            binding.layoutEmpty.isVisible = isEmpty
            binding.layoutError.isVisible = isError && adapter.itemCount == 0
            binding.rvNotifications.isVisible = !isEmpty && !(isError && adapter.itemCount == 0)

            if (!isRefreshing) {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun observeViewModel() {
        collectFlowOnStart(viewModel.notificationsFlow) {
            adapter.submitData(it)
        }
    }
}
