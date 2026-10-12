package uz.oybek.ozamiz.ui.utils

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.textfield.TextInputLayout

/**
 * Kotlin extensions for View operations.
 * Reduces boilerplate and improves code readability.
 */

fun View.show() {
    isVisible = true
}

fun View.hide() {
    isVisible = false
}

fun View.gone() {
    visibility = View.GONE
}

fun View.setVisibleIf(condition: Boolean) {
    isVisible = condition
}

fun View.setGoneIf(condition: Boolean) {
    visibility = if (condition) View.GONE else View.VISIBLE
}

fun View.visibleOrGone(visible: Boolean) {
    isVisible = visible
}

inline fun View.onClick(crossinline action: () -> Unit) {
    setOnClickListener { action() }
}

inline fun View.onLongClick(crossinline action: () -> Boolean) {
    setOnLongClickListener { action() }
}

fun View.setOnSafeClickListener(interval: Int = 1000, action: (View) -> Unit) {
    var lastClickTime: Long = 0
    setOnClickListener {
        if (System.currentTimeMillis() - lastClickTime < interval) return@setOnClickListener
        lastClickTime = System.currentTimeMillis()
        action(it)
    }
}

fun TextView.animateIntegerText(
    fromValue: Int,
    toValue: Int,
    duration: Long = 450L
) {
    if (fromValue == toValue) {
        text = toValue.toString()
        return
    }
    ValueAnimator.ofInt(fromValue, toValue).apply {
        this.duration = duration
        interpolator = DecelerateInterpolator()
        addUpdateListener { anim ->
            text = (anim.animatedValue as Int).toString()
        }
    }.start()
}

fun TextView.makeExpandable() {
    val currentLine = maxLines
    var expanded = false
    setOnClickListener {
        maxLines = if (expanded) {
            currentLine
        } else {
            Int.MAX_VALUE
        }
        expanded = !expanded
    }
}

fun TextInputLayout.setEditTextErrorIf(errorText: String, condition: () -> Boolean) {
    editText?.setOnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) {
            if (condition.invoke()) {
                error = errorText
                isErrorEnabled = true
            }
        } else {
            error = null
            isErrorEnabled = false
        }
    }
}

fun TextInputLayout.textOrEmpty(): String = editText?.text?.toString()?.trim().orEmpty()

fun TextInputLayout.intOrNull(): Int? = editText?.text?.toString()?.toIntOrNull()

fun TextInputLayout.doubleOrNull(): Double? =
    editText?.text?.toString()?.replace(',', '.')?.toDoubleOrNull()

