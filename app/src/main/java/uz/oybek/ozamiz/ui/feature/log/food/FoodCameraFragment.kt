package uz.oybek.ozamiz.ui.feature.log.food

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.databinding.FragmentFoodCameraBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.showToast
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FoodCameraFragment : BaseFragment<FragmentFoodCameraBinding>() {

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentFoodCameraBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentFoodCameraBinding) {
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()

        bind.buttonCapture.setOnSafeClickListener {
            takePhoto()
        }

        bind.buttonWrite.setOnSafeClickListener {
            setFragmentResult(REQUEST_KEY, bundleOf(RESULT_ACTION to ACTION_WRITE))
            findNavController().popBackStack()
        }

        bind.buttonList.setOnSafeClickListener {
            setFragmentResult(REQUEST_KEY, bundleOf(RESULT_ACTION to ACTION_LIST))
            findNavController().popBackStack()
        }

        bind.buttonBack.setOnSafeClickListener {
            findNavController().popBackStack()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            requireContext().externalCacheDir,
            "JPEG_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(
                            RESULT_ACTION to ACTION_PHOTO,
                            RESULT_URI to savedUri.toString()
                        )
                    )
                    findNavController().popBackStack()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        const val TAG = "FoodCameraFragment"
        const val REQUEST_KEY = "food_camera_request"
        const val RESULT_ACTION = "action"
        const val RESULT_URI = "uri"
        
        const val ACTION_PHOTO = "action_photo"
        const val ACTION_WRITE = "action_write"
        const val ACTION_LIST = "action_list"
    }
}
