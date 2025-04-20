package uz.oybek.ozamiz

import android.widget.EditText
import com.github.vacxe.phonemask.PhoneMaskManager


object PhoneUtils {

    const val plus = "+"
    private const val code = "998"
    private const val MASK = "(##) ###-##-##"

    fun getCodeFromSms(message: String): String {
        try {
            return message
                .substringAfter(":")
                .substringBefore(".")
                .trim()
        } catch (e: java.lang.Exception) {
            ExceptionHandler.handle(e)
        }
        return ""
    }

    fun phoneMask(editText: EditText?): PhoneMaskManager? {
        if (editText == null) return null
        return PhoneMaskManager().withMask(MASK)
            .withRegion("$plus$code")
            .bindTo(editText)
    }

    //Formats phone
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