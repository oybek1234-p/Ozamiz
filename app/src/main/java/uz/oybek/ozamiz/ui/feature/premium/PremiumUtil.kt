package uz.oybek.ozamiz.ui.feature.premium

import uz.oybek.ozamiz.data.user.model.User
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun User.premiumExpired(): Boolean {
    if (!premium) return true
    val premiumGotDate = premiumDate
    val currentDate = System.currentTimeMillis()
    val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
    return (currentDate - premiumGotDate) > thirtyDaysInMillis
}

fun User.isPremium(): Boolean {
    return premium && !premiumExpired()
}

fun User.isPremiumOrTrial(): Boolean {
    if (isPremium()) return true
    val isTrialEnded = (streakInfo?.currentStreak ?: 0) > 3
    return !isTrialEnded
}

fun User.getPremiumExpireDate(): String {
    val premiumBoughtTimeInMillis: Long = premiumDate
    val subscriptionDurationInDays = 30
    val purchaseDate = Date(premiumBoughtTimeInMillis)
    val calendar = Calendar.getInstance()
    calendar.time = purchaseDate
    calendar.add(Calendar.DAY_OF_YEAR, subscriptionDurationInDays)
    val expirationDate = calendar.time
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val formattedExpirationDate = dateFormat.format(expirationDate)

    return formattedExpirationDate
}