package uz.oybek.ozamiz.ui.feature.log.food

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class CameraOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = OVERLAY_COLOR
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH_DP * resources.displayMetrics.density
    }

    private val cutoutRect = RectF()
    private val cornerRadius = CORNER_RADIUS_DP * resources.displayMetrics.density
    private val verticalOffset = VERTICAL_OFFSET_DP * resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return

        val cutoutSize = viewWidth * CUTOUT_WIDTH_RATIO
        val left = (viewWidth - cutoutSize) / 2f
        val top = (viewHeight - cutoutSize) / 2f - verticalOffset
        val right = left + cutoutSize
        val bottom = top + cutoutSize

        cutoutRect.set(left, top, right, bottom)

        canvas.drawRect(0f, 0f, viewWidth, top, backgroundPaint)
        canvas.drawRect(0f, bottom, viewWidth, viewHeight, backgroundPaint)
        canvas.drawRect(0f, top, left, bottom, backgroundPaint)
        canvas.drawRect(right, top, viewWidth, bottom, backgroundPaint)
        canvas.drawRoundRect(cutoutRect, cornerRadius, cornerRadius, strokePaint)
    }

    companion object {
        private const val OVERLAY_COLOR = 0x77000000.toInt()
        private const val CUTOUT_WIDTH_RATIO = 0.85f
        private const val CORNER_RADIUS_DP = 24f
        private const val STROKE_WIDTH_DP = 2f
        private const val VERTICAL_OFFSET_DP = 40f
    }
}
