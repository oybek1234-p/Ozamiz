package uz.oybek.ozamiz.ui.log.voice

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.BottomSheetVoiceRecorderBinding
import uz.oybek.ozamiz.ui.showToast
import java.io.File
import java.io.IOException
import java.util.Locale

class VoiceRecorderBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetVoiceRecorderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VoiceRecorderViewModel by viewModels()

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var recordingTimeSeconds = 0

    companion object {
        const val TAG = "VoiceRecorderBottomSheet"
        const val REQUEST_KEY = "VoiceRecorderRequestKey"
        const val RESULT_TEXT = "TranscribedText"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetVoiceRecorderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()
        startRecording()
    }

    private fun setupListeners() {
        binding.buttonAnalyze.setOnClickListener {
            stopRecording()
            audioFile?.let { file ->
                if (file.exists() && file.length() > 500) { // Minimal fayl hajmini tekshirish
                    viewModel.transcribeAudio(file)
                } else {
                    showToast("Yozib olingan ovoz juda qisqa.")
                }
            } ?: showToast("Yozib olingan ovoz fayli topilmadi.")
        }
        binding.buttonCancelRecording.setOnClickListener {
            dismiss()
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showToast("Ovoz yozish uchun ruxsat kerak.")
            dismiss()
            return
        }

        audioFile?.delete()
        audioFile = File(requireContext().cacheDir, "temp_audio.ogg")

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(requireContext())
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.OGG)
            setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
            setAudioSamplingRate(16000)
            setAudioEncodingBitRate(20000)
            setOutputFile(audioFile!!.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                binding.lottieRecordingIndicator.playAnimation()
                startTimer()
            } catch (e: IOException) {
                showToast("Ovoz yozishni boshlashda xatolik: ${e.message}")
                releaseMediaRecorder()
            } catch (e: IllegalStateException) {
                showToast("Ovoz yozishni boshlashda xatolik: ${e.message}")
                releaseMediaRecorder()
            }
        }
    }

    private fun stopRecording() {
        if (!isRecording) return
        try {
            mediaRecorder?.stop()
        } catch (e: RuntimeException) {
            // Stop called immediately after start, ignore
        } finally {
            releaseMediaRecorder()
            isRecording = false
            stopTimer()
        }
    }

    private fun startTimer() {
        binding.textViewTimer.visibility = View.VISIBLE
        recordingTimeSeconds = 0
        timerRunnable = object : Runnable {
            override fun run() {
                recordingTimeSeconds++
                val minutes = recordingTimeSeconds / 60
                val seconds = recordingTimeSeconds % 60
                binding.textViewTimer.text =
                    String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timerRunnable!!)
    }

    private fun stopTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun releaseMediaRecorder() {
        mediaRecorder?.reset()
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.progressBarAnalyze.visibility =
                    if (state is VoiceRecorderUiState.Loading) View.VISIBLE else View.GONE
                binding.buttonAnalyze.visibility =
                    if (state is VoiceRecorderUiState.Loading) View.INVISIBLE else View.VISIBLE
                binding.lottieRecordingIndicator.visibility = if (state is VoiceRecorderUiState.Loading) View.GONE else View.VISIBLE
                binding.textViewTimer.visibility = if (state is VoiceRecorderUiState.Loading) View.GONE else View.VISIBLE

                if (state is VoiceRecorderUiState.Loading) {
                    binding.textViewRecordStatus.text = getString(R.string.audio_transcription_in_progress)
                } else {
                    binding.textViewRecordStatus.text = getString(R.string.recording_speak_now)
                }

                when (state) {
                    is VoiceRecorderUiState.Success -> {
                        setFragmentResult(REQUEST_KEY, bundleOf(RESULT_TEXT to state.transcribedText))
                        dismiss()
                    }
                    is VoiceRecorderUiState.Error -> {
                        showToast(state.message)
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopRecording()
        _binding = null
    }
}