package uz.oybek.ozamiz.ui.log

import LogFoodViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.info.FoodLog
import uz.oybek.ozamiz.databinding.BottomSheetTodaysFoodDetailBinding
import java.util.Locale

class TodaysFoodDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTodaysFoodDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LogFoodViewModel by activityViewModels()
    private lateinit var foodLog: FoodLog

    companion object {
        const val TAG = "TodaysFoodDetailBottomSheet"
        private const val ARG_FOOD_LOG = "arg_food_log"

        fun newInstance(foodLog: FoodLog): TodaysFoodDetailBottomSheet {
            return TodaysFoodDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_FOOD_LOG, Gson().toJson(foodLog))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_FOOD_LOG)?.let {
            foodLog = Gson().fromJson(it, FoodLog::class.java)
        } ?: dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTodaysFoodDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayFoodLog()
        setupListeners()
    }

    private fun displayFoodLog() {
        if (!foodLog.photoUrl.isNullOrEmpty()) {
            binding.imageViewFoodDetail.isVisible = true
            Glide.with(this)
                .load(foodLog.photoUrl)
                .placeholder(R.drawable.placeholder_food)
                .error(R.drawable.placeholder_food) // Add error drawable
                .into(binding.imageViewFoodDetail)
        } else {
            binding.imageViewFoodDetail.isVisible = false
        }

        binding.textViewFoodDescriptionDetail.text = foodLog.description
        binding.textViewFoodDescriptionDetail.isVisible = foodLog.description.isNotBlank()

        binding.textViewDetailCalories.text = getString(R.string.calories_format, foodLog.calories)
        binding.textViewDetailProtein.text = formatMacro(foodLog.protein)
        binding.textViewDetailFat.text = formatMacro(foodLog.fat)
        binding.textViewDetailCarbs.text = formatMacro(foodLog.carbs)

        if (!foodLog.analysisInsights.isNullOrBlank()) {
            binding.cardViewAiInsights.isVisible = true
            binding.textViewAiInsights.text = foodLog.analysisInsights
        } else {
            binding.cardViewAiInsights.isVisible = false
        }
    }

    private fun setupListeners() {
        binding.close.setOnClickListener {
            dismiss()
        }
        binding.toolbarFoodDetail.setNavigationOnClickListener {
            dismiss()
        }

        binding.buttonDeleteLog.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_message_generic, foodLog.description.ifBlank { "bu ovqatni" }))
            .setPositiveButton(R.string.yes_delete) { _, _ ->
                viewModel.deleteFoodLog(foodLog)
                dismiss()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun formatMacro(value: Double?): String =
        value?.let { String.format(Locale.US, "%.1f g", it) } ?: getString(R.string.not_available_short)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}