package uz.oybek.ozamiz.ui.feature.home

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.google.android.material.color.MaterialColors

class GaugeProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress = 0
    private var max = 100
    private var strokeWidth = 8f.dpToPx()
    private val startAngle = 135f
    private val sweepAngle = 270f

    private val colorPrimary =
        MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, Color.BLUE)
    private val colorTrack = MaterialColors.getColor(
        this,
        com.google.android.material.R.attr.colorSurfaceContainerHigh,
        Color.LTGRAY
    )

    // Chizish qurollari
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@GaugeProgressBar.strokeWidth
        strokeCap = Paint.Cap.ROUND
        color = colorTrack
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@GaugeProgressBar.strokeWidth
        strokeCap = Paint.Cap.ROUND
        color = colorPrimary
    }

    private val rectF = RectF()
    private var currentSweepAngle = 0f // Animatsiya uchun

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = strokeWidth / 2
        rectF.set(padding, padding, w - padding, h - padding)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Orqa fon (Track)
        canvas.drawArc(rectF, startAngle, sweepAngle, false, backgroundPaint)
        // Progress (Oldi qism)
        canvas.drawArc(rectF, startAngle, currentSweepAngle, false, progressPaint)
    }

    fun setMax(value: Int) {
        if (value > 0) {
            this.max = value
            invalidate()
        }
    }

    fun setProgress(value: Int, animate: Boolean = true) {
        val constrainedValue = value.coerceIn(0, max)
        val targetSweep = (constrainedValue.toFloat() / max) * sweepAngle

        if (animate) {
            val animator = ValueAnimator.ofFloat(currentSweepAngle, targetSweep)
            animator.duration = 1000 // 1 soniya animatsiya
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener { animation ->
                currentSweepAngle = animation.animatedValue as Float
                invalidate()
            }
            animator.start()
        } else {
            currentSweepAngle = targetSweep
            invalidate()
        }
        this.progress = constrainedValue
    }

    private fun Float.dpToPx(): Float = this * resources.displayMetrics.density
}