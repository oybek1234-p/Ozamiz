package uz.oybek.ozamiz.data

import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object AppNotification {

    suspend fun loadToken()= suspendCoroutine { sus->
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            sus.resume(it)
        }.addOnFailureListener {
            sus.resume(null)
        }
    }
}