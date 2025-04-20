package uz.oybek.ozamiz.data

import com.google.firebase.Firebase
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
import uz.oybek.ozamiz.data.firebase.FirebaseInstances
import uz.oybek.ozamiz.data.utils.FirebaseUtils.getValueSafe
import uz.oybek.ozamiz.handleException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object UserRepository {

    init {
        LocalUser.getUser()
    }

    val usersReference: DatabaseReference by lazy {
        FirebaseInstances.database.getReference("users")
    }

    val user: User get() = LocalUser.user
    private var userLoading = false

    private fun saveUserNetwork() {
        if (user.valid) {
            usersReference.child(user.uid).setValue(user).addOnSuccessListener { }
        }
    }

    fun removeLastSeenListener(listener: ValueEventListener) {
        usersReference.child(user.uid).child(User::lastSeenTime.name).removeEventListener(listener)
    }

    fun setPremium(userId: String, premium: Boolean) {
        if (userId.isEmpty()) return
        usersReference.child(userId).updateChildren(
            mapOf(
                User::premium.name to premium, User::premiumDate.name to System.currentTimeMillis()
            )
        )
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

    suspend fun authFirebaseUser(): User? {
        val firebaseUser = FirebaseInstances.auth.currentUser
        if (firebaseUser == null || firebaseUser.uid.isEmpty()) {
            return null
        }
        val id = firebaseUser.uid
        val isNewUser: Boolean
        val networkUser = loadUser(id)
        isNewUser = networkUser.valid.not()
        return if (isNewUser) {
            val pushToken = AppNotification.loadToken()
            val newUser = User(
                uid = id,
                "",
                firebaseUser.phoneNumber ?: "",
                firebaseUser.email ?: "",
                System.currentTimeMillis(),
                0,
                false,
                0L,
                pushToken ?: ""
            )
            createNewUserAndSet(newUser)
            newUser
        } else {
            LocalUser.user = networkUser!!
            LocalUser.saveUser()
            networkUser
        }
    }

    fun updatePushToken(newPushToken: String) {
        if (newPushToken.isEmpty()) return
        if (LocalUser.user.valid.not()) return
        usersReference.child(LocalUser.user.uid).child(User::pushToken.name).setValue(newPushToken)
    }

    suspend fun loadUser(id: String) = suspendCoroutine { sus ->
        val reference = usersReference.child(id)
        reference.get().addOnSuccessListener {
            val loadedUser = it.getValueSafe(User::class.java)
            sus.resume(loadedUser)
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
                val loadedUser = snapshot.getValue(User::class.java)
                if (loadedUser.valid) {
                    LocalUser.user = loadedUser!!
                    LocalUser.saveUser()
                }
                result.invoke(loadedUser)
            }
        })
    }
}