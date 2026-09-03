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

        val map = UserMapper.toMap(currentUser)
        database.userReference(id).setValue(map).await()

        val publicMap = UserMapper.toPublicMap(currentUser)
        firestore.publicUsersCollection.document(id).set(publicMap).await()

        UserProfileRepository.observeCurrentUser()

        return currentUser
    }
}