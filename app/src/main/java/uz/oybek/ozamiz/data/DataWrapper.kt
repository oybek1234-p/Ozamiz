package uz.oybek.ozamiz.data

sealed class DataWrapper<T> {
    class Loading<T> : DataWrapper<T>()
    data class Success<T>(val data: T) : DataWrapper<T>()
    data class Error<T>(val error: String) : DataWrapper<T>()
}