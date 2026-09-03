package uz.oybek.ozamiz.data.user.mapper

import com.google.firebase.database.DataSnapshot
import com.google.firebase.firestore.DocumentSnapshot
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.user.model.info.UserActivityLevel
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.utils.roundTo1Decimal
import uz.oybek.ozamiz.util.handleException

object UserMapper {

    fun toPublicMap(u: User): Map<String, Any?> {
        return mapOf(
            User::uid.name to u.uid,
            User::name.name to u.name,
            User::avatarUrl.name to u.avatarUrl,
            User::scorePoints.name to u.scorePoints,
            User::lastSeenTime.name to u.lastSeenTime,
            User::premium.name to u.premium,
            User::premiumDate.name to u.premiumDate,
            User::isPremiumYearly.name to u.isPremiumYearly,
            User::isMonthlyPremium.name to u.isMonthlyPremium,
            User::gender.name to u.gender,
            User::region.name to u.region,
            User::registerDate.name to u.registerDate,
            User::height.name to u.height,
            User::weight.name to u.weight.roundTo1Decimal(),
            User::startWeight.name to u.startWeight?.roundTo1Decimal(),
            User::targetWeight.name to u.targetWeight?.roundTo1Decimal(),
            User::age.name to u.age,
            User::goal.name to u.goal,
            User::targetCalories.name to u.targetCalories,

            User::streakInfo.name to u.streakInfo?.let { streak ->
                mapOf(
                    StreakInfo::currentStreak.name to streak.currentStreak,
                    StreakInfo::bestStreak.name to streak.bestStreak,
                    StreakInfo::lastActionDate.name to streak.lastActionDate,
                    StreakInfo::activeDays.name to streak.activeDays,
                    StreakInfo::streakGoal.name to streak.streakGoal
                )
            }
        )
    }

    fun toMap(u: User): Map<String, Any?> {
        return mapOf(
            User::uid.name to u.uid,
            User::name.name to u.name,
            User::phoneNumber.name to u.phoneNumber,
            User::gmail.name to u.gmail,
            User::lastSeenTime.name to u.lastSeenTime,
            User::unreadMessages.name to u.unreadMessages,
            User::premium.name to u.premium,
            User::premiumDate.name to u.premiumDate,
            User::isPremiumYearly.name to u.isPremiumYearly,
            User::isMonthlyPremium.name to u.isMonthlyPremium,
            User::gender.name to u.gender,
            User::region.name to u.region,
            User::age.name to u.age,
            User::height.name to u.height,
            User::weight.name to u.weight.roundTo1Decimal(),
            User::targetWeight.name to u.targetWeight?.roundTo1Decimal(),
            User::startWeight.name to u.startWeight?.roundTo1Decimal(),
            User::goal.name to u.goal,
            User::activityLevel.name to u.activityLevel,
            User::targetCalories.name to u.targetCalories,
            User::targetProtein.name to u.targetProtein,
            User::targetFat.name to u.targetFat,
            User::targetCarbs.name to u.targetCarbs,
            User::targetWaterMl.name to u.targetWaterMl,
            User::problems.name to u.problems,
            User::lastWeightLogTimestamp.name to u.lastWeightLogTimestamp,
            User::avatarUrl.name to u.avatarUrl,
            User::registerDate.name to u.registerDate,
            User::paymentChecking.name to u.paymentChecking,
            User::paymentCheckPhotoUrl.name to u.paymentCheckPhotoUrl,
            User::admin.name to u.admin,
            User::scorePoints.name to u.scorePoints,
            User::isRated.name to u.isRated,

            User::streakInfo.name to u.streakInfo?.let { streak ->
                mapOf(
                    StreakInfo::currentStreak.name to streak.currentStreak,
                    StreakInfo::bestStreak.name to streak.bestStreak,
                    StreakInfo::lastActionDate.name to streak.lastActionDate,
                    StreakInfo::activeDays.name to streak.activeDays,
                    StreakInfo::streakGoal.name to streak.streakGoal
                )
            }
        )
    }

