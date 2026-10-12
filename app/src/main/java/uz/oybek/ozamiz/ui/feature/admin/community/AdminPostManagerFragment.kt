package uz.oybek.ozamiz.ui.feature.admin.community

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.databinding.FragmentAdminPostManagerBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.toast

@AndroidEntryPoint
class AdminPostManagerFragment : BaseFragment<FragmentAdminPostManagerBinding>() {

    private val viewModel: AdminPostManagerViewModel by viewModels()
    private var adapter: AdminPostAdapter? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAdminPostManagerBinding {
        return FragmentAdminPostManagerBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAdminPostManagerBinding) {
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        viewModel.loadUnverifiedPosts()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { navigateUp() }
        binding.toolbar.inflateMenu(R.menu.menu_admin_post_manager)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_delete_all -> {
                    showDeleteAllConfirmDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = AdminPostAdapter(
            onApproveClick = { post ->
                viewModel.approvePost(post.id)
            },
            onDeleteClick = { post ->
                showDeleteConfirmDialog(post)
            }
        )
        binding.rvPosts.adapter = adapter
        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadUnverifiedPosts()
        }
    }

    private fun showDeleteConfirmDialog(post: CommunityPost) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.community_post_delete_confirm_title))
            .setMessage(getString(R.string.community_post_delete_confirm_message))
            .setPositiveButton(getString(R.string.admin_post_delete)) { _, _ ->
                viewModel.deletePost(post.id)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteAllConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.admin_posts_delete_all))
            .setMessage(getString(R.string.admin_posts_delete_all_confirm))
            .setPositiveButton(getString(R.string.admin_post_delete)) { _, _ ->
                viewModel.deleteAllPendingPosts()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderState(state)
                    }
                }
                launch {
                    viewModel.effect.collect { effect ->
                        handleEffect(effect)
                    }
                }
            }
        }
    }

    private fun renderState(state: AdminPostManagerUiState) {
        adapter?.submitList(state.posts)
        binding.tvEmpty.isVisible = state.posts.isEmpty() && !state.isLoading
        binding.toolbar.menu.findItem(R.id.action_delete_all)?.isVisible = state.posts.isNotEmpty()
        binding.progressBar.isVisible = state.isLoading && state.posts.isEmpty()
        binding.swipeRefresh.isRefreshing = state.isLoading && state.posts.isNotEmpty()
    }

    private fun handleEffect(effect: AdminPostManagerEffect) {
        when (effect) {
            is AdminPostManagerEffect.PostApprovedSuccess -> {
                toast(getString(R.string.admin_post_approved_success))
            }
            is AdminPostManagerEffect.PostDeletedSuccess -> {
                toast(getString(R.string.admin_post_deleted_success))
            }
            is AdminPostManagerEffect.AllPostsDeletedSuccess -> {
                toast(getString(R.string.admin_post_deleted_success))
            }
            is AdminPostManagerEffect.GenericError -> {
                toast(getString(R.string.admin_post_error))
            }
        }
    }

    override fun onDestroyView() {
        binding.rvPosts.adapter = null
        adapter = null
        super.onDestroyView()
    }
}
