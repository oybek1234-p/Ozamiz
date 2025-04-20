package uz.oybek.ozamiz

import android.util.Log
import com.google.firebase.BuildConfig

object ExceptionHandler {

    private const val APP_NAME = "Ozamiz"

    fun handle(e: Exception?, name: String? = null, needThrow: Boolean = false) {
        if (e == null) return
        if (e.message != null) {
            Log.e(name ?: APP_NAME, e.message!!)
        }
        if (needThrow) {
            throw e
        }
    }
}

fun handleException(
    exception: java.lang.Exception?, name: String? = null, needThrow: Boolean = false
) {
    ExceptionHandler.handle(exception, name, needThrow)
    if (BuildConfig.DEBUG) {
        throw exception!!
    }
}