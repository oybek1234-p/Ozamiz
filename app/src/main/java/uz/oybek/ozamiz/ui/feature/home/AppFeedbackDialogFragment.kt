package uz.oybek.ozamiz.ui.feature.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.feedback.AppFeedbackRepository
import uz.oybek.ozamiz.databinding.DialogAppFeedbackBinding
import uz.oybek.ozamiz.ui.MainActivity
import uz.oybek.ozamiz.ui.utils.openAppInPlayStore
import uz.oybek.ozamiz.ui.utils.showKeyboard

class AppFeedbackDialogFragment : DialogFragment() {

    private var _binding: DialogAppFeedbackBinding? = null
    private val binding get() = _binding!!

    var onDismissCallback: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAppFeedbackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.buttonNegative.setOnClickListener {
            showStep2Negative()
        }

        binding.buttonPositive.setOnClickListener {
            showStep2Positive()
        }

        binding.buttonSubmitFeedback.setOnClickListener {
            val text = binding.editTextFeedback.text.toString().trim()
            if (text.isEmpty()) {
                binding.inputLayoutFeedback.error = "Iltimos, fikringizni yozing"
                return@setOnClickListener
            }
            binding.inputLayoutFeedback.error = null
            binding.buttonSubmitFeedback.isEnabled = false

            lifecycleScope.launch {
                try {
                    AppFeedbackRepository.saveAppFeedback(text, "dislike")
                    uz.oybek.ozamiz.data.user.UserProfileRepository.updateUser { isRated = true }
                    Toast.makeText(context, "Rahmat! Fikringiz qabul qilindi. 🙏", Toast.LENGTH_SHORT).show()
                    onDismissCallback?.invoke()
                    dismiss()
                } catch (e: Exception) {
                    binding.buttonSubmitFeedback.isEnabled = true
                    Toast.makeText(context, "Xatolik yuz berdi. Qayta urinib ko'ring.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.buttonRateApp.setOnClickListener {
            uz.oybek.ozamiz.data.user.UserProfileRepository.updateUser { isRated = true }
            onDismissCallback?.invoke()
            (activity as? MainActivity)?.requestAppReview()
            openPlayStoreFallback()
            dismiss()
        }
    }

    private fun showStep2Negative() {
        binding.layoutStep1.isVisible = false
        binding.layoutStep2Positive.isVisible = false
        binding.layoutStep2Negative.isVisible = true

        binding.editTextFeedback.showKeyboard(150)
    }

    private fun showStep2Positive() {
        binding.layoutStep1.isVisible = false
        binding.layoutStep2Negative.isVisible = false
        binding.layoutStep2Positive.isVisible = true
    }

    private fun openPlayStoreFallback() {
        openAppInPlayStore()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): AppFeedbackDialogFragment = AppFeedbackDialogFragment()
    }
}
