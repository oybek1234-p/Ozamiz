package uz.oybek.ozamiz.ui.log

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.BottomSheetCalorieResultBinding
import uz.oybek.ozamiz.databinding.ItemWarningTextBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.loadPhoto
import uz.oybek.ozamiz.ui.showToast
import java.util.Locale

data class MealWarning(val message: String, val isSerious: Boolean = false) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString().toString(), parcel.readByte() != 0.toByte()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(message)
        parcel.writeByte(if (isSerious) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MealWarning> {
        override fun createFromParcel(parcel: Parcel): MealWarning {
            return MealWarning(parcel)
        }

        override fun newArray(size: Int): Array<MealWarning?> {
            return arrayOfNulls(size)
        }
    }
}

class CalorieResultBottomSheet : BaseFragment<BottomSheetCalorieResultBinding>() { // Change parent class

    private var _binding: BottomSheetCalorieResultBinding? = null

    private var calories: Int = 0
    private var protein: Double? = null;
    private var fat: Double? = null;
    private var carbs: Double? = null
    private var ingredients: List<String> = emptyList()
    private var insights: String = ""
    private var warnings: List<MealWarning> = emptyList()
    private var photoUrl: String? = null
    private var about: String = ""
    private var mealType: String = ""

    private val viewModel: LogFoodViewModel by activityViewModels() // Use shared ViewModel

    companion object {
        const val TAG = "CalorieResultBottomSheet";
        const val REQUEST_KEY = "CalorieResultRequestKey" // Key for sending result back
        const val RESULT_ACTION = "ResultAction"          // Key for the action taken

        const val ARG_CALORIES = "arg_calories";
        const val ARG_PROTEIN = "arg_protein";
        const val ARG_FAT = "arg_fat";
        const val ARG_CARBS = "arg_carbs";
        const val ARG_PHOTO = "arg_photo";
        const val ARG_ABOUT = "arg_about"
        const val ARG_INGREDIENTS = "arg_ingredients";
        const val ARG_INSIGHTS = "arg_insights";
        const val ARG_WARNINGS = "arg_warnings"
        const val ARG_MEAL_TYPE = "mealType" // Add mealType argument
    }

    override fun viewCreated(bind: BottomSheetCalorieResultBinding) {

    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomSheetCalorieResultBinding {
        return BottomSheetCalorieResultBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Optional: Set style for rounded corners if needed
        arguments?.let {
            calories = it.getInt(ARG_CALORIES, 0)
            protein = it.getDouble(ARG_PROTEIN, -1.0).takeIf { d -> d >= 0 }
            fat = it.getDouble(ARG_FAT, -1.0).takeIf { d -> d >= 0 }
            carbs = it.getDouble(ARG_CARBS, -1.0).takeIf { d -> d >= 0 }
            ingredients = it.getStringArrayList(ARG_INGREDIENTS) ?: emptyList()
            insights = it.getString(ARG_INSIGHTS, "")
            warnings = it.getParcelableArrayList<MealWarning>(ARG_WARNINGS)?.toList() ?: emptyList()
            photoUrl = it.getString(ARG_PHOTO)
            about = it.getString(ARG_ABOUT, "")
            mealType = it.getString(ARG_MEAL_TYPE, "")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayResults()
        setupActionButtons()
        observeViewModel()
    }

    private fun displayResults() {
        binding.textViewEstimatedCalories.text =
            getString(R.string.estimated_calories_format, calories)
        binding.foodName.text = about
        binding.textViewEstimatedProtein.text = formatMacro(protein)
        binding.textViewEstimatedFat.text = formatMacro(fat)
        binding.textViewEstimatedCarbs.text = formatMacro(carbs)

        if (photoUrl.isNullOrEmpty().not()) {
            binding.imageViewIllustration.loadPhoto(photoUrl)
        }

        binding.chipGroupIngredients.removeAllViews()
        if (ingredients.isNotEmpty()) {
            ingredients.forEach {
                val chip = Chip(requireContext()).apply {
                    text = it; isClickable = false; isCheckable =
                    false; setChipBackgroundColorResource(R.color.chip_background_color); setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.chip_text_color)
                )
                }
                binding.chipGroupIngredients.addView(chip)
            }; binding.chipGroupIngredients.isVisible = true
        } else {
            binding.chipGroupIngredients.isVisible = false
        }

        binding.textViewCalorieInsights.text =
            insights.ifBlank { getString(R.string.no_insights_available) }

        binding.layoutWarningsContent.removeAllViews()
        if (warnings.isNotEmpty()) {
            warnings.forEach { warning ->
                val warningBinding = ItemWarningTextBinding.inflate(
                    layoutInflater, binding.layoutWarningsContent, false
                )
                warningBinding.textViewWarning.text = warning.message
                warningBinding.iconWarning.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        if (warning.isSerious) R.color.rejected else R.color.colorWarning
                    )
                )
                binding.layoutWarningsContent.addView(warningBinding.root)
            }
            binding.layoutWarnings.isVisible = true
        } else {
            binding.layoutWarnings.isVisible = false
        }
        binding.textViewCalorieDisclaimer.text = getString(R.string.pfc_estimation_disclaimer)
    }

    private fun setupActionButtons() {
        binding.buttonSaveToLog.setOnClickListener {
            viewModel.confirmAndSaveLog() // Trigger save via ViewModel
        }
        binding.backView.setOnClickListener {
            setFragmentResult(REQUEST_KEY, bundleOf(RESULT_ACTION to "dismiss")) // Notify dismiss
            closeFragment()
        }
        binding.buttonRetryAnalysis.setOnClickListener {
            setFragmentResult(REQUEST_KEY, bundleOf(RESULT_ACTION to "retry")) // Notify retry
            closeFragment()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                val isSaving = state is LogFoodUiState.Saving
                binding.progressBarSave.isVisible = isSaving // Show/hide progress bar
                binding.buttonSaveToLog.isVisible = !isSaving
                binding.buttonRetryAnalysis.isVisible = !isSaving
                binding.backView.isEnabled = !isSaving
                binding.buttonSaveToLog.text = if(isSaving) "" else getString(R.string.save_to_log)


                if (state is LogFoodUiState.Success) {
                    closeFragment()
                    closeFragment()
                    viewModel.stateHandled()
                } else if (state is LogFoodUiState.Error) {
                    showToast(state.message)
                    viewModel.stateHandled()
                }
            }
        }
    }


    private fun formatMacro(v: Double?): String =
        v?.let { String.format(Locale.US, "%.1f g", it) } ?: getString(R.string.not_available_short)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}