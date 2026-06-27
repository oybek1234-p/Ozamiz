package uz.oybek.ozamiz.ui.feature.log.food

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class CameraOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#77000000")
        isAntiAlias = true
    }

    private val transparentPaint = Paint().apply {
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        isAntiAlias = true
    }

    private val strokePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        isAntiAlias = true
    }

    private val rectF = RectF()
    private val cornerRadius = 24f * resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        val rectWidth = width * 0.85f
        val rectHeight = rectWidth

        val left = (width - rectWidth) / 2
        val top = (height - rectHeight) / 2 - 40f * resources.displayMetrics.density
        val right = left + rectWidth
        val bottom = top + rectHeight

        rectF.set(left, top, right, bottom)

        canvas.drawRect(0f, 0f, width, height, backgroundPaint)

        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, transparentPaint)

        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, strokePaint)
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }
}
