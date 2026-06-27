package uz.oybek.ozamiz.ui.feature.home

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.data.ai.DailyAiAnalysisRepository
import uz.oybek.ozamiz.databinding.BottomSheetAiAdviceBinding

class AiAdviceBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAiAdviceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAiAdviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val text = arguments?.getString(ARG_TEXT) ?: ""
        binding.textAiAdviceContent.text = text

        binding.btnUnderstand.setOnClickListener {
            DailyAiAnalysisRepository.markAdviceAsShown()
            dismiss()
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        DailyAiAnalysisRepository.markAdviceAsShown()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AiAdviceBottomSheet"
        private const val ARG_TEXT = "arg_text"

        fun newInstance(adviceText: String): AiAdviceBottomSheet {
            return AiAdviceBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_TEXT, adviceText)
                }
            }
        }
    }
}
