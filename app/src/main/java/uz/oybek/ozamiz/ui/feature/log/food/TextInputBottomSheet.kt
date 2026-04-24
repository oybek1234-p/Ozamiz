package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.databinding.BottomSheetTextInputBinding

class TextInputBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTextInputBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTextInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString(ARG_TITLE)?.let { binding.textTitle.text = it }
        arguments?.getString(ARG_PLACEHOLDER)?.let { binding.inputQuickDescription.hint = it }

        binding.inputQuickDescription.requestFocus()

        binding.buttonAnalyzeAndSave.setOnClickListener {
            submitText()
        }

        binding.inputQuickDescription.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitText()
                true
            } else false
        }
    }

    private fun submitText() {
        val text = binding.inputQuickDescription.text.toString().trim()
        if (text.isNotEmpty()) {
            setFragmentResult(REQUEST_KEY, bundleOf(RESULT_TEXT to text))
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TextInputBottomSheet"
        const val REQUEST_KEY = "text_input_request"
        const val RESULT_TEXT = "result_text"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_PLACEHOLDER = "arg_placeholder"

        fun newInstance(title: String? = null, placeholder: String? = null) = TextInputBottomSheet().apply {
            arguments = bundleOf(
                ARG_TITLE to title,
                ARG_PLACEHOLDER to placeholder
            )
        }
    }
}
