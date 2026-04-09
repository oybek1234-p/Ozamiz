package uz.oybek.ozamiz.ui.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.databinding.LayoutDailyTipDetailsBinding
import uz.oybek.ozamiz.ui.utils.DailyTipUtil

class DailyTipDetailsBottomSheet : BottomSheetDialogFragment() {
    
    override fun getTheme(): Int = uz.oybek.ozamiz.R.style.BottomSheetDialogTheme

    private var _binding: LayoutDailyTipDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()
    private var tip: DailyTipUtil.Tip? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tip = arguments?.getParcelable(ARG_TIP)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutDailyTipDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        (dialog as? com.google.android.material.bottomsheet.BottomSheetDialog)?.behavior?.state = 
            com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        
        tip?.let { item ->
            binding.textDetailEmoji.text = item.emoji
            binding.textDetailTitle.text = item.title
            binding.textDetailDescription.text = item.content
            binding.textDetailBenefit.text = item.benefits
        }

        binding.btnCloseDetails.setOnClickListener {
            viewModel.onTipUnderstood()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DailyTipDetailsBottomSheet"
        private const val ARG_TIP = "arg_tip"

        fun newInstance(tip: DailyTipUtil.Tip): DailyTipDetailsBottomSheet {
            return DailyTipDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_TIP, tip)
                }
            }
        }
    }
}
