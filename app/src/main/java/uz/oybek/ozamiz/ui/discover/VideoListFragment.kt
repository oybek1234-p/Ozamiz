package uz.oybek.ozamiz.ui.discover // discover paketida

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.admin
import uz.oybek.ozamiz.databinding.FragmentVideoListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.getinfo.Event
import uz.oybek.ozamiz.ui.showToast

class VideoListFragment : BaseFragment<FragmentVideoListBinding>() {

    private val viewModel: VideoListViewModel by viewModels()
    private val args: VideoListFragmentArgs by navArgs() // Argumentlar categoryTag o'rniga categoryId bo'lishi kerak
    private lateinit var videoListAdapter: VideoListAdapter

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentVideoListBinding {
        return FragmentVideoListBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentVideoListBinding) {
        viewModel.initialize(args.categoryId, args.listTitle) // O'ZGARISH: categoryTag -> categoryId

        setupToolbar(bind)
        setupRecyclerView(bind)
        setupPagination(bind)
        setupAdminFeatures(bind)
        observeViewModel(bind)

        bind.swipeRefreshLayoutVideoList.setOnRefreshListener {
            viewModel.loadVideos(isInitialLoad = true, isRefresh = true) // O'ZGARISHsiz, viewModel o'zi categoryId ni biladi
        }
    }

    private fun setupToolbar(bind: FragmentVideoListBinding) {
        bind.toolbarVideoList.setNavigationOnClickListener { findNavController().popBackStack() }
        bind.toolbarVideoList.title = args.listTitle

        val addItem = bind.toolbarVideoList.menu.findItem(R.id.action_add_item_list)
        addItem?.isVisible = LocalUser.user.admin()
        addItem?.setOnMenuItemClickListener {
            viewModel.onAddVideoClick()
            true
        }
    }

    private fun setupRecyclerView(bind: FragmentVideoListBinding) {
        videoListAdapter = VideoListAdapter { video ->
            viewModel.onVideoClick(video)
        }
        bind.recyclerViewVideoList.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = videoListAdapter
            itemAnimator = null
        }
    }

    private fun setupPagination(bind: FragmentVideoListBinding) {
        bind.recyclerViewVideoList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return

                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount
                val threshold = 4

                val currentState = viewModel.uiState.value
                if (currentState.canLoadMore &&
                    !currentState.isLoadingNextPage &&
                    !currentState.isLoadingInitial &&
                    !currentState.isRefreshing &&
                    totalItemCount > 0 &&
                    totalItemCount <= (lastVisibleItemPosition + threshold)
                ) {
                    viewModel.loadNextPage()
                }
            }
        })
    }

    private fun setupAdminFeatures(bind: FragmentVideoListBinding) {
        bind.fabAddVideoListItem.isVisible = LocalUser.user.admin()
        bind.fabAddVideoListItem.setOnClickListener { viewModel.onAddVideoClick() }
    }

    private fun observeViewModel(bind: FragmentVideoListBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    bind.toolbarVideoList.title = state.listTitle
                    bind.progressBarVideoList.isVisible = state.isLoadingInitial && state.videos.isEmpty()
                    bind.swipeRefreshLayoutVideoList.isRefreshing = state.isRefreshing
                    bind.textViewEmptyVideoList.isVisible = state.videos.isEmpty() && !state.isLoadingInitial && !state.isRefreshing
                    videoListAdapter.submitList(state.videos)

                    state.errorMessage?.let {
                        showToast(it)
                        viewModel.clearError()
                    }
                }
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event: Event<VideoListNavigationEvent>? ->
            event?.getContentIfNotHandled()?.let { navEvent ->
                if (findNavController().currentDestination?.id != R.id.videoListFragment) return@let
                when (navEvent) {
                    is VideoListNavigationEvent.NavigateToVideoDetail -> {
                        val action = VideoListFragmentDirections.actionVideoListFragmentToVideoDetailFragment(
                            videoJson = Gson().toJson(navEvent.video)
                        )
                        findNavController().navigate(action)
                    }
                    is VideoListNavigationEvent.NavigateToAddVideo -> {
                        val action = VideoListFragmentDirections.actionVideoListFragmentToAddEditVideoFragment(null)
                        findNavController().navigate(action)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.recyclerViewVideoList.adapter = null
        super.onDestroyView()
    }
}