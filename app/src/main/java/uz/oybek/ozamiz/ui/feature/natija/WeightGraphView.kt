package uz.oybek.ozamiz.ui.feature.natija

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.ColorUtils
import uz.oybek.ozamiz.data.logs.model.WeightLog
import uz.oybek.ozamiz.ui.utils.VaznimColors
import java.util.Locale

class WeightGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class GraphPoint(
        val label: String,
        val weight: Float,
        val isTarget: Boolean = false,
        val isCurrent: Boolean = false,
        val isStart: Boolean = false
    )

    private val points = mutableListOf<GraphPoint>()

    private var primaryColor: Int = Color.parseColor("#4CAF50")
    private var targetColor: Int = Color.parseColor("#FF9800")
    private var textColor: Int = Color.parseColor("#757575")
    private var gridColor: Int = Color.parseColor("#E0E0E0")

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3f)
        strokeCap = Paint.Cap.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val targetLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1.5f)
        pathEffect = DashPathEffect(floatArrayOf(dpToPx(6f), dpToPx(4f)), 0f)
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = spToPx(11f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
        pathEffect = DashPathEffect(floatArrayOf(dpToPx(4f), dpToPx(4f)), 0f)
    }

    init {
        val typedValue = TypedValue()
        val theme = context.theme
        val colorPrimary = VaznimColors.getPrimary(context)
        primaryColor = colorPrimary

        if (theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                typedValue,
                true
            )
        ) {
            textColor = typedValue.data
        }
        if (theme.resolveAttribute(
                com.google.android.material.R.attr.colorOutlineVariant,
                typedValue,
                true
            )
        ) {
            gridColor = typedValue.data
        }
        if (theme.resolveAttribute(
                com.google.android.material.R.attr.colorTertiary,
                typedValue,
                true
            )
        ) {
            targetColor = typedValue.data
        }

        linePaint.color = primaryColor
        targetLinePaint.color = targetColor
        gridPaint.color = gridColor
        textPaint.color = textColor
    }

    fun setWeightData(
        startWeight: Double?,
        currentWeight: Double?,
        goalWeight: Double?,
        logs: List<WeightLog>
    ) {
        points.clear()

        val validLogs = logs.filter { it.weightKg > 0 }.sortedBy { it.timestampLong }
        val startW =
            (startWeight ?: validLogs.firstOrNull()?.weightKg ?: currentWeight ?: 70.0).toFloat()
        val currentW =
            (currentWeight ?: validLogs.lastOrNull()?.weightKg ?: startW.toDouble()).toFloat()

        points.add(GraphPoint("", startW, isStart = true))

        val intermediateLogs =
            if (validLogs.size > 2) validLogs.subList(1, validLogs.size - 1) else emptyList()
        val takeCount = if (intermediateLogs.size > 4) 4 else intermediateLogs.size
        val step =
            if (intermediateLogs.isNotEmpty()) intermediateLogs.size / takeCount.coerceAtLeast(1) else 1

        var idx = 0
        while (idx < intermediateLogs.size) {
            val log = intermediateLogs[idx]
            points.add(
                GraphPoint(
                    "",
                    log.weightKg.toFloat()
                )
            )
            idx += step.coerceAtLeast(1)
        }

        if (validLogs.size >= 2 || (startW != currentW)) {
            points.add(GraphPoint("", currentW, isCurrent = true))
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (points.isEmpty()) return

        val paddingLeft = dpToPx(16f)
        val paddingRight = dpToPx(16f)
        val paddingTop = dpToPx(24f)
        val paddingBottom = dpToPx(16f)

        val width = width.toFloat() - paddingLeft - paddingRight
        val height = height.toFloat() - paddingTop - paddingBottom

        if (width <= 0 || height <= 0) return

        val minWeight = points.minOf { it.weight } * 0.95f
        val maxWeight = points.maxOf { it.weight } * 1.05f
        val weightRange = (maxWeight - minWeight).coerceAtLeast(1f)

        fun getX(index: Int): Float {
            return if (points.size <= 1) {
                paddingLeft + width / 2f
            } else {
                paddingLeft + (index.toFloat() / (points.size - 1)) * width
            }
        }

        fun getY(weight: Float): Float {
            val norm = (weight - minWeight) / weightRange
            return paddingTop + (1f - norm) * height
        }

        // Draw horizontal grid line
        canvas.drawLine(
            paddingLeft,
            paddingTop + height / 2f,
            paddingLeft + width,
            paddingTop + height / 2f,
            gridPaint
        )

        val path = Path()
        val fillPath = Path()

        var firstX = 0f
        var firstY = 0f
        var lastX = 0f
        var lastY = 0f

        points.forEachIndexed { i, pt ->
            val x = getX(i)
            val y = getY(pt.weight)

            if (i == 0) {
                firstX = x
                firstY = y
                path.moveTo(x, y)
                fillPath.moveTo(x, paddingTop + height)
                fillPath.lineTo(x, y)
            } else {
                val prevX = getX(i - 1)
                val prevY = getY(points[i - 1].weight)

                val controlX1 = prevX + (x - prevX) / 2f
                val controlY1 = prevY
                val controlX2 = prevX + (x - prevX) / 2f
                val controlY2 = y

                path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            }

            if (i == points.size - 1) {
                lastX = x
                lastY = y
            }
        }

        fillPath.lineTo(lastX, paddingTop + height)
        fillPath.close()

        val gradient = LinearGradient(
            0f, paddingTop, 0f, paddingTop + height,
            ColorUtils.setAlphaComponent(primaryColor, 60),
            ColorUtils.setAlphaComponent(primaryColor, 0),
            Shader.TileMode.CLAMP
        )
        fillPaint.shader = gradient
        canvas.drawPath(fillPath, fillPaint)

        canvas.drawPath(path, linePaint)

        // Draw simple start and end dots
        points.forEachIndexed { i, pt ->
            val x = getX(i)
            val y = getY(pt.weight)

            if (i == 0 || i == points.size - 1) {
                dotPaint.color = primaryColor
                canvas.drawCircle(x, y, dpToPx(5f), dotBorderPaint)
                canvas.drawCircle(x, y, dpToPx(3.5f), dotPaint)
            }
        }
    }

    private fun formatWeight(value: Float): String {
        return if (value % 1.0f == 0.0f) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
    }
}
