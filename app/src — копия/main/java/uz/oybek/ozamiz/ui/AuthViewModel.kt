package uz.oybek.ozamiz.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.AuthCredential
import uz.oybek.ozamiz.data.firebase.FirebaseInstances

sealed class SendState {
    data class Input(val input: String? = null) : SendState()
    data object Sending : SendState()
    data class Success(val code: String?) : SendState()
    data class Error(val exception: Exception) : SendState()
}

data class InputCode(val code: String?) {
    fun isValid() = code?.length == 6
}

data class SendInfo(val code: String, val phone: String)

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
        FirebaseInstances.auth.signInWithCredential(credential).addOnSuccessListener {
            verified.invoke(true)
        }.addOnFailureListener {
            verified.invoke(false)
        }
    }
}
