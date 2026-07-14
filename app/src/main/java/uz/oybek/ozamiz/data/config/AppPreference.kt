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
    val isFirstRegistration: Boolean = false,
    val isSecondOnboardingSheetSeen: Boolean = false,
    val isFirstLogAdded: Boolean = false,
    val isAppReviewShown: Boolean = false,
    val dietologMessageCount: Int = 0,
    val foodLogLimitDate: String = "",
    val foodLogLimitCount: Int = 0,
    val lastDailyAnalysisDate: String = "",
    val lastWaterBenefitsDate: String = "",
    val lastWater60AnalysisDate: String = "",
    val lastExerciseBenefitsDate: String = "",
    val lastExercise50AnalysisDate: String = "",
    val currentTaskId: String? = null,
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

    val settingsFlow: Flow<AppSettings> = dataStore.data

    val current: AppSettings get() = runBlocking { dataStore.data.first() }

    fun updateSync(block: (AppSettings) -> AppSettings) {
        runBlocking {
            dataStore.updateData { block(it) }
        }
    }

    suspend fun update(block: (AppSettings) -> AppSettings) {
        dataStore.updateData { block(it) }
    }

    var isSecondOnboardingSheetSeen: Boolean
        get() = current.isSecondOnboardingSheetSeen
        set(value) = runBlocking { update { it.copy(isSecondOnboardingSheetSeen = value) } }

    var isFirstLogAdded: Boolean
        get() = current.isFirstLogAdded
        set(value) = runBlocking { update { it.copy(isFirstLogAdded = value) } }

    var isAppReviewShown: Boolean
        get() = current.isAppReviewShown
        set(value) = runBlocking { update { it.copy(isAppReviewShown = value) } }

    var lastDailyAnalysisDate: String
        get() = current.lastDailyAnalysisDate
        set(value) = runBlocking { update { it.copy(lastDailyAnalysisDate = value) } }

    var lastWaterBenefitsDate: String
        get() = current.lastWaterBenefitsDate
        set(value) = runBlocking { update { it.copy(lastWaterBenefitsDate = value) } }

    var lastWater60AnalysisDate: String
        get() = current.lastWater60AnalysisDate
        set(value) = runBlocking { update { it.copy(lastWater60AnalysisDate = value) } }

    var lastExerciseBenefitsDate: String
        get() = current.lastExerciseBenefitsDate
        set(value) = runBlocking { update { it.copy(lastExerciseBenefitsDate = value) } }

    var lastExercise50AnalysisDate: String
        get() = current.lastExercise50AnalysisDate
        set(value) = runBlocking { update { it.copy(lastExercise50AnalysisDate = value) } }

    suspend fun incrementDietologMessageCount() {
        update { s ->
            val count = s.dietologMessageCount + 1
            s.copy(dietologMessageCount = count)
        }
    }

    fun getFoodLogLimitCount(): Int {
        val s = current
        val dateStr = DateUtils.todayString()
        return if (s.foodLogLimitDate == dateStr) s.foodLogLimitCount else 0
    }

    suspend fun incrementFoodLogLimitCount() {
        val dateStr = DateUtils.todayString()
        update { s ->
            val count = if (s.foodLogLimitDate == dateStr) s.foodLogLimitCount + 1 else 1
            s.copy(foodLogLimitDate = dateStr, foodLogLimitCount = count)
        }
    }
}
