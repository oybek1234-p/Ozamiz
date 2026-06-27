package uz.oybek.ozamiz.ui.feature.log.food

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.analyzer.model.FoodAnalysis
import uz.oybek.ozamiz.data.food.model.FoodDatabase
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.databinding.FoodAnalyzingLoaderBinding
import uz.oybek.ozamiz.databinding.FragmentLogFoodBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.home.HomeViewModel
import uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import uz.oybek.ozamiz.ui.feature.premium.isPremiumOrTrial
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.showToast
import java.util.Date

class LogFoodFragment : BaseFragment<FragmentLogFoodBinding>() {

    private val viewModel: LogFoodViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()

    private var meal: String = ""
    private var logDate: Date = Date()
    private var loadingSheet: BottomSheetDialog? = null
    private var loadingShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val argument = getArgument(it)
            logDate = Date(argument.timestamp)
            meal = argument.meal
            if (argument.openCamera) {
                checkOnlyPremium {
                    launchCamera()
                }
            }
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentLogFoodBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentLogFoodBinding) {
        setupUI(bind)
        observeData()
        initTextInput()
        setMeal(meal)
        viewModel.updateMeal(meal)
        initTodayLogs(logDate)
        setupPremiumListener()
        initFoodCamera()
        checkAppOnboardingAutoPrompt()
    }

    private fun checkAppOnboardingAutoPrompt() {
        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        if (prefs.getInt("app_onboarding_step", 0) == 1) {
            TextInputBottomSheet.newInstance(
                title = "Bugun nonushtada nima yeganingizni kiriting",
                placeholder = "Masalan: 1 ta olma"
            ).show(parentFragmentManager, TextInputBottomSheet.TAG)
        }
    }

    override fun onDestroyView() {
        loadingSheet?.dismiss()
        loadingSheet = null
        super.onDestroyView()
    }

    private fun initFoodCamera() {
        setFragmentResultListener(FoodCameraFragment.REQUEST_KEY) { _, bundle ->
            when (bundle.getString(FoodCameraFragment.RESULT_ACTION)) {
                FoodCameraFragment.ACTION_PHOTO -> {
                    bundle.getString(FoodCameraFragment.RESULT_URI)?.let { uriString ->
                        viewModel.analyzePhoto(image = Uri.parse(uriString))
                    }
                }

                FoodCameraFragment.ACTION_WRITE -> {
                    TextInputBottomSheet().show(parentFragmentManager, TextInputBottomSheet.TAG)
                }

                FoodCameraFragment.ACTION_LIST -> {
                    val bundleList = Bundle().apply {
                        putString("mealType", viewModel.state.value.meal)
                        putLong("timestamp", logDate.time)
                    }
                    findNavController().navigate(
                        R.id.action_logFoodFragment_to_searchFoodFragment,
                        bundleList
                    )
                }
            }
        }
    }

    private fun setupPremiumListener() {
        setFragmentResultListener(PremiumBottomSheetDialog.REQUEST_KEY) { _, bundle ->
            if (bundle.getString(PremiumBottomSheetDialog.RESULT_ACTION) == "go_to_payment") {
                val price = bundle.getLong("selected_price", 29000L)
                val isYearly = bundle.getBoolean("is_yearly", false)
                val args = Bundle().apply { 
                    putLong("price", price)
                    putBoolean("is_yearly", isYearly)
                }
                findNavController().navigate(R.id.paymentGetCheckFragment, args)
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

    private fun checkOnlyPremium(action: () -> Unit) {
        if (UserRepository.user.isPremium()) {
            action()
        } else {
            PremiumBottomSheetDialog().show(parentFragmentManager, PremiumBottomSheetDialog.TAG)
        }
    }

    private fun initTextInput() {
        setFragmentResultListener(TextInputBottomSheet.REQUEST_KEY) { _, bundle ->
            bundle.getString(TextInputBottomSheet.RESULT_TEXT)?.let { text ->
                viewModel.updateDescription(text)
                viewModel.analyze()
            }
        }
    }

    private fun setupUI(bind: FragmentLogFoodBinding) = with(bind) {
        toolbarLogFood.setNavigationOnClickListener { closeFragment() }
        toolbarLogFood.title = "Ovqat kiritish"

        historyButton.setOnSafeClickListener {
            arguments?.let {
                val argument = getArgument(it)
                navigate(R.id.action_logFoodFragment_to_dailyFoodLogsFragment, Bundle().apply {
                    putLong(TIMESTAMP, argument.timestamp)
                })
            }
        }

        if (chipGroupMealType.childCount == 0) {
            MealTime.mealTypes.forEach { type ->
                (layoutInflater.inflate(
                    R.layout.item_meal_type_selectable,
                    chipGroupMealType,
                    false
                ) as Chip).apply {
                    text = getString(type.nameRes)
                    tag = type.id
                    setOnClickListener { viewModel.updateMeal(meal = type.id) }
                    chipGroupMealType.addView(this)
                }
            }
        }

        buttonQuickCamera.setOnSafeClickListener { checkOnlyPremium { launchCamera() } }
        buttonShowTextInput.setOnSafeClickListener {
            checkPremium {
                TextInputBottomSheet().show(
                    parentFragmentManager,
                    TextInputBottomSheet.TAG
                )
            }
        }
        buttonSearchList.setOnSafeClickListener {
            checkPremium {
                findNavController().navigate(
                    R.id.action_logFoodFragment_to_searchFoodFragment,
                    Bundle().apply {
                        putString("mealType", viewModel.state.value.meal)
                        putLong("timestamp", logDate.time)
                    })
            }
        }

        if (chipGroupSuggestions.childCount == 0) {
            FoodDatabase.allFoods.take(10).forEach { food ->
                (layoutInflater.inflate(
                    R.layout.item_suggestion_chip,
                    chipGroupSuggestions,
                    false
                ) as Chip).apply {
                    text = food.name
                    setOnClickListener {
                        FoodSelectionBottomSheet(
                            foodItem = food,
                            mealType = viewModel.state.value.meal,
                            onLogAdded = { log ->
                                viewModel.logFood(log, logDate)
                                homeViewModel.onFoodAdded()
                                showToast("Qo'shildi!")
                            }
                        ).show(parentFragmentManager, FoodSelectionBottomSheet.TAG)
                    }
                    chipGroupSuggestions.addView(this)
                }
            }
        }
    }

    private fun initTodayLogs(date: Date) {
        val adapter = uz.oybek.ozamiz.ui.feature.log.food.history.RecentLogsAdapter { foodLog ->
            checkPremium {
                navigate(R.id.analyzeResultFragment, Bundle().apply {
                    putString("logId", foodLog.logId)
                    putLong("timestamp", date.time)
                })
            }
        }
        binding.recyclerRecentLogs.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    FoodLogRepository.getLatestLogs(date, 2).collectLatest { logs ->
                        binding.layoutRecentHistory.isVisible = logs.isNotEmpty()
                        adapter.submitList(logs)
                    }
                }
                launch {
                    viewModel.savedFoodIds.collectLatest { ids ->
                        adapter.setSavedFoodIds(ids)
                    }
                }
            }
        }
    }

    private fun showLoading(state: LogFoodState) {
        val show = state.isAnalyzing
        if (loadingShown == show) return
        loadingShown = show

        if (!show) {
            loadingSheet?.dismiss()
            loadingSheet = null
            return
        }

        val sheet = BottomSheetDialog(requireContext())
        val loadBinding = FoodAnalyzingLoaderBinding.inflate(layoutInflater).apply {
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
        sheet.setOnDismissListener { loadingShown = false }
        sheet.show()
    }

    private fun setMeal(mealType: String) {
        binding.chipGroupMealType.findViewWithTag<Chip>(mealType)?.let {
            if (!it.isChecked) it.isChecked = true
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collectLatest { showLoading(it) }
                }
                launch {
                    viewModel.event.collectLatest { event ->
                        when (event) {
                            LogEvent.Error.EmptyInput -> showToast(getString(R.string.log_input_empty))
                            is LogEvent.ClarificationNeeded -> {
                                val analysis = event.data
                                val question = analysis.result?.question
                                val options = analysis.result?.options ?: emptyList()
                                if (question != null) {
                                    ClarificationBottomSheet(
                                        question,
                                        options
                                    ) { viewModel.submitAnswer(it) }
                                        .show(parentFragmentManager, "clarification_sheet")
                                }
                            }

                            is LogEvent.Analyzed -> navigateToResult(event.data)
                            is LogEvent.Message -> showToast(event.text)
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun navigateToResult(result: FoodAnalysis) {
        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        if (prefs.getInt("app_onboarding_step", 0) == 1) {
            homeViewModel.onAppOnboardingComplete()
        }
        navigate(R.id.analyzeResultFragment, Bundle().apply { 
            putInt("id", result.id) 
            putLong("timestamp", logDate.time)
        })
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            findNavController().navigate(R.id.action_logFoodFragment_to_foodCameraFragment)
        } else {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                showSettingsDialog()
            } else {
                showToast("Rasmga olish uchun kamera ruxsati zarur")
            }
        }
    }

    private fun showSettingsDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Kamera ruxsati")
            .setMessage("Ilova ovqatni rasmga olishi uchun kamera ruxsati kerak. Sozlamalarga o'tib, unga ruxsat bering.")
            .setPositiveButton("Sozlamalar") { _, _ ->
                val intent =
                    android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .apply {
                            data = Uri.fromParts("package", requireContext().packageName, null)
                        }
                startActivity(intent)
            }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }

    private fun launchCamera() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            findNavController().navigate(R.id.action_logFoodFragment_to_foodCameraFragment)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    companion object {
        data class Argument(
            val timestamp: Long = System.currentTimeMillis(),
            val meal: String = MealTime.Breakfast.id,
            val openCamera: Boolean = false
        )

        const val TIMESTAMP = "timestamp"
        const val OPEN_CAMERA = "openCamera"

        fun getArgument(arguments: Bundle): Argument {
            return Argument(
                arguments.getLong(TIMESTAMP),
                arguments.getString("meal", MealTime.Breakfast.id),
                arguments.getBoolean(OPEN_CAMERA, false)
            )
        }

        fun navigate(
            navController: NavController,
            timeStamp: Long = System.currentTimeMillis(),
            meal: String = MealTime.Breakfast.id,
            openCamera: Boolean = false
        ) {
            navController.navigate(
                R.id.logFoodFragment,
                Bundle().apply {
                    putLong(TIMESTAMP, timeStamp)
                    putString("meal", meal)
                    putBoolean(OPEN_CAMERA, openCamera)
                },
                animNavOptions
            )
        }
    }
}