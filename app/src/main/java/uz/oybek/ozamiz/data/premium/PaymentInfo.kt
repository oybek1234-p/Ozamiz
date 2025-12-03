package uz.oybek.ozamiz.data.premium

import com.google.firebase.Firebase
import com.google.firebase.database.database

object PaymentInfo {

    private val reference =
        Firebase.database.getReference("paymentCard")

    fun loadPaymentCard(result: (card: String) -> Unit) {
        reference.get().addOnSuccessListener {
            val card = it.value?.toString()
            result.invoke(card ?: "")
        }.addOnFailureListener {
            result.invoke("5614688707870480")
        }
    }
}