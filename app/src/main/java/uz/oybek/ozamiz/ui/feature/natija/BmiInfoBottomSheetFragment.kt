package uz.oybek.ozamiz.ui.feature.natija

import uz.oybek.ozamiz.common.util.orZero
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.databinding.BottomSheetBmiInfoBinding

class BmiInfoBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetBmiInfoBinding? = null
    private val binding get() = _binding!!

    private var bmi: Double = 0.0
    private var category: BmiCategory = BmiCategory.NONE

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBmiInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Retrieve args
        bmi = arguments?.getDouble(ARG_BMI).orZero()
        val categoryName = arguments?.getString(ARG_CATEGORY) ?: "NONE"
        category = try {
            BmiCategory.valueOf(categoryName)
        } catch (e: Exception) {
            BmiCategory.NONE
        }

        updateUi()
    }

    private fun updateUi() {
        if (bmi <= 0) return

        binding.tvBmiValueSheet.text = String.format("%.2f", bmi)

        val statusText = when (category) {
            BmiCategory.UNDERWEIGHT -> "Kam vazn"
            BmiCategory.HEALTHY -> "Sog'lom"
            BmiCategory.OVERWEIGHT -> "Ortiqcha vazn"
            BmiCategory.OBESE -> "Semizlik"
            else -> ""
        }

        val color = when (category) {
            BmiCategory.UNDERWEIGHT -> Color.parseColor("#2196F3")
            BmiCategory.HEALTHY -> Color.parseColor("#4CAF50")
            BmiCategory.OVERWEIGHT -> Color.parseColor("#FFC107")
            BmiCategory.OBESE -> Color.parseColor("#F44336")
            else -> Color.GRAY
        }

        binding.tvBmiStatusSheet.text = statusText
        binding.tvBmiStatusSheet.backgroundTintList = android.content.res.ColorStateList.valueOf(color)

        // Marker
        binding.layoutBmiScaleSheet.post {
            val width = binding.layoutBmiScaleSheet.width
            val percentage = when {
                bmi < 18.5 -> {
                    // Underweight: 0.0 -> 0.33
                    val ratio = ((bmi - 10.0) / (18.5 - 10.0)).coerceIn(0.0, 1.0)
                    0.0 + ratio * 0.33
                }
                bmi < 25.0 -> {
                    // Healthy: 0.33 -> 0.66
                    val ratio = ((bmi - 18.5) / (25.0 - 18.5)).coerceIn(0.0, 1.0)
                    0.33 + ratio * 0.33
                }
                bmi < 30.0 -> {
                    // Overweight: 0.66 -> 0.83 (Yellow Zone)
                    val ratio = ((bmi - 25.0) / (30.0 - 25.0)).coerceIn(0.0, 1.0)
                    0.66 + ratio * 0.17
                }
                else -> {
                    // Obese: 0.83 -> 1.0
                    val ratio = ((bmi - 30.0) / (45.0 - 30.0)).coerceIn(0.0, 1.0)
                    0.83 + ratio * 0.17
                }
            }
            val x = (width * percentage).toFloat()
            binding.viewBmiMarkerSheet.translationX = x
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_BMI = "arg_bmi"
        private const val ARG_CATEGORY = "arg_category"

        fun newInstance(bmi: Double, category: BmiCategory): BmiInfoBottomSheetFragment {
            val fragment = BmiInfoBottomSheetFragment()
            val args = Bundle()
            args.putDouble(ARG_BMI, bmi)
            args.putString(ARG_CATEGORY, category.name)
            fragment.arguments = args
            return fragment
        }
    }
}
