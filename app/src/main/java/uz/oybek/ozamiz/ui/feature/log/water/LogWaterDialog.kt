package uz.oybek.ozamiz.ui.feature.log.water

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.databinding.DialogLogWaterBinding
import uz.oybek.ozamiz.ui.utils.hideSoftInput
import uz.oybek.ozamiz.ui.feature.home.HomeViewModel
import uz.oybek.ozamiz.ui.utils.showToast

class LogWaterDialog : BottomSheetDialogFragment() {

    private var _binding: DialogLogWaterBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()

    private val glassAmount = 250
    private val smallBottleAmount = 500
    private val largeBottleAmount = 1000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLogWaterBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {

    }

    private fun addWater(amountMl: Int) {
//        hideSoftInput(requireActivity())
//        homeViewModel.addWaterLog(amountMl) // ViewModel o'zi sanani biladi
//        showToast(getString(uz.oybek.ozamiz.R.string.water_logged_successfully)) // Optimistic UI
//        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}