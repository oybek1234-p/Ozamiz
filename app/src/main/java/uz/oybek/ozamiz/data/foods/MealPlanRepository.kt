// C:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\data\foods\MealPlanRepository.kt
package uz.oybek.ozamiz.data.foods

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.firebase.FirebaseInstances
import uz.oybek.ozamiz.handleException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MealPlanRepository {

    private val database: FirebaseDatabase = FirebaseInstances.database
    private val mealPlansRootRef = database.getReference("user_meal_plans_v2")

    /**
     * Foydalanuvchining barcha 7 kunlik (dushanbadan yakshanbagacha) saqlangan
     * rejalarini bir martada o'qib oladi.
     */
    suspend fun getPlansForLast7Days(userId: String): DataWrapper<List<DailyUserMealPlan>> {
        if (userId.isBlank()) {
            return DataWrapper.Error("Foydalanuvchi ID si bo'sh")
        }
        return try {
            coroutineScope {
                val dayKeys = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
                val deferredPlans = dayKeys.map { day ->
                    async {
                        try {
                            val snapshot = mealPlansRootRef.child(userId).child(day).get().await()
                            snapshot.getValue<DailyUserMealPlan>()
                        } catch (e: Exception) {
                            null // Xatolik bo'lsa null qaytaramiz
                        }
                    }
                }
                val allPlans = deferredPlans.awaitAll().filterNotNull()
                DataWrapper.Success(allPlans)
            }
        } catch (e: Exception) {
            handleException(e, "Get7DayPlansError")
            DataWrapper.Error("Haftalik rejalarni yuklashda xatolik: ${e.localizedMessage}")
        }
    }


    /**
     * Kunlik rejani oladi. Firebase'ning o'zining keshidan foydalanadi.
     * Birinchi keshni tekshiradi, agar bo'lmasa serverga boradi.
     */
    suspend fun getPlanForDayOnce(userId: String, dayOfWeek: String): DataWrapper<DailyUserMealPlan?> {
        if (userId.isBlank() || dayOfWeek.isBlank()) {
            return DataWrapper.Error("Foydalanuvchi ID si yoki kun noto'g'ri")
        }
        val planRef = mealPlansRootRef.child(userId).child(dayOfWeek.lowercase())

        return suspendCoroutine { continuation ->
            planRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val plan = snapshot.getValue<DailyUserMealPlan>()
                        continuation.resume(DataWrapper.Success(plan))
                    } else {
                        // Serverda bu kun uchun reja yo'q
                        continuation.resume(DataWrapper.Success(null))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    handleException(error.toException(), "GetPlanOnceError")
                    continuation.resume(DataWrapper.Error(error.message))
                }
            })
        }
    }

    /**
     * Rejani serverga saqlaydi. Persistence yoqilgan bo'lsa, avtomatik kesh ham yangilanadi.
     */
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