    fun fromSnapshot(snapshot: DataSnapshot): User {
        val u = User()
        u.uid = snapshot.key ?: ""
        u.name = snapshot.child(User::name.name).value as? String ?: ""
        u.phoneNumber = snapshot.child(User::phoneNumber.name).value as? String ?: ""
        u.gmail = snapshot.child(User::gmail.name).value as? String ?: ""
        u.lastSeenTime = (snapshot.child(User::lastSeenTime.name).value as? Number)?.toLong() ?: 0L
        u.unreadMessages = (snapshot.child(User::unreadMessages.name).value as? Number)?.toInt() ?: 0
        u.premium = snapshot.child(User::premium.name).value as? Boolean ?: false
        u.premiumDate = (snapshot.child(User::premiumDate.name).value as? Number)?.toLong() ?: 0L
        u.isPremiumYearly = snapshot.child(User::isPremiumYearly.name).value as? Boolean ?: false
        u.isMonthlyPremium = snapshot.child(User::isMonthlyPremium.name).value as? Boolean ?: false
        u.gender = (snapshot.child(User::gender.name).value as? Number)?.toInt() ?: -1
        u.region = snapshot.child(User::region.name).value as? String ?: " "
        u.age = (snapshot.child(User::age.name).value as? Number)?.toInt() ?: -1
        u.height = (snapshot.child(User::height.name).value as? Number)?.toInt() ?: -1
        u.weight = (snapshot.child(User::weight.name).value as? Number)?.toDouble() ?: -1.0
        u.targetWeight = (snapshot.child(User::targetWeight.name).value as? Number)?.toDouble()
        u.startWeight = (snapshot.child(User::startWeight.name).value as? Number)?.toDouble()
        u.goal = (snapshot.child(User::goal.name).value as? String).let { if (it.isNullOrEmpty()) "lose_weight" else it }
        u.activityLevel = snapshot.child(User::activityLevel.name).value as? String ?: UserActivityLevel.LIGHTLY_ACTIVE.name
        u.targetCalories = (snapshot.child(User::targetCalories.name).value as? Number)?.toInt() ?: 0
        u.targetProtein = (snapshot.child(User::targetProtein.name).value as? Number)?.toInt() ?: 0
        u.targetFat = (snapshot.child(User::targetFat.name).value as? Number)?.toInt() ?: 0
        u.targetCarbs = (snapshot.child(User::targetCarbs.name).value as? Number)?.toInt() ?: 0
        u.targetWaterMl = (snapshot.child(User::targetWaterMl.name).value as? Number)?.toInt() ?: 2500
        u.avatarUrl = snapshot.child(User::avatarUrl.name).value as? String
        u.registerDate =
            (snapshot.child(User::registerDate.name).value as? Number)?.toLong() ?: System.currentTimeMillis()
        u.paymentChecking = snapshot.child(User::paymentChecking.name).value as? Boolean ?: false
        u.paymentCheckPhotoUrl = snapshot.child(User::paymentCheckPhotoUrl.name).value as? String
        u.admin = snapshot.child(User::admin.name).value as? Boolean ?: false
        u.scorePoints = (snapshot.child(User::scorePoints.name).value as? Number)?.toInt() ?: 0
        u.isRated = snapshot.child(User::isRated.name).value as? Boolean ?: false

        u.problems = try {
            val problemsChild = snapshot.child(User::problems.name)
            if (problemsChild.exists()) {
                problemsChild.children.mapNotNull { it.value?.toString() }
            } else null
        } catch (e: Exception) {
            null
        }
        u.streakInfo = try {
            val streakChild = snapshot.child(User::streakInfo.name)
            if (streakChild.exists()) {
                val lastActionDate = streakChild.child(StreakInfo::lastActionDate.name).value as? String ?: ""
                val currentStreak = (streakChild.child(StreakInfo::currentStreak.name).value as? Number)?.toInt() ?: 0
                val bestStreak = (streakChild.child(StreakInfo::bestStreak.name).value as? Number)?.toInt() ?: 0
                val activeDays = (streakChild.child(StreakInfo::activeDays.name).value as? Number)?.toInt() ?: 0
                val streakGoal = (streakChild.child(StreakInfo::streakGoal.name).value as? Number)?.toInt() ?: 7
                StreakInfo(
                    lastActionDate = lastActionDate,
                    currentStreak = currentStreak,
                    bestStreak = bestStreak,
                    activeDays = activeDays,
                    streakGoal = streakGoal
                )
            } else null
        } catch (e: Exception) {
            null
        }
        return u
    }

