package uz.oybek.ozamiz.ui.log

import LogFoodUiState
import LogFoodViewModel
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ai.GeminiAnalysisResult
import uz.oybek.ozamiz.data.info.MealsType
import uz.oybek.ozamiz.databinding.BottomSheetCalorieResultBinding
import uz.oybek.ozamiz.databinding.ItemWarningTextBinding
import uz.oybek.ozamiz.ui.dialogs.BadgeUnlockedDialog
import uz.oybek.ozamiz.ui.dialogs.FirstLogCongratulationDialog
import uz.oybek.ozamiz.ui.loadPhoto
import uz.oybek.ozamiz.ui.showToast
import java.util.Locale

@Parcelize
data class MealWarning(val message: String, val isSerious: Boolean = false) : Parcelable

class CalorieResultBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCalorieResultBinding? = null
    private val binding get() = _binding!!

    private lateinit var analysisResultFromArgs: GeminiAnalysisResult
    private lateinit var healthAssessmentTextFromArgs: String
    private var isHealthyAssessmentFromArgs: Boolean? = null
    private lateinit var photoUrlFromArgs: String
    private lateinit var aboutFromArgs: String
    private lateinit var mealTypeFromArgs: String

    private val viewModel: LogFoodViewModel by activityViewModels()

    companion object {
        const val TAG = "CalorieResultBottomSheet"
        const val REQUEST_KEY = "CalorieResultRequestKey"
        const val RESULT_ACTION = "ResultAction"

        const val ARG_ANALYSIS_RESULT = "arg_analysis_result_parcelable"
        const val ARG_PHOTO_URL_STRING = "arg_photo_url_string"
        const val ARG_ABOUT_TEXT = "arg_about_text"
        const val ARG_MEAL_TYPE_ID = "arg_meal_type_id"

        fun newInstance(
            analysisResult: GeminiAnalysisResultParcelable,
            photoUrl: String,
            about: String,
            mealTypeId: String
        ): CalorieResultBottomSheet {
            return CalorieResultBottomSheet().apply {
                arguments = bundleOf(
                    ARG_ANALYSIS_RESULT to analysisResult,
                    ARG_PHOTO_URL_STRING to photoUrl,
                    ARG_ABOUT_TEXT to about,
                    ARG_MEAL_TYPE_ID to mealTypeId
                )
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val parentLayout =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let {
                val behaviour = BottomSheetBehavior.from(it)
                setupFullHeight(it)
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
                behaviour.skipCollapsed = true
                behaviour.isFitToContents = true
            }
        }
        return dialog
    }

    private fun setupFullHeight(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCalorieResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val parcelableResult: GeminiAnalysisResultParcelable? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    it.getParcelable(
                        ARG_ANALYSIS_RESULT,
                        GeminiAnalysisResultParcelable::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    it.getParcelable(ARG_ANALYSIS_RESULT)
                }
            parcelableResult?.let { pr ->
                analysisResultFromArgs = pr.toGeminiAnalysisResult()
                healthAssessmentTextFromArgs = analysisResultFromArgs.healthFeedbackShort ?: ""
                isHealthyAssessmentFromArgs = analysisResultFromArgs.isHealthyForWeightLoss
            } ?: kotlin.run {
                showToast("Natija ma'lumotlari topilmadi.")
                dismissAllowingStateLoss()
                return
            }

            photoUrlFromArgs = it.getString(ARG_PHOTO_URL_STRING, "")
            aboutFromArgs = it.getString(ARG_ABOUT_TEXT, "")
            mealTypeFromArgs = it.getString(ARG_MEAL_TYPE_ID, "")
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
            getString(R.string.estimated_calories_format, analysisResultFromArgs.estimatedCalories) // Bu string resursdan edi, shunday qoladi
        val meal = MealsType.fromId(mealTypeFromArgs)
        binding.imageViewIllustration.loadPhoto(meal?.iconRes ?: R.drawable.food_ic)
        binding.foodName.text = aboutFromArgs.ifBlank { meal?.name ?: "Ovqat" } // Original "Ovqat" stringi qaytarildi

        binding.textViewEstimatedProtein.text = formatMacro(analysisResultFromArgs.estimatedProtein)
        binding.textViewEstimatedFat.text = formatMacro(analysisResultFromArgs.estimatedFat)
        binding.textViewEstimatedCarbs.text = formatMacro(analysisResultFromArgs.estimatedCarbs)

        if (photoUrlFromArgs.isNotEmpty()) {
            binding.imageViewIllustration.loadPhoto(photoUrlFromArgs)
        }
        binding.textViewHealthFeedback.text = healthAssessmentTextFromArgs
        val iconRes = when (isHealthyAssessmentFromArgs) {
            true -> R.drawable.check_circle_ic
            false -> R.drawable.cancel_ic
            null -> R.drawable.info_ic
        }
        val iconTint = when (isHealthyAssessmentFromArgs) {
            true -> ContextCompat.getColor(requireContext(), R.color.green)
            false -> ContextCompat.getColor(requireContext(), R.color.rejected)
            null -> ContextCompat.getColor(requireContext(), R.color.yellow)
        }
        binding.iconHealthFeedback.setImageResource(iconRes)
        binding.iconHealthFeedback.setColorFilter(iconTint)

        binding.chipGroupIngredients.removeAllViews()
        if (analysisResultFromArgs.recognizedIngredients.isNotEmpty()) {
            analysisResultFromArgs.recognizedIngredients.forEach {
                val chip = Chip(requireContext()).apply {
                    typeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_regular)
                    text = it; isClickable = false; isCheckable =
                    false; setChipBackgroundColorResource(R.color.chip_background_color); setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.chip_text_color)
                )
                }
                binding.chipGroupIngredients.addView(chip)
            }
            binding.chipGroupIngredients.isVisible = true
            binding.labelRecognizedIngredients.isVisible = true
        } else {
            binding.chipGroupIngredients.isVisible = false
            binding.labelRecognizedIngredients.isVisible = false
        }

        binding.textViewCalorieInsights.text =
            analysisResultFromArgs.insights.ifBlank { getString(R.string.no_insights_available) } // Bu string resursdan edi
        binding.textViewCalorieInsights.isVisible = analysisResultFromArgs.insights.isNotBlank()

        binding.layoutWarningsContent.removeAllViews()
        if (!analysisResultFromArgs.warningsText.isNullOrBlank()) {
            analysisResultFromArgs.warningsText!!.lines().forEach { warningLine ->
                if (warningLine.trim().startsWith("⚠️")) {
                    val warningBinding = ItemWarningTextBinding.inflate(
                        layoutInflater,
                        binding.layoutWarningsContent,
                        false
                    )
                    warningBinding.textViewWarning.text =
                        warningLine.trim().substringAfter("⚠️").trim()
                    binding.layoutWarningsContent.addView(warningBinding.root)
                }
            }
            binding.layoutWarnings.isVisible = binding.layoutWarningsContent.childCount > 0
        } else {
            binding.layoutWarnings.isVisible = false
        }
        binding.textViewCalorieDisclaimer.text = getString(R.string.pfc_estimation_disclaimer) // Bu string resursdan edi
    }

    private fun closeSheet() {
        setFragmentResult(REQUEST_KEY, bundleOf(RESULT_ACTION to "dismiss"))
        viewModel.cancelAnalysisOrResetState()
        findNavController().popBackStack()
    }

    private fun setupActionButtons() {
        binding.buttonSaveToLog.setOnClickListener { viewModel.confirmAndSaveLog() }
        binding.backView.setOnClickListener {
            closeSheet()
        }
        binding.buttonRetryAnalysis.setOnClickListener {
            setFragmentResult(REQUEST_KEY, bundleOf(RESULT_ACTION to "retry"))
            viewModel.cancelAnalysisOrResetState()
            findNavController().popBackStack()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (!isAdded) return@collectLatest

                val isSaving = state is LogFoodUiState.Saving
                binding.progressBarSave.isVisible = isSaving
                binding.buttonSaveToLog.isVisible = !isSaving
                binding.backView.isEnabled = !isSaving
                binding.buttonRetryAnalysis.isEnabled = !isSaving
                binding.buttonSaveToLog.text = if (isSaving) "" else getString(R.string.save_to_log) // Bu string resursdan edi

                if (dialog?.isShowing != true && findNavController().currentDestination?.id != R.id.calorieResultBottomSheet) {
                    if (state is LogFoodUiState.Success && findNavController().currentDestination?.id == R.id.logFoodFragment) {
                        // Bu holatda LogFoodFragment o'zi yopiladi
                    }
                    if (state !is LogFoodUiState.Success) return@collectLatest
                }

                when (state) {
                    is LogFoodUiState.ShowPointsEarned -> {
                    }

                    is LogFoodUiState.ShowFirstLogCongratulation -> {
                        setFragmentResultListener("logCongra") { key, bundle ->
                            closeSheet()
                        }
                        FirstLogCongratulationDialog.newInstance()
                            .show(parentFragmentManager, FirstLogCongratulationDialog.TAG)
                    }

                    is LogFoodUiState.ShowStreakContinued -> {
                        showToast("Ajoyib! Siz ${state.currentStreak} kundan beri rejimdasiz!") // Original string
                    }

                    is LogFoodUiState.ShowMealGoalReached -> {
                        if (findNavController().currentDestination?.id == R.id.calorieResultBottomSheet) {
                            val action =
                                CalorieResultBottomSheetDirections.actionCalorieResultBottomSheetToFoodEnoughFragment(
                                    mealType = state.mealType,
                                    snacks = state.recommendedSnacks.toTypedArray()
                                )
                            findNavController().navigate(action)
                        }
                    }

                    is LogFoodUiState.Success -> {
                        if (dialog?.isShowing == true) {
                            dismissAllowingStateLoss()
                        }
                        closeSheet()
                    }

                    is LogFoodUiState.Error -> {
                        showToast(state.message)
                        viewModel.stateHandled()
                    }

                    else -> {}
                }
            }
        }
    }

    private fun formatMacro(value: Double?): String =
        value?.let { String.format(Locale.US, "%.1f g", it) }
            ?: getString(R.string.not_available_short) // Bu string resursdan edi
}

@Parcelize
data class GeminiAnalysisResultParcelable(
    val estimatedCalories: Int,
    val estimatedProtein: Double?,
    val estimatedFat: Double?,
    val estimatedCarbs: Double?,
    val recognizedIngredients: List<String>,
    val insights: String,
    val alternativeFoodSuggestions: String,
    val isHealthyForWeightLoss: Boolean?,
    val healthFeedbackShort: String?,
    val warningsText: String?
) : Parcelable {
    constructor(original: GeminiAnalysisResult) : this(
        original.estimatedCalories,
        original.estimatedProtein,
        original.estimatedFat,
        original.estimatedCarbs,
        original.recognizedIngredients,
        original.insights,
        original.alternativeFoodSuggestions,
        original.isHealthyForWeightLoss,
        original.healthFeedbackShort,
        original.warningsText
    )

    fun toGeminiAnalysisResult(): GeminiAnalysisResult {
        return GeminiAnalysisResult(
            estimatedCalories,
            estimatedProtein,
            estimatedFat,
            estimatedCarbs,
            recognizedIngredients,
            insights,
            alternativeFoodSuggestions,
            isHealthyForWeightLoss,
            healthFeedbackShort,
            warningsText
        )
    }
}