package uz.oybek.ozamiz.ui.feature.menu.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.databinding.BottomSheetMenuInputBinding

class MenuAiInputDialog(
    private val onGenerate: (String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMenuInputBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMenuInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnGenerate.setOnClickListener {
            val text = binding.etFridgeFoods.text.toString()
            if (text.isNotBlank()) {
                onGenerate(text)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
