package uz.oybek.ozamiz.ui.feature.log.food

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.uploader.ImagePaths
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader
import uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzer
import uz.oybek.ozamiz.data.food.analyzer.model.FoodAnalysis
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import uz.oybek.ozamiz.data.streak.StreakRepository
import uz.oybek.ozamiz.ui.feature.home.LogFragment

data class AnalyzeResultState(
    val result: FoodAnalysis? = null, val isLoading: Boolean = false, val isError: Boolean = false
)

sealed interface AnalyzeResultEvent {
    object Saved : AnalyzeResultEvent
    data class LimitExceeded(val exceededTypes: List<String>) : AnalyzeResultEvent
}

class AnalyzeResultViewModel : ViewModel() {

    private val liveResult = MutableStateFlow(AnalyzeResultState())
    val resultLive: StateFlow<AnalyzeResultState?> = liveResult

    private val _event = kotlinx.coroutines.channels.Channel<AnalyzeResultEvent>()
    val event = _event.receiveAsFlow()

    private var loadedLogDate: java.util.Date? = null
    private var loadedLogId: String? = null

    private var uploadJob: kotlinx.coroutines.Deferred<String?>? = null

    private fun startUploadIfNeeded(photoUrl: String?) {
        if (photoUrl != null && !photoUrl.startsWith("http") && uploadJob == null) {
            uploadJob = CoroutineScope(Dispatchers.IO).async {
                val url = ImageUploader.uploadImage(
                    photoUrl.toUri(),
                    ImagePaths.ANALYZE_FOOD
                )
                url
            }
        }
    }

    fun setResult(result: FoodAnalysis?) {
        liveResult.update {
            it.copy(
                result = result
            )
        }
        startUploadIfNeeded(result?.photoUrl)
    }

    fun loadLog(logId: String, date: java.util.Date) {
        loadedLogDate = date
        loadedLogId = logId
        viewModelScope.launch {
            liveResult.update { it.copy(isLoading = true) }
            val log = FoodLogRepository.getFoodLog(logId, date)
            if (log != null) {
                val analysis = FoodAnalysis(
                    id = log.logId.hashCode(), // Store hash or just rely on loadedLogId
                    photoUrl = log.photoUrl?.takeIf { it.isNotBlank() },
                    result = uz.oybek.ozamiz.data.food.analyzer.model.FoodAnalysisResult(
                        name = log.description,
                        calories = log.calories,
                        protein = log.protein,
                        fat = log.fat,
                        carbs = log.carbs,
                        ingredients = emptyList(),
                        healthScore = log.healthLevel,
                        aiInsight = log.aiInsights,
                        timestamp = (log.timestamp as? Number)?.toLong()
                            ?: System.currentTimeMillis()
                    ),
                    mealType = log.mealType
                )
                liveResult.update { it.copy(result = analysis, isLoading = false) }
            } else {
                liveResult.update { it.copy(isError = true, isLoading = false) }
            }
        }
    }

    fun deleteLog(onComplete: () -> Unit) {
        val logId = loadedLogId ?: return
        val date = loadedLogDate ?: return
        viewModelScope.launch {
            liveResult.update { it.copy(isLoading = true) }
            FoodLogRepository.delete(logId, date)
            onComplete()
        }
    }


    fun updateLog(newDescription: String) {
        val currentResult = liveResult.value.result ?: return
        val logId = loadedLogId
        val date = loadedLogDate

        viewModelScope.launch {
            liveResult.update { it.copy(isLoading = true) }

            val prompt = "$newDescription. (Do not ask questions, provide best possible estimate)"
            val initialContent =
                FoodAnalyzer.createInitialContent(
                    null,
                    prompt,
                    currentResult.mealType
                )
            val history = listOf(initialContent)

            val analysis = FoodAnalyzer.analyze(history)

            if (analysis != null) {
                analysis.photoUrl = currentResult.photoUrl
                analysis.mealType = currentResult.mealType
                FoodAnalyzer.updateCachedResult(
                    analysis.id,
                    currentResult.photoUrl,
                    currentResult.mealType
                )

                liveResult.update { it.copy(result = analysis, isLoading = false) }
                if (logId != null && date != null) {
                    val updatedLog = analysis.toFoodLog()
                    updatedLog.logId = logId
                    FoodLogRepository.update(logId, updatedLog, date)
                }
            } else {
                liveResult.update { it.copy(isLoading = false, isError = true) }
            }
        }
    }

