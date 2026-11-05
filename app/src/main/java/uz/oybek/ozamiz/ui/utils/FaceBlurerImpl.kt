package uz.oybek.ozamiz.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.coroutines.resume
import androidx.core.graphics.scale
import androidx.core.graphics.withClip

class FaceBlurerImpl @Inject constructor() : FaceBlurer {

    override suspend fun blurFace(context: Context, imageUri: Uri): Uri {
        return suspendCancellableCoroutine { continuation ->
            val inputImage = try {
                InputImage.fromFilePath(context, imageUri)
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(imageUri)
                return@suspendCancellableCoroutine
            }

            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()

            val detector = FaceDetection.getClient(options)
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        val bitmap = try {
                            @Suppress("DEPRECATION")
                            android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                                .copy(Bitmap.Config.ARGB_8888, true)
                        } catch (_: Exception) {
                            if (continuation.isActive) continuation.resume(imageUri)
                            return@addOnSuccessListener
                        }

                        val canvas = Canvas(bitmap)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

                        for (face in faces) {
                            val bounds = face.boundingBox
                            val left = bounds.left.coerceAtLeast(0)
                            val top = bounds.top.coerceAtLeast(0)
                            val right = bounds.right.coerceAtMost(bitmap.width)
                            val bottom = bounds.bottom.coerceAtMost(bitmap.height)
                            val width = right - left
                            val height = bottom - top

                            if (width > 10 && height > 10) {
                                try {
                                    val faceCropped = Bitmap.createBitmap(bitmap, left, top, width, height)

                                    // Downscale for smooth Gaussian blur
                                    val smallW = (width / 12).coerceAtLeast(4)
                                    val smallH = (height / 12).coerceAtLeast(4)
                                    val smallFace = faceCropped.scale(smallW, smallH)

                                    // Upscale back
                                    val blurredFace = smallFace.scale(width, height)

                                    // Clip canvas to face oval contour
                                    val ovalPath = Path().apply {
                                        addOval(RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat()), Path.Direction.CW)
                                    }

                                    canvas.withClip(ovalPath) {
                                        drawBitmap(
                                            blurredFace,
                                            left.toFloat(),
                                            top.toFloat(),
                                            paint
                                        )
                                    }
                                } catch (_: Exception) {}
                            }
                        }

                        val blurredFile = File(context.cacheDir, "blurred_${System.currentTimeMillis()}.jpg")
                        try {
                            FileOutputStream(blurredFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                            }
                            if (continuation.isActive) continuation.resume(Uri.fromFile(blurredFile))
                        } catch (_: Exception) {
                            if (continuation.isActive) continuation.resume(imageUri)
                        }
                    } else {
                        if (continuation.isActive) continuation.resume(imageUri)
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(imageUri)
                }
        }
    }
}
