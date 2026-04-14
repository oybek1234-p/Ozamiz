package uz.oybek.ozamiz.ui.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object NumberFormattingUtils {

    fun formatCardNumber(number: String?): String {
        if (number == null) return "**** **** **** ****"
        val cleanNumber = number.replace(" ", "")
        return cleanNumber.chunked(4).joinToString(" ")
    }

    fun formatPrice(price: Long?): String {
        if (price == null) return "0"
        val symbols = DecimalFormatSymbols(Locale.getDefault())
        symbols.groupingSeparator = ' '
        val df = DecimalFormat("#,###", symbols)
        return df.format(price)
    }
}
