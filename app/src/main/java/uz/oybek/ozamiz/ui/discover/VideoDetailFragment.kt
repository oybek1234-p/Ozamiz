package uz.oybek.ozamiz.ui.discover // discover paketida

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.gson.Gson
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.admin
import uz.oybek.ozamiz.data.workouts.Video
import uz.oybek.ozamiz.databinding.FragmentVideoDetailBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.getinfo.Event
import uz.oybek.ozamiz.ui.showToast

class VideoDetailFragment : BaseFragment<FragmentVideoDetailBinding>() {

    private val viewModel: VideoDetailViewModel by viewModels()
    private val args: VideoDetailFragmentArgs by navArgs()
    private var youTubePlayer: YouTubePlayer? = null

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentVideoDetailBinding {
        return FragmentVideoDetailBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentVideoDetailBinding) {
        lifecycle.addObserver(bind.youtubePlayerView) // YouTubePlayerView lifecycle management
        setupToolbar(bind)
        observeViewModel(bind)
        viewModel.loadVideo(args.videoJson)
    }

    private fun setupToolbar(bind: FragmentVideoDetailBinding) {
        bind.toolbarVideoDetail.setNavigationOnClickListener { findNavController().popBackStack() }
        bind.toolbarVideoDetail.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit_video -> {
                    viewModel.onEditVideoClick()
                    true
                }
                R.id.action_delete_video -> {
                    showDeleteConfirmationDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun observeViewModel(bind: FragmentVideoDetailBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    bind.progressBarVideoDetail.isVisible = state.isLoading
                    state.video?.let { video ->
                        displayVideoData(bind, video)
                        initializePlayer(bind, video.youtubeVideoId)
                    }

                    val editMenuItem = bind.toolbarVideoDetail.menu.findItem(R.id.action_edit_video)
                    val deleteMenuItem = bind.toolbarVideoDetail.menu.findItem(R.id.action_delete_video)
                    val isAdmin = LocalUser.user.admin()
                    editMenuItem?.isVisible = isAdmin
                    deleteMenuItem?.isVisible = isAdmin

                    state.errorMessage?.let {
                        showToast(it)
                        viewModel.clearError()
                    }
                }
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event: Event<VideoDetailNavigationEvent>? ->
            event?.getContentIfNotHandled()?.let { navEvent ->
                if (findNavController().currentDestination?.id != R.id.videoDetailFragment) return@let
                when (navEvent) {
                    is VideoDetailNavigationEvent.NavigateToEditVideo -> {
                        val action = VideoDetailFragmentDirections.actionVideoDetailFragmentToAddEditVideoFragment(
                            videoJson = Gson().toJson(navEvent.video)
                        )
                        findNavController().navigate(action)
                    }
                    is VideoDetailNavigationEvent.NavigateBackWithMessage -> {
                        showToast(navEvent.message)
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun displayVideoData(bind: FragmentVideoDetailBinding, video: Video) {
        bind.toolbarVideoDetail.title = video.name // Toolbar title
        bind.textViewVideoTitleDetail.text = video.name
        bind.textViewVideoDescriptionDetail.text = video.description ?: "Tavsif mavjud emas."
        bind.textViewVideoDuration.text = video.durationMinutes?.let { "$it daqiqa" } ?: "Davomiyligi noma'lum"
        bind.textViewVideoDuration.isVisible = video.durationMinutes != null
    }

    private fun initializePlayer(bind: FragmentVideoDetailBinding, videoId: String?) {
        if (videoId.isNullOrBlank()) {
            bind.youtubePlayerView.isVisible = false
            bind.textViewPlayerError.isVisible = true
            bind.textViewPlayerError.text = "Video ID topilmadi."
            return
        }
        bind.youtubePlayerView.isVisible = true
        bind.textViewPlayerError.isVisible = false

        val playerOptions = IFramePlayerOptions.Builder()
            .controls(1) // 0 for no controls, 1 for controls, 2 for minimal controls
            .rel(0) // Show related videos from the same channel (0) or not (1)
            .autoplay(1)
            .build()

        bind.youtubePlayerView.initialize(object : AbstractYouTubePlayerListener() {
            override fun onReady(player: YouTubePlayer) {
                this@VideoDetailFragment.youTubePlayer = player
                player.cueVideo(videoId, 0f)
            }
            override fun onError(player: YouTubePlayer, error: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerError) {
                super.onError(player, error)
                bind.youtubePlayerView.isVisible = false
                bind.textViewPlayerError.isVisible = true
                bind.textViewPlayerError.text = "Videoni yuklashda xatolik: ${error.name}"
            }
        }, true, playerOptions) // true for handling network recovery
    }


    private fun showDeleteConfirmationDialog() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("Videoni o'chirish")
            .setMessage("Haqiqatan ham bu videoni o'chirmoqchimisiz?")
            .setPositiveButton(R.string.yes_delete) { dialog, _ ->
                viewModel.deleteVideo()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.youtubePlayerView.release() // Muhim!
    }
}