package uz.oybek.ozamiz.ui.feature.exercises

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.ItemWeightLossVideoBinding
import uz.oybek.ozamiz.ui.base.BaseAdapter
import uz.oybek.ozamiz.ui.utils.loadPhoto
import uz.oybek.ozamiz.ui.utils.visibleOrGone

class WeightLossVideosAdapter : BaseAdapter<WeightLossVideoUiModel, ItemWeightLossVideoBinding>(
    R.layout.item_weight_loss_video,
    WeightLossVideoDiffCallback()
) {

    override fun inflate(inflater: LayoutInflater, parent: ViewGroup): ItemWeightLossVideoBinding {
        return ItemWeightLossVideoBinding.inflate(inflater, parent, false)
    }

    override fun bind(holder: ViewHolder<*>, model: WeightLossVideoUiModel, pos: Int) {
        val binding = holder.binding as ItemWeightLossVideoBinding
        val context = binding.root.context
        val video = model.video

        binding.textVideoName.text = video.name
        binding.textCalories.text = context.getString(R.string.exerc_cal_format, video.calories)

        val durationMin = video.durationSeconds / 60
        binding.textDuration.text = context.getString(R.string.exerc_min_format, durationMin)

        val benefitsStr = video.benefits.joinToString(", ")
        binding.textVideoBenefits.text = context.getString(R.string.exerc_video_benefits_format, benefitsStr)

        binding.layoutLockOverlay.visibleOrGone(model.isLocked)

        if (!video.thumbnailUrl.isNullOrEmpty()) {
            binding.imageThumbnail.loadPhoto(video.thumbnailUrl)
        } else {
            binding.imageThumbnail.setImageResource(R.drawable.placeholder_food)
        }
    }

    class WeightLossVideoDiffCallback : DiffUtil.ItemCallback<WeightLossVideoUiModel>() {
        override fun areItemsTheSame(oldItem: WeightLossVideoUiModel, newItem: WeightLossVideoUiModel): Boolean {
            return oldItem.video.id == newItem.video.id
        }

        override fun areContentsTheSame(oldItem: WeightLossVideoUiModel, newItem: WeightLossVideoUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
