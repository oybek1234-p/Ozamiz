package uz.oybek.ozamiz.ui.foods

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import uz.oybek.ozamiz.databinding.FragmentYoutubePlayerBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.showToast

class YoutubePlayerFragment : BaseFragment<FragmentYoutubePlayerBinding>() {

    private val args: YoutubePlayerFragmentArgs by navArgs()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentYoutubePlayerBinding {
        return FragmentYoutubePlayerBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentYoutubePlayerBinding) {
        lifecycle.addObserver(bind.youtubePlayerView)

        bind.youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                val videoId = args.videoId
                if (videoId.isNotBlank()) {
                    youTubePlayer.loadVideo(videoId, 0f)
                } else {
                    showToast("Video ID topilmadi")
                    findNavController().popBackStack()
                }
            }
        })

        bind.toolbarYoutubePlayer.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Muhim: Pleylistni to'g'ri tozalash
        binding.youtubePlayerView.release()
    }
}