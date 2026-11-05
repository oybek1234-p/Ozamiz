package uz.oybek.ozamiz.ui.feature.log.photo

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentWeightLogSuccessBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.community.add.AddPostArgs
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter
import uz.oybek.ozamiz.ui.utils.getArgs
import uz.oybek.ozamiz.ui.utils.toBundle
import kotlin.math.abs

class WeightLogSuccessFragment : BaseFragment<FragmentWeightLogSuccessBinding>() {

    private var mediaPlayer: MediaPlayer? = null

    // Lose Weight Success Compliments (5 variations)
    private val loseSuccessCompliments = listOf(
        R.string.lose_success_1,
        R.string.lose_success_2,
        R.string.lose_success_3,
        R.string.lose_success_4,
        R.string.lose_success_5
    )

    // Recomposition Messages (5 variations)
    private val recompositionMessages = listOf(
        R.string.recomposition_1,
        R.string.recomposition_2,
        R.string.recomposition_3,
        R.string.recomposition_4,
        R.string.recomposition_5
    )

    // Lose Weight Plateau / Water Retention (5 variations)
    private val losePlateauMessages = listOf(
        R.string.lose_plateau_1,
        R.string.lose_plateau_2,
        R.string.lose_plateau_3,
        R.string.lose_plateau_4,
        R.string.lose_plateau_5
    )

    // Lose Weight Gained Advice (5 variations)
    private val loseGainedAdviceMessages = listOf(
        R.string.lose_gained_advice_1,
        R.string.lose_gained_advice_2,
        R.string.lose_gained_advice_3,
        R.string.lose_gained_advice_4,
        R.string.lose_gained_advice_5
    )

    // Gain Weight Success Compliments (5 variations)
    private val gainSuccessCompliments = listOf(
        R.string.gain_success_1,
        R.string.gain_success_2,
        R.string.gain_success_3,
        R.string.gain_success_4,
        R.string.gain_success_5
    )

