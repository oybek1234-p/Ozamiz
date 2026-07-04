package uz.oybek.ozamiz.data.user

import android.net.Uri
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.config.LocalPreferencesRepository
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.auth
import uz.oybek.ozamiz.data.firebase.database
import uz.oybek.ozamiz.data.firebase.firestore
import uz.oybek.ozamiz.data.firebase.getValueSafe
import uz.oybek.ozamiz.data.firebase.publicUsersCollection
import uz.oybek.ozamiz.data.firebase.toObjectSafe
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.user.model.UserSortType
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.util.handleException
import uz.oybek.ozamiz.util.returnResult
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object UserRepository {

    private const val PAYMENT_CHECK_PHOTOS_PATH = "payment_checks"

    init {
        LocalUser.getUser()
    }

    suspend fun uploadPremiumCheckData(
        userId: String, checkImageUri: Uri, isYearly: Boolean
    ): DataWrapper<Unit> {
        if (userId.isBlank()) return DataWrapper.Error("Foydalanuvchi ID si bo'sh")
        val userSpecificPaymentPath = "$PAYMENT_CHECK_PHOTOS_PATH/$userId" // <<< YENI YO'L

        return try {
            val uploadResult = uz.oybek.ozamiz.data.firebase.uploader.ImageUploader.uploadImage(
                checkImageUri, userSpecificPaymentPath
            )
            if (uploadResult != null) {
                val imageUrl = uploadResult
                val updates = mapOf(
                    User::paymentChecking.name to true,
                    User::paymentCheckPhotoUrl.name to imageUrl,
                    User::premiumDate.name to ServerValue.TIMESTAMP,
                    "isPremiumYearly" to isYearly,
                    "isMonthlyPremium" to !isYearly
                )
                usersReference.child(userId).updateChildren(updates).await()
                updateUser {
                    it.paymentChecking = true
                    it.paymentCheckPhotoUrl = imageUrl
                    it.isPremiumYearly = isYearly
                    it.isMonthlyPremium = !isYearly
                }
                DataWrapper.Success(Unit)
            } else {
                DataWrapper.Error("Rasm yuklanmadi")
            }
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error("")
        }
    }

    private val usersReference: DatabaseReference get() = database.getReference("users")

    private val publicUsersCollection get() = firestore.publicUsersCollection

    val user: User get() = LocalUser.user

    fun getManualAiUsageCount(): Int {
        val todayStr = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")
        return LocalPreferencesRepository.getManualAiUsageCount(todayStr)
    }

    fun incrementManualAiUsageCount() {
        val todayStr = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")
        LocalPreferencesRepository.incrementManualAiUsageCount(todayStr)
    }

    private var isObserving = false
    private var userListener: ValueEventListener? = null

    private fun saveUserNetwork() {
        val currentUid = auth.currentUser?.uid
        if (user.valid && user.uid == currentUid) {
            if (user.lastSeenTime is Long) {
                user.lastSeenTime = ServerValue.TIMESTAMP
            }
            if (user.registerDate is Long) {
                user.registerDate = ServerValue.TIMESTAMP
            }
            usersReference.child(user.uid).setValue(user)
            publicUsersCollection.document(user.uid).set(user)
        }
    }

    fun setPremium(
        userId: String, premium: Boolean, approved: Boolean, isYearly: Boolean = false
    ) {
        if (userId.isEmpty()) return
        val updates = mutableMapOf<String, Any?>(
            User::premium.name to premium, User::paymentChecking.name to false
        )
        if (approved) {
            updates[User::premiumDate.name] = ServerValue.TIMESTAMP
            updates["isPremiumYearly"] = isYearly
            updates["isMonthlyPremium"] = !isYearly
        } else {
            updates[User::paymentCheckPhotoUrl.name] = null
            updates[User::premiumDate.name] = 0L
            updates["isPremiumYearly"] = false
            updates["isMonthlyPremium"] = false
        }
        usersReference.child(userId).updateChildren(updates)

        val publicUpdates = mapOf(
            "premium" to premium
        )
        publicUsersCollection.document(userId).update(publicUpdates)
    }

    fun loadAllUsers(scope: CoroutineScope, done: (list: List<User>) -> Unit) {
        usersReference.get().addOnSuccessListener { it ->
            scope.launch(Dispatchers.Default) {
                val totalChildren = it.childrenCount
                Log.d(
                    "UserRepository",
                    "loadAllUsers: fetched $totalChildren children from Realtime Database"
                )
                val parsedList = it.children.mapNotNull { child ->
                    val user = child.getValueSafe(User::class.java)
                    if (user == null) {
                        Log.w(
                            "UserRepository",
                            "loadAllUsers: failed to parse user with key ${child.key}"
                        )
                    }
                    user
                }
                Log.d(
                    "UserRepository",
                    "loadAllUsers: successfully parsed ${parsedList.size} of $totalChildren users"
                )
                done.invoke(parsedList)
            }
        }.addOnFailureListener { e ->
            Log.e("UserRepository", "loadAllUsers: failed to fetch users", e)
            done.invoke(emptyList())
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
            DataWrapper.Error("")
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
        auth.signOut()
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
            userListeners.forEach { it.invoke(user) }
        }
    }

    fun updateLastSeen() {
        if (user.valid) {
            LocalUser.user.lastSeenTime = System.currentTimeMillis()
            val timestamp = ServerValue.TIMESTAMP
            usersReference.child(user.uid).child(User::lastSeenTime.name)
                .setValue(timestamp)
            publicUsersCollection.document(user.uid)
                .update(User::lastSeenTime.name, System.currentTimeMillis())
            LocalUser.saveUser()
        }
    }

    suspend fun authFirebaseUser(): User {
        val firebaseUser = auth.currentUser
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
                lastSeenTime = ServerValue.TIMESTAMP,
                unreadMessages = 0,
                premium = false,
                premiumDate = 0L,
                registerDate = ServerValue.TIMESTAMP,
                startWeight = null,
                paymentChecking = false,
                paymentCheckPhotoUrl = null,
                admin = false,
                scorePoints = 0
            )
            createNewUserAndSet(user)
            LocalUser.user.apply {
                registerDate = System.currentTimeMillis()
                lastSeenTime = System.currentTimeMillis()
                LocalUser.saveUser()
            }
            newUser
        } else {
            LocalUser.user = networkUser
            LocalUser.saveUser()
            networkUser
        }
    }

    suspend fun refreshUser() {
        val currentUid = auth.currentUser?.uid
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
                registerDate = loadedUser.registerDateLong ?: System.currentTimeMillis(),
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

        // Call immediately if local data is available
        if (user.valid) {
            result.invoke(user)
        }

        if (isObserving) {
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
                    registerDate = loadedUser.registerDateLong ?: System.currentTimeMillis(),
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
        val publicDocRef = publicUsersCollection.document(user.uid)

        ref.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                val current = currentData.getValue(Int::class.java) ?: 0
                currentData.value = maxOf(0, current + amount)
                return com.google.firebase.database.Transaction.success(currentData)
            }

            override fun onComplete(
                error: com.google.firebase.database.DatabaseError?,
                committed: Boolean,
                snapshot: com.google.firebase.database.DataSnapshot?
            ) {
                if (committed && error == null) {
                    val newTotal = snapshot?.getValue(Int::class.java) ?: maxOf(
                        0, LocalUser.user.scorePoints + amount
                    )
                    LocalUser.user.scorePoints = newTotal
                    LocalUser.saveUser()

                    // Sync with public_users
                    publicDocRef.update(User::scorePoints.name, newTotal)
                }
            }
        })
    }

    fun incrementUnreadMessages(userId: String) {
        if (userId.isEmpty()) return
        val ref = usersReference.child(userId).child(User::unreadMessages.name)
        ref.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                val current = currentData.getValue(Int::class.java) ?: 0
                currentData.value = current + 1
                return com.google.firebase.database.Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?
            ) {
                // Done
            }
        })
    }

    suspend fun getUsers(
        limit: Int,
        lastValue: Any? = null,
        lastUserId: String? = null,
        sortBy: UserSortType = UserSortType.MOST_POINTS
    ): Result<List<User>> {
        return returnResult {
            val queryField = when (sortBy) {
                UserSortType.MOST_POINTS -> User::scorePoints.name
                UserSortType.RECENT_ACTIVITY -> User::lastSeenTime.name
                UserSortType.MOST_ACTIVE -> User::scorePoints.name
                UserSortType.TOP_STREAK -> "streakInfo.${StreakInfo::currentStreak.name}"
            }

            var query = publicUsersCollection.orderBy(queryField, Query.Direction.DESCENDING)

            if (lastValue != null && lastUserId != null) {
                val lastDocument = publicUsersCollection.document(lastUserId).get().await()
                if (lastDocument.exists()) {
                    query = query.startAfter(lastDocument)
                }
            }

            val snapshot = query.limit(limit.toLong()).get().await()
            snapshot.toObjects<User>()
        }
    }

    suspend fun getPublicUser(userId: String): User? {
        if (userId.isBlank()) throw IllegalArgumentException("User ID cannot be blank")
        return try {
            val snapshot = publicUsersCollection.document(userId).get().await()
            snapshot.toObjectSafe<User>()
        } catch (e: Exception) {
            handleException(e)
            null
        }
    }

    suspend fun getAdminUser(userId: String): User? {
        if (userId.isBlank()) throw IllegalArgumentException("User ID cannot be blank")
        return try {
            val snapshot = FirebasePaths.userReference(userId).get().await()
            snapshot.getValueSafe(User::class.java)
        } catch (e: Exception) {
            handleException(e)
            null
        }
    }

    suspend fun updateAdminUser(userId: String, updates: Map<String, Any?>) {
        if (userId.isBlank()) throw IllegalArgumentException("User ID cannot be blank")
        FirebasePaths.userReference(userId).updateChildren(updates).await()
    }

    suspend fun deleteUserAdmin(userId: String) {
        if (userId.isBlank()) throw IllegalArgumentException("User ID cannot be blank")
        FirebasePaths.userReference(userId).removeValue().await()
        publicUsersCollection.document(userId).delete().await()
    }
}
