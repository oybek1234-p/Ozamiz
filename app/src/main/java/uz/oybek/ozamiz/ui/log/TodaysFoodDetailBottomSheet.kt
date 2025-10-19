package uz.oybek.ozamiz.ui.log

import LogFoodViewModel
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
        binding.imageViewFoodDetail.isVisible = !foodLog.photoUrl.isNullOrEmpty()
        if (!foodLog.photoUrl.isNullOrEmpty()) {
            Glide.with(this).load(foodLog.photoUrl).placeholder(R.drawable.placeholder_food)
                .error(R.drawable.placeholder_food).into(binding.imageViewFoodDetail)
        }

        binding.textViewFoodDescriptionDetail.text = foodLog.description
        binding.textViewDetailCalories.text = getString(R.string.calories_format, foodLog.calories)
        binding.textViewEstimatedProtein.text = formatMacro(foodLog.protein)
        binding.textViewEstimatedFat.text = formatMacro(foodLog.fat)
        binding.textViewEstimatedCarbs.text = formatMacro(foodLog.carbs)

        setupHealthinessIndicator(foodLog.healthinessLevel, foodLog.analysisInsights)
    }

    private fun setupHealthinessIndicator(level: String?, advice: String?) {
        binding.layoutHealthFeedbackDetail.isVisible = !advice.isNullOrBlank()
        if (advice.isNullOrBlank()) return

        val (colorRes, iconRes, textColorRes) = when (level) {
            "GOOD" -> Triple(R.color.green_alpha_10, R.drawable.check_circle_ic, R.color.green)
            "MODERATE" -> Triple(
                R.color.yellow_alpha_10, R.drawable.warning_24px, R.color.yellowDark
            )

            "POOR" -> Triple(R.color.red_alpha_10, R.drawable.cancel_ic, R.color.rejected)
            else -> Triple(R.color.grey_alpha_50, R.drawable.info_ic, R.color.black)
        }

        binding.layoutHealthFeedbackDetail.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                requireContext(), colorRes
            )
        )
        binding.iconHealthFeedbackDetail.setImageResource(iconRes)
        binding.iconHealthFeedbackDetail.setColorFilter(
            ContextCompat.getColor(
                requireContext(), textColorRes
            )
        )
        binding.textViewHealthFeedbackDetail.text = advice
        binding.textViewHealthFeedbackDetail.setTextColor(
            ContextCompat.getColor(
                requireContext(), textColorRes
            )
        )
    }

    private fun setupListeners() {
        binding.toolbarFoodDetail.setNavigationOnClickListener { dismiss() }

        binding.buttonDeleteLog.setOnClickListener {
            showDeleteConfirmation()
        }

        binding.buttonEditLog.setOnClickListener {
            navigateToEditLog()
        }
    }

    private fun navigateToEditLog() {
        dismiss()
        val bundle = Bundle().apply {
            putString("logToEditJson", Gson().toJson(foodLog))
            putLong(
                "selectedDateTimestamp", (foodLog.timestamp as? Long) ?: System.currentTimeMillis()
            )
        }
        findNavController().navigate(R.id.logFoodFragment, bundle)
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext()).setTitle(R.string.confirm_delete_title).setMessage(
            getString(
                R.string.confirm_delete_message_generic,
                foodLog.description.ifBlank { "bu ovqatni" })
        ).setPositiveButton(R.string.yes_delete) { _, _ ->
            viewModel.deleteFoodLog(foodLog)
            dismiss()
        }.setNegativeButton(R.string.no, null).show()
    }

    private fun formatMacro(value: Double?): String =
        value?.let { String.format(Locale.US, "%.1f g", it) }
            ?: getString(R.string.not_available_short)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}