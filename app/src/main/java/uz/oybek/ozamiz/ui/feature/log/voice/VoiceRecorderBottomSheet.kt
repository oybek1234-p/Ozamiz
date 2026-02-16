package uz.oybek.ozamiz.ui.feature.log.voice

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.BottomSheetVoiceRecorderBinding
import uz.oybek.ozamiz.ui.utils.showToast
import java.io.File
import java.util.Locale

class VoiceRecorderBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetVoiceRecorderBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VoiceRecorderViewModel by viewModels()
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var startTime = 0L

    companion object {
        const val TAG = "VoiceRecorderSheet"
        const val REQUEST_KEY = "VoiceResult"
        const val RESULT_TEXT = "text"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetVoiceRecorderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeState()
        checkPermissionsAndStart()
    }

    private fun setupUI() {
        binding.buttonCancelRecording.setOnClickListener { dismiss() }
        binding.buttonAnalyze.setOnClickListener { stopAndAnalyze() }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.progressBarAnalyze.isVisible = state is VoiceUiState.Processing
                binding.buttonAnalyze.isEnabled = state !is VoiceUiState.Processing
                binding.lottieRecordingIndicator.isVisible = state is VoiceUiState.Recording

                if (state is VoiceUiState.Recording) {
                    binding.textViewRecordStatus.text = getString(R.string.recording_speak_now)
                    binding.lottieRecordingIndicator.playAnimation()
                    startTimer()
                } else {
                    binding.lottieRecordingIndicator.pauseAnimation()
                }

                if (state is VoiceUiState.Success) {
                    setFragmentResult(REQUEST_KEY, bundleOf(RESULT_TEXT to state.text))
                    dismiss()
                }
                if (state is VoiceUiState.Error) {
                    showToast(state.message)
                    dismiss()
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startRecording()
            } else {
                showToast(getString(R.string.permission_denied_audio))
                dismiss()
            }
        }

    private fun checkPermissionsAndStart() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startRecording()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecording() {
        try {
            audioFile = File.createTempFile("voice_", ".ogg", requireContext().cacheDir)
            mediaRecorder =
                (if (Build.VERSION.SDK_INT >= 31) MediaRecorder(requireContext()) else MediaRecorder()).apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.OGG)
                    setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                    setOutputFile(audioFile!!.absolutePath)
                    prepare()
                    start()
                }
            viewModel.onRecordStarted()
        } catch (e: Exception) {
            showToast("Mikrofon xatosi: ${e.message}")
            dismiss()
        }
    }

    private fun stopAndAnalyze() {
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
        }
        releaseRecorder()
        audioFile?.let { viewModel.analyzeAudio(it.toUri()) }
    }

    private fun releaseRecorder() {
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun startTimer() {
        startTime = System.currentTimeMillis()
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        
        val runnable = object : Runnable {
            override fun run() {
                if (_binding != null && viewModel.uiState.value is VoiceUiState.Recording) {
                    val seconds = (System.currentTimeMillis() - startTime) / 1000
                    binding.textViewTimer.text =
                        String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(runnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releaseRecorder()
        _binding = null
    }
}