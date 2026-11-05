package uz.oybek.ozamiz.data.user

import uz.oybek.ozamiz.common.util.orZero
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.firebase.DatabasePaths
import uz.oybek.ozamiz.data.profile.mapper.UserMapper
import uz.oybek.ozamiz.data.profile.model.User
import uz.oybek.ozamiz.data.utils.awaitResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
internal class UserNetworkDataSourceImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val publicUsersNetworkSource: PublicUsersNetworkSource
) : UserNetworkDataSource {

    override suspend fun getUser(userId: String): BaseResult<User, Exception?> {
        if (userId.isBlank()) return BaseResult.Failure()
        val ref = database.getReference(DatabasePaths.Realtime.USERS).child(userId)
        var lastError: Exception? = null
        for (attempt in 0..2) {
            when (val result = ref.get().awaitResult()) {
                is BaseResult.Success -> {
                    val snapshot = result.data
                    return if (snapshot.exists()) {
                        val user = UserMapper.fromSnapshot(snapshot)
                        BaseResult.Success(user)
                    } else {
                        BaseResult.Failure(UserNotFoundException())
                    }
                }
                is BaseResult.Failure -> {
                    lastError = result.error
                    if (attempt < 2) {
                        delay((1000L * (attempt + 1)).milliseconds)
                    }
                }
            }
        }
        return BaseResult.Failure(lastError)
    }

    override suspend fun getUsers(
        limit: Int,
        lastUserId: String?
    ): BaseResult<List<User>, Exception?> {
        var query = database.getReference(DatabasePaths.Realtime.USERS).orderByKey().limitToFirst(limit)
        if (!lastUserId.isNullOrBlank()) {
            query = query.startAfter(lastUserId)
        }
        return when (val result = query.get().awaitResult()) {
            is BaseResult.Success -> {
                val users = result.data.children.mapNotNull { UserMapper.fromSnapshot(it) }
                BaseResult.Success(users)
            }
            is BaseResult.Failure -> BaseResult.Failure(result.error)
        }
    }

    override fun observeUnreadMessages(userId: String): Flow<Int> = callbackFlow {
        if (userId.isBlank()) {
            trySend(0)
            close()
            return@callbackFlow
        }
        val ref = database.getReference(DatabasePaths.Realtime.USERS)
            .child(userId)
            .child(User::unreadMessages.name)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = (snapshot.value as? Number)?.toInt().orZero()
                trySend(count)
            }

            override fun onCancelled(error: DatabaseError) {
                close()
            }
        }

        ref.addValueEventListener(listener)

        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    override fun observeUser(userId: String): Flow<User> = callbackFlow {
        if (userId.isBlank()) {
            close()
            return@callbackFlow
        }
        val ref = database.getReference(DatabasePaths.Realtime.USERS).child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user = UserMapper.fromSnapshot(snapshot)
                    trySend(user)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)

        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    override suspend fun setUnreadMessages(userId: String, count: Int): Boolean {
        if (userId.isBlank()) return false
        val task = database.getReference(DatabasePaths.Realtime.USERS)
            .child(userId)
            .child(User::unreadMessages.name)
            .setValue(count)
        return when (task.awaitResult()) {
            is BaseResult.Success -> true
            is BaseResult.Failure -> false
        }
    }

    override suspend fun updateLastSeenTime(userId: String, timestamp: Long): Boolean {
        if (userId.isBlank()) return false
        val rtdbResult = database.getReference(DatabasePaths.Realtime.USERS)
            .child(userId)
            .child(User::lastSeenTime.name)
            .setValue(timestamp)
            .awaitResult()

        val firestoreSuccess = publicUsersNetworkSource.updateLastSeenTime(userId, timestamp)
        return rtdbResult is BaseResult.Success || firestoreSuccess
    }

    override suspend fun syncUser(user: User): Boolean {
        if (user.uid.isBlank()) return false
        val userMap = UserMapper.toMap(user)
        val rtdbResult = database.getReference(DatabasePaths.Realtime.USERS)
            .child(user.uid)
            .updateChildren(userMap)
            .awaitResult()

        publicUsersNetworkSource.updatePublicUser(user)
        return rtdbResult is BaseResult.Success
    }
}