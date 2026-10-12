package uz.oybek.ozamiz.data.user

import kotlinx.coroutines.flow.Flow
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.profile.model.User

interface UserNetworkDataSource {
    suspend fun getUser(userId: String): BaseResult<User, Exception?>
    suspend fun getUsers(
        limit: Int = 20,
        lastUserId: String? = null
    ): BaseResult<List<User>, Exception?>
    fun observeUnreadMessages(userId: String): Flow<Int>
    suspend fun setUnreadMessages(userId: String, count: Int): Boolean
    suspend fun updateLastSeenTime(userId: String, timestamp: Long): Boolean
    suspend fun syncUser(user: User): Boolean
}