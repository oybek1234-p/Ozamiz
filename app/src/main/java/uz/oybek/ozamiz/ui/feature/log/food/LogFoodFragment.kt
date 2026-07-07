package uz.oybek.ozamiz.ui.feature.log.food

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
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
import uz.oybek.ozamiz.databinding.DialogLimitWarningBinding
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

    private lateinit var fragmentArgs: Argument
    private var logDate: Date = Date()

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openFoodCamera()
        } else if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showSettingsDialog()
        } else {
            showToast(getString(R.string.log_food_camera_permission_denied))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentArgs = arguments?.let { getArgument(it) } ?: Argument()
        logDate = Date(fragmentArgs.timestamp)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentLogFoodBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentLogFoodBinding) {
        setupUI(bind)
        setupFragmentResultListeners()
        observeUiState()

        viewModel.setDateAndMeal(logDate, fragmentArgs.meal)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = navigateToHome()
            }
        )

        if (fragmentArgs.openEditText) {
            showTextInputSheet()
        } else if (fragmentArgs.openCamera) {
            bind.root.post { checkPremiumAndRun { launchCamera() } }
        }
    }

    private fun setupUI(bind: FragmentLogFoodBinding) = with(bind) {
        toolbarLogFood.setNavigationOnClickListener { navigateToHome() }
        toolbarLogFood.title = getString(R.string.log_food_toolbar_title)

        val showPremiumBadges = !user.isPremium()
        badgeCameraPremium.isVisible = showPremiumBadges
        badgeTextPremium.isVisible = showPremiumBadges
        badgeVoicePremium.isVisible = showPremiumBadges

        textViewSearchListTitle.text = getString(
            R.string.log_food_search_list_count_format,
            getString(R.string.log_food_search_list_title)
        )

        setupMealChips()
        setupActionButtons()
        setupRecentLogsList()
    }

    private fun setupMealChips() = with(binding) {
        if (chipGroupMealType.isEmpty()) {
            MealTime.mealTypes.forEach { type ->
                (layoutInflater.inflate(
                    R.layout.item_meal_type_selectable,
                    chipGroupMealType,
                    false
                ) as Chip).apply {
                    text = getString(type.nameRes)
                    tag = type.id
                    setOnClickListener { viewModel.updateMeal(type.id) }
                    chipGroupMealType.addView(this)
                }
            }
        }
    }

    private fun setupActionButtons() = with(binding) {
        buttonQuickCamera.setOnSafeClickListener {
            checkPremiumAndRun { launchCamera() }
        }
        buttonShowTextInput.setOnSafeClickListener {
            checkPremiumAndRun { showTextInputSheet() }
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
            navigate(R.id.savedFoodsFragment, Bundle().apply {
                putString(ARG_MEAL_TYPE, currentMeal())
                putLong(ARG_TIMESTAMP, logDate.time)
            })
        }
        buttonSearchList.setOnSafeClickListener { navigateToSearchFood() }
    }

    private fun setupRecentLogsList() {
        binding.recyclerRecentLogs.adapter = RecentLogsAdapter(
            onItemClick = { foodLog ->
                navigate(R.id.analyzeResultFragment, Bundle().apply {
                    putString(AnalyzeResultFragment.KEY_LOG_ID, foodLog.logId)
                    putLong(AnalyzeResultFragment.KEY_TIMESTAMP, logDate.time)
                })
            },
            onDeleteClick = { foodLog -> showDeleteConfirmation(foodLog) },
            onLikeClick = { foodLog -> viewModel.toggleFavorite(foodLog) }
        )
    }

    private fun setupFragmentResultListeners() {
        setFragmentResultListener(FoodCameraFragment.REQUEST_KEY) { _, bundle ->
            when (bundle.getString(FoodCameraFragment.RESULT_ACTION)) {
                FoodCameraFragment.ACTION_PHOTO -> {
                    bundle.getString(FoodCameraFragment.RESULT_URI)?.let { uriString ->
                        navigateToResult(
                            imageUri = Uri.parse(uriString),
                            description = "",
                            meal = currentMeal()
                        )
                    }
                }

                FoodCameraFragment.ACTION_WRITE -> showTextInputSheet()

                FoodCameraFragment.ACTION_LIST -> navigateToSearchFood()
            }
        }

        setFragmentResultListener(TextInputBottomSheet.REQUEST_KEY) { _, bundle ->
            bundle.getString(TextInputBottomSheet.RESULT_TEXT)?.let { text ->
                checkPremiumAndRun {
                    viewModel.updateDescription(text)
                    navigateToResult(imageUri = null, description = text, meal = currentMeal())
                }
            }
        }

        setFragmentResultListener(VoiceRecorderBottomSheet.REQUEST_KEY) { _, bundle ->
            bundle.getString(VoiceRecorderBottomSheet.RESULT_URI)?.let { uriString ->
                checkPremiumAndRun {
                    navigateToResult(
                        imageUri = null,
                        description = "",
                        meal = currentMeal(),
                        audioUri = Uri.parse(uriString)
                    )
                }
            }
        }

        setFragmentResultListener(PremiumBottomSheetDialog.REQUEST_KEY) { _, bundle ->
            if (bundle.getString(PremiumBottomSheetDialog.RESULT_ACTION) ==
                PremiumBottomSheetDialog.ACTION_GO_TO_PAYMENT
            ) {
                val price = bundle.getLong(PremiumBottomSheetDialog.KEY_SELECTED_PRICE, 29000L)
                val isYearly = bundle.getBoolean(PremiumBottomSheetDialog.KEY_IS_YEARLY, false)
                findNavController().navigate(
                    R.id.paymentGetCheckFragment,
                    Bundle().apply {
                        putLong(PaymentGetCheckFragment.ARG_PRICE, price)
                        putBoolean(PaymentGetCheckFragment.ARG_IS_YEARLY, isYearly)
                    }
                )
            }
        }

        setFragmentResultListener(PaymentGetCheckFragment.REQUEST_KEY) { _, bundle ->
            if (bundle.getBoolean(PaymentGetCheckFragment.RESULT_SUBMITTED)) {
                PremiumBottomSheetDialog().show(
                    parentFragmentManager,
                    PremiumBottomSheetDialog.TAG
                )
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state
                        .map { it.meal }
                        .distinctUntilChanged()
                        .collect(::selectMealChip)
                }
                launch {
                    viewModel.state
                        .map { it.logs to it.savedFoodIds }
                        .distinctUntilChanged()
                        .collect { (logs, savedIds) ->
                            binding.layoutRecentHistory.isVisible = logs.isNotEmpty()
                            recentLogsAdapter()?.apply {
                                submitList(logs)
                                setSavedFoodIds(savedIds)
                            }
                        }
                }
                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            LogEvent.Error.EmptyInput ->
                                showToast(getString(R.string.log_input_empty))
                            is LogEvent.Message -> showToast(event.text)
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun recentLogsAdapter(): RecentLogsAdapter? =
        binding.recyclerRecentLogs.adapter as? RecentLogsAdapter

    private fun currentMeal(): String = viewModel.state.value.meal

    private fun navigateToHome() {
        findNavController().popBackStack(R.id.home, false)
    }

    private fun navigateToSearchFood() {
        SearchFoodFragment.navigate(findNavController(), currentMeal(), logDate.time)
    }

    private fun showTextInputSheet() {
        TextInputBottomSheet.newInstance(
            title = getString(R.string.log_food_text_title),
            placeholder = getString(R.string.food_description_hint)
        ).show(parentFragmentManager, TextInputBottomSheet.TAG)
    }

    private fun checkPremiumAndRun(action: () -> Unit) {
        if (viewModel.canAddLog()) {
            action()
        } else {
            showLimitWarning()
        }
    }

    private fun showLimitWarning() {
        val dialogBinding = DialogLimitWarningBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedCornersDialog)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
        dialogBinding.btnPositive.setOnClickListener {
            dialog.dismiss()
            PremiumBottomSheetDialog().show(parentFragmentManager, PremiumBottomSheetDialog.TAG)
        }
        dialog.show()
    }

    private fun showDeleteConfirmation(foodLog: FoodLog) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_message_generic, foodLog.description))
            .setPositiveButton(R.string.yes_delete) { _, _ ->
                viewModel.deleteFoodLog(foodLog, logDate)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun selectMealChip(mealType: String) {
        binding.chipGroupMealType.findViewWithTag<Chip>(mealType)?.let { chip ->
            if (!chip.isChecked) chip.isChecked = true
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

    private fun showSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.log_food_camera_permission_title)
            .setMessage(R.string.log_food_camera_permission_msg)
            .setPositiveButton(R.string.log_food_camera_permission_settings) { _, _ ->
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", requireContext().packageName, null)
                    }
                )
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun launchCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            openFoodCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openFoodCamera() {
        findNavController().navigate(R.id.action_logFoodFragment_to_foodCameraFragment)
    }

    companion object {
        const val ARG_MEAL_TYPE = "mealType"
        const val ARG_TIMESTAMP = "timestamp"
        const val TIMESTAMP = "timestamp"
        const val MEAL = "meal"
        const val OPEN_CAMERA = "openCamera"
        const val OPEN_EDIT_TEXT = "openEditText"

        data class Argument(
            val timestamp: Long = System.currentTimeMillis(),
            val meal: String = MealTime.Breakfast.id,
            val openCamera: Boolean = false,
            val openEditText: Boolean = false
        )

        fun getArgument(arguments: Bundle): Argument {
            return Argument(
                timestamp = arguments.getLong(TIMESTAMP),
                meal = arguments.getString(MEAL, MealTime.Breakfast.id),
                openCamera = arguments.getBoolean(OPEN_CAMERA, false),
                openEditText = arguments.getBoolean(OPEN_EDIT_TEXT, false)
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
