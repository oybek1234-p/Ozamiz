package uz.oybek.ozamiz.data.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.utils.DateUtils
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class AppSettings(
    val isAppReviewShown: Boolean = false,
    val foodLogLimitDate: String = "",
    val foodLogLimitCount: Int = 0,
    val firstAppOpenTimestamp: Long = 0L,
    val bonusFoodLogDate: String = "",
    val bonusFoodLogCount: Int = 0,
    val lastDailyGoalReachedDate: String = "",
    val aiChatLimitDate: String = "",
    val aiChatLimitCount: Int = 0
)

object AppSettingsSerializer : Serializer<AppSettings> {
    override val defaultValue = AppSettings()
    override suspend fun readFrom(input: InputStream): AppSettings {
        return try {
            Json.decodeFromString<AppSettings>(input.readBytes().decodeToString())
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        output.write(Json.encodeToString(t).encodeToByteArray())
    }
}

val Context.appSettingsDataStore: DataStore<AppSettings> by dataStore(
    fileName = "app_settings.json",
    serializer = AppSettingsSerializer
)

object AppPreference {

    private val dataStore: DataStore<AppSettings> = appContext.appSettingsDataStore

    val current: AppSettings get() = runBlocking { dataStore.data.first() }

    suspend fun update(block: (AppSettings) -> AppSettings) {
        dataStore.updateData { block(it) }
    }

    var isAppReviewShown: Boolean
        get() = current.isAppReviewShown
        set(value) = runBlocking { update { it.copy(isAppReviewShown = value) } }

    var firstAppOpenTimestamp: Long
        get() = current.firstAppOpenTimestamp
        set(value) = runBlocking { update { it.copy(firstAppOpenTimestamp = value) } }

    var lastDailyGoalReachedDate: String
        get() = current.lastDailyGoalReachedDate
        set(value) = runBlocking { update { it.copy(lastDailyGoalReachedDate = value) } }

    private var inMemoryLogCount: Int = -1
    private var inMemoryLogDate: String = ""
    private var inMemoryBonusLogs: Int = -1
    private var inMemoryBonusDate: String = ""
    private var inMemoryAiChatCount: Int = -1
    private var inMemoryAiChatDate: String = ""

    fun getFoodLogLimitCount(): Int {
        val dateStr = DateUtils.todayString()
        if (inMemoryLogDate == dateStr && inMemoryLogCount >= 0) {
            return inMemoryLogCount
        }
        val s = current
        val count = if (s.foodLogLimitDate == dateStr) s.foodLogLimitCount else 0
        inMemoryLogDate = dateStr
        inMemoryLogCount = count
        return count
    }

    suspend fun incrementFoodLogLimitCount() {
        val dateStr = DateUtils.todayString()
        val currentCount = getFoodLogLimitCount()
        val newCount = currentCount + 1
        inMemoryLogDate = dateStr
        inMemoryLogCount = newCount
        update { s ->
            s.copy(foodLogLimitDate = dateStr, foodLogLimitCount = newCount)
        }
    }

    fun getBonusFoodLogs(): Int {
        val dateStr = DateUtils.todayString()
        if (inMemoryBonusDate == dateStr && inMemoryBonusLogs >= 0) {
            return inMemoryBonusLogs
        }
        val s = current
        val count = if (s.bonusFoodLogDate == dateStr) s.bonusFoodLogCount else 0
        inMemoryBonusDate = dateStr
        inMemoryBonusLogs = count
        return count
    }

    fun incrementBonusFoodLogs(amount: Int = 2) {
        val dateStr = DateUtils.todayString()
        val currentCount = getBonusFoodLogs()
        val newCount = currentCount + amount
        inMemoryBonusDate = dateStr
        inMemoryBonusLogs = newCount
        runBlocking {
            update { s ->
                s.copy(bonusFoodLogDate = dateStr, bonusFoodLogCount = newCount)
            }
        }
    }

    fun getAiChatLimitCount(): Int {
        val dateStr = DateUtils.todayString()
        if (inMemoryAiChatDate == dateStr && inMemoryAiChatCount >= 0) {
            return inMemoryAiChatCount
        }
        val s = current
        val count = if (s.aiChatLimitDate == dateStr) s.aiChatLimitCount else 0
        inMemoryAiChatDate = dateStr
        inMemoryAiChatCount = count
        return count
    }

    suspend fun incrementAiChatLimitCount() {
        val dateStr = DateUtils.todayString()
        val currentCount = getAiChatLimitCount()
        val newCount = currentCount + 1
        inMemoryAiChatDate = dateStr
        inMemoryAiChatCount = newCount
        update { s ->
            s.copy(aiChatLimitDate = dateStr, aiChatLimitCount = newCount)
        }
    }
}
