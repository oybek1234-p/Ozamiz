package uz.oybek.ozamiz.ui.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.databinding.BottomSheetDailyAnalysisBinding
import java.util.Locale

class DailyAnalysisBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDailyAnalysisBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDailyAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val foodCount = arguments?.getInt(ARG_FOOD_COUNT) ?: 0
        val points = arguments?.getInt(ARG_POINTS) ?: 0
        val waterMl = arguments?.getInt(ARG_WATER_ML) ?: 0

        binding.textFoodCount.text = "$foodCount ta"
        binding.textPoints.text = "+$points"
        binding.textWaterAmount.text = String.format(Locale.US, "%.1f L", waterMl / 1000f)

        binding.btnDone.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DailyAnalysisBottomSheet"
        private const val ARG_FOOD_COUNT = "food_count"
        private const val ARG_POINTS = "points"
        private const val ARG_WATER_ML = "water_ml"

        fun newInstance(foodCount: Int, points: Int, waterMl: Int) = DailyAnalysisBottomSheet().apply {
            arguments = Bundle().apply {
                putInt(ARG_FOOD_COUNT, foodCount)
                putInt(ARG_POINTS, points)
                putInt(ARG_WATER_ML, waterMl)
            }
        }
    }
}
