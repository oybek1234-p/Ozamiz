package uz.oybek.ozamiz.ui.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.databinding.BottomSheetOverageInfoBinding

class OverageInfoBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetOverageInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetOverageInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val overage = arguments?.getInt(ARG_OVERAGE) ?: 0
        
        binding.textOverageAmount.text = "Siz kunlik me'yordan ortiqcha $overage kaloriya yedingiz."
        
        binding.btnGotIt.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "OverageInfoBottomSheet"
        private const val ARG_OVERAGE = "arg_overage"

        fun newInstance(overage: Int): OverageInfoBottomSheet {
            return OverageInfoBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_OVERAGE, overage)
                }
            }
        }
    }
}