    // Gain Weight Lost Advice (5 variations)
    private val gainLostAdviceMessages = listOf(
        R.string.gain_lost_advice_1,
        R.string.gain_lost_advice_2,
        R.string.gain_lost_advice_3,
        R.string.gain_lost_advice_4,
        R.string.gain_lost_advice_5
    )

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentWeightLogSuccessBinding {
        return FragmentWeightLogSuccessBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentWeightLogSuccessBinding) {
        val args = arguments.getArgs<WeightLogSuccessArgs>()
        val isGainGoal = args?.isGainGoal ?: false
        val weightChangeKg = args?.weightChangeKg ?: 0.0
        val totalWeightChangeKg = args?.totalWeightChangeKg ?: 0.0
        val measurementChangeCm = args?.measurementChangeCm ?: 0.0
        val photoUri = args?.photoUri
        val beforePhotoUri = args?.beforePhotoUri

        bind.toolbar.setNavigationOnClickListener {
            closeFragment()
        }

        val isWeightLoss = weightChangeKg < -0.05
        val isWeightGain = weightChangeKg > 0.05
        val isWeightUnchanged = !isWeightLoss && !isWeightGain
        val hasMeasurementLoss = measurementChangeCm < -0.05

        val isProgressTowardsGoal = if (isGainGoal) {
            isWeightGain || (isWeightUnchanged && hasMeasurementLoss)
        } else {
            isWeightLoss || (isWeightUnchanged && hasMeasurementLoss)
        }

        val hasPhoto = !photoUri.isNullOrBlank()
        val canShare = hasPhoto && isProgressTowardsGoal

        // Auto post notice visible if photo attached AND progress was made
        bind.cardAutoPostNotice.isVisible = canShare
        bind.btnShareResult.isVisible = false
        bind.tvShareInfoLabel.isVisible = false

        if (!isGainGoal) {
            // === GOAL = LOSE WEIGHT / MAINTAIN ===
            when {
                // Branch 1: User Lost Weight [PROGRESS MADE]
                isWeightLoss -> {
                    playSuccessEffects()
                    bind.tvHeaderEmoji.text = "🎉"
                    val formattedDiff = NutritionFormatter.kgChange(weightChangeKg)
                    bind.tvTitle.text = getString(R.string.weight_success_title_loss, formattedDiff)
                    bind.tvAnalysisMessage.text = getString(loseSuccessCompliments.random())
                }

                // Branch 2: Weight Unchanged, BUT Body Measurements Decreased [RECOMPOSITION]
                hasMeasurementLoss -> {
                    bind.lottieConfetti.isVisible = false
                    bind.tvHeaderEmoji.text = "💪"
                    val absCm = abs(measurementChangeCm).toInt().coerceAtLeast(1)
                    bind.tvTitle.text = getString(R.string.weight_success_title_recomposition_fmt, absCm)
                    bind.tvAnalysisMessage.text = getString(recompositionMessages.random(), absCm)
                }

                // Branch 3: Weight and Measurements Unchanged [PLATEAU / WATER RETENTION]
                isWeightUnchanged -> {
                    bind.lottieConfetti.isVisible = false
                    bind.tvHeaderEmoji.text = "⏳"
                    bind.tvTitle.text = getString(R.string.weight_success_title_unchanged)
                    bind.tvAnalysisMessage.text = getString(R.string.weight_success_message_unchanged_lose)
                }

                // Branch 4: User Gained Weight on Loss Goal [WEIGHT GAINED]
                else -> {
                    bind.lottieConfetti.isVisible = false
                    bind.tvHeaderEmoji.text = "📊"
                    val formattedDiff = NutritionFormatter.kgChange(weightChangeKg)
                    bind.tvTitle.text = getString(R.string.weight_success_title_gained_lose, formattedDiff)
                    bind.tvAnalysisMessage.text = getString(loseGainedAdviceMessages.random())
                }
            }
        } else {
            // === GOAL = GAIN WEIGHT ===
            when {
                // Branch 5: User Gained Weight [PROGRESS MADE]
                isWeightGain -> {
                    playSuccessEffects()
                    bind.tvHeaderEmoji.text = "🎉"
                    val formattedDiff = NutritionFormatter.kgChange(weightChangeKg)
                    bind.tvTitle.text = getString(R.string.weight_success_title_gain, formattedDiff)
                    bind.tvAnalysisMessage.text = getString(gainSuccessCompliments.random())
                }

                // Branch 6: User Lost Weight or Unchanged on Gain Goal
                else -> {
                    bind.lottieConfetti.isVisible = false
                    bind.tvHeaderEmoji.text = "⏳"
                    if (isWeightUnchanged) {
                        bind.tvTitle.text = getString(R.string.weight_success_title_unchanged)
                        bind.tvAnalysisMessage.text = getString(R.string.weight_success_message_unchanged_gain)
                    } else {
                        val formattedDiff = NutritionFormatter.kgChange(weightChangeKg)
                        bind.tvTitle.text = getString(R.string.weight_success_title_lost_gain, formattedDiff)
                        bind.tvAnalysisMessage.text = getString(gainLostAdviceMessages.random())
                    }
                }
            }
        }

        bind.btnShareResult.setOnClickListener {
            val totalChangeStr = NutritionFormatter.kgChange(totalWeightChangeKg)
            val startWeightStr = NutritionFormatter.kg(args?.startWeightKg)
            val currentWeightStr = NutritionFormatter.kg(args?.currentWeightKg)
            val days = (args?.daysCount ?: 1).coerceAtLeast(1)

            val descRes = if (isGainGoal) {
                R.string.weight_success_share_desc_gain_fmt
            } else {
                R.string.weight_success_share_desc_loss_fmt
            }
            val desc = getString(descRes, days, totalChangeStr, startWeightStr, currentWeightStr)
            val addPostArgs = AddPostArgs(
                imageUri = photoUri,
                beforeImageUri = beforePhotoUri,
                initialDescription = desc,
                isWeightProgressPost = true
            )
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.weightLogSuccessFragment, true)
                .setEnterAnim(R.anim.fragment_open_anim)
                .setExitAnim(R.anim.fragment_close_anim)
                .build()
            navigate(R.id.addPostFragment, addPostArgs.toBundle(), navOptions)
        }

        bind.btnDone.setOnClickListener {
            closeFragment()
        }
    }

    private fun playSuccessEffects() {
        try {
            mediaPlayer = MediaPlayer.create(requireContext(), R.raw.success)
            mediaPlayer?.start()
        } catch (_: Exception) {}

        binding.lottieConfetti.isVisible = true
        binding.lottieConfetti.playAnimation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
    }
}
