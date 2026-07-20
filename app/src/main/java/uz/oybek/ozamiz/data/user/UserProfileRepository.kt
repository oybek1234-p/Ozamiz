package uz.oybek.ozamiz.data.user

import android.net.Uri
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
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
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.util.handleException
import kotlin.coroutines.resume

object UserProfileRepository {

    private val userListeners = mutableListOf<(user: User?) -> Unit>()
    private var isObserving = false
    private var userListener: ValueEventListener? = null
    var onboardingStarted = false

    val user: User get() = LocalUser.user

    fun init() {
        LocalUser.getUser()
        checkRegisterDateError()
    }

    private fun checkRegisterDateError() {
        if (DateUtils.toLongTimestamp(user.registerDate) < 500000) {
            val now = System.currentTimeMillis()
            updateRegisterDate(now)
        }
    }

    suspend fun uploadPremiumCheckData(
        userId: String, checkImageUri: Uri, isYearly: Boolean
    ): DataWrapper<Unit> {
        if (userId.isBlank()) return DataWrapper.Error()

        return try {
            val imageUrl = ImageUploader.uploadImage(checkImageUri, FirebasePaths.Storage.paymentChecks(userId))
            if (imageUrl != null) {
                updateUser {
                    paymentChecking = true
                    paymentCheckPhotoUrl = imageUrl
                    premiumDate = System.currentTimeMillis()
                    isPremiumYearly = isYearly
                    isMonthlyPremium = !isYearly
                }
                DataWrapper.Success(Unit)
            } else {
                DataWrapper.Error("Rasm yuklanmadi")
            }
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error("Xatolik yuz berdi")
        }
    }

    fun updateUser(work: User.() -> Unit = {}) {
        work(user)

        if (user.valid) {
            LocalUser.saveUser()
            saveUserNetworkForce(user)
            userListeners.forEach { it.invoke(user) }
        }
    }

    private fun saveUserNetworkForce(user: User) {
        if (user.valid) {
            val children = UserMapper.toMap(user)
            FirebasePaths.Database.user(user.uid).updateChildren(children)
            FirebasePaths.Firestore.publicUser(user.uid).set(children, com.google.firebase.firestore.SetOptions.merge())
        }
    }

    fun updateLastSeen() {
        if (user.valid) {
            val now = System.currentTimeMillis()
            updateField(User::lastSeenTime.name, now) { lastSeenTime = now }
        }
    }

    fun updateName(value: String) = updateField(User::name.name, value) { name = value }
    fun updateAge(value: Int) = updateField(User::age.name, value) { age = value }
    fun updateHeight(value: Int) = updateField(User::height.name, value) { height = value }
    fun updateWeight(value: Double) = updateField(User::weight.name, value) { weight = value }
    fun updateTargetWeight(value: Double?) =
        updateField(User::targetWeight.name, value) { targetWeight = value }

    fun updateStartWeight(value: Double?) =
        updateField(User::startWeight.name, value) { startWeight = value }

    fun updateGoal(value: String) = updateField(User::goal.name, value) { goal = value }
    fun updateActivityLevel(value: String) =
        updateField(User::activityLevel.name, value) { activityLevel = value }

    fun updateAvatarUrl(value: String?) =
        updateField(User::avatarUrl.name, value) { avatarUrl = value }

    fun updateGender(value: Int) = updateField(User::gender.name, value) { gender = value }
    fun updateRegion(value: String) = updateField(User::region.name, value) { region = value }
    fun updateScorePoints(value: Int) =
        updateField(User::scorePoints.name, value) { scorePoints = value }

    fun updateTargetCalories(value: Int) =
        updateField(User::targetCalories.name, value) { targetCalories = value }

    fun updateTargetProtein(value: Int) =
        updateField(User::targetProtein.name, value) { targetProtein = value }

