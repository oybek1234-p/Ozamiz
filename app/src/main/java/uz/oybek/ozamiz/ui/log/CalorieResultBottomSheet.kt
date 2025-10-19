package uz.oybek.ozamiz.ui.log

import LogFoodUiState
import LogFoodViewModel
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ai.AnalyzedIngredient
import uz.oybek.ozamiz.data.ai.GeminiAnalysisResult
import uz.oybek.ozamiz.data.info.MealsType
import uz.oybek.ozamiz.databinding.BottomSheetCalorieResultBinding
import uz.oybek.ozamiz.ui.loadPhoto
import uz.oybek.ozamiz.ui.showToast
import java.util.Locale

@Parcelize
data class MealWarning(val message: String, val isSerious: Boolean = false) : Parcelable

class CalorieResultBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCalorieResultBinding? = null
    private val binding get() = _binding!!

    private lateinit var analysisResultFromArgs: GeminiAnalysisResult
    private lateinit var photoUrlFromArgs: String
    private lateinit var aboutFromArgs: String
    private lateinit var mealTypeFromArgs: String

    private val viewModel: LogFoodViewModel by activityViewModels()

    private data class HealthIndicatorUI(
        val backgroundColorRes: Int, val iconRes: Int, val textColorRes: Int, val levelText: String
    )

    companion object {
        private const val LEVEL_GOOD = "GOOD"
        private const val LEVEL_MODERATE = "MODERATE"
        private const val LEVEL_POOR = "POOR"

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireArguments().let {
            val parcelableResult: GeminiAnalysisResultParcelable? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    it.getParcelable(
                        ARG_ANALYSIS_RESULT, GeminiAnalysisResultParcelable::class.java
                    )
                } else {
                    @Suppress("DEPRECATION") it.getParcelable(ARG_ANALYSIS_RESULT)
                }

            analysisResultFromArgs = parcelableResult?.toGeminiAnalysisResult() ?: run {
                showToast("Natija ma'lumotlari topilmadi.")
                dismissAllowingStateLoss()
                return
            }

            photoUrlFromArgs = it.getString(ARG_PHOTO_URL_STRING, "")
            aboutFromArgs = it.getString(ARG_ABOUT_TEXT, "")
            mealTypeFromArgs = it.getString(ARG_MEAL_TYPE_ID, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCalorieResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayResults()
        setupActionButtons()
        setupInfoIconListeners()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (!isAdded) return@collectLatest

                val isProcessing = state is LogFoodUiState.Saving
                binding.progressBarSave.isVisible = isProcessing
                binding.buttonSaveToLog.isVisible = !isProcessing
                binding.backView.isEnabled = !isProcessing
                binding.buttonRetryAnalysis.isEnabled = !isProcessing
                binding.buttonSaveToLog.text =
                    if (isProcessing) "" else getString(R.string.save_to_log)

                if (state is LogFoodUiState.Success) {
                    closeSheet("success")
                } else if (state is LogFoodUiState.Error) {
                    showToast(state.message)
                    viewModel.stateHandled()
                }
            }
        }
    }

    private fun setupActionButtons() {
        binding.buttonSaveToLog.setOnClickListener {
            viewModel.confirmAndSaveLog()
        }
        binding.backView.setOnClickListener {
            closeSheet("dismiss")
        }
        binding.buttonRetryAnalysis.setOnClickListener {
            closeSheet("retry")
        }
    }

    private fun closeSheet(action: String) {
        if (isAdded && findNavController().currentDestination?.id == R.id.calorieResultBottomSheet) {
            viewModel.cancelAnalysisOrResetState()
            setFragmentResult(REQUEST_KEY, bundleOf(RESULT_ACTION to action))
            findNavController().popBackStack()
        }
    }

    private fun displayResults() {
        binding.textViewEstimatedCalories.text = analysisResultFromArgs.estimatedCalories.toString()
        val meal = MealsType.fromId(mealTypeFromArgs)
        binding.foodName.text = aboutFromArgs.ifBlank { meal?.name ?: "Ovqat" }
        binding.textViewEstimatedProtein.text =
            formatMacroValue(analysisResultFromArgs.estimatedProtein)
        binding.textViewEstimatedCarbs.text =
            formatMacroValue(analysisResultFromArgs.estimatedCarbs)
        binding.textViewEstimatedFat.text = formatMacroValue(analysisResultFromArgs.estimatedFat)
        binding.imageViewIllustration.isVisible = photoUrlFromArgs.isNotEmpty()
        if (photoUrlFromArgs.isNotEmpty()) {
            binding.imageViewIllustration.loadPhoto(photoUrlFromArgs)
        }
        setupHealthinessIndicator(
            analysisResultFromArgs.isHealthyForWeightLoss,
            analysisResultFromArgs.healthFeedbackShort
        )
    }

    private fun setupHealthinessIndicator(level: String?, advice: String?) {
        if (advice.isNullOrBlank()) {
            binding.layoutHealthFeedback.isVisible = false
            return
        }
        val uiData = when (level) {
            LEVEL_GOOD -> HealthIndicatorUI(
                R.color.green_alpha_10,
                R.drawable.check_ic,
                R.color.green,
                getString(R.string.healthiness_good)
            )

            LEVEL_MODERATE -> HealthIndicatorUI(
                R.color.yellow_alpha_10, R.drawable.warning_24px, R.color.yellowDark, "Tavsiya"
            )

            LEVEL_POOR -> HealthIndicatorUI(
                R.color.red_alpha_10, R.drawable.close_ic, R.color.rejected, "Tavsiya"
            )

            else -> HealthIndicatorUI(R.color.grey_alpha_50, R.drawable.info_ic, R.color.black, "")
        }

        binding.layoutHealthFeedback.isVisible = true
//        binding.layoutHealthFeedback.backgroundTintList =
//            ContextCompat.getColorStateList(requireContext(), uiData.backgroundColorRes)
        binding.iconHealthFeedback.setImageResource(uiData.iconRes)
        binding.iconHealthFeedback.setColorFilter(
            ContextCompat.getColor(
                requireContext(), uiData.textColorRes
            )
        )
        binding.levelText.text = uiData.levelText
        binding.levelText.setTextColor(
            ContextCompat.getColor(
                requireContext(), uiData.textColorRes
            )
        )
        binding.textViewHealthFeedback.text = advice
    }

    private fun setupInfoIconListeners() {
        binding.infoProtein.setOnClickListener {
            showMacroInfoDialog(
                "Oqsil (Protein) nima?",
                "Oqsil — tananing qurilish materiali. U mushaklarni tiklaydi, to'q tutadi va soch-tirnoqlar salomatligi uchun zarur. Go'sht, tuxum, sut mahsulotlari va dukkaklilarda ko'p bo'ladi."
            )
        }
        binding.infoCarbs.setOnClickListener {
            showMacroInfoDialog(
                "Uglevodlar nima?",
                "Uglevodlar — tananing asosiy energiya manbai. Ular 'tez' (shirinliklar, oq non) va 'sekin' (donlar, sabzavotlar) turlarga bo'linadi. Ozish uchun 'sekin' uglevodlarni tanlash muhim."
            )
        }
        binding.infoFat.setOnClickListener {
            showMacroInfoDialog(
                "Yog'lar nima?",
                "Yog'lar gormonlar ishlab chiqarish va vitaminlarni o'zlashtirish uchun kerak. 'Foydali' yog'lar (yong'oq, zaytun yog'i, baliq) va 'zararli' yog'lar (margarin, fastfud) mavjud. Foydali yog'larni me'yorida iste'mol qilish kerak."
            )
        }
    }

    private fun showMacroInfoDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext()).setTitle(title).setMessage(message)
            .setPositiveButton("Tushunarli") { dialog, _ -> dialog.dismiss() }.show()
    }

    private fun formatMacroValue(value: Double?): String =
        value?.let { String.format(Locale.US, "%.1f g", it) }
            ?: getString(R.string.not_available_short)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ====================================================================================
