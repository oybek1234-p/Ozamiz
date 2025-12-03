package uz.oybek.ozamiz.data.utils

sealed class DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>()
    data object Error : DataResult<Nothing>()
}