    fun updateTargetFat(value: Int) = updateField(User::targetFat.name, value) { targetFat = value }
    fun updateTargetCarbs(value: Int) =
        updateField(User::targetCarbs.name, value) { targetCarbs = value }

    fun updateTargetWaterMl(value: Int) =
        updateField(User::targetWaterMl.name, value) { targetWaterMl = value }

    fun updatePremium(value: Boolean) = updateField(User::premium.name, value) { premium = value }
    fun updatePremiumDate(value: Long) =
        updateField(User::premiumDate.name, value) { premiumDate = value }

    fun updateIsPremiumYearly(value: Boolean) =
        updateField(User::isPremiumYearly.name, value) { isPremiumYearly = value }

    fun updateIsMonthlyPremium(value: Boolean) =
        updateField(User::isMonthlyPremium.name, value) { isMonthlyPremium = value }

    fun updatePhoneNumber(value: String) =
        updateField(User::phoneNumber.name, value) { phoneNumber = value }

    fun updateGmail(value: String) = updateField(User::gmail.name, value) { gmail = value }
    fun updateUnreadMessages(value: Int) =
        updateField(User::unreadMessages.name, value) { unreadMessages = value }

    fun updateLastWeightLogTimestamp(value: Long) =
        updateField(User::lastWeightLogTimestamp.name, value) { lastWeightLogTimestamp = value }

    fun updatePaymentChecking(value: Boolean) =
        updateField(User::paymentChecking.name, value) { paymentChecking = value }

    fun updatePaymentCheckPhotoUrl(value: String?) =
        updateField(User::paymentCheckPhotoUrl.name, value) { paymentCheckPhotoUrl = value }

    fun updateAdmin(value: Boolean) = updateField(User::admin.name, value) { admin = value }
    fun updateProblems(value: List<String>?) =
        updateField(User::problems.name, value) { problems = value }

    fun updateStreakInfo(value: StreakInfo?) =
        updateField(User::streakInfo.name, value) { streakInfo = value }

    fun updateRegisterDate(value: Long) =
        updateField(User::registerDate.name, value) { registerDate = value }

    private fun <T> updateField(fieldName: String, value: T, work: User.() -> Unit) {
        if (!user.valid) return
        work(user)
        LocalUser.saveUser()
        runCatching {
            FirebasePaths.Database.user(user.uid).child(fieldName).setValue(value)
            FirebasePaths.Firestore.publicUser(user.uid).set(mapOf(fieldName to value), com.google.firebase.firestore.SetOptions.merge())
        }
        userListeners.forEach { it.invoke(user) }
    }

    suspend fun refreshUser() {
        auth.currentUser?.uid?.let { uid ->
            loadUser(uid)?.let { networkUser ->
                if (networkUser.valid) {
                    LocalUser.user = networkUser
                    LocalUser.saveUser()
                }
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

    fun observeCurrentUser(result: (user: User?) -> Unit) {
        userListeners.add(result)
        if (user.valid) result.invoke(user)
        if (isObserving || !user.valid || user.name.isEmpty()) return

        isObserving = true
        userListener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                isObserving = false
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                if (onboardingStarted || !snapshot.exists()) return

                val loadedUser = UserMapper.fromSnapshot(snapshot)
                if (loadedUser.valid) {
                    if (loadedUser != LocalUser.user) {
                        LocalUser.user = loadedUser
                        LocalUser.saveUser()
                        userListeners.forEach { it.invoke(loadedUser) }
                    }
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

    fun removeUnreadMessageListener(eventListener: ValueEventListener) {
        if (user.valid) {
            FirebasePaths.Database.unreadMessages(user.uid).removeEventListener(eventListener)
        }
    }

    fun observeUnReadMessages(onChange: (count: Int) -> Unit): ValueEventListener? {
        if (!user.valid) return null
        return FirebasePaths.Database.unreadMessages(user.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {}
                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.getValueSafe(Int::class.java, 0) ?: 0
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
                    val current = currentData.getValue(Int::class.java) ?: 0
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
