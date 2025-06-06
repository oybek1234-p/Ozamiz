// data/foods/MealPlanRepository.kt
package uz.oybek.ozamiz.data.foods

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.firebase.FirebaseInstances
import uz.oybek.ozamiz.handleException

class MealPlanRepository {

    private val database: FirebaseDatabase = FirebaseInstances.database
    private val mealPlansRootRef = database.getReference("user_meal_plans_v2") // Yangi yo'l

    fun getPlanForDay(userId: String, dayOfWeek: String): Flow<DataWrapper<DailyUserMealPlan?>> = callbackFlow {
        if (userId.isBlank() || dayOfWeek.isBlank()) {
            trySend(DataWrapper.Error("Foydalanuvchi ID si yoki kun noto'g'ri"))
            close()
            return@callbackFlow
        }
        val planRef = mealPlansRootRef.child(userId).child(dayOfWeek.lowercase())
        trySend(DataWrapper.Loading())

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val plan = snapshot.getValue<DailyUserMealPlan>()
                    trySend(DataWrapper.Success(plan))
                } else {
                    trySend(DataWrapper.Success(null)) // Reja topilmadi
                }
            }

            override fun onCancelled(error: DatabaseError) {
                handleException(error.toException(), "GetPlanForDayCancelled")
                trySend(DataWrapper.Error(error.message))
                close(error.toException())
            }
        }
        planRef.addListenerForSingleValueEvent(listener) // Bir marta o'qish uchun
        // Agar real-time yangilanish kerak bo'lsa, addValueEventListener dan foydalaning
        // va awaitClose da removeEventListener ni chaqiring. Hozircha bir martalik yetarli.

        awaitClose {
            // Agar addValueEventListener ishlatilsa:
            // planRef.removeEventListener(listener)
        }
    }

    suspend fun savePlanForDay(userId: String, dayOfWeek: String, plan: DailyUserMealPlan): DataWrapper<Unit> {
        if (userId.isBlank() || dayOfWeek.isBlank()) {
            return DataWrapper.Error("Foydalanuvchi ID si yoki kun noto'g'ri")
        }
        val planRef = mealPlansRootRef.child(userId).child(dayOfWeek.lowercase())
        return try {
            planRef.setValue(plan).await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "SavePlanForDayError")
            DataWrapper.Error(e.localizedMessage ?: "Rejani saqlashda xatolik")
        }
    }

    suspend fun deletePlanForDay(userId: String, dayOfWeek: String): DataWrapper<Unit> {
        if (userId.isBlank() || dayOfWeek.isBlank()) {
            return DataWrapper.Error("Foydalanuvchi ID si yoki kun noto'g'ri")
        }
        val planRef = mealPlansRootRef.child(userId).child(dayOfWeek.lowercase())
        return try {
            planRef.removeValue().await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "DeletePlanForDayError")
            DataWrapper.Error(e.localizedMessage ?: "Rejani o'chirishda xatolik")
        }
    }
}