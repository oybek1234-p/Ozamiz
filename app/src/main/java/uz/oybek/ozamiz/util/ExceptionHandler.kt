package uz.oybek.ozamiz.util

import com.google.firebase.crashlytics.FirebaseCrashlytics

fun handleException(e: Exception) {
    e.printStackTrace()
    FirebaseCrashlytics.getInstance().recordException(e)
}

inline fun <T> returnResult(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        handleException(e)
        Result.failure(e)
    }
}