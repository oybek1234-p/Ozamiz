package uz.oybek.ozamiz.ui.utils

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.LayoutGuidedTourCardBinding

class GuidedTourOverlay(context: Context) : FrameLayout(context) {

    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.black)
        alpha = 210 // ~82% opacity
    }

    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private var targetRect = RectF()
    private var cornerRadius = 0f

    private val binding = LayoutGuidedTourCardBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_HARDWARE, null)
        isClickable = true
        isFocusable = true
        elevation = 1000f
    }

    override fun onDraw(canvas: Canvas) {
        // We use saveLayer to permit correct transparent cutout composition
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        canvas.drawRoundRect(targetRect, cornerRadius, cornerRadius, clearPaint)
        canvas.restoreToCount(saveCount)
    }

    fun showFor(activity: Activity, targetView: View, title: String, text: String, buttonText: String, onNext: () -> Unit) {
        val location = IntArray(2)
        targetView.getLocationInWindow(location)

        val padding = 12f * context.resources.displayMetrics.density

        targetRect.set(
            location[0].toFloat() - padding,
            location[1].toFloat() - padding,
            (location[0] + targetView.width).toFloat() + padding,
            (location[1] + targetView.height).toFloat() + padding
        )

        // Give a generous corner radius matching Ozamiz's rounded card aesthetic
        cornerRadius = 24f * context.resources.displayMetrics.density

        binding.textTitle.text = title
        binding.textDescription.text = text
        binding.btnNext.text = buttonText
        binding.btnNext.setOnClickListener {
            hide(onNext)
        }

        // Add to view hierarchy first
        val decorView = activity.window.decorView as ViewGroup
        decorView.addView(this, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Position the card nicely after layout mapping
        val screenHeight = resources.displayMetrics.heightPixels
        post {
            val isTargetInTopHalf = targetRect.centerY() < (screenHeight / 2)
            if (isTargetInTopHalf) {
                // Target is up top, show card below
                binding.cardContainer.y = targetRect.bottom + padding + 16f
            } else {
                // Target is down below, show card above
                binding.cardContainer.y = targetRect.top - binding.cardContainer.height - padding - 16f
            }

            // Smooth fade-in
            alpha = 0f
            animate().alpha(1f).setDuration(250).start()
        }
    }

    private fun hide(onNext: () -> Unit) {
        animate().alpha(0f).setDuration(200).withEndAction {
            removeImmediately()
            onNext()
        }.start()
    }

    fun removeImmediately() {
        (parent as? ViewGroup)?.removeView(this)
    }
}
