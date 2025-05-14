package uz.oybek.ozamiz.ui.log

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.databinding.DialogLogWaterBinding
import uz.oybek.ozamiz.ui.hideSoftInput
import uz.oybek.ozamiz.ui.home.HomeViewModel
import uz.oybek.ozamiz.ui.showToast

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
        // Optional: Set background transparent for custom shape
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.cardGlass.setOnClickListener { addWater(glassAmount) }
        binding.cardSmallBottle.setOnClickListener { addWater(smallBottleAmount) }
        binding.cardLargeBottle.setOnClickListener { addWater(largeBottleAmount) }

        binding.buttonAddWaterConfirm.setOnClickListener {
            val manualAmount = binding.editTextWaterAmount.text.toString().toIntOrNull()
            if (manualAmount != null && manualAmount > 0) {
                addWater(manualAmount)
            } else if (!binding.editTextWaterAmount.text.isNullOrBlank()) {
                // Show error if text entered but not a valid number > 0
                showToast(getString(R.string.error_invalid_water_amount))
            }
            // If input is empty, clicking confirm does nothing unless a preset was clicked
        }

        binding.buttonCancelWater.setOnClickListener {
            dismiss() // Close the dialog
        }
    }

    private fun addWater(amountMl: Int) {
        hideSoftInput(requireActivity()) // Hide keyboard before processing

        // Call ViewModel function to add water log
        viewLifecycleOwner.lifecycleScope.launch {
            val result = homeViewModel.addWaterLog(amountMl) // Call ViewModel function
            if (result is DataWrapper.Success) {
                showToast(getString(R.string.water_logged_successfully))
            } else if (result is DataWrapper.Error) {
                showToast("${getString(R.string.error_logging_water)}: ${result.error}")
            }
            dismiss() // Close dialog after attempting to add
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}