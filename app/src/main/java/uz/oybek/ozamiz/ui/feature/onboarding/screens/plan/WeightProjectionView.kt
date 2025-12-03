package uz.oybek.ozamiz.ui.feature.onboarding.screens.plan

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import uz.oybek.ozamiz.R
import androidx.core.graphics.toColorInt

class WeightProjectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private object Colors {
        var line: Int = 0
        var grid: Int = 0
        var fillStart: Int = 0
        var dot: Int = 0
        var text: Int = 0
        var labelBox: Int = 0
        var labelBoxBackground: Int = 0
        var labelText: Int = 0
        var extension: Int = 0
        var grayDot: Int = 0
    }

    private var currentWeight: Float = 80f
    private var targetWeight: Float = 75f
    private var dateLabel: String = "3 oy"

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelBoxBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val extensionPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val grayDotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val mainPath = Path()
    private val fillPath = Path()
    private val dottedPath = Path()

    private var startX = 0f
    private var startY = 0f
    private var midX = 0f
    private var midY = 0f
    private var endX = 0f

    private val pathMeasure = PathMeasure()
    private val animatedPath = Path()
    private var pathAnimationFraction = 0f
    private var animator: ValueAnimator? = null

    init {
        initColors(context)
        initPaints()
    }

    private fun initColors(context: Context) {
        val primaryColor = getColorFromAttr(context, androidx.appcompat.R.attr.colorPrimary)

        Colors.line = primaryColor
        Colors.grid = "#E0E0E0".toColorInt()
        Colors.fillStart = Color.TRANSPARENT
        Colors.dot = primaryColor
        Colors.text = "#757575".toColorInt()
        Colors.labelBox = "#E0E0E0".toColorInt()
        Colors.labelBoxBackground = Color.WHITE
        Colors.labelText = primaryColor
        Colors.extension = Color.GRAY
        Colors.grayDot = Color.GRAY
    }

    @ColorInt
    private fun getColorFromAttr(context: Context, @AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }

    private fun initPaints() {
        linePaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 10f
            strokeCap = Paint.Cap.ROUND
            color = Colors.line
        }
        gridPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f)
            color = Colors.grid
        }
        fillPaint.style = Paint.Style.FILL
        dotPaint.apply {
            style = Paint.Style.FILL
            color = Colors.dot
        }
        textPaint.apply {
            color = Colors.text
            textSize = 32f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        labelBoxPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Colors.labelBox
        }
        labelBoxBackgroundPaint.apply {
            style = Paint.Style.FILL
            color = Colors.labelBoxBackground
        }
        labelTextPaint.apply {
            color = Colors.labelText
            textSize = 36f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        extensionPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f)
            color = Colors.extension
        }
        grayDotPaint.apply {
            style = Paint.Style.FILL
            color = Colors.grayDot
        }
    }

    fun setData(current: Float, target: Float, date: String) {
        this.currentWeight = current
        this.targetWeight = target
        this.dateLabel = date
        requestLayout()
        startAnimation()
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                pathAnimationFraction = it.animatedValue as Float
                invalidate()
            }
        }
        animator?.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return

        val padding = 60f
        val bottomPadding = 80f

        startX = padding
        startY = h * 0.35f
        midX = w * 0.55f
        midY = h - bottomPadding - 20f
        endX = w - padding

        mainPath.reset()
        mainPath.moveTo(startX, startY)
        mainPath.cubicTo(
            startX + (midX - startX) / 2, startY,
            midX - (midX - startX) / 2, midY,
            midX, midY
        )

        fillPath.reset()
        fillPath.addPath(mainPath)
        fillPath.lineTo(midX, h - bottomPadding)
        fillPath.lineTo(startX, h - bottomPadding)
        fillPath.close()

        dottedPath.reset()
        dottedPath.moveTo(midX, midY)
        dottedPath.quadTo(midX + (endX - midX) / 2, midY, endX, midY)

        fillPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            Colors.fillStart,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        pathMeasure.setPath(mainPath, false)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        canvas.drawLine(startX, height / 2f, endX, height / 2f, gridPaint)
        canvas.drawLine(startX, midY + 20f, endX, midY + 20f, gridPaint)

        canvas.drawPath(fillPath, fillPaint)

        animatedPath.reset()
        pathMeasure.getSegment(0f, pathMeasure.length * pathAnimationFraction, animatedPath, true)
        canvas.drawPath(animatedPath, linePaint)

        if (pathAnimationFraction == 1f) {
            canvas.drawPath(dottedPath, extensionPaint)
        }

        canvas.drawCircle(startX, startY, 12f, dotPaint)
        if (pathAnimationFraction == 1f) {
            canvas.drawCircle(midX, midY, 16f, dotPaint)
            canvas.drawCircle(endX, midY, 12f, grayDotPaint)
        }

        if (pathAnimationFraction == 1f) {
            val labelText = "${targetWeight}kg"
            val boxW = 150f
            val boxH = 65f
            val boxX = midX - (boxW / 2)
            val boxY = midY - boxH - 25f

            canvas.drawLine(midX, midY - 10f, midX, boxY + boxH, labelBoxPaint)
            canvas.drawRoundRect(
                boxX,
                boxY,
                boxX + boxW,
                boxY + boxH,
                16f,
                16f,
                labelBoxBackgroundPaint
            )
            canvas.drawRoundRect(boxX, boxY, boxX + boxW, boxY + boxH, 16f, 16f, labelBoxPaint)

            val textBounds = Rect()
            labelTextPaint.getTextBounds(labelText, 0, labelText.length, textBounds)
            val textY = boxY + boxH / 2 + textBounds.height() / 2
            canvas.drawText(labelText, boxX + boxW / 2, textY, labelTextPaint)
        }

        canvas.drawText(context.getString(R.string.today), startX, height - 20f, textPaint)
        canvas.drawText(dateLabel, midX, height - 20f, textPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }
}