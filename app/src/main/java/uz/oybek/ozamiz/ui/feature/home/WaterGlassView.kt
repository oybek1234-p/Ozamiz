package uz.oybek.ozamiz.ui.feature.home

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.graphics.toColorInt

class WaterGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val emptyColor = "#E1F5FE".toColorInt()
    private val filledColor = "#29B6F6".toColorInt()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val path = Path()
    private var fillLevel = 0f
    private var animator: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        path.reset()
        val inset = w * 0.18f

        path.moveTo(0f, 0f)
        path.lineTo(w.toFloat(), 0f)
        path.lineTo(w - inset, h.toFloat())
        path.lineTo(inset, h.toFloat())
        path.close()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.color = emptyColor
        canvas.drawPath(path, paint)

        if (fillLevel > 0f) {
            canvas.save()
            canvas.clipPath(path)

            paint.color = filledColor
            val waterHeight = height * fillLevel
            val top = height - waterHeight

            canvas.drawRect(0f, top, width.toFloat(), height.toFloat(), paint)
            canvas.restore()
        }
    }

    fun setLevel(targetLevel: Float, animate: Boolean) {
        animator?.cancel()

        if (animate) {
            animator = ValueAnimator.ofFloat(fillLevel, targetLevel).apply {
                duration = 800
                interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
                addUpdateListener {
                    fillLevel = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            fillLevel = targetLevel
            invalidate()
        }
    }
}