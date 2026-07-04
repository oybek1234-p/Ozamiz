package uz.oybek.ozamiz.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.BlurMaskFilter
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
        
        // Shrink the rect slightly (by 10% on each side) to keep it strictly inside the face boundaries
        val shrinkX = (width * 0.10f).toInt()
        val shrinkY = (height * 0.10f).toInt()
        
        val left = (rect.left + shrinkX).coerceAtLeast(0)
        val top = (rect.top + shrinkY).coerceAtLeast(0)
        val right = (rect.right - shrinkX).coerceAtMost(bitmap.width)
        val bottom = (rect.bottom - shrinkY).coerceAtMost(bitmap.height)
        
        val faceWidth = right - left
        val faceHeight = bottom - top
        
        if (faceWidth <= 5 || faceHeight <= 5) return

        try {
            // 1. Extract the face region
            val faceBitmap = Bitmap.createBitmap(bitmap, left, top, faceWidth, faceHeight)
            
            // 2. Perform extreme downscale to completely obfuscate details while keeping color tones
            val scaleSize = 10
            val scaledDown = Bitmap.createScaledBitmap(faceBitmap, scaleSize, scaleSize, true)
            // Scale back up with bilinear filtering for a super soft, smooth blur (no pixelated blocks)
            val blurredFace = Bitmap.createScaledBitmap(scaledDown, faceWidth, faceHeight, true)
            
            faceBitmap.recycle()
            scaledDown.recycle()

            // 3. Create a temporary result bitmap to apply the feathered oval mask
            val resultBitmap = Bitmap.createBitmap(faceWidth, faceHeight, Bitmap.Config.ARGB_8888)
            val tempCanvas = Canvas(resultBitmap)
            
            // Draw the blurred face first
            tempCanvas.drawBitmap(blurredFace, 0f, 0f, null)
            
            // Draw an oval mask with DST_IN to clip the blurred image into an oval shape.
            // Using BlurMaskFilter.Blur.INNER feathers the boundary inwards, keeping the outside strictly transparent (0 alpha).
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                val blurRadius = (faceWidth * 0.20f).coerceAtLeast(1f)
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.INNER)
            }
            
            val ovalRect = RectF(0f, 0f, faceWidth.toFloat(), faceHeight.toFloat())
            tempCanvas.drawOval(ovalRect, maskPaint)
            
            // 4. Draw the feathered, blurred oval onto the original bitmap canvas
            val canvas = Canvas(bitmap)
            canvas.drawBitmap(resultBitmap, left.toFloat(), top.toFloat(), null)
            
            // Clean up temporary bitmaps
            blurredFace.recycle()
            resultBitmap.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
