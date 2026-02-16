package uz.oybek.ozamiz.ui.feature.log.food.history

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
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.databinding.BottomSheetTodaysFoodDetailBinding
import uz.oybek.ozamiz.databinding.ItemMacroDisplayBinding
import uz.oybek.ozamiz.ui.feature.log.food.LogFoodViewModel
import java.text.SimpleDateFormat
import java.util.Date
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
        setStyle(STYLE_NORMAL, R.style.BottomSheetStyle)
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
        // Rasm
        if (!foodLog.photoUrl.isNullOrEmpty()) {
            Glide.with(this).load(foodLog.photoUrl).placeholder(R.drawable.placeholder_food).into(binding.imageViewFoodDetail)
        }

        // Vaqt
        val timestamp = foodLog.timestamp as? Long
        if (timestamp != null) {
            val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            binding.textViewTimestampDetail.text = dateFormat.format(Date(timestamp))
        }

        // Asosiy ma'lumotlar
        binding.textViewFoodDescriptionDetail.text = foodLog.description
        binding.textViewDetailCalories.text = foodLog.calories.toString()

        // Makrolar
        bindMacro(binding.layoutProteinDetail, "Protein", foodLog.protein, R.color.green)
        bindMacro(binding.layoutCarbsDetail, "Carbs", foodLog.carbs, R.color.yellow)
        bindMacro(binding.layoutFatDetail, "Fats", foodLog.fat, R.color.rejected)

        // AI Insight
        if (!foodLog.aiInsights.isNullOrBlank()) {
            binding.cardAiHealthDetail.isVisible = true
            binding.textViewHealthFeedbackDetail.text = foodLog.aiInsights
        }
    }

    private fun bindMacro(layout: ItemMacroDisplayBinding, name: String, value: Double?, colorRes: Int) {
        layout.textViewMacroName.text = name
        layout.textViewMacroValue.text = "${value?.toInt() ?: 0}g"
        layout.textViewMacroValue.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
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
            putLong("selectedDateTimestamp", (foodLog.timestamp as? Long) ?: System.currentTimeMillis())
        }

    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_message_generic, foodLog.description))
            .setPositiveButton(R.string.yes_delete) { _, _ ->
             //   viewModel.deleteFoodLog(foodLog)
                dismiss()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}