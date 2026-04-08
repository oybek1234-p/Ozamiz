package uz.oybek.ozamiz.data.user

import uz.oybek.ozamiz.data.streak.StreakInfo
import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.OnDisconnect
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.getValueSafe
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DataWrapper

import uz.oybek.ozamiz.util.handleException
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
            val uploadResult = uz.oybek.ozamiz.data.firebase.uploader.ImageUploader.uploadImage(
                checkImageUri,
                userSpecificPaymentPath
            )
            if (uploadResult != null) {
                val imageUrl = uploadResult
                val updates = mapOf(
                    User::paymentChecking.name to true,
                    User::paymentCheckPhotoUrl.name to imageUrl,
                    User::premiumDate.name to ServerValue.TIMESTAMP
                )
                usersReference.child(userId).updateChildren(updates).await()
                updateUser {
                    it.paymentChecking = true
                    it.paymentCheckPhotoUrl = imageUrl
                }
                DataWrapper.Success(Unit)
            } else {
                DataWrapper.Error("Rasm yuklanmadi")
            }
        } catch (e: Exception) {
            DataWrapper.Error(e.localizedMessage ?: "Chek ma'lumotlarini yuklashda xatolik")
        }
    }

    private val usersReference: DatabaseReference by lazy {
        Firebase.database.getReference("users")
    }

    val user: User get() = LocalUser.user
    private var isObserving = false
    private var userListener: ValueEventListener? = null

    private fun saveUserNetwork() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (user.valid && user.uid == currentUid) {
            val userMap = user.toMap()
            usersReference.child(user.uid).updateChildren(userMap)
        }
    }

    private fun User.toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "name" to name,
            "phoneNumber" to phoneNumber,
            "gmail" to gmail,
            "lastSeenTime" to lastSeenTime,
            "unreadMessages" to unreadMessages,
            "premium" to premium,
            "premiumDate" to premiumDate,

            "gender" to gender,
            "region" to region,
            "age" to age,
            "height" to height,
            "weight" to weight,
            "targetWeight" to targetWeight,
            "startWeight" to startWeight,
            "activityLevel" to activityLevel,
            "targetCalories" to targetCalories,
            "targetWaterMl" to targetWaterMl,
            "problems" to problems,

            "streakInfo" to streakInfo,
            "lastWeightLogTimestamp" to lastWeightLogTimestamp,
            "avatarUrl" to avatarUrl,
            "registerDate" to registerDate,
            "paymentChecking" to paymentChecking,
            "paymentCheckPhotoUrl" to paymentCheckPhotoUrl,
            "admin" to admin,
            "scorePoints" to scorePoints
        )
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
                done.invoke(it.children.mapNotNull { it.getValueSafe(User::class.java) })
            }
        }
    }

    suspend fun getPendingPremiumUsers(): DataWrapper<List<User>> {
        return try {
            val snapshot =
                usersReference.orderByChild(User::paymentChecking.name).equalTo(true).get().await()
            val users = snapshot.children.mapNotNull { it.getValueSafe(User::class.java) }
            DataWrapper.Success(users)
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error(
                e.localizedMessage ?: "Tekshiruvdagi foydalanuvchilarni yuklashda xatolik"
            )
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
        if (isObserving) {
            userListener?.let { usersReference.child(user.uid).removeEventListener(it) }
            isObserving = false
            userListener = null
        }
        Firebase.auth.signOut()
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

    fun updateLastSeen() {
        if (user.valid) {
            LocalUser.user.lastSeenTime = System.currentTimeMillis()
            usersReference.child(user.uid).child(User::lastSeenTime.name).setValue(ServerValue.TIMESTAMP)
            LocalUser.saveUser()
        }
    }

    suspend fun authFirebaseUser(): User {
        val firebaseUser = Firebase.auth.currentUser
        if (firebaseUser == null || firebaseUser.uid.isEmpty()) {
            return User()
        }
        val id = firebaseUser.uid
        val networkUser = loadUser(id)
        val isNewUser = (networkUser == null || networkUser.valid.not())

        return if (isNewUser) {
            val newUser = User(
                uid = id,
                name = "",
                phoneNumber = firebaseUser.phoneNumber ?: "",
                gmail = firebaseUser.email ?: "",
                lastSeenTime = System.currentTimeMillis(),
                unreadMessages = 0,
                premium = false,
                premiumDate = 0L,
                registerDate = Date(),
                startWeight = null,
                paymentChecking = false,
                paymentCheckPhotoUrl = null,
                admin = false,
                scorePoints = 0
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
                admin = networkUser.admin
            )
            LocalUser.user = userWithIntegrity
            LocalUser.saveUser()
            userWithIntegrity
        }
    }


    suspend fun refreshUser() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null) {
            val networkUser = loadUser(currentUid)
            if (networkUser != null && networkUser.valid) {
                LocalUser.user = networkUser
                LocalUser.saveUser()
            }
        }
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
                admin = loadedUser.admin
            )
            sus.resume(userWithIntegrity)
        }.addOnFailureListener {
            sus.resume(null)
        }
    }

    private val userListeners = mutableListOf<(user: User?) -> Unit>()

    fun observeCurrentUser(result: (user: User?) -> Unit) {
        userListeners.add(result)
        if (isObserving) {
            // Already observing Firebase, but call the new listener immediately with current data
            if (user.valid) {
                result.invoke(user)
            }
            return
        }
        if (user.valid.not()) return

        isObserving = true
        val reference = usersReference.child(user.uid)
        userListener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                isObserving = false
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val loadedUser = snapshot.getValueSafe(User::class.java) ?: return
                val userWithIntegrity = loadedUser.copy(
                    streakInfo = loadedUser.streakInfo ?: StreakInfo(),
                    registerDate = loadedUser.registerDate ?: Date(),
                    startWeight = loadedUser.startWeight,
                    paymentChecking = loadedUser.paymentChecking,
                    paymentCheckPhotoUrl = loadedUser.paymentCheckPhotoUrl,
                    premium = loadedUser.premium,
                    premiumDate = loadedUser.premiumDate,
                    admin = loadedUser.admin
                )
                if (userWithIntegrity.valid) {
                    LocalUser.user = userWithIntegrity
                    LocalUser.saveUser()
                }
                userListeners.forEach { it.invoke(userWithIntegrity) }
            }
        }
        reference.addValueEventListener(userListener!!)
    }


    fun addPoints(amount: Int) {
        if (user.valid.not() || amount == 0) return
        val ref = usersReference.child(user.uid).child(User::scorePoints.name)
        ref.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                val current = currentData.getValue(Int::class.java) ?: 0
                currentData.value = maxOf(0, current + amount)
                return com.google.firebase.database.Transaction.success(currentData)
            }
            override fun onComplete(error: com.google.firebase.database.DatabaseError?, committed: Boolean, snapshot: com.google.firebase.database.DataSnapshot?) {
                if (committed && error == null) {
                    val newTotal = snapshot?.getValue(Int::class.java) ?: maxOf(0, LocalUser.user.scorePoints + amount)
                    LocalUser.user.scorePoints = newTotal
                    LocalUser.saveUser()
                }
            }
        })
    }
}
