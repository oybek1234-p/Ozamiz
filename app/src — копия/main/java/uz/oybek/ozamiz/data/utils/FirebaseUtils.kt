package uz.oybek.ozamiz.data.utils

import com.google.firebase.database.DataSnapshot
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FirebaseUtils {

    suspend fun <T> QuerySnapshot.toObjectsSafe(type: Class<T>): List<T> =
        withContext(Dispatchers.Default) {
            return@withContext documents.mapNotNull {
                it.toObjectSafe(type)
            }
        }

    fun <T> DocumentSnapshot.toObjectSafe(type: Class<T>, default: T? = null): T? {
        return try {
            toObject(type)
        } catch (e: Exception) {
            default
        }
    }

    fun <T> QueryDocumentSnapshot.toObjectSafe(type: Class<T>, default: T? = null): T? {
        return try {
            toObject(type)
        } catch (e: Exception) {
            default
        }
    }

    fun <T> DataSnapshot.getValueSafe(type: Class<T>, default: T? = null): T? {
        return try {
            getValue(type)
        } catch (e: Exception) {
            return default
        }
    }
}

