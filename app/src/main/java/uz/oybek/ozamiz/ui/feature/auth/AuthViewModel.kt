package uz.oybek.ozamiz.ui.feature.auth

import androidx.annotation.Keep
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.AuthCredential
import uz.oybek.ozamiz.data.auth.AuthRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.user.UserAuthRepository
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.ui.utils.postVal

@Keep
sealed class SendState {
    data class Input(val input: String? = null) : SendState()
    data object Sending : SendState()
    data class Success(val code: String?) : SendState()
    data class Error(val exception: Exception) : SendState()
}

@Keep
data class InputCode(val code: String?) {
    fun isValid() = code?.length == 6
}

@Keep
data class SendInfo(val code: String, val phone: String)

@Keep
sealed class VerifyState {
    data class Input(val input: InputCode? = null) : VerifyState()
    data object Verifying : VerifyState()
    data object Success : VerifyState()
    data class Error(val exception: Exception) : VerifyState()
}

class AuthViewModel : ViewModel() {

    val sendState = MutableLiveData<SendState>(SendState.Input(""))
    val verifyState = MutableLiveData<VerifyState>(VerifyState.Input(null))

    fun signInGoogle(
        credential: AuthCredential, verified: (done: Boolean) -> Unit
    ) {
        verifyState.postVal(VerifyState.Verifying)
        viewModelScope.launch {
            val result = AuthRepository.signInWithCredential(credential)
            if (result.isSuccess) {
                verified.invoke(true)
            } else {
                verified.invoke(false)
            }
        }
    }

    fun signInEmail(
        email: String, password: String, result: (Result<Any>) -> Unit
    ) {
        viewModelScope.launch {
            val res = AuthRepository.signInWithEmailAndPassword(email, password)
            if (res.isSuccess) result.invoke(Result.success(Any()))
            else result.invoke(Result.failure(res.exceptionOrNull() ?: Exception("Unknown fail")))
        }
    }

    fun signUpEmail(
        email: String, password: String, result: (Result<Any>) -> Unit
    ) {
        viewModelScope.launch {
            val res = AuthRepository.createUserWithEmailAndPassword(email, password)
            if (res.isSuccess) result.invoke(Result.success(Any()))
            else result.invoke(Result.failure(res.exceptionOrNull() ?: Exception("Unknown fail")))
        }
    }

    suspend fun authFirebaseUser(): User {
        return UserAuthRepository.authFirebaseUser()
    }
}
