package uz.oybek.ozamiz.ui.feature.log.food

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentFoodCameraBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.showToast
import uz.oybek.ozamiz.util.handleException
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import uz.oybek.ozamiz.data.food.limit.FoodLimitManager
import uz.oybek.ozamiz.data.user.UserProfileRepository.user
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import java.io.File

class FoodCameraFragment : BaseFragment<FragmentFoodCameraBinding>() {

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isCapturing = false

    private var isFlashOn = false

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentFoodCameraBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentFoodCameraBinding) {
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

        bind.viewFinder.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        setupClickListeners(bind)
        updateLimitBadge()
        bind.viewFinder.post {
            if (isAdded) startCamera()
        }
    }

    private var camera: androidx.camera.core.Camera? = null

    override fun onResume() {
        super.onResume()
        isCapturing = false
        binding.buttonCapture.isEnabled = true
        updateLimitBadge()
        if (cameraProvider == null || camera == null) {
            startCamera()
        }
    }

    private fun updateLimitBadge() {
        if (user.isPremium()) {
            binding.tvLimitBadge.text = "✨ Cheksiz"
        } else {
            val remaining = FoodLimitManager.getRemainingLogs(user)
            val total = FoodLimitManager.FREE_DAILY_AI_LIMIT
            binding.tvLimitBadge.text = "$remaining/$total ta limit qoldi"
        }
    }

    override fun onDestroyView() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageCapture = null
        camera = null
        isCapturing = false
        super.onDestroyView()
    }

    private fun getSelectedTimestamp(): Long {
        val ts = arguments?.getLong("timestamp", -1L) ?: -1L
        return if (ts > 0) ts else System.currentTimeMillis()
    }

    private fun getSelectedMealType(): String {
        val raw = arguments?.getString("mealType") ?: arguments?.getString("meal")
        return uz.oybek.ozamiz.data.food.model.MealTime.getValidMeal(raw, getSelectedTimestamp())
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupClickListeners(bind: FragmentFoodCameraBinding) = with(bind) {
        buttonCapture.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.88f)
                        .scaleY(0.88f)
                        .setDuration(100)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(180)
                        .setInterpolator(OvershootInterpolator(2.5f))
                        .start()
                }
            }
            false
        }
        buttonCapture.setOnSafeClickListener { takePhoto() }
        buttonWrite.setOnSafeClickListener {
            val mealType = getSelectedMealType()
            val timestamp = getSelectedTimestamp()
            if (!uz.oybek.ozamiz.data.food.limit.FoodLimitManager.canLogFood()) {
                activity?.let { act ->
                    uz.oybek.ozamiz.data.food.limit.FoodLimitManager.showLimitReachedSheet(
                        activity = act,
                        title = "Bugungi rasmga olish limitingiz tugadi"
                    ) {
                        navigate(
                            R.id.action_foodCameraFragment_to_searchFoodFragment,
                            bundleOf("mealType" to mealType, "meal" to mealType, "timestamp" to timestamp)
                        )
                    }
                }
                return@setOnSafeClickListener
            }
            navigate(
                R.id.action_foodCameraFragment_to_writeFoodFragment,
                bundleOf("mealType" to mealType, "meal" to mealType, "timestamp" to timestamp)
            )
        }
        buttonList.setOnSafeClickListener {
            val mealType = getSelectedMealType()
            val timestamp = getSelectedTimestamp()
            navigate(
                R.id.action_foodCameraFragment_to_searchFoodFragment,
                bundleOf("mealType" to mealType, "meal" to mealType, "timestamp" to timestamp)
            )
        }
        buttonSaved.setOnSafeClickListener {
            val mealType = getSelectedMealType()
            val timestamp = getSelectedTimestamp()
            navigate(
                R.id.savedFoodsFragment,
                bundleOf("mealType" to mealType, "meal" to mealType, "timestamp" to timestamp)
            )
        }
        buttonFlash.setOnSafeClickListener {
            isFlashOn = !isFlashOn
            imageCapture?.flashMode =
                if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
            camera?.cameraControl?.enableTorch(isFlashOn)
            buttonFlash.setImageResource(if (isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off)
        }
        buttonBack.setOnSafeClickListener { popBackStack() }
    }

    private fun startCamera() {
        val ctx = context ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        cameraProviderFuture.addListener({
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
            handleException(e)
            context?.let { showToast(it.getString(R.string.error_opening_camera)) }
        }
    }

    private var capturedPhotoUri: Uri? = null

    private fun takePhoto() {
        if (isCapturing) return
        if (!uz.oybek.ozamiz.data.food.limit.FoodLimitManager.canLogFood()) {
            activity?.let { act ->
                uz.oybek.ozamiz.data.food.limit.FoodLimitManager.showLimitReachedSheet(
                    activity = act,
                    title = "Bugungi rasmga olish limitingiz tugadi"
                ) {
                    val mealType = getSelectedMealType()
                    val timestamp = getSelectedTimestamp()
                    navigate(
                        R.id.action_foodCameraFragment_to_searchFoodFragment,
                        bundleOf("mealType" to mealType, "meal" to mealType, "timestamp" to timestamp)
                    )
                }
            }
            return
        }

        val capture = imageCapture ?: return
        val ctx = context ?: return

        isCapturing = true
        binding.buttonCapture.isEnabled = false

        val photoFile =
            File(ctx.cacheDir, "food_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(ctx),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    if (!isAdded) return
                    isCapturing = false
                    binding.buttonCapture.isEnabled = true
                    handleException(exc)
                    showToast(getString(R.string.error_opening_camera))
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    if (!isAdded) return
                    isCapturing = false
                    binding.buttonCapture.isEnabled = true
                    val photoUri = Uri.fromFile(photoFile)
                    capturedPhotoUri = photoUri
                    val mealType = getSelectedMealType()
                    val timestamp = getSelectedTimestamp()
                    val bundle = bundleOf(
                        "imageUri" to photoUri.toString(),
                        "mealType" to mealType,
                        "meal" to mealType,
                        "timestamp" to timestamp
                    )
                    navigate(
                        R.id.action_foodCameraFragment_to_foodPhotoReviewFragment,
                        bundle
                    )
                }
            }
        )
    }

    companion object {
        const val REQUEST_KEY = "food_camera_request"
    }
}
