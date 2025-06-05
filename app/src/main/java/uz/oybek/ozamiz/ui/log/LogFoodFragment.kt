package uz.oybek.ozamiz.ui.log

import MiniInfo
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ai.GeminiAnalysisResult
import uz.oybek.ozamiz.data.info.FoodLog
import uz.oybek.ozamiz.databinding.FoodAnalyzingLoaderBinding
import uz.oybek.ozamiz.databinding.FragmentLogFoodBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.dialogs.FirstLogCongratulationDialog
import uz.oybek.ozamiz.ui.hideSoftInput
import uz.oybek.ozamiz.ui.premium.PremiumBottomSheetDialog // Import premium bottom sheet
import uz.oybek.ozamiz.ui.showKeyboard
import uz.oybek.ozamiz.ui.showToast

class LogFoodFragment : BaseFragment<FragmentLogFoodBinding>() {

    private val viewModel: LogFoodViewModel by activityViewModels()

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                viewModel.selectedImageUri = it
                binding.layoutAddImagePlaceholderLog.isVisible = false
                Glide.with(this).load(it).into(binding.imageViewSelectedLogImage)
                binding.imageViewSelectedLogImage.alpha = 1.0f
            }
        }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentLogFoodBinding {
        return FragmentLogFoodBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadArguments(arguments)
    }

    override fun viewCreated(bind: FragmentLogFoodBinding) {
        setupToolbar(bind)
        setupClickListeners(bind)
        observeUiState(bind)
        setupBottomSheetResultListener()
    }

    private fun setupToolbar(bind: FragmentLogFoodBinding) {
        bind.toolbarLogFood.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    private var description: String = ""

    private fun setupClickListeners(bind: FragmentLogFoodBinding) {
        bind.cardImagePickerLog.setOnClickListener { pickImageLauncher.launch("image/*") }
        bind.buttonAnalyzeAndSave.setOnClickListener {
            val description = bind.editTextFoodDescriptionLog.text.toString()
            this.description = description
            if (viewModel.logToEdit != null) {
                viewModel.saveEditedLog(description)
            } else {
                viewModel.analyzeAndPrepareLog(description, viewModel.mealType)
            }
            hideSoftInput(requireActivity())
        }
    }

    private val bottomSheet: BottomSheetDialog by lazy {
        BottomSheetDialog(requireContext())
    }

    private fun closeAnalyzeSheet() {
        bottomSheet.dismiss()
    }

    private fun showAnalyzeSheet() {
        bottomSheet.setOnDismissListener {
            viewModel.cancelAnalysisOrResetState()
        }
        val binding = FoodAnalyzingLoaderBinding.inflate(layoutInflater)
        binding.apply {
            miniInfoText.text = MiniInfo.list.random().text
            backView.setOnClickListener {
                bottomSheet.dismiss()
            }
        }
        bottomSheet.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheetInternal = d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)

            bottomSheetInternal?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isFitToContents = false

                val layoutParams = it.layoutParams
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                it.layoutParams = layoutParams
            }
        }
        bottomSheet.setContentView(binding.root)
        bottomSheet.show()
    }

    private fun observeUiState(bind: FragmentLogFoodBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                val isLoading =
                    state is LogFoodUiState.Saving || state is LogFoodUiState.Analyzing || state is LogFoodUiState.Loading
                bind.progressAnalyze.isVisible = isLoading
                bind.buttonAnalyzeAndSave.isEnabled = !isLoading

                bind.buttonAnalyzeAndSave.text = if (isLoading) "" else {
                    when (state) {
                        is LogFoodUiState.Editing -> getString(R.string.save_changes)
                        else -> getString(R.string.analyze_and_save)
                    }
                }

                when (state) {
                    is LogFoodUiState.Loading -> {
                        bind.progressAnalyze.isVisible = true
                    }
                    is LogFoodUiState.Inputting -> {
                        bind.toolbarLogFood.title = getString(
                            R.string.log_food_title_format, getMealTypeName(state.mealType)
                        )
                        bind.buttonAnalyzeAndSave.setIconResource(R.drawable.search_ic)
                    }
                    is LogFoodUiState.Editing -> {
                        bind.toolbarLogFood.title = getString(
                            R.string.edit_log_food_title_format, getMealTypeName(state.log.mealType)
                        )
                        populateFieldsForEditing(bind, state.log)
                        bind.buttonAnalyzeAndSave.setIconResource(R.drawable.check_ic)
                    }
                    is LogFoodUiState.Analyzing -> {
                        bind.buttonAnalyzeAndSave.text = getString(R.string.analyzing)
                        bind.buttonAnalyzeAndSave.setIconResource(0)
                        showAnalyzeSheet()
                    }
                    is LogFoodUiState.ShowResult -> {
                        viewModel.stateHandled()
                        closeAnalyzeSheet()
                        showCalorieResultSheet(state.result, state.warnings)
                    }
                    is LogFoodUiState.Saving -> {
                        bind.buttonAnalyzeAndSave.text = getString(R.string.saving)
                        bind.buttonAnalyzeAndSave.setIconResource(0)
                    }
                    is LogFoodUiState.ShowFirstLogCongratulation -> {
                        if (isAdded && findNavController().currentDestination?.id == R.id.logFoodFragment) {
                            FirstLogCongratulationDialog.newInstance().show(parentFragmentManager, FirstLogCongratulationDialog.TAG)
                        }
                        viewModel.stateHandled() // Important to transition state
                    }
                    is LogFoodUiState.ShowStreakContinued -> {
                        showToast("Ajoyib! Siz ${state.currentStreak} kundan beri rejimdasiz!")
                        viewModel.stateHandled() // Important to transition state
                    }
                    is LogFoodUiState.Success -> {
                        if (findNavController().currentDestination?.id == R.id.logFoodFragment) {
                            showToast(getString(R.string.log_saved_successfully))
                            findNavController().popBackStack()
                        }
                        viewModel.stateHandled()
                    }
                    is LogFoodUiState.Error -> {
                        bind.progressAnalyze.isVisible = false
                        showToast(state.message)
                        viewModel.stateHandled()
                    }
                    is LogFoodUiState.ShowPremiumPrompt -> {
                        if (isAdded && findNavController().currentDestination?.id == R.id.logFoodFragment) {
                            PremiumBottomSheetDialog().show(parentFragmentManager, PremiumBottomSheetDialog.TAG)
                        }
                        // Don't call stateHandled() here immediately.
                        // The user needs to interact with the premium dialog.
                        // The state will be reset if the dialog is dismissed or an action is taken.
                    }
                }
            }
        }
    }


    private fun populateFieldsForEditing(bind: FragmentLogFoodBinding, log: FoodLog) {
        bind.editTextFoodDescriptionLog.setText(log.description)
        viewModel.selectedImageUri = null
        if (!log.photoUrl.isNullOrEmpty()) {
            bind.layoutAddImagePlaceholderLog.isVisible = false
            Glide.with(this).load(log.photoUrl).placeholder(R.drawable.placeholder_food)
                .error(R.drawable.placeholder_food).into(bind.imageViewSelectedLogImage)
            bind.imageViewSelectedLogImage.alpha = 1.0f
        } else {
            bind.layoutAddImagePlaceholderLog.isVisible = true
            bind.imageViewSelectedLogImage.setImageDrawable(null)
            bind.imageViewSelectedLogImage.alpha = 0.5f
        }
    }

    private fun showCalorieResultSheet(result: GeminiAnalysisResult, warnings: List<MealWarning>) {
        val bundle = Bundle().apply {
            putInt(CalorieResultBottomSheet.ARG_CALORIES, result.estimatedCalories)
            putDouble(CalorieResultBottomSheet.ARG_PROTEIN, result.estimatedProtein ?: -1.0)
            putDouble(CalorieResultBottomSheet.ARG_FAT, result.estimatedFat ?: -1.0)
            putDouble(CalorieResultBottomSheet.ARG_CARBS, result.estimatedCarbs ?: -1.0)
            putString(CalorieResultBottomSheet.ARG_PHOTO, viewModel.selectedImageUri?.toString())
            putString(CalorieResultBottomSheet.ARG_ABOUT, description)
            putString("mealType", viewModel.mealType)
            putStringArrayList(
                CalorieResultBottomSheet.ARG_INGREDIENTS, ArrayList(result.recognizedIngredients)
            )
            putString(CalorieResultBottomSheet.ARG_INSIGHTS, result.insights)
            putParcelableArrayList(CalorieResultBottomSheet.ARG_WARNINGS, ArrayList(warnings))
        }
        try {
            if (findNavController().currentDestination?.id == R.id.logFoodFragment) {
                findNavController().navigate(
                    R.id.action_logFoodFragment_to_calorieResultBottomSheet, // Corrected action ID
                    bundle
                )
            }
        } catch (e: Exception) {
            showToast("Error showing results: ${e.message}")
            viewModel.cancelAnalysisOrResetState()
        }
    }

    private fun getMealTypeName(mealTypeId: String): String {
        return when (mealTypeId) {
            "breakfast" -> getString(R.string.breakfast); "lunch" -> getString(R.string.lunch)
            "dinner" -> getString(R.string.dinner); "snack" -> getString(R.string.snack)
            else -> mealTypeId
        }
    }

    override fun onPause() {
        super.onPause();
        hideSoftInput(requireActivity())
    }

    private fun setupBottomSheetResultListener() {
        setFragmentResultListener(CalorieResultBottomSheet.REQUEST_KEY) { _, bundle ->
            val action = bundle.getString(CalorieResultBottomSheet.RESULT_ACTION)
            when (action) {
                "save" -> viewModel.confirmAndSaveLog()
                "retry" -> viewModel.cancelAnalysisOrResetState() // Or trigger retry logic
                "dismiss" -> viewModel.cancelAnalysisOrResetState()
            }
        }
        // Listener for PremiumBottomSheetDialog
        parentFragmentManager.setFragmentResultListener(PremiumBottomSheetDialog.REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            val action = bundle.getString(PremiumBottomSheetDialog.RESULT_ACTION)
            if (action == "go_to_payment") {
                if (findNavController().currentDestination?.id == R.id.logFoodFragment) {
                    navigate(R.id.paymentGetCheckFragment)
                }
            }
            // After premium dialog interaction, reset LogFoodViewModel state
            viewModel.stateHandled()
        }
    }
}