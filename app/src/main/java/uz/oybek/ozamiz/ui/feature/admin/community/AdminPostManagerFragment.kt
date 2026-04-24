package uz.oybek.ozamiz.ui.feature.admin.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.databinding.FragmentAdminPostManagerBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.community.CommunityPostAdapter

class AdminPostManagerFragment : BaseFragment<FragmentAdminPostManagerBinding>() {

    private val viewModel: AdminPostManagerViewModel by viewModels()
    private lateinit var adapter: CommunityPostAdapter

    override fun viewCreated(bind: FragmentAdminPostManagerBinding) {
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        viewModel.loadUnverifiedPosts()
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAdminPostManagerBinding {
        return FragmentAdminPostManagerBinding.inflate(inflater, container, false)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    private fun setupRecyclerView() {
        adapter = CommunityPostAdapter(
            onLikeClick = {}, // No likes in admin view
            onUserClick = { userId, _ -> },
            onViewed = {},
            onDeleteClick = { post -> viewModel.deletePost(post.id) },
            onVerifyClick = { post -> viewModel.verifyPost(post.id) }
        )
        binding.rvPosts.adapter = adapter
        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.posts.collect { list ->
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