// =================== MUHIM QISM: PARCELABLE KLASSLAR =============================
// ====================================================================================

@Parcelize
data class GeminiAnalysisResultParcelable(
    val estimatedCalories: Int,
    val estimatedProtein: Double?,
    val estimatedFat: Double?,
    val estimatedCarbs: Double?,
    val recognizedIngredients: List<AnalyzedIngredientParcelable>,
    val alternativeFoodSuggestions: String,
    val isHealthyForWeightLoss: String?,
    val healthFeedbackShort: String?,
) : Parcelable {
    constructor(original: GeminiAnalysisResult) : this(
        original.estimatedCalories,
        original.estimatedProtein,
        original.estimatedFat,
        original.estimatedCarbs,
        original.recognizedIngredients.map { AnalyzedIngredientParcelable(it) },
        original.alternativeFoodSuggestions,
        original.isHealthyForWeightLoss,
        original.healthFeedbackShort
    )

    fun toGeminiAnalysisResult(): GeminiAnalysisResult {
        return GeminiAnalysisResult(
            estimatedCalories,
            estimatedProtein,
            estimatedFat,
            estimatedCarbs,
            recognizedIngredients.map { it.toAnalyzedIngredient() },
            alternativeFoodSuggestions,
            isHealthyForWeightLoss,
            healthFeedbackShort
        )
    }
}

@Parcelize
data class AnalyzedIngredientParcelable(
    val name: String, val estimatedCalories: Int?
) : Parcelable {
    constructor(original: AnalyzedIngredient) : this(
        original.name, original.estimatedCalories
    )

    fun toAnalyzedIngredient(): AnalyzedIngredient {
        return AnalyzedIngredient(name, estimatedCalories)
    }
}