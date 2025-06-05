package uz.oybek.ozamiz.util // Yoki boshqa utility paket

import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.UserRepository
import java.util.concurrent.TimeUnit

object LivesManager {

    const val MAX_LIVES = 5
    val REFILL_INTERVAL_MINUTES = 240 // Har 4 soatda bitta jon (yoki boshqa interval)
    // Yoki kuniga bir marta to'liq to'ldirish uchun:
    // private val FULL_REFILL_INTERVAL_HOURS = 24

    /**
     * Foydalanuvchining jonlarini tekshiradi va kerak bo'lsa to'ldiradi.
     * Bu funksiya ilova ishga tushganda yoki dars boshlashdan oldin chaqirilishi kerak.
     * @return Yangilangan User obyektini qaytaradi (agar o'zgarish bo'lsa) yoki null.
     */
    fun checkAndRefillLives(): User? {
        val currentUser = LocalUser.user
        if (!currentUser.valid || currentUser.lessonLives >= MAX_LIVES) {
            return null // Jonlar to'liq yoki foydalanuvchi valid emas
        }

        val currentTime = System.currentTimeMillis()
        val lastRefillTime = currentUser.lastLifeRefillTimestamp

        if (lastRefillTime == 0L) { // Birinchi marta yoki eski foydalanuvchi
            val updatedUser = currentUser.copy(
                lessonLives = MAX_LIVES,
                lastLifeRefillTimestamp = currentTime
            )
            updateUserInLocalAndRemote(updatedUser)
            return updatedUser
        }

        val minutesPassed = TimeUnit.MILLISECONDS.toMinutes(currentTime - lastRefillTime)
        val livesToRefill = (minutesPassed / REFILL_INTERVAL_MINUTES).toInt()

        if (livesToRefill > 0) {
            val newLives = (currentUser.lessonLives + livesToRefill).coerceAtMost(MAX_LIVES)
            if (newLives > currentUser.lessonLives) {
                val newRefillTimestamp = if (newLives == MAX_LIVES) {
                    currentTime // To'liq to'ldirilgan bo'lsa, joriy vaqt
                } else {
                    // Oxirgi to'liq to'ldirilgan vaqt + qisman to'ldirilgan vaqt
                    lastRefillTime + TimeUnit.MINUTES.toMillis(livesToRefill * REFILL_INTERVAL_MINUTES.toLong())
                }
                val updatedUser = currentUser.copy(
                    lessonLives = newLives,
                    lastLifeRefillTimestamp = newRefillTimestamp
                )
                updateUserInLocalAndRemote(updatedUser)
                return updatedUser
            }
        }
        return null // O'zgarish yo'q
    }

    /**
     * Darsda jon ishlatilganda chaqiriladi.
     * @return Jon qolgan bo'lsa true, aks holda false.
     */
    fun consumeLife(): Boolean {
        val currentUser = LocalUser.user
        if (currentUser.lessonLives > 0) {
            val updatedUser = currentUser.copy(lessonLives = currentUser.lessonLives - 1)
            // Agar bu birinchi marta jon ishlatilishi bo'lsa va lastLifeRefillTimestamp 0 bo'lsa,
            // uni joriy vaqtga o'rnatamiz (qachondan boshlab hisoblash uchun).
            if (currentUser.lastLifeRefillTimestamp == 0L && currentUser.lessonLives == MAX_LIVES) {
                updatedUser.lastLifeRefillTimestamp = System.currentTimeMillis()
            }
            updateUserInLocalAndRemote(updatedUser)
            return true
        }
        return false
    }

    private fun updateUserInLocalAndRemote(user: User) {
        LocalUser.user = user // Mahalliy keshlangan foydalanuvchini yangilash
        LocalUser.saveUser()    // O'zgarishlarni SharedPreferences ga saqlash

        // Firebase'da ham yangilash
        UserRepository.updateUser { dbUser ->
            dbUser.lessonLives = user.lessonLives
            dbUser.lastLifeRefillTimestamp = user.lastLifeRefillTimestamp
        }
    }

    // Agar premium foydalanuvchilar uchun cheksiz jonlar bo'lsa:
    fun hasInfiniteLives(): Boolean {
        return LocalUser.user.premium
    }

    fun getCurrentLives(): Int {
        return if (hasInfiniteLives()) MAX_LIVES else LocalUser.user.lessonLives // Premium uchun har doim to'liq
    }
}