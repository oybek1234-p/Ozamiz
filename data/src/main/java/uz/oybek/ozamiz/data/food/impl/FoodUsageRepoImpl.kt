package uz.oybek.ozamiz.data.food.impl

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.data.di.IoDispatcher
import uz.oybek.ozamiz.data.food.api.FoodUsageRepo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FoodUsageRepoImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val gson: Gson,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FoodUsageRepo {

    private val prefs by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private val _usageCountsFlow = MutableStateFlow(loadUsageCounts())

    private fun loadUsageCounts(): Map<String, Int> {
        val json = prefs.getString(KEY_USAGE_COUNTS, null) ?: return emptyMap()
        return runCatching {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson<Map<String, Int>>(json, type) ?: emptyMap()
        }.getOrDefault(emptyMap())
    }

    override fun getUsageCounts(): Map<String, Int> = _usageCountsFlow.value

    override fun observeUsageCounts(): Flow<Map<String, Int>> = _usageCountsFlow.asStateFlow()

    override suspend fun incrementUsage(foodId: String) {
        if (foodId.isBlank()) return
        withContext(ioDispatcher) {
            val currentMap = _usageCountsFlow.value.toMutableMap()
            val newCount = (currentMap[foodId] ?: 0) + 1
            currentMap[foodId] = newCount

            val trimmedMap = if (currentMap.size > MAX_TRACKED_FOODS) {
                currentMap.entries
                    .sortedByDescending { it.value }
                    .take(MAX_TRACKED_FOODS)
                    .associate { it.key to it.value }
            } else {
                currentMap
            }

            _usageCountsFlow.value = trimmedMap

            prefs.edit {
                putString(KEY_USAGE_COUNTS, gson.toJson(trimmedMap))
            }
        }
    }

    private companion object {
        private const val PREF_NAME = "food_usage_prefs"
        private const val KEY_USAGE_COUNTS = "key_food_usage_counts"
        private const val MAX_TRACKED_FOODS = 50
    }
}
