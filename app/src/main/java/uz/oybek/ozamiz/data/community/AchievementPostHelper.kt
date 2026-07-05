package uz.oybek.ozamiz.data.community

import uz.oybek.ozamiz.data.community.model.PostType.MANUAL
import uz.oybek.ozamiz.data.community.model.PostType.WATER
import uz.oybek.ozamiz.data.community.model.PostType.WEIGHT_LOSS

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import uz.oybek.ozamiz.data.firebase.firestore
import uz.oybek.ozamiz.data.firebase.auth
import uz.oybek.ozamiz.data.firebase.userWeightLogsCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.ui.feature.premium.PremiumUtils
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.logs.ExerciseLogRepository
import uz.oybek.ozamiz.data.logs.model.ExerciseLog
import uz.oybek.ozamiz.data.logs.model.WeightLog
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.utils.sortByRecent

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

    private fun buildPost(
        title: String,
        description: String,
        type: String = MANUAL,
        photos: List<String> = emptyList()
    ): CommunityPost? {
        val uid = auth.currentUser?.uid ?: return null
        val user = UserRepository.user
        return CommunityPost(
            userId = uid,
            userName = user.name.ifBlank { "Foydalanuvchi" },
            userAvatarUrl = user.avatarUrl,
            title = title,
            description = description,
            type = type,
            photos = photos,
            verified = true,
            isVisible = true,
            isPremiumUser = PremiumUtils.isPremium(user)
        )
    }

    fun onWaterGoalReached(consumedMl: Int, targetMl: Int) {
        if (targetMl !in 1..consumedMl) return
        if (!isUserVerified()) return
        if (hasPostedToday("water_goal")) return
        markPostedToday("water_goal")

        val liters = String.format("%.1f", targetMl / 1000f)
        val messages = listOf(
            "💧 Bugun kunlik suv normasi bajarildi. Suv metobolizmni tezlashtirib, ozishga yordam beradi.",
            "🌊 Suv ichish ishtahani bo'g'ib, ortiqcha ovqatlanishdan saqlaydi.",
            "✅ Kunlik norma to'ldirildi. Toza suv terini yoshartiradi va organizmni tozalaydi.",
            "💪 Suv mushaklar ishlashi va quvvat uchun muhim manba hisoblanadi.",
            "💡 Suv ichish miya faoliyatini yaxshilaydi va charchoqni oladi.",
            "🏃 Bugun maqsadga erishildi. Suv bo'g'imlar sog'lig'i va harakatlanish uchun juda muhim.",
            "✨ Suv ichish yog'larni parchalash jarayonini sezilarli darajada tezlashtiradi.",
            "🎯 Norma bajarildi. Har bir piyola suv sog'lom qomatga yaqinlashtiradi.",
            "🌟 Suv ichish odati organizmdagi barcha tana a'zolari ishini muvozanatga soladi.",
            "🔔 Sog'lom hayot doimiy ravishda kerakli miqdorda suv ichish demakdir."
        )
        val post = buildPost(
            title = "$liters litr suv ichildi! 💧",
            description = messages.random(),
            type = WATER
        ) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            CommunityPostRepository.addPost(post)
        }
    }

    fun onExerciseGoalReached(burntCalories: Int, targetCalories: Int) {
        if (targetCalories !in 1..burntCalories) return
        if (!isUserVerified()) return
        if (hasPostedToday("exercise_goal")) return
        markPostedToday("exercise_goal")

        val uid = auth.currentUser?.uid ?: return
        val today = DateUtils.getFormattedDateString(java.util.Date(), "yyyy-MM-dd")
        
        CoroutineScope(Dispatchers.IO).launch {
            val result = ExerciseLogRepository.getLogsFlow(uid, today).first()
            val logs = when(result) {
                is DataWrapper.Success -> result.data
                else -> emptyList()
            }

            val exerciseNames = logs.map { it.exerciseType }.distinct().take(3).joinToString(", ")

            val fatBurnedGr = (burntCalories / 7.7).toInt()
            val prefix = if (exerciseNames.isNotEmpty()) "$exerciseNames. " else ""
            
            val resultTitle = "$prefix$burntCalories kkal yoqildi! 🔥"
            val description = "Bugun o'rtacha $fatBurnedGr gr yog' kamaydi."
            
            val post = buildPost(
                title = resultTitle,
                description = description,
                type = MANUAL
            ) ?: return@launch
            
            CommunityPostRepository.addPost(post)
        }
    }

    fun onWeightDropped(previousWeightKg: Double, newWeightKg: Double) {
        val dropped = previousWeightKg - newWeightKg
        if (dropped <= 0.0) return
        if (!isUserVerified()) return

        val uid = auth.currentUser?.uid ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            val logsSnapshot = firestore.userWeightLogsCollection(uid)
                .orderBy(WeightLog::timestamp.name, com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(2)
                .get()
                .await()
            val logs = logsSnapshot.toObjects(WeightLog::class.java).sortByRecent { it.timestamp }
            
            val photos = if (logs.size >= 2) {
                listOf(logs[1].photoUrl, logs[0].photoUrl).filter { it.isNotEmpty() }
            } else if (logs.isNotEmpty()) {
                listOf(logs[0].photoUrl).filter { it.isNotEmpty() }
            } else {
                emptyList()
            }

            val prevStr = if (previousWeightKg % 1.0 == 0.0) previousWeightKg.toInt().toString() else String.format("%.1f", previousWeightKg)
            val newStr = if (newWeightKg % 1.0 == 0.0) newWeightKg.toInt().toString() else String.format("%.1f", newWeightKg)
            val droppedStr = if (dropped % 1.0 == 0.0) dropped.toInt().toString() else String.format("%.1f", dropped)
            
            val title = "${droppedStr}kg vazn tashlandi! ${prevStr}kg dan ${newStr}kg ga."
            val messages = listOf("Tabriklaymiz! 🎉", "Ajoyib natija! 🏆", "Qoyil! 💪")
            val description = messages.random()

            val post = buildPost(
                title = title,
                description = description,
                type = WEIGHT_LOSS,
                photos = photos
            ) ?: return@launch

            CommunityPostRepository.addPost(post)
        }
    }
}
