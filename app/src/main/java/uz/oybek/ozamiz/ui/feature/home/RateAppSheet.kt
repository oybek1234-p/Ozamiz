package uz.oybek.ozamiz.ui.feature.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.databinding.RateAppDialogBinding
import java.util.Date
import androidx.core.net.toUri

class RateAppSheet : BottomSheetDialogFragment() {

    private var _binding: RateAppDialogBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = RateAppDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("has_shown_rate_sheet", true)
            ?.apply()

        setupListeners()
    }

    private fun setupListeners() {
        binding.rateButton.onRatingBarChangeListener =
            RatingBar.OnRatingBarChangeListener { _, rating, _ ->
                if (rating >= 4) {
                    binding.inputFeedbackLayout.isVisible = false
                    binding.btnAction.text = "Baholash"
                    binding.btnAction.isVisible = true
                    
                } else if (rating > 0) {
                    binding.inputFeedbackLayout.isVisible = true
                    binding.btnAction.text = "Yuborish"
                    binding.btnAction.isVisible = true
                } else {
                    binding.inputFeedbackLayout.isVisible = false
                    binding.btnAction.isVisible = false
                }
            }

        binding.btnAction.setOnClickListener {
            val rating = binding.rateButton.rating
            if (rating >= 4) {
                openPlayStore()
                dismiss()
            } else {
                val feedback = binding.inputFeedback.text.toString()
                if (feedback.isBlank()) {
                    Toast.makeText(context, "Iltimos, fikringizni yozing", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                saveFeedback(rating, feedback)
                Toast.makeText(context, "Rahmat! Fikringiz inobatga olinadi.", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    private fun openPlayStore() {
        val appPackageName = requireContext().packageName
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "market://details?id=$appPackageName".toUri()
                )
            )
        } catch (e: android.content.ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=$appPackageName".toUri()
                )
            )
        }
    }

    private fun saveFeedback(rating: Float, feedback: String) {
        val user = LocalUser.user
        val feedbackMap = hashMapOf(
            "userId" to user.uid,
            "userPhone" to user.phoneNumber,
            "rating" to rating,
            "feedback" to feedback,
            "timestamp" to Date()
        )

        val userId = user.uid
        FirebaseFirestore.getInstance().collection("ratings")
            .document(userId)
            .set(feedbackMap)
            .addOnFailureListener { e -> e.printStackTrace() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "RateAppSheet"
        fun newInstance() = RateAppSheet()
    }
}
