package uz.oybek.ozamiz.data.food

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.currentUserId
import uz.oybek.ozamiz.data.firebase.generateKey
import uz.oybek.ozamiz.data.logs.model.FoodLog

object SavedFoodRepository {

    fun getSavedFoods(): Flow<List<FoodLog>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reference = FirebasePaths.savedFoodsReference(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val foods = snapshot.children.mapNotNull { it.getValue(FoodLog::class.java) }
                trySend(foods)
            }

            override fun onCancelled(error: DatabaseError) {
                if (error.code == DatabaseError.PERMISSION_DENIED) {
                    close()
                } else {
                    close(error.toException())
                }
            }
        }
        reference.addValueEventListener(listener)
        awaitClose { reference.removeEventListener(listener) }
    }

    suspend fun saveFood(food: FoodLog): Boolean {
        val userId = currentUserId ?: return false
        val reference = FirebasePaths.savedFoodsReference(userId)
        val key = if (food.logId.isNotEmpty()) food.logId else generateKey()
        food.logId = key
        return try {
            reference.child(key).setValue(food).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeSavedFood(foodId: String): Boolean {
        val userId = currentUserId ?: return false
        val reference = FirebasePaths.savedFoodsReference(userId)
        return try {
            reference.child(foodId).removeValue().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isFoodSaved(foodId: String): Boolean {
        val userId = currentUserId ?: return false
        return try {
            val snapshot = FirebasePaths.savedFoodsReference(userId).child(foodId).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            false
        }
    }
}
