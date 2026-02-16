package uz.oybek.ozamiz.ui.utils

import android.view.View
import androidx.core.view.isVisible

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

inline fun View.onClick(crossinline action: () -> Unit) {
    setOnClickListener { action() }
}

inline fun View.onLongClick(crossinline action: () -> Boolean) {
    setOnLongClickListener { action() }
}
