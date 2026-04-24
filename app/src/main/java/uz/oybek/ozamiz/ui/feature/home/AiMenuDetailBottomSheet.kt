package uz.oybek.ozamiz.ui.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.BottomSheetAiMenuDetailBinding
import uz.oybek.ozamiz.ui.utils.MarkdownUtils

class AiMenuDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAiMenuDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAiMenuDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuText = arguments?.getString(ARG_MENU_TEXT)
        binding.textMenuContent.text = MarkdownUtils.formatMarkdown(menuText)

        binding.btnOpenChat.setOnClickListener {
            findNavController().navigate(R.id.aiChatFragment)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AiMenuDetailBottomSheet"
        private const val ARG_MENU_TEXT = "menu_text"

        fun newInstance(menuText: String) = AiMenuDetailBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_MENU_TEXT, menuText)
            }
        }
    }
}
