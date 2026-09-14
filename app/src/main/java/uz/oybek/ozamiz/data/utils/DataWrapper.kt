package uz.oybek.ozamiz.data.utils

import androidx.annotation.Keep

@Keep
sealed class DataWrapper<out T> {
    data class Success<T>(val data: T) : DataWrapper<T>()
    data class Error(val error: String? = null) : DataWrapper<Nothing>()
}