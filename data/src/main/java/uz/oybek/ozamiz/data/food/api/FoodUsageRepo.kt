package uz.oybek.ozamiz.data.food.api

import kotlinx.coroutines.flow.Flow

interface FoodUsageRepo {
    fun getUsageCounts(): Map<String, Int>
    fun observeUsageCounts(): Flow<Map<String, Int>>
    suspend fun incrementUsage(foodId: String)
}
