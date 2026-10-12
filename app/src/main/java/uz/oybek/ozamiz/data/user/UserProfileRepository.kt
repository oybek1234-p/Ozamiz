package uz.oybek.ozamiz.data.user

import uz.oybek.ozamiz.common.util.orZero
import android.net.Uri
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.auth
import uz.oybek.ozamiz.data.firebase.database
import uz.oybek.ozamiz.data.firebase.firestore
import uz.oybek.ozamiz.data.firebase.getValueSafe
import uz.oybek.ozamiz.data.firebase.publicUsersCollection
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader
import uz.oybek.ozamiz.data.firebase.userReference
import uz.oybek.ozamiz.data.firebase.usersReference
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.mapper.UserMapper
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.common.util.DateFormatter
import uz.oybek.ozamiz.data.profile.model.User
import uz.oybek.ozamiz.util.handleException
import kotlin.coroutines.resume


object UserProfileRepository {

    private var isObserving = false
    private var userListener: ValueEventListener? = null
    var onboardingStarted = false

    val user: uz.oybek.ozamiz.data.user.model.User get() = LocalUser.user

    fun init() {
        LocalUser.getUser()
        if (user.uid.isNotEmpty()) {
            observeCurrentUser()
        }
    }

    suspend fun saveUserNetworkSuspend(user: User): Boolean {
       return true
    }

    suspend fun uploadPremiumCheckData(
        userId: String, checkImageUri: Uri, isYearly: Boolean
    ): BaseResult<Unit, String> {
        if (userId.isBlank()) return BaseResult.Failure("User ID bo'sh")

        return try {
            val imageUrl = ImageUploader.uploadImage(
                checkImageUri,
                FirebasePaths.Storage.paymentChecks(userId)
            )
            if (imageUrl != null) {
                updateUser {
                    paymentChecking = true
                    paymentCheckPhotoUrl = imageUrl
                    premiumDate = System.currentTimeMillis()
                    isPremiumYearly = isYearly
                    isMonthlyPremium = !isYearly
                }
                BaseResult.Success(Unit)
            } else {
                BaseResult.Failure("Rasm yuklanmadi")
            }
        } catch (e: Exception) {
            handleException(e)
            BaseResult.Failure("Xatolik yuz berdi")
        }
    }

    fun updateUser(work: User.() -> Unit = {}) {

    }

    fun saveUserNetworkForce(user: User) {

    }

    fun updateLastSeen() {
        if (user.valid) {
            val now = System.currentTimeMillis()
            updateField(User::lastSeenTime.name, now) { lastSeenTime = now }
        }
    }

    fun updateWeight(value: Double) = updateField(User::weight.name, value) { weight = value }
    fun updateStartWeight(value: Double?) =
        updateField(User::startWeight.name, value) { startWeight = value }


    fun updateAvatarUrl(value: String?) =
        updateField(User::avatarUrl.name, value) { avatarUrl = value }

    fun updateScorePoints(value: Int) =
        updateField(User::scorePoints.name, value) { scorePoints = value }
    fun updateLastWeightLogTimestamp(value: Long) =
        updateField(User::lastWeightLogTimestamp.name, value) { lastWeightLogTimestamp = value }

    fun updateStreakInfo(value: StreakInfo?) =
        updateField(User::streakInfo.name, value) { }

    private val PUBLIC_FIELDS = setOf(
        User::uid.name,
        User::name.name,
        User::avatarUrl.name,
        User::scorePoints.name,
        User::lastSeenTime.name,
        User::premium.name,
        User::premiumDate.name,
        User::isPremiumYearly.name,
        User::isMonthlyPremium.name,
        User::gender.name,
        User::region.name,
        User::registerDate.name,
        User::height.name,
        User::weight.name,
        User::startWeight.name,
        User::targetWeight.name,
        User::age.name,
        User::goal.name,
        User::targetCalories.name,
        User::streakInfo.name
    )

    private fun <T> updateField(fieldName: String, value: T, work: User.() -> Unit) {

        LocalUser.saveUser()
        if (!user.valid) return

        runCatching {
            FirebasePaths.Database.user(user.uid).child(fieldName).setValue(value)
            if (fieldName in PUBLIC_FIELDS) {
                val publicValue = if (fieldName == User::streakInfo.name && value is StreakInfo) {
                    mapOf(
                        StreakInfo::currentStreak.name to value.currentStreak,
                        StreakInfo::bestStreak.name to value.bestStreak,
                        StreakInfo::lastActionDate.name to value.lastActionDate,
                        StreakInfo::activeDays.name to value.activeDays,
                        StreakInfo::streakGoal.name to value.streakGoal
                    )
                } else value
                FirebasePaths.Firestore.publicUser(user.uid).set(
                    mapOf(fieldName to publicValue),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            }
        }
    }

    suspend fun loadUser(id: String) = suspendCancellableCoroutine { sus ->
        FirebasePaths.Database.user(id).get().addOnSuccessListener {
            if (it.exists()) {
                sus.resume(UserMapper.fromSnapshot(it))
            } else {
                sus.resume(null)
            }
        }.addOnFailureListener {
            sus.resume(null)
        }
    }

    fun observeCurrentUser() {
        if (isObserving || user.uid.isEmpty()) return

        isObserving = true
        userListener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                isObserving = false
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                if (onboardingStarted || !snapshot.exists()) return

                val loadedUser = UserMapper.fromSnapshot(snapshot)
                if (loadedUser.valid && loadedUser.name.isNotBlank() && loadedUser != LocalUser.user) {
                    LocalUser.user = loadedUser
                    LocalUser.saveUser()
                }
            }
        }
        FirebasePaths.Database.user(user.uid).addValueEventListener(userListener!!)
    }

    fun stopObserving() {
        if (isObserving) {
            userListener?.let { FirebasePaths.Database.user(user.uid).removeEventListener(it) }
            isObserving = false
            userListener = null
        }
    }

    fun setUnreadNotificationsZero() {
        if (user.valid && user.unreadMessages != 0) {
            FirebasePaths.Database.unreadMessages(user.uid).setValue(0)
        }
    }

    fun observeUnReadMessages(onChange: (count: Int) -> Unit): ValueEventListener? {
        if (!user.valid) return null
        return FirebasePaths.Database.unreadMessages(user.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {}
                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.getValueSafe(Int::class.java, 0).orZero()
                    LocalUser.user.unreadMessages = value
                    LocalUser.saveUser()
                    onChange.invoke(value)
                }
            })
    }

    fun incrementUnreadMessages(userId: String) {
        if (userId.isEmpty()) return
        FirebasePaths.Database.unreadMessages(userId)
            .runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val current = currentData.getValue(Int::class.java).orZero()
                    currentData.value = current + 1
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    snapshot: DataSnapshot?
                ) {
                }
            })
    }
}

