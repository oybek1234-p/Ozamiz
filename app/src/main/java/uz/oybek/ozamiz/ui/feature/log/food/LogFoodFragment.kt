package uz.oybek.ozamiz.ui.feature.log.food

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest

import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.analyzer.model.FoodAnalysis
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.databinding.FoodAnalyzingLoaderBinding
import uz.oybek.ozamiz.databinding.FragmentLogFoodBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.log.voice.VoiceRecorderBottomSheet
import uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog
import uz.oybek.ozamiz.ui.feature.premium.isPremiumOrTrial
import uz.oybek.ozamiz.ui.utils.hideSoftInput
import uz.oybek.ozamiz.ui.utils.showToast
import java.io.File
import java.util.Date

class LogFoodFragment : BaseFragment<FragmentLogFoodBinding>() {

    private val viewModel: LogFoodViewModel by viewModels()

    private var imageFileUri: Uri? = null

    private val camera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) {
        if (it) {
            if (imageFileUri != null) {
                viewModel.analyzePhoto(image = imageFileUri!!)
            }
        }
    }

    private val permissionCameraResult =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            if (it) launchCamera()
            else showToast(
                getString(R.string.permission_denied_camera)
            )
        }

    private var meal: String = ""
    private var logDate: Date = Date()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val argument = getArgument(it)
            logDate = Date(argument.timestamp)
            meal = argument.meal
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentLogFoodBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentLogFoodBinding) {
        setupUI(bind)
        observeData(bind)
        initVoiceRecorder()
        setMeal(meal)
        viewModel.updateMeal(meal)
        initTodayLogs(logDate)
        viewModel.updateMeal(meal)
        initTodayLogs(logDate)
        setupPremiumListener()
    }

    private fun setupPremiumListener() {
        setFragmentResultListener(PremiumBottomSheetDialog.REQUEST_KEY) { _, bundle ->
            val result = bundle.getString(PremiumBottomSheetDialog.RESULT_ACTION)
            if (result == "go_to_payment") {
                findNavController().navigate(R.id.paymentGetCheckFragment)
            }
        }
        setFragmentResultListener("payment_check_submitted") { _, bundle ->
            if (bundle.getBoolean("submitted")) {
                PremiumBottomSheetDialog().show(parentFragmentManager, PremiumBottomSheetDialog.TAG)
            }
        }
    }

    private fun checkPremium(action: () -> Unit) {
        if (UserRepository.user.isPremiumOrTrial()) {
            action()
        } else {
            PremiumBottomSheetDialog().show(parentFragmentManager, PremiumBottomSheetDialog.TAG)
        }
    }

    private fun initVoiceRecorder() {
        setFragmentResultListener(
            VoiceRecorderBottomSheet.REQUEST_KEY
        ) { _, bundle ->
            bundle.getString(VoiceRecorderBottomSheet.RESULT_TEXT)
                ?.let {
                    val currentText = viewModel.state.value.description
                    viewModel.updateDescription(
                        text = "$currentText $it".trim()
                    )
                    viewModel.analyze()
                }
        }
    }

    private fun setupUI(bind: FragmentLogFoodBinding) =
        with(bind) {
            toolbarLogFood.setNavigationOnClickListener {
                closeFragment()
            }
            historyButton.setOnClickListener {
                arguments?.let {
                    val argument = getArgument(it)
                    navigate(
                        R.id.action_logFoodFragment_to_dailyFoodLogsFragment,
                        Bundle().apply {
                            putLong(TIMESTAMP, argument.timestamp)
                        }
                    )
                }
            }
            MealTime.mealTypes.forEach { type ->
                (layoutInflater.inflate(
                    R.layout.item_meal_type_selectable,
                    chipGroupMealType,
                    false
                ) as Chip).apply {
                    text = getString(type.nameRes)
                    tag = type.id
                    setOnClickListener {
                        viewModel.updateMeal(meal = type.id)
                    }
                    chipGroupMealType.addView(this)
                }
            }
            textInputButton.setOnClickListener {
                checkPremium { showTextInputSheet() }
            }
            voiceInputButton.setOnClickListener {
                checkPremium { findNavController().navigate(R.id.voiceInputBottomSheet) }
            }
            uploadImageButton.setOnClickListener {
                checkPremium { checkAndLaunchCamera() }
            }
        }

    private var textInputSheet: BottomSheetDialog? = null
    private fun showTextInputSheet() {
        val sheet = BottomSheetDialog(requireContext())
        val sheetBinding =
            uz.oybek.ozamiz.databinding.BottomSheetLogFoodTextInputBinding.inflate(layoutInflater)

        sheet.setContentView(sheetBinding.root)

        sheetBinding.inputDescription.setText(viewModel.state.value.description)
        sheetBinding.inputDescription.addTextChangedListener {
            viewModel.updateDescription(text = it.toString())
        }
        sheetBinding.buttonAnalyzeAndSave.setOnClickListener {
            hideSoftInput(requireActivity())
            viewModel.analyze()
            sheet.dismiss()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest {
                if (sheetBinding.inputDescription.text.toString() != it.description) {
                    sheetBinding.inputDescription.setText(it.description)
                }
            }
        }

        sheet.show()
        sheetBinding.inputDescription.requestFocus()
        // Wait for dialog to show before showing keyboard
        sheetBinding.inputDescription.postDelayed({
            val imm =
                requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(
                sheetBinding.inputDescription,
                android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
            )
        }, 200)

        textInputSheet = sheet
    }

    private fun initTodayLogs(date: Date) {
        val adapter = uz.oybek.ozamiz.ui.feature.log.food.history.RecentLogsAdapter { foodLog ->
            navigate(
                R.id.calorieResultBottomSheet,
                Bundle().apply {
                    putString("logId", foodLog.logId)
                    putLong("timestamp", date.time)
                }
            )
        }
        binding.recyclerRecentLogs.adapter = adapter
        
        viewLifecycleOwner.lifecycleScope.launch {
            FoodLogRepository.getLatestLogs(date, 2)
                .collectLatest { logs ->
                    val hasLogs = logs.isNotEmpty()
                    binding.layoutRecentHistory.isVisible = hasLogs
                    adapter.submitList(logs)
                }
        }
    }

    private fun checkAndLaunchCamera() {
        if (requireContext().checkSelfPermission(Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        )
            launchCamera()
        else permissionCameraResult.launch(Manifest.permission.CAMERA)
    }

    private var loadingShown = false
    private var loadingSheet: BottomSheetDialog? = null

    private fun showLoading(state: LogFoodState) {
        val show = state.isAnalyzing
        if (loadingShown == show) return
        loadingShown = show
        if (show.not()) {
            loadingSheet?.dismiss()
            loadingSheet = null
            return
        }
        val sheet = BottomSheetDialog(requireContext())
        val loadBinding = FoodAnalyzingLoaderBinding.inflate(
            layoutInflater
        ).apply {
            backView.setOnClickListener {
                viewModel.cancelAnalyze()
                sheet.dismiss()
            }
            if (state.image != null) {
                imageRequest.isVisible = true
                textRequest.isVisible = false
                imageRequest.setImageURI(state.image)
            } else if (state.description.isNotBlank()) {
                imageRequest.isVisible = false
                textRequest.isVisible = true
                textRequest.text = state.description
            }
        }
        loadingSheet = sheet
        sheet.setContentView(loadBinding.root)
        sheet.show()
    }

    private fun setMeal(
        mealType: String
    ) {
        binding.chipGroupMealType.findViewWithTag<Chip>(mealType)
            ?.let {
                if (!it.isChecked)
                    it.isChecked = true
            }
    }

    private fun observeData(bind: FragmentLogFoodBinding) =
        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                viewModel.state.collectLatest { state ->
                    showLoading(state)
                }
            }
            launch {
                viewModel.event.collectLatest {
                    when (it) {
                        LogEvent.Error.AnalyzeError -> {

                        }

                        LogEvent.Error.EmptyInput -> {
                            showToast(getString(R.string.log_input_empty))
                        }

                        is LogEvent.ClarificationNeeded -> {
                            val analysis = it.data
                            val question = analysis.result?.question
                            val options = analysis.result?.options ?: emptyList()
                            
                            if (question != null) {
                                ClarificationBottomSheet(
                                    question = question,
                                    options = options
                                ) { selectedOption ->
                                    viewModel.submitAnswer(selectedOption)
                                }.show(parentFragmentManager, "clarification_sheet")
                            }
                        }

                        is LogEvent.Analyzed -> {
                            navigateToResult(it.data)
                        }
                    }
                }
            }
        }

    private fun navigateToResult(result: FoodAnalysis) {
        navigate(
            R.id.calorieResultBottomSheet,
            Bundle().apply {
                putInt("id", result.id)
            }
        )

    }

    private fun launchCamera() {
        runCatching {
            val file = File.createTempFile(
                "JPEG_${System.currentTimeMillis()}_",
                ".jpg",
                requireContext().externalCacheDir
            )
            imageFileUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )
            camera.launch(imageFileUri)
        }.onFailure {
            showToast(getString(R.string.error_opening_camera))
        }
    }

    companion object {
        data class Argument(
            val timestamp: Long = System.currentTimeMillis(),
            val meal: String = MealTime.Breakfast.id
        )

        const val TIMESTAMP = "timestamp"

        fun getArgument(arguments: Bundle): Argument {
            return Argument(
                arguments.getLong(TIMESTAMP),
                arguments.getString("meal", MealTime.Breakfast.id)
            )
        }

        fun navigate(
            navController: NavController,
            timeStamp: Long = System.currentTimeMillis(),
            meal: String = MealTime.Breakfast.id
        ) {
            navController.navigate(
                R.id.logFoodFragment,
                Bundle().apply {
                    putLong(TIMESTAMP, timeStamp)
                    putString("meal", meal)
                },
                animNavOptions
            )
        }
    }
}