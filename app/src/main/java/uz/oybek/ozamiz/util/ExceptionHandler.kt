package uz.oybek.ozamiz.util

import uz.oybek.ozamiz.data.utils.recordException

fun handleException(e: Exception) {
    if (e is java.util.concurrent.CancellationException) return
    e.printStackTrace()
    recordException(e)
}

inline fun <T> returnResult(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: java.util.concurrent.CancellationException) {
        throw e
    } catch (e: Exception) {
        handleException(e)
        Result.failure(e)
    }
}