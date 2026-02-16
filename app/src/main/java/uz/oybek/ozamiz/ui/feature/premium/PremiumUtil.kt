package uz.oybek.ozamiz.ui.feature.premium

import com.google.firebase.Firebase
import com.google.firebase.database.database
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PremiumUtil {

    private const val PREMIUM_DEFAULT_PRICE = 50000L

    private val premiumReference = Firebase.database.getReference("premiumPrice")

    fun loadPremiumPrice(done: (premiumPrice: Long) -> Unit) {
        premiumReference.get().addOnSuccessListener {
            val price = it.value.toString().toLongOrNull() ?: PREMIUM_DEFAULT_PRICE
            done.invoke(price)
        }.addOnFailureListener {
            done.invoke(PREMIUM_DEFAULT_PRICE)
        }
    }
}

fun User.premiumExpired(): Boolean {
//    val premiumGotDate = premiumDate
//    val currentDate = System.currentTimeMillis()
//    val days = DateUtils.getS(premiumGotDate, currentDate)
//    return days > 31
    return false
}

fun User.getPremiumExpireDate(): String {
    val premiumBoughtTimeInMillis: Long = premiumDate
    val subscriptionDurationInDays = 30
    val purchaseDate = Date(premiumBoughtTimeInMillis)

    val calendar = Calendar.getInstance()
    calendar.time = purchaseDate

    // Add the subscription duration to the purchase date
    calendar.add(Calendar.DAY_OF_YEAR, subscriptionDurationInDays)

    // Get the expiration date
    val expirationDate = calendar.time

    // Define the desired date format
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    // Format the expiration date
    val formattedExpirationDate = dateFormat.format(expirationDate)

    return formattedExpirationDate
}