    fun saveLog() {
        val result = liveResult.value.result ?: return
        viewModelScope.launch {
            liveResult.update { it.copy(isLoading = true) }

            val finalUrl = uploadJob?.await() ?: result.photoUrl
            if (finalUrl != result.photoUrl && finalUrl != null) {
                result.photoUrl = finalUrl
                FoodAnalyzer.updateCachedResult(result.id, finalUrl, result.mealType)
            }

            val log = result.toFoodLog()
            val targetDate = loadedLogDate ?: LogFragment.currentDate

            FoodLogRepository.add(
                result.photoUrl,
                log,
                targetDate
            )
            StreakRepository.updateStreak()

            val uid = Firebase.auth.currentUser?.uid
            if (uid != null) {
                val user = uz.oybek.ozamiz.data.user.UserRepository.user
                val targetCalories = user?.targetCalories ?: 0

                val dateStr = uz.oybek.ozamiz.data.utils.DateUtils.getFormattedDateString(
                    targetDate,
                    "yyyy-MM-dd"
                )
                val summaryRef =
                    uz.oybek.ozamiz.data.firebase.FirebasePaths.dailySummaryReference(uid, dateStr)
                val snapshot = summaryRef.get().await()
                val summary =
                    snapshot.getValue(uz.oybek.ozamiz.data.logs.model.DailySummary::class.java)

                val exceededTypes = mutableListOf<String>()
                if (summary != null && targetCalories > 0) {
                    // Kaloriya
                    if (summary.consumedCalories > targetCalories) {
                        exceededTypes.add(LimitExceededDialog.TYPE_CALORIES)
                    }
                    // Makro limitlarni kaloriyadan hisoblash
                    // Oqsil: 30% (1g = 4 kal), Yog': 25% (1g = 9 kal), Uglevod: 45% (1g = 4 kal)
                    val targetProtein = (targetCalories * 0.30) / 4.0
                    val targetFat = (targetCalories * 0.25) / 9.0
                    val targetCarbs = (targetCalories * 0.45) / 4.0

                    if (summary.consumedProtein > targetProtein) {
                        exceededTypes.add(LimitExceededDialog.TYPE_PROTEIN)
                    }
                    if (summary.consumedFat > targetFat) {
                        exceededTypes.add(LimitExceededDialog.TYPE_FAT)
                    }
                    if (summary.consumedCarbs > targetCarbs) {
                        exceededTypes.add(LimitExceededDialog.TYPE_CARBS)
                    }
                }

                if (exceededTypes.isNotEmpty()) {
                    uz.oybek.ozamiz.data.motivation.MotivationAlarmScheduler.scheduleNutrientWarning(
                        uz.oybek.ozamiz.appContext,
                        exceededTypes
                    )
                    _event.send(AnalyzeResultEvent.LimitExceeded(exceededTypes))
                } else {
                    _event.send(AnalyzeResultEvent.Saved)
                }
            } else {
                _event.send(AnalyzeResultEvent.Saved)
            }

            liveResult.update { it.copy(isLoading = false) }
        }
    }

    fun submitAnswer(answer: String) {
        val currentAnalysis = liveResult.value.result ?: return
        val id = currentAnalysis.id

        viewModelScope.launch {
            liveResult.update { it.copy(isLoading = true) }

            val history = FoodAnalyzer.getHistory(id)
            if (history.isEmpty()) {
                liveResult.update { it.copy(isLoading = false, isError = true) }
                return@launch
            }
            val answerContent = com.google.firebase.ai.type.content(role = "user") {
                text(answer)
            }
            val newHistory = history + answerContent

            val newAnalysis = FoodAnalyzer.analyze(newHistory)
            if (newAnalysis != null) {
                newAnalysis.photoUrl = currentAnalysis.photoUrl
                newAnalysis.mealType = currentAnalysis.mealType
                FoodAnalyzer.updateCachedResult(
                    newAnalysis.id,
                    currentAnalysis.photoUrl,
                    currentAnalysis.mealType
                )

                liveResult.update { it.copy(result = newAnalysis, isLoading = false) }
            } else {
                liveResult.update { it.copy(isLoading = false, isError = true) }
            }
        }
    }

    fun updateCalories(calories: Double) {
        liveResult.update { state ->
            val currentResult = state.result ?: return@update state
            val analysis = currentResult.result ?: return@update state
            state.copy(
                result = currentResult.copy(
                    result = analysis.copy(calories = calories.toInt())
                )
            )
        }
    }

    fun updateProtein(protein: Double) {
        liveResult.update { state ->
            val currentResult = state.result ?: return@update state
            val analysis = currentResult.result ?: return@update state
            state.copy(
                result = currentResult.copy(
                    result = analysis.copy(protein = protein)
                )
            )
        }
    }

    fun updateFat(fat: Double) {
        liveResult.update { state ->
            val currentResult = state.result ?: return@update state
            val analysis = currentResult.result ?: return@update state
            state.copy(
                result = currentResult.copy(
                    result = analysis.copy(fat = fat)
                )
            )
        }
    }

    fun updateCarbs(carbs: Double) {
        liveResult.update { state ->
            val currentResult = state.result ?: return@update state
            val analysis = currentResult.result ?: return@update state
            state.copy(
                result = currentResult.copy(
                    result = analysis.copy(carbs = carbs)
                )
            )
        }
    }
}