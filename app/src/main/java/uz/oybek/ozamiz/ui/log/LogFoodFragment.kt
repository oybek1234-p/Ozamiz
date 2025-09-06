package uz.oybek.ozamiz.ui.log

import LogFoodUiState
import LogFoodViewModel
import MiniInfo
import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ai.GeminiAnalysisResult
import uz.oybek.ozamiz.data.info.FoodLog
import uz.oybek.ozamiz.data.info.MealsType
import uz.oybek.ozamiz.databinding.FoodAnalyzingLoaderBinding
import uz.oybek.ozamiz.databinding.FragmentLogFoodBinding
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.hideSoftInput
import uz.oybek.ozamiz.ui.premium.PremiumBottomSheetDialog
import uz.oybek.ozamiz.ui.showToast
import java.io.File
import java.util.Locale

class LogFoodFragment : BaseFragment<FragmentLogFoodBinding>() {

    private val viewModel: LogFoodViewModel by activityViewModels()
    private var tempImageUri: Uri? = null

    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestAudioPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var speechToTextLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadArguments(arguments)
        initializePermissionLaunchers()
        initializeActivityLaunchers()
    }

    private fun initializePermissionLaunchers() {
        requestCameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    launchCamera()
                } else {
                    showToast(getString(R.string.permission_denied_camera))
                }
            }
        requestAudioPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    launchSpeechToText()
                } else {
                    showToast(getString(R.string.permission_denied_audio))
                }
            }
    }

    private fun initializeActivityLaunchers() {
        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    tempImageUri?.let {
                        viewModel.selectedImageUri = it
                        updateSelectedImageView(it)
                    }
                } else {
                    tempImageUri = null
                }
            }

        speechToTextLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    val spokenText: ArrayList<String>? =
                        data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    if (!spokenText.isNullOrEmpty()) {
                        val currentText = binding.editTextFoodDescriptionLog.text.toString()
                        binding.editTextFoodDescriptionLog.setText(
                            if (currentText.isBlank()) spokenText[0] else "$currentText ${spokenText[0]}"
                        )
                        binding.editTextFoodDescriptionLog.setSelection(binding.editTextFoodDescriptionLog.text!!.length)
                    }
                }
            }
    }

    private fun updateSelectedImageView(uri: Uri?) {
        if (uri != null) {
            binding.layoutAddImagePlaceholderLog.isVisible = false
            binding.imageViewSelectedLogImage.isVisible = true
            Glide.with(this).load(uri).placeholder(R.drawable.placeholder_food)
                .into(binding.imageViewSelectedLogImage)
            binding.buttonRemoveImageLog.isVisible = true
        } else {
            binding.layoutAddImagePlaceholderLog.isVisible = true
            binding.imageViewSelectedLogImage.isVisible = false
            binding.imageViewSelectedLogImage.setImageDrawable(null)
            binding.buttonRemoveImageLog.isVisible = false
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentLogFoodBinding {
        return FragmentLogFoodBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentLogFoodBinding) {
        setupToolbar(bind)
        setupMealTypeChips(bind)
        setupClickListeners(bind)
        observeUiState(bind)
        setupBottomSheetResultListener()
        updateSelectedImageView(viewModel.selectedImageUri)
    }

    private fun setupToolbar(bind: FragmentLogFoodBinding) {
        bind.toolbarLogFood.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    private fun setupMealTypeChips(bind: FragmentLogFoodBinding) {
        MealsType.mealTypes.forEach { mealType ->
            val chip = layoutInflater.inflate(R.layout.item_meal_type_selectable, bind.chipGroupMealType, false) as Chip
            chip.text = mealType.name
            chip.tag = mealType.id
            chip.id = View.generateViewId()
            chip.isCheckable = true
            bind.chipGroupMealType.addView(chip)
        }

        bind.chipGroupMealType.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId != View.NO_ID) {
                val chip = group.findViewById<Chip>(checkedId)
                viewModel.mealType = chip.tag as String
                bind.toolbarLogFood.title = getString(R.string.log_food_title_format, chip.text)
            }
        }
    }


    private var currentDescriptionFromInput: String = ""

    private fun setupClickListeners(bind: FragmentLogFoodBinding) {
        bind.layoutAddImagePlaceholderLog.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }
        bind.buttonRemoveImageLog.setOnClickListener {
            viewModel.selectedImageUri = null
            tempImageUri = null
            updateSelectedImageView(null)
        }

        bind.textInputLayoutFoodDescriptionLog.setEndIconOnClickListener {
            checkAudioPermissionAndLaunchSpeechToText()
        }

        bind.buttonAnalyzeAndSave.setOnClickListener {
            currentDescriptionFromInput = bind.editTextFoodDescriptionLog.text.toString()
            val imageUriToAnalyze = viewModel.selectedImageUri ?: tempImageUri
            viewModel.selectedImageUri = imageUriToAnalyze

            if (viewModel.logToEdit != null) {
                viewModel.saveEditedLog(currentDescriptionFromInput)
            } else {
                viewModel.analyzeAndPrepareLog(currentDescriptionFromInput, "")
            }
            hideSoftInput(requireActivity())
        }
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showToast(getString(R.string.permission_denied_camera))
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }

            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        try {
            val photoFile: File? = try {
                File.createTempFile(
                    "JPEG_${System.currentTimeMillis()}_", ".jpg", requireContext().externalCacheDir
                )
            } catch (ex: Exception) {
                handleException(ex, "CreateTempFileError")
                null
            }
            photoFile?.also {
                tempImageUri = FileProvider.getUriForFile(
                    requireContext(), "${requireContext().packageName}.provider", it
                )
                takePictureLauncher.launch(tempImageUri)
            }
        } catch (e: Exception) {
            handleException(e, "LaunchCameraError")
            showToast(getString(R.string.error_opening_camera))
        }
    }

    private fun checkAudioPermissionAndLaunchSpeechToText() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchSpeechToText()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                showToast(getString(R.string.permission_denied_audio))
                requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }

            else -> {
                requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun launchSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("uz-UZ"))
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt))
        }
        try {
            speechToTextLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            showToast(getString(R.string.speech_not_supported))
        }
    }

    private val bottomSheet: BottomSheetDialog by lazy {
        BottomSheetDialog(requireContext())
    }

    private fun closeAnalyzeSheet() {
        if (bottomSheet.isShowing) {
            bottomSheet.dismiss()
        }
    }

    private fun showAnalyzeSheet() {
        if (bottomSheet.isShowing) return

        bottomSheet.setOnDismissListener {
            if (viewModel.uiState.value is LogFoodUiState.Analyzing) {
                viewModel.cancelAnalysisOrResetState()
            }
        }
        val dialogBinding = FoodAnalyzingLoaderBinding.inflate(layoutInflater)
        dialogBinding.apply {
            miniInfoText.text = MiniInfo.list.random().text
            backView.setOnClickListener {
                bottomSheet.dismiss()
            }
        }
        bottomSheet.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheetInternal =
                d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
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
        bottomSheet.setContentView(dialogBinding.root)
        bottomSheet.show()
    }

    override fun onResume() {
        super.onResume()
        if (dismiss) {
            closeFragment()
            dismiss = false
        }
    }

    private fun observeUiState(bind: FragmentLogFoodBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                val isLoading =
                    state is LogFoodUiState.Saving || state is LogFoodUiState.Analyzing || state is LogFoodUiState.Loading
                bind.progressAnalyze.isVisible = isLoading
                bind.buttonAnalyzeAndSave.isEnabled = !isLoading
                bind.editTextFoodDescriptionLog.isEnabled = !isLoading

                bind.buttonAnalyzeAndSave.text =
                    if (isLoading && state !is LogFoodUiState.Analyzing) "" else {
                        when (state) {
                            is LogFoodUiState.Editing -> getString(R.string.save_changes)
                            is LogFoodUiState.Analyzing -> getString(R.string.analyzing)
                            is LogFoodUiState.Saving -> getString(R.string.saving)
                            else -> getString(R.string.analyze_and_save)
                        }
                    }
                bind.buttonAnalyzeAndSave.setIconResource(
                    if (state is LogFoodUiState.Analyzing || state is LogFoodUiState.Saving) 0
                    else if (state is LogFoodUiState.Editing) R.drawable.check_ic
                    else R.drawable.search_ic
                )

                when (state) {
                    is LogFoodUiState.Inputting -> {
                        val chipToSelect = bind.chipGroupMealType.findViewWithTag<Chip>(state.mealType)
                        if (chipToSelect != null && !chipToSelect.isChecked) {
                            chipToSelect.isChecked = true
                        } else if (chipToSelect == null && bind.chipGroupMealType.checkedChipId == View.NO_ID) {
                            (bind.chipGroupMealType.getChildAt(0) as? Chip)?.isChecked = true
                        }
                    }

                    is LogFoodUiState.Editing -> {
                        populateFieldsForEditing(bind, state.log)
                        val chipToSelect = bind.chipGroupMealType.findViewWithTag<Chip>(state.log.mealType)
                        if(chipToSelect != null && !chipToSelect.isChecked) {
                            chipToSelect.isChecked = true
                        }
                    }

                    is LogFoodUiState.Analyzing -> {
                        if (!bottomSheet.isShowing) showAnalyzeSheet()
                    }

                    is LogFoodUiState.ShowResult -> {
                        viewModel.stateHandled()
                        closeAnalyzeSheet()
                        showCalorieResultSheet(state.analysisResult)
                    }

                    is LogFoodUiState.Success -> {
                        if (findNavController().currentDestination?.id == R.id.logFoodFragment) {
                            showToast(getString(R.string.log_saved_successfully))
                            findNavController().popBackStack()
                        }
                        viewModel.stateHandled()
                    }

                    is LogFoodUiState.Error -> {
                        closeAnalyzeSheet()
                        showToast(state.message)
                        viewModel.stateHandled()
                    }

                    is LogFoodUiState.ShowPremiumPrompt -> {
                        closeAnalyzeSheet()
                        if (isAdded && findNavController().currentDestination?.id == R.id.logFoodFragment) {
                            PremiumBottomSheetDialog().show(
                                parentFragmentManager, PremiumBottomSheetDialog.TAG
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun populateFieldsForEditing(bind: FragmentLogFoodBinding, log: FoodLog) {
        bind.editTextFoodDescriptionLog.setText(log.description)
        viewModel.selectedImageUri = null
        tempImageUri = null
        if (!log.photoUrl.isNullOrEmpty()) {
            updateSelectedImageView(Uri.parse(log.photoUrl))
        } else {
            updateSelectedImageView(null)
        }
    }

    private fun showCalorieResultSheet(analysisResult: GeminiAnalysisResult) {
        val imageUriToShow = viewModel.selectedImageUri ?: tempImageUri
        val bundle = Bundle().apply {
            putParcelable(
                CalorieResultBottomSheet.ARG_ANALYSIS_RESULT,
                GeminiAnalysisResultParcelable(analysisResult)
            )
            putString(CalorieResultBottomSheet.ARG_PHOTO_URL_STRING, imageUriToShow?.toString())
            putString(CalorieResultBottomSheet.ARG_ABOUT_TEXT, currentDescriptionFromInput)
            putString(CalorieResultBottomSheet.ARG_MEAL_TYPE_ID, viewModel.mealType)
        }
        try {
            if (findNavController().currentDestination?.id == R.id.logFoodFragment) {
                navigate(
                    R.id.action_logFoodFragment_to_calorieResultBottomSheet, bundle
                )
            }
        } catch (e: Exception) {
            showToast("Natijalarni ko'rsatishda xatolik: ${e.message}")
            viewModel.cancelAnalysisOrResetState()
        }
    }

    private fun getMealTypeName(mealTypeId: String): String {
        return MealsType.fromId(mealTypeId)?.name ?: mealTypeId
    }

    override fun onPause() {
        super.onPause()
        hideSoftInput(requireActivity())
    }

    private var dismiss = false

    private fun setupBottomSheetResultListener() {
        setFragmentResultListener(CalorieResultBottomSheet.REQUEST_KEY) { _, bundle ->
            val action = bundle.getString(CalorieResultBottomSheet.RESULT_ACTION)
            when (action) {
                "retry" -> {
                    viewModel.stateHandled()
                }
                "dismiss" -> {
                    dismiss = true
                }
            }
        }
        parentFragmentManager.setFragmentResultListener(
            PremiumBottomSheetDialog.REQUEST_KEY, viewLifecycleOwner
        ) { _, bundle ->
            val action = bundle.getString(PremiumBottomSheetDialog.RESULT_ACTION)
            if (action == "go_to_payment") {
                if (findNavController().currentDestination?.id == R.id.logFoodFragment) {
                    navigate(R.id.paymentGetCheckFragment)
                }
            }
            viewModel.stateHandled()
        }
    }
}