package uz.oybek.ozamiz.data.premium

import uz.oybek.ozamiz.data.firebase.FirebaseInstances

object PaymentInfo {

    private val reference = FirebaseInstances.database.getReference("paymentCard")

    fun loadPaymentCard(result: (card: String) -> Unit) {
        reference.get().addOnSuccessListener {
            val card = it.value?.toString()
            result.invoke(card ?: "")
        }.addOnFailureListener {
            result.invoke("5614688707870480")
        }
    }
}