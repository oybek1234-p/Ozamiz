package uz.oybek.ozamiz.ui.feature.premium

import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.ui.utils.showToast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PremiumUtils {
    private const val MONTHLY_DAYS = 30L
    private const val YEARLY_DAYS = 365L
    private const val MS_PER_DAY = 24L * 60 * 60 * 1000

    fun subscriptionDurationDays(user: User): Long {
        return when {
            user.isPremiumYearly -> YEARLY_DAYS
            user.isMonthlyPremium -> MONTHLY_DAYS
            else -> MONTHLY_DAYS
        }
    }

    fun premiumExpired(user: User): Boolean {
        if (!user.premium) return true
        val durationMs = subscriptionDurationDays(user) * MS_PER_DAY
        return (System.currentTimeMillis() - user.premiumDate) > durationMs
    }

    fun isPremium(user: User): Boolean {
        return user.premium && !premiumExpired(user)
    }

    fun getPremiumExpireDate(user: User): String {
        val purchaseDate = Date(user.premiumDate)
        val calendar = Calendar.getInstance()
        calendar.time = purchaseDate
        calendar.add(Calendar.DAY_OF_YEAR, subscriptionDurationDays(user).toInt())
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }
}

fun User.isPremium(): Boolean = PremiumUtils.isPremium(this)
fun User.getPremiumExpireDate(): String = PremiumUtils.getPremiumExpireDate(this)