package uz.oybek.ozamiz.util

import uz.oybek.ozamiz.data.utils.recordException

fun handleException(e: Exception) {
    e.printStackTrace()
    recordException(e)
}

inline fun <T> returnResult(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        handleException(e)
        Result.failure(e)
    }
}