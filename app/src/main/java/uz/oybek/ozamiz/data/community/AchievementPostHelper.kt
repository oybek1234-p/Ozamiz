package uz.oybek.ozamiz.data.community

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.utils.DateUtils

object AchievementPostHelper {

    private fun prefs(): SharedPreferences =
        appContext.getSharedPreferences("achievement_posts", Context.MODE_PRIVATE)

    private fun todayKey(key: String): String {
        val today = DateUtils.getFormattedDateString(java.util.Date(), "yyyy-MM-dd")
        return "${key}_$today"
    }

    private fun hasPostedToday(key: String): Boolean = prefs().getBoolean(todayKey(key), false)

    private fun markPostedToday(key: String) {
        prefs().edit().putBoolean(todayKey(key), true).apply()
    }

    private fun isUserVerified(): Boolean {
        val user = UserRepository.user
        return user.valid && !user.uid.isNullOrBlank()
    }

    private fun buildPost(title: String, description: String): CommunityPost? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val user = UserRepository.user
        return CommunityPost(
            userId = uid,
            userName = user.name.ifBlank { "Foydalanuvchi" },
            userAvatarUrl = user.avatarUrl,
            title = title,
            description = description,
            verified = false,
            isVisible = false
        )
    }

    fun onWaterGoalReached(consumedMl: Int, targetMl: Int) {
        if (targetMl <= 0 || consumedMl < targetMl) return
        if (!isUserVerified()) return
        if (hasPostedToday("water_goal")) return
        markPostedToday("water_goal")

        val liters = String.format("%.1f", targetMl / 1000f)
        val messages = listOf(
            "💧 Bugun kunlik suv me'yorini to'ldirdi! $liters litr suv — bu katta yutuq!",
            "🏆 $liters litr — maqsad bajarildi! Suv ichish odati sog'liq kaliti.",
            "✅ Ajoyib! Bugun $liters litr suv ichib, kunlik normani to'ldirdi!",
            "💪 $liters litr suv — kunlik me'yor to'liq bajarildi. Davom eting!"
        )
        val post = buildPost(
            title = "Suv me'yori bajarildi 💧",
            description = messages.random()
        ) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            CommunityPostRepository.addPost(post)
        }
    }

    fun onExerciseGoalReached(burntCalories: Int, targetCalories: Int) {
        if (targetCalories <= 0 || burntCalories < targetCalories) return
        if (!isUserVerified()) return
        if (hasPostedToday("exercise_goal")) return
        markPostedToday("exercise_goal")

        val messages = listOf(
            "🔥 Bugun $burntCalories kaloriya yoqdi! Kunlik mashq maqsadi to'liq bajarildi!",
            "💪 Ajoyib! $burntCalories kaloriya — bu haqiqiy iroda va shijoat!",
            "🏃 $burntCalories kaloriya yoqib, bugungi mashq maqsadiga yetdi. Zo'r!",
            "⚡ $burntCalories kaloriya yog' yoqildi — bugungi mashq rekord bo'ldi!"
        )
        val post = buildPost(
            title = "Mashq maqsadi bajarildi 🏆",
            description = messages.random()
        ) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            CommunityPostRepository.addPost(post)
        }
    }

    fun onWeightDropped(previousWeightKg: Double, newWeightKg: Double) {
        val dropped = previousWeightKg - newWeightKg
        if (dropped <= 0.0) return
        if (!isUserVerified()) return

        val droppedStr = String.format("%.1f", dropped)
        val prevStr = String.format("%.1f", previousWeightKg)
        val newStr = String.format("%.1f", newWeightKg)

        val title = "-$droppedStr kg 📉"

        val description = when {
            dropped >= 5.0 -> listOf(
                "🏆 Ulkan natija! $prevStr → $newStr kg. Bu hayot o'zgartiruvchi yutuq!",
                "🔥 $prevStr dan $newStr kg'ga tushdi. Kuchli iroda va barqaror mehnat natijasidir!",
                "💥 $prevStr → $newStr kg. $droppedStr kilogram — haqiqiy g'alaba!"
            )
            dropped >= 2.0 -> listOf(
                "💪 $prevStr → $newStr kg. Harakatlar o'z natijasini berayapti, davom eting!",
                "👏 $droppedStr kg kamaydi — $newStr kg. Sog'lom yo'l bilan oldinga!",
                "✅ $prevStr dan $newStr kg — yaxshi natija! Muntazamlik ishlayapti."
            )
            dropped >= 1.0 -> listOf(
                "🌟 $prevStr → $newStr kg. Har bir qadam maqsad sari katta qadam!",
                "📊 $newStr kg — kun sayin yaxshilanmoqda. Shoshmasdan lekin aniq!",
                "🎯 $droppedStr kg tushdi! $prevStr → $newStr kg. Yo'lda davom eting!"
            )
            else -> listOf(
                "📉 $prevStr → $newStr kg. Kichik qadamlar ham katta yo'lning boshlang'ichidir!",
                "💡 $newStr kg — natijalar ko'rinmoqda. Davom eting!",
                "🔔 $prevStr → $newStr kg. Har gramm hisoba — oldinga!"
            )
        }.random()

        val post = buildPost(title = title, description = description) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            CommunityPostRepository.addPost(post)
        }
    }
}
