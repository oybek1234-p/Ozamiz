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
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentFoodCameraBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.SystemBarStyle
import uz.oybek.ozamiz.ui.utils.applySystemBarStyle
import uz.oybek.ozamiz.ui.utils.restoreSystemBarStyle
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.showToast
import uz.oybek.ozamiz.util.handleException
import java.io.File

class FoodCameraFragment : BaseFragment<FragmentFoodCameraBinding>() {

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var savedSystemBarStyle: SystemBarStyle? = null
    private var isCapturing = false

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentFoodCameraBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentFoodCameraBinding) {
        bind.viewFinder.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        setupClickListeners(bind)
        startCamera()
    }

    override fun onResume() {
        super.onResume()
        savedSystemBarStyle = applySystemBarStyle(
            statusBarColor = Color.BLACK,
            navigationBarColor = Color.BLACK,
            lightStatusBars = false,
            lightNavigationBars = false,
        )
    }

    override fun onPause() {
        savedSystemBarStyle?.let { restoreSystemBarStyle(it) }
        savedSystemBarStyle = null
        super.onPause()
    }

    override fun onDestroyView() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageCapture = null
        super.onDestroyView()
    }

    private fun setupClickListeners(bind: FragmentFoodCameraBinding) = with(bind) {
        buttonCapture.setOnSafeClickListener { takePhoto() }
        buttonWrite.setOnSafeClickListener { deliverAction(ACTION_WRITE) }
        buttonList.setOnSafeClickListener { deliverAction(ACTION_LIST) }
        buttonBack.setOnSafeClickListener { findNavController().popBackStack() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            if (!isAdded) return@addListener
            try {
                bindCameraUseCases(cameraProviderFuture.get())
            } catch (e: Exception) {
                handleException(e)
                showToast(getString(R.string.error_opening_camera))
            }
        }, ContextCompat.getMainExecutor(requireContext()))
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
            provider.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            handleException(e)
            showToast(getString(R.string.error_opening_camera))
        }
    }

    private fun takePhoto() {
        if (isCapturing) return
        val capture = imageCapture ?: return

        isCapturing = true
        binding.buttonCapture.isEnabled = false

        val photoFile = File(requireContext().cacheDir, "food_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
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
                    deliverAction(
                        ACTION_PHOTO,
                        bundleOf(RESULT_URI to Uri.fromFile(photoFile).toString())
                    )
                }
            }
        )
    }

    private fun deliverAction(action: String, extras: Bundle = bundleOf()) {
        setFragmentResult(REQUEST_KEY, bundleOf(RESULT_ACTION to action).apply {
            putAll(extras)
        })
        findNavController().popBackStack()
    }

    companion object {
        const val REQUEST_KEY = "food_camera_request"
        const val RESULT_ACTION = "action"
        const val RESULT_URI = "uri"

        const val ACTION_PHOTO = "action_photo"
        const val ACTION_WRITE = "action_write"
        const val ACTION_LIST = "action_list"
    }
}
