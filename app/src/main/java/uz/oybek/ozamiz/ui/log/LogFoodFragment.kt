// ✅ YAKUNIY, XATOSIZ VA YANGILANGAN VERSIYA
// Fayl manzili: uz/oybek/ozamiz/ui/log/LogFoodFragment.kt
package uz.oybek.ozamiz.ui.log

import LogFoodUiState
import LogFoodViewModel
import MiniInfo
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ai.GeminiAnalysisResult
import uz.oybek.ozamiz.data.info.MealsType
import uz.oybek.ozamiz.databinding.FoodAnalyzingLoaderBinding
import uz.oybek.ozamiz.databinding.FragmentLogFoodBinding
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.hideSoftInput
import uz.oybek.ozamiz.ui.log.voice.VoiceRecorderBottomSheet
import uz.oybek.ozamiz.ui.premium.PremiumBottomSheetDialog
import uz.oybek.ozamiz.ui.showToast
import java.io.File

class LogFoodFragment : BaseFragment<FragmentLogFoodBinding>() {

    private val viewModel: LogFoodViewModel by activityViewModels()
    private var tempImageUri: Uri? = null

    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestAudioPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var todaysFoodAdapter: TodaysFoodAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.clearData()
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
                    showVoiceRecorderSheet()
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
    }

    // YAXSHILANGAN: Endi bu funksiya "Tahlil qilish" tugmasining holatini ham boshqaradi
    private fun updateSelectedImageView(uri: Uri?) {
        val hasImage = uri != null
        binding.layoutAddImagePlaceholderLog.isVisible = !hasImage
        binding.imageViewSelectedLogImage.isVisible = hasImage
        binding.buttonRemoveImageLog.isVisible = hasImage

        if (hasImage) {
            Glide.with(this).load(uri).placeholder(R.drawable.placeholder_food)
                .into(binding.imageViewSelectedLogImage)
        } else {
            binding.imageViewSelectedLogImage.setImageDrawable(null)
        }
        updateAnalyzeButtonVisibility() // Tugma holatini yangilash
    }

    private fun updateAnalyzeButtonVisibility() {
        val hasText = binding.editTextFoodDescriptionLog.text.toString().isNotBlank()
        val hasImage = viewModel.selectedImageUri != null || tempImageUri != null
        binding.buttonAnalyzeAndSave.isEnabled = hasText || hasImage
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentLogFoodBinding {
        return FragmentLogFoodBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentLogFoodBinding) {
        bind.editTextFoodDescriptionLog.requestFocus()

        // YAXSHILANGAN: Matn o'zgarganda tugma holatini yangilaymiz
        bind.editTextFoodDescriptionLog.addTextChangedListener {
            updateAnalyzeButtonVisibility()
        }

        bind.toolbarLogFood.setOnMenuItemClickListener {
            analyze()
            return@setOnMenuItemClickListener true
        }

        setupToolbar(bind)
        setupMealTypeChips(bind)
        setupTodaysFoodList(bind)
        setupClickListeners(bind)
        observeUiState(bind)
        observeNavigationEvents()
        setupBottomSheetResultListener()
        updateSelectedImageView(viewModel.selectedImageUri)
        updateAnalyzeButtonVisibility() // Fragment ochilganda tugma holatini tekshirish
    }

    private fun setupBottomSheetResultListener() {
        setFragmentResultListener(CalorieResultBottomSheet.REQUEST_KEY) { _, bundle ->
            val action = bundle.getString(CalorieResultBottomSheet.RESULT_ACTION)
            if (action == "retry" || action == "dismiss") {
                viewModel.cancelAnalysisOrResetState()
            } else {
                if (action == "success") {
                    findNavController().popBackStack()
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

        parentFragmentManager.setFragmentResultListener(
            VoiceRecorderBottomSheet.REQUEST_KEY, viewLifecycleOwner
        ) { _, bundle ->
            val transcribedText = bundle.getString(VoiceRecorderBottomSheet.RESULT_TEXT)
            if (!transcribedText.isNullOrBlank()) {
                val currentText = binding.editTextFoodDescriptionLog.text.toString()
                binding.editTextFoodDescriptionLog.setText(
                    if (currentText.isBlank()) transcribedText else "$currentText $transcribedText"
                )
                binding.editTextFoodDescriptionLog.setSelection(binding.editTextFoodDescriptionLog.text!!.length)
            }
        }
    }

    private fun observeUiState(bind: FragmentLogFoodBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (!isAdded) return@collectLatest

                val isLoading =
                    state is LogFoodUiState.Saving || state is LogFoodUiState.Analyzing || state is LogFoodUiState.Loading
                bind.progressAnalyze.isVisible = isLoading
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
                        val chipToSelect =
                            bind.chipGroupMealType.findViewWithTag<Chip>(state.mealType)
                        if (chipToSelect != null && !chipToSelect.isChecked) {
                            chipToSelect.isChecked = true
                        } else if (chipToSelect == null && bind.chipGroupMealType.checkedChipId == View.NO_ID) {
                            (bind.chipGroupMealType.getChildAt(0) as? Chip)?.isChecked = true
                        }
                    }

                    is LogFoodUiState.Editing -> {
                        populateFieldsForEditing(bind, state.log)
                        val chipToSelect =
                            bind.chipGroupMealType.findViewWithTag<Chip>(state.log.mealType)
                        if (chipToSelect != null && !chipToSelect.isChecked) {
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
                        findNavController().popBackStack()
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
        viewLifecycleOwner.lifecycleScope.launch {
            todaysFoodAdapter.loadStateFlow.collectLatest {
                if (it.refresh is androidx.paging.LoadState.NotLoading) {
                    bind.todaysFoodTitle.isVisible = todaysFoodAdapter.itemCount > 0
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.todaysFoodLogs.collectLatest { pagingData ->
                todaysFoodAdapter.submitData(pagingData)
            }
        }
    }

    private fun setupToolbar(bind: FragmentLogFoodBinding) {
        bind.toolbarLogFood.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    private fun setupMealTypeChips(bind: FragmentLogFoodBinding) {
        MealsType.mealTypes.forEach { mealType ->
            val chip = layoutInflater.inflate(
                R.layout.item_meal_type_selectable, bind.chipGroupMealType, false
            ) as Chip
            chip.text = mealType.name
            chip.tag = mealType.id
            chip.id = View.generateViewId()
            chip.isCheckable = true
            bind.chipGroupMealType.addView(chip)
        }
    }

    private fun setupTodaysFoodList(bind: FragmentLogFoodBinding) {
        todaysFoodAdapter = TodaysFoodAdapter { foodLog ->
            val bottomSheet = TodaysFoodDetailBottomSheet.newInstance(foodLog)
            bottomSheet.show(parentFragmentManager, TodaysFoodDetailBottomSheet.TAG)
        }
        bind.recyclerViewTodaysFood.layoutManager = LinearLayoutManager(requireContext())
        bind.recyclerViewTodaysFood.adapter = todaysFoodAdapter
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

        bind.buttonVoiceInput.setOnClickListener {
            checkAudioPermissionAndLaunchRecorder()
        }

        bind.buttonAnalyzeAndSave.setOnClickListener {
            analyze()
        }
    }

    private fun analyze() {
        currentDescriptionFromInput = binding.editTextFoodDescriptionLog.text.toString()
        val imageUriToAnalyze = viewModel.selectedImageUri ?: tempImageUri
        viewModel.selectedImageUri = imageUriToAnalyze
        val selectedChipId = binding.chipGroupMealType.checkedChipId
        val mealTypeTag =
            binding.chipGroupMealType.findViewById<Chip>(selectedChipId)?.tag?.toString()
                ?: MealsType.Snack.id

        if (viewModel.logToEdit != null) {
            viewModel.saveEditedLog(currentDescriptionFromInput)
        } else {
            viewModel.analyzeAndPrepareLog(currentDescriptionFromInput, mealTypeTag)
        }
        hideSoftInput(requireActivity())
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> launchCamera()

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showToast(getString(R.string.permission_denied_camera))
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }

            else -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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

    private fun checkAudioPermissionAndLaunchRecorder() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> showVoiceRecorderSheet()

            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                showToast(getString(R.string.permission_denied_audio))
                requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }

            else -> requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun showVoiceRecorderSheet() {
        if (isAdded && findNavController().currentDestination?.id == R.id.logFoodFragment) {
            findNavController().navigate(R.id.action_logFoodFragment_to_voiceRecorderBottomSheet)
        }
    }

    private val bottomSheet: BottomSheetDialog by lazy { BottomSheetDialog(requireContext()) }

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
                it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
        bottomSheet.setContentView(dialogBinding.root)
        bottomSheet.show()
    }

    private fun observeNavigationEvents() {
        viewModel.navigationEvent.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { navigationEvent ->
                when (navigationEvent) {
                    is LogFoodNavigationEvent.ShowToast -> showToast(navigationEvent.message)
                    is LogFoodNavigationEvent.RefreshList -> todaysFoodAdapter.refresh()
                }
            }
        }
    }

    private fun populateFieldsForEditing(
        bind: FragmentLogFoodBinding, log: uz.oybek.ozamiz.data.info.FoodLog
    ) {
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
}