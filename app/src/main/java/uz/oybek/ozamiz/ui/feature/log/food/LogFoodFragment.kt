package uz.oybek.ozamiz.ui.feature.log.food

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.databinding.FragmentLogFoodBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.log.food.history.RecentLogsAdapter
import uz.oybek.ozamiz.ui.feature.log.voice.VoiceRecorderBottomSheet
import uz.oybek.ozamiz.ui.feature.premium.PaymentGetCheckFragment
import uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.showToast
import java.util.Date

class LogFoodFragment : BaseFragment<FragmentLogFoodBinding>() {

    private val viewModel: LogFoodViewModel by viewModels()

    private var meal: String = ""
    private var logDate: Date = Date()
    private var recentLogsAdapter: RecentLogsAdapter? = null

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
        observeUiState()
        initTextInput()
        initVoiceInput()
        viewModel.setDateAndMeal(logDate, meal)
        setupPremiumListener()
        initFoodCamera()

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateToHome()
                }
            }
        )

        arguments?.let {
            val argument = getArgument(it)
            if (argument.openEditText) {
                TextInputBottomSheet().show(parentFragmentManager, TextInputBottomSheet.TAG)
            }
        }
    }

    private fun navigateToHome() {
        findNavController().popBackStack(R.id.home, false)
    }

    override fun onDestroyView() {
        recentLogsAdapter = null
        super.onDestroyView()
    }

    private fun initFoodCamera() {
        setFragmentResultListener(FoodCameraFragment.REQUEST_KEY) { _, bundle ->
            when (bundle.getString(FoodCameraFragment.RESULT_ACTION)) {
                FoodCameraFragment.ACTION_PHOTO -> {
                    bundle.getString(FoodCameraFragment.RESULT_URI)?.let { uriString ->
                        navigateToResult(
                            imageUri = Uri.parse(uriString),
                            description = "",
                            meal = viewModel.state.value.meal
                        )
                    }
                }

                FoodCameraFragment.ACTION_WRITE -> {
                    TextInputBottomSheet().show(parentFragmentManager, TextInputBottomSheet.TAG)
                }

                FoodCameraFragment.ACTION_LIST -> {
                    SearchFoodFragment.navigate(
                        findNavController(),
                        viewModel.state.value.meal,
                        logDate.time
                    )
                }
            }
        }
    }

    private fun setupPremiumListener() {
        setFragmentResultListener(PremiumBottomSheetDialog.REQUEST_KEY) { _, bundle ->
            if (bundle.getString(PremiumBottomSheetDialog.RESULT_ACTION) == PremiumBottomSheetDialog.ACTION_GO_TO_PAYMENT) {
                val price = bundle.getLong(PremiumBottomSheetDialog.KEY_SELECTED_PRICE, 29000L)
                val isYearly = bundle.getBoolean(PremiumBottomSheetDialog.KEY_IS_YEARLY, false)
                val args = Bundle().apply {
                    putLong(PaymentGetCheckFragment.ARG_PRICE, price)
                    putBoolean(PaymentGetCheckFragment.ARG_IS_YEARLY, isYearly)
                }
                findNavController().navigate(R.id.paymentGetCheckFragment, args)
            }
        }
        setFragmentResultListener(PaymentGetCheckFragment.REQUEST_KEY) { _, bundle ->
            if (bundle.getBoolean(PaymentGetCheckFragment.RESULT_SUBMITTED)) {
                PremiumBottomSheetDialog().show(parentFragmentManager, PremiumBottomSheetDialog.TAG)
            }
        }
    }

    private fun showPremiumSheet() {
        PremiumBottomSheetDialog().show(parentFragmentManager, PremiumBottomSheetDialog.TAG)
    }

    private fun checkPremiumAndRun(
        action: () -> Unit
    ) {
        lifecycleScope.launch {
            if (viewModel.canAddLog()) {
                action()
            } else {
                showPremiumSheet()
            }
        }
    }

    private fun initTextInput() {
        setFragmentResultListener(TextInputBottomSheet.REQUEST_KEY) { _, bundle ->
            bundle.getString(TextInputBottomSheet.RESULT_TEXT)?.let { text ->
                checkPremiumAndRun {
                    viewModel.updateDescription(text)
                    navigateToResult(
                        imageUri = null,
                        description = text,
                        meal = viewModel.state.value.meal
                    )
                }
            }
        }
    }

    private fun initVoiceInput() {
        setFragmentResultListener(VoiceRecorderBottomSheet.REQUEST_KEY) { _, bundle ->
            bundle.getString(VoiceRecorderBottomSheet.RESULT_URI)?.let { uriString ->
                checkPremiumAndRun {
                    navigateToResult(
                        imageUri = null,
                        description = "",
                        meal = viewModel.state.value.meal,
                        audioUri = Uri.parse(uriString)
                    )
                }
            }
        }
    }

    private fun setupUI(bind: FragmentLogFoodBinding) = with(bind) {
        toolbarLogFood.setNavigationOnClickListener { navigateToHome() }
        toolbarLogFood.title = getString(R.string.log_food_toolbar_title)

        val showPremiumBadges = user.isPremium().not()
        badgeCameraPremium.isVisible = showPremiumBadges
        badgeTextPremium.isVisible = showPremiumBadges
        badgeVoicePremium.isVisible = showPremiumBadges

        textViewSearchListTitle.text = getString(
            R.string.log_food_search_list_count_format,
            getString(R.string.log_food_search_list_title)
        )

        if (chipGroupMealType.isEmpty()) {
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
        buttonQuickCamera.setOnSafeClickListener {
            checkPremiumAndRun { launchCamera() }
        }
        buttonShowTextInput.setOnSafeClickListener {
            checkPremiumAndRun {
                TextInputBottomSheet.newInstance(
                    title = getString(R.string.log_food_text_title),
                    placeholder = getString(R.string.food_description_hint)
                ).show(
                    parentFragmentManager,
                    TextInputBottomSheet.TAG
                )
            }
        }
        buttonVoiceInput.setOnSafeClickListener {
            checkPremiumAndRun {
                VoiceRecorderBottomSheet().show(
                    parentFragmentManager,
                    VoiceRecorderBottomSheet.TAG
                )
            }
        }
        buttonSavedFoods.setOnSafeClickListener {
            val bundle = Bundle().apply {
                putString("mealType", viewModel.state.value.meal)
                putLong("timestamp", logDate.time)
            }
            navigate(R.id.savedFoodsFragment, bundle)
        }
        buttonSearchList.setOnSafeClickListener {
            SearchFoodFragment.navigate(
                findNavController(),
                viewModel.state.value.meal,
                logDate.time
            )
        }
        recentLogsAdapter = RecentLogsAdapter({ foodLog ->
            navigate(R.id.analyzeResultFragment, Bundle().apply {
                putString(AnalyzeResultFragment.KEY_LOG_ID, foodLog.logId)
                putLong(AnalyzeResultFragment.KEY_TIMESTAMP, logDate.time)
            })
        }, { foodLog ->
            showDeleteConfirmation(foodLog, logDate)
        }, { foodLog ->
            viewModel.toggleFavorite(foodLog)
        })
        recyclerRecentLogs.adapter = recentLogsAdapter
    }


    private fun showDeleteConfirmation(foodLog: FoodLog, date: Date) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_message_generic, foodLog.description))
            .setPositiveButton(R.string.yes_delete) { _, _ ->
                viewModel.deleteFoodLog(foodLog, date)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun setMeal(mealType: String) {
        binding.chipGroupMealType.findViewWithTag<Chip>(mealType)?.let {
            if (!it.isChecked) it.isChecked = true
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.map { it.meal }.distinctUntilChanged().collect { mealType ->
                        setMeal(mealType)
                    }
                }
                launch {
                    viewModel.state.map { it.logs to it.savedFoodIds }.distinctUntilChanged()
                        .collect { (logs, savedIds) ->
                            binding.layoutRecentHistory.isVisible = logs.isNotEmpty()
                            recentLogsAdapter?.submitList(logs)
                            recentLogsAdapter?.setSavedFoodIds(savedIds)
                        }
                }
                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            LogEvent.Error.EmptyInput -> showToast(getString(R.string.log_input_empty))
                            is LogEvent.Analyzed -> {} // Handled by immediate navigation
                            is LogEvent.Message -> showToast(event.text)
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun navigateToResult(
        imageUri: Uri?,
        description: String,
        meal: String,
        audioUri: Uri? = null
    ) {
        navigate(R.id.analyzeResultFragment, Bundle().apply {
            imageUri?.let { putString(AnalyzeResultFragment.KEY_IMAGE_URI, it.toString()) }
            audioUri?.let { putString(AnalyzeResultFragment.KEY_AUDIO_URI, it.toString()) }
            putString(AnalyzeResultFragment.KEY_PENDING_DESCRIPTION, description)
            putString(AnalyzeResultFragment.KEY_MEAL_TYPE, meal)
            putLong(AnalyzeResultFragment.KEY_TIMESTAMP, logDate.time)
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
                showToast(getString(R.string.log_food_camera_permission_denied))
            }
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.log_food_camera_permission_title)
            .setMessage(R.string.log_food_camera_permission_msg)
            .setPositiveButton(R.string.log_food_camera_permission_settings) { _, _ ->
                val intent =
                    android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .apply {
                            data = Uri.fromParts("package", requireContext().packageName, null)
                        }
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel), null)
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
            val openCamera: Boolean = false,
            val openEditText: Boolean = false
        )

        const val TIMESTAMP = "timestamp"
        const val MEAL = "meal"
        const val OPEN_CAMERA = "openCamera"
        const val OPEN_EDIT_TEXT = "openEditText"

        fun getArgument(arguments: Bundle): Argument {
            return Argument(
                arguments.getLong(TIMESTAMP),
                arguments.getString(MEAL, MealTime.Breakfast.id),
                arguments.getBoolean(OPEN_CAMERA, false),
                arguments.getBoolean(OPEN_EDIT_TEXT, false)
            )
        }

        fun navigate(
            navController: NavController,
            timeStamp: Long = System.currentTimeMillis(),
            meal: String = MealTime.Breakfast.id,
            openCamera: Boolean = false,
            openEditText: Boolean = false
        ) {
            navController.navigate(
                R.id.logFoodFragment,
                Bundle().apply {
                    putLong(TIMESTAMP, timeStamp)
                    putString(MEAL, meal)
                    putBoolean(OPEN_CAMERA, openCamera)
                    putBoolean(OPEN_EDIT_TEXT, openEditText)
                },
                animNavOptions
            )
        }
    }
}