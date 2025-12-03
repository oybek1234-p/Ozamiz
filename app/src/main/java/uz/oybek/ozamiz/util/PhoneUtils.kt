package uz.oybek.ozamiz.util

import android.content.Intent
import androidx.core.net.toUri
import uz.oybek.ozamiz.appContext

object PhoneUtils {

    const val plus = "+"
    private const val code = "998"
    private const val MASK = "(##) ###-##-##"

    fun openPhoneCall(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = "tel:$phone".toUri()
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            appContext.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun formatPhoneNumber(phone: String): String {
        var exactPhone = phone.replaceFirst(plus, "")
        if (exactPhone.length > 9) {
            exactPhone = exactPhone.replaceFirst(code, "")
        }
        exactPhone = exactPhone.trim()

        val builder = StringBuilder().apply {
            append(plus)
            append(code)
            append(exactPhone)
        }
        return builder.toString()
    }
}
