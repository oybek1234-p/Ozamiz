package uz.oybek.ozamiz.ui.feature.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.databinding.BottomSheetSharePostBinding

class SharePostBottomSheet(
    private val foodName: String,
    private val photoUrl: String,
    private val calories: Int,
    private val onShare: () -> Unit,
    private val onCancel: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSharePostBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSharePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCancel.setOnClickListener {
            onCancel()
            dismiss()
        }

        binding.btnShare.setOnClickListener {
            onShare()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
