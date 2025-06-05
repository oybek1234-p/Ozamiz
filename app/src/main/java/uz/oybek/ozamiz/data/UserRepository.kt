// C:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\data\UserRepository.kt
package uz.oybek.ozamiz.data

import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.OnDisconnect
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.ImageUploader
import uz.oybek.ozamiz.MainActivity
import uz.oybek.ozamiz.data.firebase.FirebaseInstances
import uz.oybek.ozamiz.data.model.StreakInfo
import uz.oybek.ozamiz.data.utils.FirebaseUtils.getValueSafe
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.showToast
import java.util.Calendar
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object UserRepository {

    private const val PAYMENT_CHECK_PHOTOS_PATH = "payment_checks"

    init {
        LocalUser.getUser()
    }

    suspend fun uploadPremiumCheckData(userId: String, checkImageUri: Uri): DataWrapper<Unit> {
        if (userId.isBlank()) return DataWrapper.Error("Foydalanuvchi ID si bo'sh")
        val userSpecificPaymentPath = "$PAYMENT_CHECK_PHOTOS_PATH/$userId" // <<< YENI YO'L

        return try {
            val uploadResult = ImageUploader.uploadImage(checkImageUri, userSpecificPaymentPath) // <<< O'ZGARTIRILDI
            when (uploadResult) {
                is DataWrapper.Success -> {
                    val imageUrl = uploadResult.data
                    val updates = mapOf(
                        User::paymentChecking.name to true,
                        User::paymentCheckPhotoUrl.name to imageUrl,
                        // User::premiumDate.name to ServerValue.TIMESTAMP // Mark submission time
                    )
                    usersReference.child(userId).updateChildren(updates).await()
                    // Update local user
                    updateUser {
                        it.paymentChecking = true
                        it.paymentCheckPhotoUrl = imageUrl
                        // it.premiumDate = System.currentTimeMillis() // Approximate, server time is better
                    }
                    DataWrapper.Success(Unit)
                }
                is DataWrapper.Error -> {
                    DataWrapper.Error("Chek rasmini yuklashda xatolik: ${uploadResult.error}")
                }
                is DataWrapper.Loading -> {
                    // This should ideally not be returned by uploadImage, but handle if it does
                    DataWrapper.Loading()
                }
            }
        } catch (e: Exception) {
            handleException(e, "UploadPremiumCheckDataError")
            DataWrapper.Error(e.localizedMessage ?: "Chek ma'lumotlarini yuklashda xatolik")
        }
    }


    private val usersReference: DatabaseReference by lazy {
        FirebaseInstances.database.getReference("users")
    }

    val user: User get() = LocalUser.user
    private var userLoading = false

    private fun saveUserNetwork() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (user.valid && user.uid == currentUid) {
            val userMap = mapOf(
                "uid" to user.uid,
                "name" to user.name,
                "admin" to user.isAdmin,
                "phoneNumber" to user.phoneNumber,
                "gmail" to user.gmail,
                "lastSeenTime" to user.lastSeenTime,
                "age" to user.age,
                "height" to user.height,
                "weight" to user.weight,
                "targetWeight" to user.targetWeight,
                "startWeight" to user.startWeight,
                "activityLevel" to user.activityLevel,
                "targetCalories" to user.targetCalories,
                "targetWaterMl" to user.targetWaterMl,
                "targetCaloriesBurned" to user.targetCaloriesBurned,
                "problems" to user.problems,
                "reminderSettings" to user.reminderSettings,
                "streakInfo" to user.streakInfo,
                "lastWeightLogTimestamp" to user.lastWeightLogTimestamp,
                "avatarUrl" to user.avatarUrl,
                "hasSeenWeightPhotoInfo" to user.hasSeenWeightPhotoInfo,
                "registerDate" to user.registerDate,
                "premium" to user.premium,
                "premiumDate" to user.premiumDate,
                "paymentChecking" to user.paymentChecking,
                "paymentCheckPhotoUrl" to user.paymentCheckPhotoUrl,
                "gender" to user.gender,
                "manzil" to user.manzil,
                "pushToken" to user.pushToken
            )
            usersReference.child(user.uid).setValue(userMap)
                .addOnSuccessListener {}
        }
    }


    fun removeLastSeenListener(listener: ValueEventListener) {
        usersReference.child(user.uid).child(User::lastSeenTime.name).removeEventListener(listener)
    }

    fun setPremium(userId: String, premium: Boolean, approved: Boolean) { // Added 'approved'
        if (userId.isEmpty()) return
        val updates = mutableMapOf<String, Any?>(
            User::premium.name to premium,
            User::paymentChecking.name to false // No longer checking
        )
        if (approved) {
            updates[User::premiumDate.name] = ServerValue.TIMESTAMP // Set premium start date
        } else {
            updates[User::paymentCheckPhotoUrl.name] = null // Clear photo URL on rejection
            // Optionally clear premiumDate too, or set to 0
            updates[User::premiumDate.name] = 0L
        }
        usersReference.child(userId).updateChildren(updates)
    }


    fun observeLastSeen(userId: String, onChange: (time: Long) -> Unit): ValueEventListener? {
        if (userId.isEmpty()) return null
        val listener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(Long::class.java)
                if (value is Long) {
                    onChange.invoke(value)
                }
            }
        }
        usersReference.child(userId).child(User::lastSeenTime.name).addValueEventListener(listener)
        return listener
    }

    fun getLastSeen(userId: String, done: (time: Long) -> Unit) {
        try {
            usersReference.child(userId).child(User::lastSeenTime.name).get().addOnSuccessListener {
                val time = it.getValue(Long::class.java) ?: System.currentTimeMillis()
                done.invoke(time)
            }.addOnFailureListener {
                done.invoke(0L)
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    fun getPhoneNumber(userId: String, done: (number: String) -> Unit) {
        try {
            usersReference.child(userId).child(User::phoneNumber.name).get().addOnSuccessListener {
                val number = it.getValue(String::class.java) ?: ""
                done.invoke(number)
            }.addOnFailureListener {
                done.invoke("")
            }
        } catch (e: Exception) {
            handleException(e)
        }
    }

    fun loadAllUsers(scope: CoroutineScope, done: (list: List<User>) -> Unit) {
        usersReference.get().addOnSuccessListener { it ->
            scope.launch(Dispatchers.Default) {
                done.invoke(it.children.mapNotNull { it.getValue(User::class.java) })
            }
        }
    }

    suspend fun getPendingPremiumUsers(): DataWrapper<List<User>> {
        return try {
            val snapshot = usersReference.orderByChild(User::paymentChecking.name).equalTo(true).get().await()
            val users = snapshot.children.mapNotNull { it.getValue(User::class.java) }
            DataWrapper.Success(users)
        } catch (e: Exception) {
            handleException(e, "GetPendingPremiumUsersError")
            DataWrapper.Error(e.localizedMessage ?: "Tekshiruvdagi foydalanuvchilarni yuklashda xatolik")
        }
    }


    fun setUnreadNotificationsZero() {
        if (user.valid) {
            if (user.unreadMessages == 0) return
            usersReference.child(user.uid).child(User::unreadMessages.name).setValue(0)
        }
    }

    fun removeUnreadMessageListener(eventListener: ValueEventListener) {
        if (user.valid.not()) return
        usersReference.child(user.uid).child(User::unreadMessages.name)
            .removeEventListener(eventListener)
    }

    private var readMessageListener: ValueEventListener? = null
    fun observeUnReadMessages(onChange: (count: Int) -> Unit): ValueEventListener? {
        if (user.valid.not()) return null
        readMessageListener = usersReference.child(user.uid).child(User::unreadMessages.name)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {

                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    val value = snapshot.getValueSafe(Int::class.java, 0)
                    if (value is Int) {
                        LocalUser.user.unreadMessages = value
                        LocalUser.saveUser()
                        onChange.invoke(value)
                    }
                }
            })
        return readMessageListener
    }

    fun signOut() {
        Firebase.messaging.unsubscribeFromTopic(LocalUser.user.uid + "topic")
        FirebaseInstances.auth.signOut()
        LocalUser.user = User()
        LocalUser.saveUser()
    }

    private fun createNewUserAndSet(user: User) {
        if (user.valid) {
            LocalUser.user = user
            LocalUser.saveUser()
            saveUserNetwork()
        }
    }

    fun updateUser(block: (user: User) -> Unit) {
        block.invoke(LocalUser.user)
        updateUser(LocalUser.user)
    }

    private fun updateUser(user: User) {
        if (user.valid && user.uid == LocalUser.user.uid) {
            LocalUser.user = user
            LocalUser.saveUser()
            saveUserNetwork()
        }
    }

    private const val ONLINE = 0L

    private var onlineListener: OnDisconnect? = null

    fun setOnline() {
        if (user.valid) {
            onlineListener?.cancel()
            LocalUser.user.lastSeenTime = ONLINE
            usersReference.child(user.uid).child(User::lastSeenTime.name).setValue(ONLINE)
            LocalUser.saveUser()
            val lastSeen = ServerValue.TIMESTAMP
            usersReference.child(user.uid).child(User::lastSeenTime.name).onDisconnect().apply {
                onlineListener = this
                setValue(lastSeen)
            }
        }
    }

    suspend fun authFirebaseUser(): User {
        val firebaseUser = FirebaseInstances.auth.currentUser
        if (firebaseUser == null || firebaseUser.uid.isEmpty()) {
            return User()
        }
        val id = firebaseUser.uid
        val networkUser = loadUser(id)
        val isNewUser = (networkUser == null || networkUser.valid.not())

        return if (isNewUser) {
            val pushToken = AppNotification.loadToken()

            val newUser = User(
                uid = id,
                name = "",
                phoneNumber = firebaseUser.phoneNumber ?: "",
                gmail = firebaseUser.email ?: "",
                lastSeenTime = System.currentTimeMillis(),
                unreadMessages = 0,
                premium = false,
                premiumDate = 0L,
                pushToken = pushToken ?: "",
                registerDate = Date(),
                startWeight = null,
                paymentChecking = false,
                paymentCheckPhotoUrl = null,
                isAdmin = false
            )
            createNewUserAndSet(newUser)
            newUser
        } else {
            val userWithIntegrity = networkUser!!.copy(
                streakInfo = networkUser.streakInfo ?: StreakInfo(),
                registerDate = networkUser.registerDate ?: Date(),
                startWeight = networkUser.startWeight, // Keep existing startWeight or null
                paymentChecking = networkUser.paymentChecking,
                paymentCheckPhotoUrl = networkUser.paymentCheckPhotoUrl,
                premium = networkUser.premium,
                premiumDate = networkUser.premiumDate,
                isAdmin = networkUser.isAdmin || id == "r7o2q6Ttd1XVY9w6M4gW1lqKqB52" // TODO: Replace with actual admin check logic
            )
            LocalUser.user = userWithIntegrity
            LocalUser.saveUser()
            userWithIntegrity
        }
    }

    fun updatePushToken(newPushToken: String) {
        if (newPushToken.isEmpty()) return
        if (LocalUser.user.valid.not()) return
        usersReference.child(LocalUser.user.uid).child(User::pushToken.name).setValue(newPushToken)
    }

    private suspend fun loadUser(id: String) = suspendCoroutine { sus ->
        val reference = usersReference.child(id)
        reference.get().addOnSuccessListener {
            val loadedUser = it.getValueSafe(User::class.java)
            val userWithIntegrity = loadedUser?.copy(
                streakInfo = loadedUser.streakInfo ?: StreakInfo(),
                registerDate = loadedUser.registerDate ?: Date(),
                startWeight = loadedUser.startWeight,
                paymentChecking = loadedUser.paymentChecking,
                paymentCheckPhotoUrl = loadedUser.paymentCheckPhotoUrl,
                premium = loadedUser.premium,
                premiumDate = loadedUser.premiumDate,
                isAdmin = loadedUser.isAdmin
            )
            sus.resume(userWithIntegrity)
        }.addOnFailureListener {
            sus.resume(null)
        }
    }

    fun observeCurrentUser(result: (user: User?) -> Unit) {
        if (userLoading || user.valid.not()) {
            return
        }
        userLoading = true
        val reference = usersReference.child(user.uid)
        reference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                result.invoke(null)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                userLoading = false
                val loadedUser = snapshot.getValue(User::class.java) ?: return
                val userWithIntegrity = loadedUser.copy(
                    streakInfo = loadedUser.streakInfo ?: StreakInfo(),
                    registerDate = loadedUser.registerDate ?: Date(),
                    startWeight = loadedUser.startWeight,
                    paymentChecking = loadedUser.paymentChecking,
                    paymentCheckPhotoUrl = loadedUser.paymentCheckPhotoUrl,
                    premium = loadedUser.premium,
                    premiumDate = loadedUser.premiumDate,
                    isAdmin = loadedUser.isAdmin
                )
                if (userWithIntegrity.valid) {
                    LocalUser.user = userWithIntegrity
                    LocalUser.saveUser()
                }
                result.invoke(userWithIntegrity)
            }
        })
    }
}