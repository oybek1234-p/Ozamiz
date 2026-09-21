package uz.oybek.ozamiz.data.user

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.common.BaseResult
import uz.oybek.ozamiz.data.profile.mapper.UserMapper
import uz.oybek.ozamiz.data.profile.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UserNetworkDataSourceImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val publicUsersNetworkSource: PublicUsersNetworkSource
) : UserNetworkDataSource {

    companion object {
        private const val USERS_PATH = "users"
    }

    override suspend fun getUser(userId: String): BaseResult<User, Exception?> {
        if (userId.isBlank()) return BaseResult.Failure()
        return try {
            val snapshot = database.getReference(USERS_PATH)
                .child(userId)
                .get()
                .await()
            if (snapshot.exists()) {
                val user = UserMapper.fromSnapshot(snapshot)
                BaseResult.Success(user)
            } else {
                BaseResult.Failure(UserNotFoundException())
            }
        } catch (e: Exception) {
            BaseResult.Failure(e)
        }
    }

    override suspend fun getUsers(
        limit: Int,
        lastUserId: String?
    ): BaseResult<List<User>, Exception?> {
        return try {
            var query = database.getReference(USERS_PATH).orderByKey().limitToFirst(limit)
            if (!lastUserId.isNullOrBlank()) {
                query = query.startAfter(lastUserId)
            }
            val snapshot = query.get().await()
            val users = snapshot.children.mapNotNull { UserMapper.fromSnapshot(it) }
            BaseResult.Success(users)
        } catch (e: Exception) {
            BaseResult.Failure(e)
        }
    }

    override fun observeUnreadMessages(userId: String): Flow<Int> = callbackFlow {
        if (userId.isBlank()) {
            trySend(0)
            close()
            return@callbackFlow
        }
        val ref = database.getReference(USERS_PATH)
            .child(userId)
            .child(User::unreadMessages.name)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = (snapshot.value as? Number)?.toInt() ?: 0
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

    override suspend fun syncUser(user: User): Boolean {
        if (user.uid.isBlank()) return false
        return try {
            val userMap = UserMapper.toMap(user)
            database.getReference(USERS_PATH)
                .child(user.uid)
                .updateChildren(userMap)
                .await()
            publicUsersNetworkSource.updatePublicUser(user)
            true
        } catch (_: Exception) {
            false
        }
    }
}