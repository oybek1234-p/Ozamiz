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

object UserAuthRepository {

    fun signOut() {
        UserProfileRepository.stopObserving()
        auth.signOut()
        LocalUser.user = User()
        LocalUser.saveUser()
    }

    fun deleteAccount() {
        val uid = LocalUser.user.uid
        if (uid.isNotBlank()) {
            runCatching { FirebasePaths.Database.user(uid).removeValue() }
            runCatching { FirebasePaths.Firestore.publicUser(uid).delete() }
        }
        runCatching { auth.currentUser?.delete() }
        signOut()
    }

    suspend fun authFirebaseUser(): User {
        val firebaseUser = auth.currentUser ?: return User()
        val id = firebaseUser.uid
        val networkUser: User? = UserProfileRepository.loadUser(id)

        if (networkUser != null && networkUser.valid) {
            LocalUser.user = networkUser
            LocalUser.saveUser()
            return networkUser
        }

        var userEmail = firebaseUser.email
        if (userEmail.isNullOrBlank()) {
            userEmail = firebaseUser.providerData.firstNotNullOfOrNull { it.email }
        }

        val newUser = User(
            uid = id,
            phoneNumber = firebaseUser.phoneNumber ?: "",
            gmail = userEmail.orEmpty(),
            lastSeenTime = System.currentTimeMillis(),
            registerDate = System.currentTimeMillis()
        )

        LocalUser.user = newUser
        LocalUser.saveUser()

        val map = UserMapper.toMap(newUser)
        database.userReference(id).setValue(map).await()

        val publicMap = UserMapper.toPublicMap(newUser)
        firestore.publicUsersCollection.document(id).set(publicMap).await()

        return newUser
    }
}