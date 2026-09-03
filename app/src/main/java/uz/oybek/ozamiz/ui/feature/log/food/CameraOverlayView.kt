package uz.oybek.ozamiz.ui.feature.log.food

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class CameraOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val strokeWidth = 3.5f * resources.displayMetrics.density

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = this@CameraOverlayView.strokeWidth
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val cornerPath = Path()
    private val density = resources.displayMetrics.density
    private val cornerLength = 24f * density
    private val cornerRadius = 14f * density

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return

        val rectWidth = viewWidth * 0.84f
        val rectHeight = rectWidth * 0.95f

        val left = (viewWidth - rectWidth) / 2f
        val top = (viewHeight - rectHeight) / 2f
        val right = left + rectWidth
        val bottom = top + rectHeight

        val r = cornerRadius.coerceAtMost(cornerLength / 2f)

        cornerPath.reset()

        // 1. Top-Left Corner ┌
        cornerPath.moveTo(left, top + cornerLength)
        cornerPath.lineTo(left, top + r)
        cornerPath.quadTo(left, top, left + r, top)
        cornerPath.lineTo(left + cornerLength, top)

        // 2. Top-Right Corner ┐
        cornerPath.moveTo(right - cornerLength, top)
        cornerPath.lineTo(right - r, top)
        cornerPath.quadTo(right, top, right, top + r)
        cornerPath.lineTo(right, top + cornerLength)

        // 3. Bottom-Right Corner ┘
        cornerPath.moveTo(right, bottom - cornerLength)
        cornerPath.lineTo(right, bottom - r)
        cornerPath.quadTo(right, bottom, right - r, bottom)
        cornerPath.lineTo(right - cornerLength, bottom)

        // 4. Bottom-Left Corner └
        cornerPath.moveTo(left + cornerLength, bottom)
        cornerPath.lineTo(left + r, bottom)
        cornerPath.quadTo(left, bottom, left, bottom - r)
        cornerPath.lineTo(left, bottom - cornerLength)

        canvas.drawPath(cornerPath, cornerPaint)
    }
}
