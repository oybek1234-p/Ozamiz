package uz.oybek.ozamiz.ui.feature.premium

import uz.oybek.ozamiz.data.user.model.User
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val MONTHLY_DAYS = 30L
private const val YEARLY_DAYS = 365L
private const val MS_PER_DAY = 24L * 60 * 60 * 1000

fun User.subscriptionDurationDays(): Long {
    return when {
        isPremiumYearly -> YEARLY_DAYS
        isMonthlyPremium -> MONTHLY_DAYS
        else -> MONTHLY_DAYS
    }
}

fun User.premiumExpired(): Boolean {
    if (!premium) return true
    val durationMs = subscriptionDurationDays() * MS_PER_DAY
    return (System.currentTimeMillis() - premiumDate) > durationMs
}

fun User.isPremium(): Boolean {
    return premium && !premiumExpired()
}

fun User.isDietologTrialActive(): Boolean {
    val regDate = registerDateLong ?: return true
    val diffMs = System.currentTimeMillis() - regDate
    val threeDaysMs = 3L * MS_PER_DAY
    val currentStreak = streakInfo?.currentStreak ?: 0
    return diffMs <= threeDaysMs && currentStreak <= 3
}

fun User.isPremiumOrTrial(): Boolean {
    return isPremium() || isDietologTrialActive()
}

fun User.getPremiumExpireDate(): String {
    val purchaseDate = Date(premiumDate)
    val calendar = Calendar.getInstance()
    calendar.time = purchaseDate
    calendar.add(Calendar.DAY_OF_YEAR, subscriptionDurationDays().toInt())
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return dateFormat.format(calendar.time)
}
