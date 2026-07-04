package uz.oybek.ozamiz.data.firebase

import com.google.firebase.database.DataSnapshot
import android.util.Log
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.streak.StreakInfo

fun <T> DataSnapshot.getValueSafe(type: Class<T>, default: T? = null): T? {
    if (type == User::class.java) {
        return this.toUserSafe() as? T
    }
    return try {
        val value = getValue(type)
        Log.d("FirebaseUtils", "Successfully parsed ${type.simpleName} for key: $key")
        value
    } catch (e: Exception) {
        Log.e("FirebaseUtils", "Error parsing ${type.simpleName} for key: $key. Raw value: ${this.value}", e)
        default
    }
}

fun DataSnapshot.toUserSafe(): User {
    val u = User()
    u.uid = this.key ?: ""
    u.name = child("name").value as? String ?: ""
    u.phoneNumber = child("phoneNumber").value as? String ?: ""
    u.gmail = child("gmail").value as? String ?: ""
    u.lastSeenTime = (child("lastSeenTime").value as? Number)?.toLong() ?: 0L
    u.unreadMessages = (child("unreadMessages").value as? Number)?.toInt() ?: 0
    u.premium = child("premium").value as? Boolean ?: false
    u.premiumDate = (child("premiumDate").value as? Number)?.toLong() ?: 0L
    u.isPremiumYearly = child("isPremiumYearly").value as? Boolean ?: false
    u.isMonthlyPremium = child("isMonthlyPremium").value as? Boolean ?: false
    u.gender = (child("gender").value as? Number)?.toInt() ?: -1
    u.region = child("region").value as? String ?: " "
    u.age = (child("age").value as? Number)?.toInt() ?: -1
    u.height = (child("height").value as? Number)?.toInt() ?: -1
    u.weight = (child("weight").value as? Number)?.toDouble() ?: -1.0
    u.targetWeight = (child("targetWeight").value as? Number)?.toDouble()
    u.startWeight = (child("startWeight").value as? Number)?.toDouble()
    u.goal = child("goal").value as? String ?: ""
    u.activityLevel = child("activityLevel").value as? String ?: "LIGHTLY_ACTIVE"
    u.targetCalories = (child("targetCalories").value as? Number)?.toInt() ?: 0
    u.targetProtein = (child("targetProtein").value as? Number)?.toInt() ?: 0
    u.targetFat = (child("targetFat").value as? Number)?.toInt() ?: 0
    u.targetCarbs = (child("targetCarbs").value as? Number)?.toInt() ?: 0
    u.targetWaterMl = (child("targetWaterMl").value as? Number)?.toInt() ?: 2500
    u.avatarUrl = child("avatarUrl").value as? String
    u.registerDate = child("registerDate").value
    u.paymentChecking = child("paymentChecking").value as? Boolean ?: false
    u.paymentCheckPhotoUrl = child("paymentCheckPhotoUrl").value as? String
    u.admin = child("admin").value as? Boolean ?: false
    u.scorePoints = (child("scorePoints").value as? Number)?.toInt() ?: 0
    u.lastTaskDate = child("lastTaskDate").value as? String

    u.problems = try {
        val problemsChild = child("problems")
        if (problemsChild.exists()) {
            problemsChild.children.mapNotNull { it.value?.toString() }
        } else null
    } catch (e: java.lang.Exception) {
        null
    }

    u.completedTaskIds = try {
        val completedChild = child("completedTaskIds")
        if (completedChild.exists()) {
            completedChild.children.mapNotNull { it.value?.toString() }
        } else emptyList()
    } catch (e: java.lang.Exception) {
        emptyList()
    }
    u.streakInfo = try {
        val streakChild = child("streakInfo")
        if (streakChild.exists()) {
            val lastActionDate = streakChild.child("lastActionDate").value as? String ?: ""
            val currentStreak = (streakChild.child("currentStreak").value as? Number)?.toInt() ?: 0
            val bestStreak = (streakChild.child("bestStreak").value as? Number)?.toInt() ?: 0
            val activeDays = (streakChild.child("activeDays").value as? Number)?.toInt() ?: 0
            val streakGoal = (streakChild.child("streakGoal").value as? Number)?.toInt() ?: 7
            val activityHistory = streakChild.child("activityHistory").children.mapNotNull { it.value?.toString() }
            StreakInfo(
                lastActionDate = lastActionDate,
                currentStreak = currentStreak,
                bestStreak = bestStreak,
                activeDays = activeDays,
                streakGoal = streakGoal,
                activityHistory = activityHistory
            )
        } else null
    } catch (e: java.lang.Exception) {
        Log.e("FirebaseUtils", "Error parsing streakInfo", e)
        null
    }
    return u
}

val currentUserId: String
    get() = auth.currentUser?.uid ?: ""

fun generateKey() = System.currentTimeMillis().toString()