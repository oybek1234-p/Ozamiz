package uz.oybek.ozamiz.ui.feature.exercises

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentWeightLossVideoDetailBinding
import uz.oybek.ozamiz.databinding.ItemBenefitBulletBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.getArgs
import uz.oybek.ozamiz.ui.utils.loadPhoto
import uz.oybek.ozamiz.ui.utils.showToast

class WeightLossVideoDetailFragment : BaseFragment<FragmentWeightLossVideoDetailBinding>() {

    private val viewModel: WeightLossVideoDetailViewModel by viewModels()

    init {
        showBottomSheet = false
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWeightLossVideoDetailBinding {
        return FragmentWeightLossVideoDetailBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentWeightLossVideoDetailBinding) {
        val videoId = arguments.getArgs<WeightLossVideoArgs>()?.videoId
            ?: arguments?.getString("videoId").orEmpty()
        viewModel.loadVideo(videoId)

        bind.toolbar.setNavigationOnClickListener {
            navigateUp()
        }

        bind.btnDone.setOnClickListener {
            viewModel.completeWorkout()
        }

        bind.layoutVideoPlayerPlaceholder.setOnClickListener {
            val video = viewModel.uiState.value?.video ?: return@setOnClickListener
            val videoId = video.youtubeVideoId
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
            try {
                requireContext().startActivity(appIntent)
            } catch (ex: Exception) {
                try {
                    requireContext().startActivity(webIntent)
                } catch (e: Exception) {
                    showToast(getString(R.string.food_add_error))
                }
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val video = state.video ?: return@observe

            bind.toolbar.title = video.name
            bind.textVideoTitle.text = video.name
            bind.textBurnedCaloriesValue.text = video.calories.toString()

            val durationMin = video.durationSeconds / 60
            bind.textDurationValue.text = getString(R.string.exerc_min_format, durationMin)

            if (!video.thumbnailUrl.isNullOrEmpty()) {
                bind.imageVideoThumbnail.loadPhoto(video.thumbnailUrl)
            } else {
                bind.imageVideoThumbnail.setImageResource(R.drawable.placeholder_food)
            }

            bind.layoutBenefitsList.removeAllViews()
            video.benefits.forEach { benefit ->
                val bulletBinding = ItemBenefitBulletBinding.inflate(
                    LayoutInflater.from(requireContext()),
                    bind.layoutBenefitsList,
                    false
                )
                bulletBinding.textBenefitContent.text = benefit
                bind.layoutBenefitsList.addView(bulletBinding.root)
            }

            if (state.isLoggedSuccessfully) {
                showToast(getString(R.string.exerc_logged_success))
                navigateUp()
            }
        }
    }
}
