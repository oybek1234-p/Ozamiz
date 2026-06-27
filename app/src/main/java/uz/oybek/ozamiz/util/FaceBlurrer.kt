package uz.oybek.ozamiz.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await

object FaceBlurrer {

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .build()

    private val detector = FaceDetection.getClient(detectorOptions)

    suspend fun blurFaces(bitmap: Bitmap): Bitmap {
        val image = InputImage.fromBitmap(bitmap, 0)
        return try {
            val faces = detector.process(image).await()
            if (faces.isEmpty()) return bitmap

            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            for (face in faces) {
                val bounds = face.boundingBox
                applyBlur(mutableBitmap, bounds)
            }
            mutableBitmap
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun applyBlur(bitmap: Bitmap, rect: Rect) {
        val left = rect.left.coerceAtLeast(0)
        val top = rect.top.coerceAtLeast(0)
        val right = rect.right.coerceAtMost(bitmap.width)
        val bottom = rect.bottom.coerceAtMost(bitmap.height)
        
        val width = right - left
        val height = bottom - top
        
        if (width <= 0 || height <= 0) return

        val pixelSize = 25
        for (y in top until bottom step pixelSize) {
            for (x in left until right step pixelSize) {
                val color = try { bitmap.getPixel(x, y) } catch (e: Exception) { 0 }
                
                val blockRight = (x + pixelSize).coerceAtMost(right)
                val blockBottom = (y + pixelSize).coerceAtMost(bottom)
                
                for (i in x until blockRight) {
                    for (j in y until blockBottom) {
                        try { bitmap.setPixel(i, j, color) } catch (e: Exception) {}
                    }
                }
            }
        }
    }
}
