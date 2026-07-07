package uz.oybek.ozamiz.data.user

import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.getValueSafe
import uz.oybek.ozamiz.data.firebase.toObjectSafe
import uz.oybek.ozamiz.data.firebase.toObjectsList
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.user.model.UserSortType
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.util.handleException
import uz.oybek.ozamiz.util.returnResult

object AdminRepository {

    fun setPremium(
        userId: String,
        premium: Boolean,
        approved: Boolean,
        isYearly: Boolean = false
    ) {
        if (userId.isEmpty()) return
        val updates = mutableMapOf<String, Any?>(
            User::premium.name to premium,
            User::paymentChecking.name to false
        )
        if (approved) {
            updates[User::premiumDate.name] = System.currentTimeMillis()
            updates[User::isPremiumYearly.name] = isYearly
            updates[User::isMonthlyPremium.name] = !isYearly
        } else {
            updates[User::paymentCheckPhotoUrl.name] = null
            updates[User::premiumDate.name] = 0L
            updates[User::isPremiumYearly.name] = false
            updates[User::isMonthlyPremium.name] = false
        }

        FirebasePaths.Database.user(userId).updateChildren(updates)
        FirebasePaths.Firestore.publicUser(userId).update(User::premium.name, premium)
    }

    suspend fun getPendingPremiumUsers(): DataWrapper<List<User>> {
        return try {
            val snapshot =
                FirebasePaths.Database.users().orderByChild(User::paymentChecking.name)
                    .equalTo(true)
                    .get()
                    .await()
            val users = snapshot.children.mapNotNull {
                it.getValueSafe(User::class.java)
            }
            DataWrapper.Success(users)
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error()
        }
    }

    suspend fun getUsers(
        limit: Int,
        lastUserId: String? = null,
        sortBy: UserSortType = UserSortType.RECENT_ACTIVITY,
        premiumOnly: Boolean = false
    ): Result<List<User>> = returnResult {
        val queryField = when (sortBy) {
            UserSortType.MOST_POINTS -> User::scorePoints.name
            UserSortType.RECENT_ACTIVITY -> User::lastSeenTime.name
            UserSortType.MOST_ACTIVE -> User::scorePoints.name
            UserSortType.TOP_STREAK -> "streakInfo.${StreakInfo::currentStreak.name}"
        }
        var query: Query = FirebasePaths.Firestore.publicUsers()

        if (premiumOnly) {
            query = query.whereEqualTo(User::premium.name, true)
        }

        query = query.orderBy(queryField, Query.Direction.DESCENDING)

        if (!lastUserId.isNullOrBlank()) {
            val lastDocument = FirebasePaths.Firestore.publicUser(lastUserId).get().await()
            if (lastDocument.exists()) {
                query = query.startAfter(lastDocument)
            }
        }
        query.limit(limit.toLong()).get().await().toObjectsList<User>()
    }

    suspend fun getPublicUser(userId: String): User? {
        if (userId.isBlank()) return null
        return try {
            FirebasePaths.Firestore.publicUser(userId).get().await().toObjectSafe<User>()
        } catch (e: Exception) {
            handleException(e)
            null
        }
    }

    suspend fun getAdminUser(userId: String): User? {
        if (userId.isBlank()) return null
        return try {
            FirebasePaths.Database.user(userId).get().await().getValueSafe(User::class.java)
        } catch (e: Exception) {
            handleException(e)
            null
        }
    }

    suspend fun updateAdminUser(userId: String, updates: Map<String, Any?>) {
        if (userId.isNotBlank()) FirebasePaths.Database.user(userId).updateChildren(updates).await()
    }

    suspend fun deleteUserAdmin(userId: String) {
        if (userId.isNotBlank()) {
            FirebasePaths.Database.user(userId).removeValue().await()
            FirebasePaths.Firestore.publicUser(userId).delete().await()
        }
    }
}