    fun fromDocumentSnapshot(snapshot: DocumentSnapshot): User {
        val u = User()
        u.uid = snapshot.id

        fun getStringSafe(field: String): String = try {
            snapshot.getString(field) ?: ""
        } catch (e: Exception) {
            ""
        }

        fun getLongSafe(field: String, default: Long): Long = try {
            snapshot.get(field)?.let { DateUtils.toLongTimestamp(it) } ?: default
        } catch (e: Exception) {
            default
        }

        fun getLongOptionalSafe(field: String): Long? = try {
            snapshot.get(field)?.let { DateUtils.toLongTimestamp(it) }
        } catch (e: Exception) {
            null
        }

        fun getIntSafe(field: String, default: Int): Int = try {
            (snapshot.get(field) as? Number)?.toInt() ?: default
        } catch (e: Exception) {
            default
        }

        fun getDoubleSafe(field: String, default: Double): Double = try {
            (snapshot.get(field) as? Number)?.toDouble() ?: default
        } catch (e: Exception) {
            default
        }

        fun getDoubleOptionalSafe(field: String): Double? = try {
            (snapshot.get(field) as? Number)?.toDouble()
        } catch (e: Exception) {
            null
        }

        fun getBooleanSafe(field: String, default: Boolean): Boolean = try {
            snapshot.getBoolean(field) ?: default
        } catch (e: Exception) {
            default
        }

        u.name = getStringSafe(User::name.name)
        u.phoneNumber = getStringSafe(User::phoneNumber.name)
        u.gmail = getStringSafe(User::gmail.name)
        u.lastSeenTime = getLongOptionalSafe(User::lastSeenTime.name)
        u.unreadMessages = getIntSafe(User::unreadMessages.name, 0)
        u.premium = getBooleanSafe(User::premium.name, false)
        u.premiumDate = getLongSafe(User::premiumDate.name, 0L)
        u.isPremiumYearly = getBooleanSafe(User::isPremiumYearly.name, false)
        u.isMonthlyPremium = getBooleanSafe(User::isMonthlyPremium.name, false)
        u.gender = getIntSafe(User::gender.name, -1)
        u.region = getStringSafe(User::region.name)
        u.age = getIntSafe(User::age.name, -1)
        u.height = getIntSafe(User::height.name, -1)
        u.weight = getDoubleSafe(User::weight.name, -1.0)
        u.targetWeight = getDoubleOptionalSafe(User::targetWeight.name)
        u.startWeight = getDoubleOptionalSafe(User::startWeight.name)
        u.goal = getStringSafe(User::goal.name).let { if (it.isEmpty()) "lose_weight" else it }
        u.activityLevel = getStringSafe(User::activityLevel.name).let { if (it.isEmpty()) UserActivityLevel.LIGHTLY_ACTIVE.name else it }
        u.targetCalories = getIntSafe(User::targetCalories.name, 0)
        u.targetProtein = getIntSafe(User::targetProtein.name, 0)
        u.targetFat = getIntSafe(User::targetFat.name, 0)
        u.targetCarbs = getIntSafe(User::targetCarbs.name, 0)
        u.targetWaterMl = getIntSafe(User::targetWaterMl.name, 2500)
        u.avatarUrl = try {
            snapshot.getString(User::avatarUrl.name)
        } catch (e: Exception) {
            null
        }

        u.registerDate = getLongSafe(User::registerDate.name, System.currentTimeMillis())
        u.paymentChecking = getBooleanSafe(User::paymentChecking.name, false)
        u.paymentCheckPhotoUrl = try {
            snapshot.getString(User::paymentCheckPhotoUrl.name)
        } catch (e: Exception) {
            null
        }
        u.admin = getBooleanSafe(User::admin.name, false)
        u.scorePoints = getIntSafe(User::scorePoints.name, 0)
        u.isRated = getBooleanSafe(User::isRated.name, false)

        u.problems = try {
            (snapshot.get(User::problems.name) as? List<*>)?.mapNotNull { it?.toString() }
        } catch (e: Exception) {
            null
        }

        u.streakInfo = try {
            val streakMap = snapshot.get(User::streakInfo.name) as? Map<*, *>
            if (streakMap != null) {
                val lastActionDate = streakMap[StreakInfo::lastActionDate.name]?.toString() ?: ""
                val currentStreak = (streakMap[StreakInfo::currentStreak.name] as? Number)?.toInt() ?: 0
                val bestStreak = (streakMap[StreakInfo::bestStreak.name] as? Number)?.toInt() ?: 0
                val activeDays = (streakMap[StreakInfo::activeDays.name] as? Number)?.toInt() ?: 0
                val streakGoal = (streakMap[StreakInfo::streakGoal.name] as? Number)?.toInt() ?: 7
                StreakInfo(lastActionDate, currentStreak, bestStreak, activeDays, streakGoal)
            } else null
        } catch (e: Exception) {
            null
        }

        u.lastWeightLogTimestamp = getLongSafe(User::lastWeightLogTimestamp.name, 0L)

        return u
    }

}
