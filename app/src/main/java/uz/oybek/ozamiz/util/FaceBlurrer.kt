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
        val width = rect.width()
        val height = rect.height()
        
        // Expand the rect by 20% to cover the whole head, not just facial features
        val expandX = (width * 0.2f).toInt()
        val expandY = (height * 0.2f).toInt()
        
        val left = (rect.left - expandX).coerceAtLeast(0)
        val top = (rect.top - expandY).coerceAtLeast(0)
        val right = (rect.right + expandX).coerceAtMost(bitmap.width)
        val bottom = (rect.bottom + expandY).coerceAtMost(bitmap.height)
        
        val newWidth = right - left
        val newHeight = bottom - top
        
        if (newWidth <= 0 || newHeight <= 0) return

        // Dynamic pixel block size based on the width to guarantee heavy obfuscation
        val pixelSize = maxOf(newWidth / 6, 30)
        
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            style = Paint.Style.FILL
        }

        for (y in top until bottom step pixelSize) {
            for (x in left until right step pixelSize) {
                val sampleX = x.coerceIn(0, bitmap.width - 1)
                val sampleY = y.coerceIn(0, bitmap.height - 1)
                
                paint.color = try { bitmap.getPixel(sampleX, sampleY) } catch (e: Exception) { 0 }
                
                val blockRight = (x + pixelSize).coerceAtMost(right)
                val blockBottom = (y + pixelSize).coerceAtMost(bottom)
                
                canvas.drawRect(
                    x.toFloat(), 
                    y.toFloat(), 
                    blockRight.toFloat(), 
                    blockBottom.toFloat(), 
                    paint
                )
            }
        }
    }
}
