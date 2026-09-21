package uz.oybek.ozamiz.data.utils

import android.content.Context
import android.util.Log
import android.util.Patterns
import uz.oybek.ozamiz.data.firebase.crashlytics
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

fun Double.formatToDecimal(digits: Int = 1): String {
    return String.format(Locale.US, "%.${digits}f", this)
}

fun Double.roundTo1Decimal(): Double {
    if (this.isNaN() || this.isInfinite() || Math.abs(this) < 0.0001) return 0.0
    return Math.round(this * 10.0) / 10.0
}

fun Double?.roundTo1Decimal(): Double? {
    return this?.roundTo1Decimal()
}

fun Double.toWeightString(): String {
    return "${this.formatToDecimal(1)} kg"
}

fun Int.toCalorieString(): String {
    return "$this kcal"
}

fun String.isValidEmail(): Boolean {
    return this.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isValidPhone(): Boolean {
    val phoneRegex = "^[+]?[0-9]{9,15}$".toRegex()
    return this.isNotBlank() && phoneRegex.matches(this)
}

fun String.toIntOrDefault(default: Int = 0): Int {
    return this.toIntOrNull() ?: default
}

fun String.toDoubleOrDefault(default: Double = 0.0): Double {
    return this.toDoubleOrNull() ?: default
}

fun String.capitalizeUz(): String {
    if (this.isEmpty()) return this
    return this.substring(0, 1).uppercase(Locale.getDefault()) + this.substring(1)
}

fun logMessage(message: Any?, tag: String = "XLogs") {
    Log.d(tag, message?.toString() ?: "null")
}

fun recordException(throwable: Throwable?) {
    if (throwable == null || throwable is CancellationException) return
    try {
        crashlytics.recordException(throwable)
    } catch (_: Exception) {
    }
}

fun Throwable.record() {
    recordException(this)
}

