package uz.oybek.ozamiz.data.user

import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.auth
import uz.oybek.ozamiz.data.firebase.database
import uz.oybek.ozamiz.data.firebase.firestore
import uz.oybek.ozamiz.data.firebase.publicUsersCollection
import uz.oybek.ozamiz.data.firebase.userReference
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.mapper.UserMapper
import uz.oybek.ozamiz.data.user.model.User

import com.bumptech.glide.Glide
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.ai.aiChat.local.AiChatDatabase
import uz.oybek.ozamiz.data.config.AppPreference

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UserAuthRepository {

    suspend fun signOut() {
        // runCatching { UserProfileRepository.stopObserving() }
        runCatching { auth.signOut() }
        runCatching { LocalUser.clear() }
        runCatching { AppPreference.clear() }
        clearAppCache()
    }

    private suspend fun clearAppCache() {
        withContext(Dispatchers.Main) {
            runCatching {
                Glide.get(appContext).clearMemory()
            }
        }
        withContext(Dispatchers.IO) {
            runCatching {
                uz.oybek.ozamiz.data.logs.LogRepository.clearCache()
            }
            runCatching {
                AiChatDatabase.getDatabase(appContext).clearAllTables()
            }
            runCatching {
                Glide.get(appContext).clearDiskCache()
            }
            runCatching {
                appContext.cacheDir?.deleteRecursively()
            }
            runCatching {
                appContext.externalCacheDir?.deleteRecursively()
            }
            runCatching {
                appContext.codeCacheDir?.deleteRecursively()
            }
        }
    }

    suspend fun deleteAccount() {
        val uid = LocalUser.user.uid
        if (uid.isNotBlank()) {
            runCatching { FirebasePaths.Database.user(uid).removeValue().await() }
            runCatching { FirebasePaths.Firestore.publicUser(uid).delete().await() }
        }
        runCatching { auth.currentUser?.delete()?.await() }
        signOut()
    }

    suspend fun authFirebaseUser(): User {
        val firebaseUser = auth.currentUser ?: return User()
        val id = firebaseUser.uid
        // val networkUser: User? = UserProfileRepository.loadUser(id)
        val networkUser: User? = null
        val existingLocal = LocalUser.user

        var userEmail = firebaseUser.email
        if (userEmail.isNullOrBlank()) {
            userEmail = firebaseUser.providerData.firstNotNullOfOrNull { it.email }
        }

        val currentUser = if (networkUser != null && networkUser.valid) {
            networkUser.copy(
                uid = id,
                phoneNumber = if (networkUser.phoneNumber.isNotBlank()) networkUser.phoneNumber else (if (firebaseUser.phoneNumber.isNullOrBlank()) existingLocal.phoneNumber else firebaseUser.phoneNumber!!),
                gmail = if (networkUser.gmail.isNotBlank()) networkUser.gmail else (if (userEmail.isNullOrBlank()) existingLocal.gmail else userEmail)
            )
        } else {
            existingLocal.copy(
                uid = id,
                phoneNumber = if (firebaseUser.phoneNumber.isNullOrBlank()) existingLocal.phoneNumber else firebaseUser.phoneNumber!!,
                gmail = if (userEmail.isNullOrBlank()) existingLocal.gmail else userEmail,
                lastSeenTime = System.currentTimeMillis(),
                registerDate = if (existingLocal.registerDate < 500000) System.currentTimeMillis() else existingLocal.registerDate
            )
        }

        LocalUser.user = currentUser
        LocalUser.saveUser()

        if (networkUser != null && networkUser.name.isNotBlank() && networkUser.targetCalories > 0) {
            uz.oybek.ozamiz.data.config.AppPreference.isOnboardingCompleted = true
        } else {
            uz.oybek.ozamiz.data.config.AppPreference.isOnboardingCompleted = false
        }

        val map = UserMapper.toMap(currentUser)
        database.userReference(id).updateChildren(map).await()

        try {
            val publicMap = UserMapper.toPublicMap(currentUser)
            firestore.publicUsersCollection.document(id).set(publicMap).await()
        } catch (e: Exception) {
            uz.oybek.ozamiz.util.handleException(e)
        }

        // UserProfileRepository.observeCurrentUser()

        return currentUser
    }
}