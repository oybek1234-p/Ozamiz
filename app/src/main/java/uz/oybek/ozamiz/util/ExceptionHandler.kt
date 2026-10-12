package uz.oybek.ozamiz.util

import java.util.concurrent.CancellationException
import kotlin.coroutines.cancellation.CancellationException as CoroutineCancellationException

fun handleException(e: Throwable) {
    if (e is CancellationException || e is CoroutineCancellationException) return
    recordException(e)
}

inline fun <T> returnResult(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: CoroutineCancellationException) {
        throw e
    } catch (e: Exception) {
        handleException(e)
        Result.failure(e)
    }
}