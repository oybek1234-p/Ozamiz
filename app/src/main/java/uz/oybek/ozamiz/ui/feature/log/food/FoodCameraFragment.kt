package uz.oybek.ozamiz.ui.feature.log.food

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentFoodCameraBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.collectFlowOnStart
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.showToast
import uz.oybek.ozamiz.ui.utils.toBundle
import uz.oybek.ozamiz.util.handleException
import java.io.File

@AndroidEntryPoint
class FoodCameraFragment : BaseFragment<FragmentFoodCameraBinding>() {

    private val viewModel: FoodCameraViewModel by viewModels()

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var isStartingCamera = false

    private var hasRequestedPermission = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRequestedPermission = true
        if (isGranted) {
            updatePermissionUI(hasPermission = true)
            startCamera()
        } else {
            updatePermissionUI(hasPermission = false)
        }
    }

    private val popCameraNavOptions by lazy {
        NavOptions.Builder()
            .setEnterAnim(R.anim.fragment_open_anim)
            .setExitAnim(R.anim.fragment_close_anim)
            .setPopEnterAnim(R.anim.fragment_pop_enter)
            .setPopExitAnim(R.anim.fragment_pop_exit)
            .setPopUpTo(R.id.foodCameraFragment, true)
            .build()
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentFoodCameraBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentFoodCameraBinding) {
        initWindowInsets(bind)
        initCameraUI(bind)
        initControlsUI(bind)
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshLimitState()
        syncCameraStateOnResume()
    }

    private fun syncCameraStateOnResume() {
        val hasPermission = hasCameraPermission()
        updatePermissionUI(hasPermission)
        if (hasPermission) {
            startCameraIfNeeded()
        } else if (!hasRequestedPermission) {
            requestCameraPermission()
        }
    }

    private fun hasCameraPermission(): Boolean {
        val ctx = context ?: return false
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCameraIfNeeded() {
        if (cameraProvider == null || camera == null) {
            startCamera()
        }
    }

    private fun requestCameraPermission() {
        hasRequestedPermission = true
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun initWindowInsets(bind: FragmentFoodCameraBinding) {
        ViewCompat.setOnApplyWindowInsetsListener(bind.root) { _, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val density = resources.displayMetrics.density

            bind.bottomControlBar.setPadding(
                bind.bottomControlBar.paddingLeft,
                (14 * density).toInt(),
                bind.bottomControlBar.paddingRight,
                navBars.bottom + (28 * density).toInt()
            )
            insets
        }
    }

    private fun initCameraUI(bind: FragmentFoodCameraBinding) {
        bind.viewFinder.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    }

    private fun initControlsUI(bind: FragmentFoodCameraBinding) = with(bind) {
        buttonCapture.setOnSafeClickListener { onCaptureButtonClicked() }
        buttonWrite.setOnSafeClickListener { viewModel.onWriteClicked() }
        buttonList.setOnSafeClickListener { viewModel.onListClicked() }
        buttonSaved.setOnSafeClickListener { viewModel.onSavedClicked() }
        buttonFlash.setOnSafeClickListener { viewModel.onToggleFlash() }
        buttonBack.setOnSafeClickListener { viewModel.onBackClicked() }
        btnGrantCameraPermission.setOnSafeClickListener { onGrantCameraPermissionClicked() }
    }

    private fun onCaptureButtonClicked() {
        if (!hasCameraPermission()) {
            checkAndRequestCameraPermission()
            return
        }
        if (viewModel.uiState.value.canLog) {
            animateCaptureButton()
            takePhoto()
        }
    }

    private fun animateCaptureButton() {
        binding.buttonCapture.animate()
            .scaleX(0.88f)
            .scaleY(0.88f)
            .setDuration(80)
            .withEndAction {
                binding.buttonCapture.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    private fun onGrantCameraPermissionClicked() {
        val ctx = context ?: return
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            || !hasRequestedPermission
        ) {
            requestCameraPermission()
        } else {
            openAppSettings(ctx)
        }
    }

    private fun openAppSettings(ctx: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", ctx.packageName, null)
            }
            startActivity(intent)
        } catch (_: Exception) {
            requestCameraPermission()
        }
    }

    private fun observeViewModel() {
        collectFlowOnStart(
            viewModel.uiState.map { it.canLog to it.isCapturing }.distinctUntilChanged()
        ) { (canLog, isCapturing) ->
            renderCaptureAvailability(canLog, isCapturing)
        }

        collectFlowOnStart(
            viewModel.uiState.map {
                Triple(it.isPremium, it.remainingLimit, it.totalLimit)
            }.distinctUntilChanged()
        ) { (isPremium, remainingLimit, totalLimit) ->
            renderLimitBadge(isPremium, remainingLimit, totalLimit)
        }

        collectFlowOnStart(
            viewModel.uiState.map { it.isFlashOn }.distinctUntilChanged()
        ) { isFlashOn ->
            renderFlashState(isFlashOn)
        }

        collectFlowOnStart(viewModel.events) { event ->
            handleEvent(event)
        }
    }

    private fun renderCaptureAvailability(canLog: Boolean, isCapturing: Boolean) = with(binding) {
        val canCapture = canLog && !isCapturing
        buttonCapture.isEnabled = canCapture
        buttonCapture.alpha = if (canCapture) 1.0f else 0.35f

        buttonWrite.isVisible = canLog
        buttonSaved.isVisible = canLog
        buttonList.isVisible = true
    }

    private fun renderLimitBadge(isPremium: Boolean, remainingLimit: Int, totalLimit: Int) {
        binding.tvLimitBadge.text = if (isPremium) {
            getString(R.string.food_camera_limit_unlimited)
        } else {
            getString(
                R.string.food_camera_limit_remaining_format,
                remainingLimit,
                totalLimit
            )
        }
    }

    private fun renderFlashState(isFlashOn: Boolean) {
        imageCapture?.flashMode =
            if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        camera?.cameraControl?.enableTorch(isFlashOn)
        binding.buttonFlash.setImageResource(
            if (isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
        )
    }

    private fun handleEvent(event: FoodCameraEvent) {
        when (event) {
            is FoodCameraEvent.NavigateToPhotoReview -> navigateToPhotoReview(event)
            is FoodCameraEvent.NavigateToWriteFood -> navigateToWriteFood(event)
            is FoodCameraEvent.NavigateToSearchFood -> navigateToSearchFood(event)
            is FoodCameraEvent.NavigateToSavedFoods -> navigateToSavedFoods(event)
            is FoodCameraEvent.NavigateBack -> popBackStack()
        }
    }

    private fun navigateToPhotoReview(event: FoodCameraEvent.NavigateToPhotoReview) {
        val args = FoodPhotoReviewArgs(
            imageUri = event.imageUri,
            mealType = event.mealType
        )
        navigate(R.id.foodPhotoReviewFragment, args.toBundle())
    }

    private fun navigateToWriteFood(event: FoodCameraEvent.NavigateToWriteFood) {
        val args = WriteFoodArgs(
            mealType = event.mealType,
            timestamp = event.timestamp
        )
        navigate(R.id.writeFoodFragment, args.toBundle())
    }

    private fun navigateToSearchFood(event: FoodCameraEvent.NavigateToSearchFood) {
        val args = SearchFoodArgs(mealType = event.mealType)
        navigate(R.id.searchFoodFragment, args.toBundle(), popCameraNavOptions)
    }

    private fun navigateToSavedFoods(event: FoodCameraEvent.NavigateToSavedFoods) {
        val args = SavedFoodsArgs(
            mealType = event.mealType,
            timestamp = event.timestamp
        )
        navigate(R.id.savedFoodsFragment, args.toBundle(), popCameraNavOptions)
    }

    private fun checkAndRequestCameraPermission() {
        val hasPermission = hasCameraPermission()
        updatePermissionUI(hasPermission)
        if (hasPermission) {
            startCameraIfNeeded()
        } else {
            requestCameraPermission()
        }
    }

    private fun updatePermissionUI(hasPermission: Boolean) = with(binding) {
        layoutCameraPermission.isVisible = !hasPermission
        viewFinder.isVisible = hasPermission
        cameraOverlayView.isVisible = hasPermission
        buttonFlash.isVisible = hasPermission
        val state = viewModel.uiState.value
        val canCapture = hasPermission && state.canLog && !state.isCapturing
        buttonCapture.isEnabled = canCapture
        buttonCapture.alpha = if (canCapture) 1.0f else 0.35f
        bottomControlBar.isVisible = true
    }

    private fun startCamera() {
        if (isStartingCamera) return
        val ctx = context ?: return
        if (!hasCameraPermission()) {
            updatePermissionUI(hasPermission = false)
            return
        }
        isStartingCamera = true
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
            isStartingCamera = false
            if (!isAdded || context == null) return@addListener
            try {
                bindCameraUseCases(cameraProviderFuture.get())
            } catch (e: Exception) {
                handleException(e)
                context?.let { showToast(it.getString(R.string.error_opening_camera)) }
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun bindCameraUseCases(provider: ProcessCameraProvider) {
        cameraProvider = provider
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = binding.viewFinder.surfaceProvider
        }
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            context?.let {
                showToast(it.getString(R.string.error_opening_camera))
            }
        }
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        val ctx = context ?: return

        viewModel.onCaptureStarted()

        val photoFile = File(ctx.cacheDir, "food_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(ctx),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    if (!isAdded) return
                    viewModel.onCaptureFinished()
                    showToast(getString(R.string.error_opening_camera))
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    if (!isAdded) return
                    viewModel.onCaptureFinished()
                    val savedUri = createCapturedPhotoUri(photoFile, output)
                    viewModel.onPhotoCaptured(savedUri)
                }
            }
        )
    }

    private fun createCapturedPhotoUri(
        photoFile: File,
        output: ImageCapture.OutputFileResults
    ): Uri {
        val currentContext = context ?: return output.savedUri ?: Uri.fromFile(photoFile)
        return try {
            FileProvider.getUriForFile(
                currentContext,
                "${currentContext.packageName}.provider",
                photoFile
            )
        } catch (e: Exception) {
            handleException(e)
            output.savedUri ?: Uri.fromFile(photoFile)
        }
    }

    override fun onDestroyView() {
        isStartingCamera = false
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageCapture = null
        camera = null
        super.onDestroyView()
    }
}
