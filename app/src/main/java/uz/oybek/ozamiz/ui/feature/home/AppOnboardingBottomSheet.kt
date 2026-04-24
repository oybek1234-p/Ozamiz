package uz.oybek.ozamiz.ui.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.databinding.BottomSheetAppOnboardingBinding

class AppOnboardingBottomSheet(
    private val onComplete: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAppOnboardingBinding? = null
    private val binding get() = _binding!!

    private var currentPage = 0

    private val pages = listOf(
        OnboardingPage(
            "👋",
            "Xush kelibsiz!",
            "Ozamiz ilovasiga xush kelibsiz. Bu yerda siz sog'lom ozish va ovqatlanishni o'rganasiz."
        ),
        OnboardingPage(
            "⚖️",
            "Kaloriya nima?",
            "Kaloriya - bu ovqatdan oladigan quvvatingiz. Ozish uchun siz sarflagandan ko'ra kamroq kaloriya yeyishingiz kerak."
        ),
        OnboardingPage(
            "🚀",
            "Qanday boshlash kerak?",
            "Yegan har bir ovqatingizni kiritib boring. Buning uchun 'Plus' tugmasini bosing va nima yeganingizni yozing."
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAppOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updatePage()

        binding.btnNext.setOnClickListener {
            if (currentPage < pages.size - 1) {
                currentPage++
                updatePage()
            } else {
                onComplete()
                dismiss()
            }
        }
    }

    private fun updatePage() {
        val page = pages[currentPage]
        binding.textEmoji.text = page.emoji
        binding.textTitle.text = page.title
        binding.textDescription.text = page.description
        binding.btnNext.text = if (currentPage == pages.size - 1) "Boshlash" else "Davom etish"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class OnboardingPage(
        val emoji: String,
        val title: String,
        val description: String
    )

    companion object {
        const val TAG = "AppOnboardingBottomSheet"
    }
}
