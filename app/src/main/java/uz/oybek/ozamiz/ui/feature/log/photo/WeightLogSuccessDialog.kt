package uz.oybek.ozamiz.ui.feature.log.photo

import android.app.Dialog
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.common.util.readableFormat
import uz.oybek.ozamiz.databinding.DialogWeightLogSuccessBinding
import kotlin.math.abs

class WeightLogSuccessDialog : DialogFragment() {

    private var _binding: DialogWeightLogSuccessBinding? = null
    private val binding get() = _binding!!

    var hasProgress: Boolean = true
    var weightChangeKg: Double = 0.0
    var measurementChangeCm: Double = 0.0
    var analysisMessage: String? = null
    var onShareClick: (() -> Unit)? = null
    var onDismissCallback: (() -> Unit)? = null

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogWeightLogSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val hasWeightChange = abs(weightChangeKg) >= 0.05
        val hasMeasurementChange = abs(measurementChangeCm) >= 0.05

        if (hasProgress) {
            // Play success audio
            try {
                mediaPlayer = MediaPlayer.create(requireContext(), R.raw.success)
                mediaPlayer?.start()
            } catch (_: Exception) {}

            // Play Lottie Confetti Animation
            binding.lottieConfetti.isVisible = true
            binding.lottieConfetti.playAnimation()

            binding.tvTitle.text = getString(R.string.add_weight_screen_title)
            binding.tvSubtitle.text = getString(R.string.info_saved_success)
            binding.imgHeaderIcon.setImageResource(R.drawable.ic_trophy)
        } else {
            binding.lottieConfetti.isVisible = false
            binding.tvTitle.text = getString(R.string.add_weight_screen_title)
            binding.tvSubtitle.text = getString(R.string.info_saved_success)
            binding.imgHeaderIcon.setImageResource(R.drawable.info_ic)
        }

        // Weight Progress
        if (hasWeightChange) {
            val weightText = if (weightChangeKg > 0) "+${weightChangeKg.readableFormat()} kg" else "${weightChangeKg.readableFormat()} kg"
            binding.tvWeightProgressVal.text = weightText
            binding.tvWeightProgressVal.isVisible = true
        } else {
            binding.tvWeightProgressVal.isVisible = false
        }

        // Measurement Progress
        if (hasMeasurementChange) {
            val cmText = if (measurementChangeCm > 0) "+${measurementChangeCm.toInt()} sm" else "${measurementChangeCm.toInt()} sm"
            binding.tvMeasurementProgressVal.text = "• $cmText o'lchov"
            binding.tvMeasurementProgressVal.isVisible = true
        } else {
            binding.tvMeasurementProgressVal.isVisible = false
        }

        binding.cardProgressDetails.isVisible = hasWeightChange || hasMeasurementChange

        // Analysis message
        val msg = analysisMessage
        if (!msg.isNullOrBlank()) {
            binding.tvAnalysisMessage.text = msg
            binding.cardAnalysis.isVisible = true
        } else {
            binding.cardAnalysis.isVisible = false
        }

        // Share button
        binding.btnShareResult.setOnClickListener {
            dismiss()
            onShareClick?.invoke()
        }

        // Close buttons
        binding.btnCloseDialog.setOnClickListener {
            dismiss()
        }
        binding.btnDismissDialog.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
        _binding = null
        onDismissCallback?.invoke()
    }

    companion object {
        fun newInstance(
            hasProgress: Boolean,
            weightChangeKg: Double,
            measurementChangeCm: Double,
            analysisMessage: String?,
            onShareClick: () -> Unit
        ): WeightLogSuccessDialog {
            return WeightLogSuccessDialog().apply {
                this.hasProgress = hasProgress
                this.weightChangeKg = weightChangeKg
                this.measurementChangeCm = measurementChangeCm
                this.analysisMessage = analysisMessage
                this.onShareClick = onShareClick
            }
        }
    }
}
