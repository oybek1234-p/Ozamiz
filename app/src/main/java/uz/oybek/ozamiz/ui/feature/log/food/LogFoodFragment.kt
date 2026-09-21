package uz.oybek.ozamiz.ui.feature.log.food

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
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
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ads.AdsConfig
import uz.oybek.ozamiz.data.ads.AdsManager
import uz.oybek.ozamiz.data.config.AppPreference
import uz.oybek.ozamiz.data.food.limit.FoodLimitManager
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.databinding.FragmentLogFoodBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.log.food.history.RecentLogsAdapter
import uz.oybek.ozamiz.ui.feature.log.voice.VoiceRecorderBottomSheet
import uz.oybek.ozamiz.ui.feature.premium.PaymentGetCheckFragment
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.showToast
import java.util.Date
import kotlin.math.absoluteValue

class LogFoodFragment : BaseFragment<FragmentLogFoodBinding>() {

    private val viewModel: LogFoodViewModel by viewModels()
    private lateinit var fragmentArgs: Argument
    private var logDate: Date = Date()

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isAdded || isDetached || context == null) return@registerForActivityResult
        if (isGranted) {
            openFoodCamera()
        } else if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showSettingsDialog()
        } else {
            context?.let { ctx ->
                showToast(ctx.getString(R.string.log_food_camera_permission_denied))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            getArgument(it).let {
                fragmentArgs = it
            }
        }
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
            bind.root.post { checkPremiumAndRun { showTextInputSheet() } }
        } else if (fragmentArgs.openCamera) {
            bind.root.post { checkPremiumAndRun { launchCamera() } }
        }
    }

    private fun setupUI(bind: FragmentLogFoodBinding) = with(bind) {
        toolbarLogFood.setNavigationOnClickListener { navigateToHome() }
        val meal = MealTime.fromId(fragmentArgs.meal)
        val mealName = meal?.let { getString(it.nameRes) } ?: ""
        toolbarLogFood.title = mealName

        loadBanner()
        setupActionButtons()
        setupRecentLogsList()
    }

    private fun loadBanner() {
        if (user.isPremium()) {
            binding.bannerAdContainerLogFood.isVisible = false
            return
        }
        try {
            val bannerView = AdsManager.createBannerView(requireContext(), AdsConfig.BANNER_LOG_FOOD)
            binding.bannerAdContainerLogFood.removeAllViews()
            binding.bannerAdContainerLogFood.addView(bannerView)
            binding.bannerAdContainerLogFood.isVisible = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun setupActionButtons() = with(binding) {
        buttonAddFood.setOnSafeClickListener {
            checkPremiumAndRun { launchCamera() }
        }
        buttonMealRecommendation.setOnSafeClickListener {
            val mealId = currentMeal()
            val destination = when (mealId) {
                MealTime.Breakfast.id -> R.id.breakfastGuideFragment
                MealTime.Lunch.id -> R.id.lunchGuideFragment
                MealTime.Dinner.id -> R.id.dinnerGuideFragment
                MealTime.Snack.id -> R.id.snacksGuideFragment
                else -> R.id.healthRecommendationsFragment
            }
            navigate(destination)
        }
        buttonReadyMenus.setOnSafeClickListener {
            navigate(R.id.menuListFragment)
        }
        buttonSavedFoods.setOnSafeClickListener {
            checkPremiumAndRun {
                navigate(R.id.savedFoodsFragment, Bundle().apply {
                    putString(ARG_MEAL_TYPE, currentMeal())
                    putLong(ARG_TIMESTAMP, logDate.time)
                })
            }
        }
    }

    private fun setupRecentLogsList() {
        binding.recyclerRecentLogs.adapter = RecentLogsAdapter(
            onItemClick = { foodLog ->
                navigate(R.id.analyzeResultFragment, Bundle().apply {
                    putString(AnalyzeResultFragment.KEY_LOG_ID, foodLog.logId)
                    putLong(AnalyzeResultFragment.KEY_TIMESTAMP, logDate.time)
                })
            },
            onAddClick = { foodLog ->
                checkPremiumAndRun {
                    viewModel.relogFood(foodLog)
                    context?.let { ctx ->
                        showToast(ctx.getString(R.string.food_added_success, foodLog.description))
                    }
                }
            },
            onDeleteClick = { foodLog ->
                val ctx = context ?: return@RecentLogsAdapter
                AlertDialog.Builder(ctx)
                    .setMessage("Ushbu ovqatni o'chirmoqchimisiz?")
                    .setPositiveButton("O'chirish") { _, _ ->
                        viewModel.deleteFoodLog(foodLog, logDate)
                    }
                    .setNegativeButton("Bekor qilish", null)
                    .show()
            }
        )
    }

    private fun setupFragmentResultListeners() {
        childFragmentManager.setFragmentResultListener(
            "add_food_selection",
            viewLifecycleOwner
        ) { _, bundle ->
            val action = bundle.getString("action")
            when (action) {
                "camera" -> launchCamera()
                "write" -> showTextInputSheet()
                "search" -> navigateToSearchFood()
                "saved" -> navigate(R.id.savedFoodsFragment, Bundle().apply {
                    putString(ARG_MEAL_TYPE, currentMeal())
                    putLong(ARG_TIMESTAMP, logDate.time)
                })
            }
        }

        setFragmentResultListener(TextInputBottomSheet.REQUEST_KEY) { _, bundle ->
            bundle.getString(TextInputBottomSheet.RESULT_TEXT)?.let { text ->
                viewModel.updateDescription(text)
                navigateToResult(imageUri = null, description = text, meal = currentMeal())
            }
        }

        setFragmentResultListener(VoiceRecorderBottomSheet.REQUEST_KEY) { _, bundle ->
            bundle.getString(VoiceRecorderBottomSheet.RESULT_URI)?.let { uriString ->
                navigateToResult(
                    imageUri = null,
                    description = "",
                    meal = currentMeal(),
                    audioUri = Uri.parse(uriString)
                )
            }
        }

    }
    
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state
                        .map { it.logs to it.meal }
                        .distinctUntilChanged()
                        .collect { (logs, mealId) ->
                            binding.recyclerRecentLogs.isVisible = logs.isNotEmpty()
                            binding.layoutEmptyHistory.isVisible = logs.isEmpty()
                            (binding.recyclerRecentLogs.adapter as? RecentLogsAdapter)?.apply {
                                submitList(logs)
                            }
                            updateRecommendationInfo(mealId)
                        }
                }
                launch {
                    viewModel.state
                        .map { it.savedFoodIds }
                        .distinctUntilChanged()
                        .collect { savedIds ->
                            (binding.recyclerRecentLogs.adapter as? RecentLogsAdapter)?.setSavedFoodIds(savedIds)
                        }
                }
            }
        }
    }

    private fun updateRecommendationInfo(mealId: String) {
        val meal = MealTime.fromId(mealId)
        val mealName = meal?.let { getString(it.nameRes) } ?: ""
        
        binding.textMealRecommendationInfo.text = getString(R.string.meal_recommendation_title_format, mealName)
    }

    private fun currentMeal(): String = viewModel.state.value.meal

    private fun navigateToHome() {
        popBackStack(R.id.home, false)
    }

    private fun navigateToSearchFood() {
        navigate(
            R.id.searchFoodFragment,
            bundleOf(
                SearchFoodFragment.ARG_MEAL_TYPE to currentMeal(),
                SearchFoodFragment.ARG_TIMESTAMP to logDate.time
            )
        )
    }

    private fun showTextInputSheet() {
        val bundle = bundleOf(
            "mealType" to currentMeal(),
            "timestamp" to logDate.time
        )
        navigate(R.id.writeFoodFragment, bundle)
    }

    private fun checkPremiumAndRun(action: () -> Unit) {
        checkLimitAndProceed {
            action()
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
        val ctx = context ?: return
        AlertDialog.Builder(ctx)
            .setTitle(R.string.log_food_camera_permission_title)
            .setMessage(R.string.log_food_camera_permission_msg)
            .setPositiveButton(R.string.log_food_camera_permission_settings) { _, _ ->
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", ctx.packageName, null)
                    }
                )
            }
            .setNegativeButton(ctx.getString(R.string.cancel), null)
            .show()
    }

    private fun launchCamera() {
        val ctx = context ?: return
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            openFoodCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openFoodCamera() {
        val bundle = bundleOf(
            "mealType" to currentMeal(),
            "meal" to currentMeal(),
            "timestamp" to logDate.time
        )
        navigate(R.id.foodCameraFragment, bundle)
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
            val ts = arguments.getLong(TIMESTAMP).takeIf { it > 0 }
                ?: arguments.getLong(ARG_TIMESTAMP).takeIf { it > 0 }
                ?: System.currentTimeMillis()
            val rawMeal = arguments.getString(ARG_MEAL_TYPE)
                ?: arguments.getString(MEAL)
            val normalizedMeal = MealTime.getValidMeal(rawMeal, ts)
            return Argument(
                timestamp = ts,
                meal = normalizedMeal,
                openCamera = arguments.getBoolean(OPEN_CAMERA, false),
                openEditText = arguments.getBoolean(OPEN_EDIT_TEXT, false)
            )
        }

        fun navigate(
            fragment: BaseFragment<*>,
            timeStamp: Long = System.currentTimeMillis(),
            meal: String = MealTime.Breakfast.id,
            openCamera: Boolean = false,
            openEditText: Boolean = false
        ) {
            val validTimestamp = if (timeStamp > 0) timeStamp else System.currentTimeMillis()
            val normalizedMeal = MealTime.getValidMeal(meal, validTimestamp)
            fragment.navigate(
                R.id.logFoodFragment,
                Bundle().apply {
                    putLong(TIMESTAMP, validTimestamp)
                    putLong(ARG_TIMESTAMP, validTimestamp)
                    putString(MEAL, normalizedMeal)
                    putString(ARG_MEAL_TYPE, normalizedMeal)
                    putBoolean(OPEN_CAMERA, openCamera)
                    putBoolean(OPEN_EDIT_TEXT, openEditText)
                },
                animNavOptions
            )
        }

        fun navigate(
            navController: NavController,
            timeStamp: Long = System.currentTimeMillis(),
            meal: String = MealTime.Breakfast.id,
            openCamera: Boolean = false,
            openEditText: Boolean = false
        ) {
            try {
                val validTimestamp = if (timeStamp > 0) timeStamp else System.currentTimeMillis()
                val normalizedMeal = MealTime.getValidMeal(meal, validTimestamp)
                navController.navigate(
                    R.id.logFoodFragment,
                    Bundle().apply {
                        putLong(TIMESTAMP, validTimestamp)
                        putLong(ARG_TIMESTAMP, validTimestamp)
                        putString(MEAL, normalizedMeal)
                        putString(ARG_MEAL_TYPE, normalizedMeal)
                        putBoolean(OPEN_CAMERA, openCamera)
                        putBoolean(OPEN_EDIT_TEXT, openEditText)
                    },
                    animNavOptions
                )
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
