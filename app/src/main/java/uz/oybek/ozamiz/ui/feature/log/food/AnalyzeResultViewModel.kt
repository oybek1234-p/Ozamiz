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
    val result: FoodAnalysis? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val currentStreak: Int = 0,

    val isSaved: Boolean = false,
    val isRecallMode: Boolean = false
)

sealed interface AnalyzeResultEvent {
    object Saved : AnalyzeResultEvent
    data class Failure(val message: String) : AnalyzeResultEvent
    data class LimitExceeded(val exceededTypes: List<String>) : AnalyzeResultEvent
    data class PromptShare(val name: String, val photoUrl: String, val calories: Int, val exceededTypes: List<String>? = null) : AnalyzeResultEvent
}

class AnalyzeResultViewModel : ViewModel() {

    private val liveResult = MutableStateFlow(AnalyzeResultState())
    val resultLive: StateFlow<AnalyzeResultState?> = liveResult

    private val _event = kotlinx.coroutines.channels.Channel<AnalyzeResultEvent>()
    val event = _event.receiveAsFlow()

    private var loadedLogDate: java.util.Date? = null
    private var loadedLogId: String? = null
    private var loadedUserId: String? = null

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
        val streak = uz.oybek.ozamiz.data.user.UserRepository.user?.streakInfo?.currentStreak ?: 0
        liveResult.update {
            it.copy(
                result = result,
                currentStreak = streak
            )
        }
        startUploadIfNeeded(result?.photoUrl)

        checkIfSaved()
    }

    fun setRecallResult(result: FoodAnalysis) {
        liveResult.update {
            it.copy(
                result = result,
                isRecallMode = true
            )
        }
        startUploadIfNeeded(result.photoUrl)

        checkIfSaved()
    }

    fun loadLog(logId: String, date: java.util.Date, userId: String? = null) {
        loadedLogDate = date
        loadedLogId = logId
        loadedUserId = userId
        viewModelScope.launch {
            liveResult.update { it.copy(isLoading = true) }
            val streak = uz.oybek.ozamiz.data.user.UserRepository.user?.streakInfo?.currentStreak ?: 0
            val log = FoodLogRepository.getFoodLog(logId, date, userId)
            if (log != null) {
                val analysis = FoodAnalysis(
                    id = log.logId.hashCode(),
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
                liveResult.update { 
                    it.copy(
                        result = analysis, 
                        isLoading = false, 
                        currentStreak = streak,
                        isRecallMode = true
                    ) 
                }
            } else {
                liveResult.update { it.copy(isError = true, isLoading = false, isRecallMode = false) }
            }

            checkIfSaved()
        }
    }

    private fun checkIfSaved() {
        val currentResult = liveResult.value.result ?: return
        viewModelScope.launch {
            val isSaved = uz.oybek.ozamiz.data.food.SavedFoodRepository.isFoodSaved(currentResult.result?.name ?: "")
            liveResult.update { it.copy(isSaved = isSaved) }
        }
    }

    fun toggleFavorite() {
        if (liveResult.value.isLoading) return
        val currentResult = liveResult.value.result ?: return
        val foodLog = currentResult.toFoodLog()
        viewModelScope.launch {
            val isSaved = uz.oybek.ozamiz.data.food.SavedFoodRepository.isFoodSaved(foodLog.description)
            if (isSaved) {
                uz.oybek.ozamiz.data.food.SavedFoodRepository.removeSavedFood(foodLog.description)
                _event.send(AnalyzeResultEvent.Failure("Saqlanganlardan olib tashlandi"))
            } else {
                uz.oybek.ozamiz.data.food.SavedFoodRepository.saveFood(foodLog)
                _event.send(AnalyzeResultEvent.Failure("Saqlanganlarga qo'shildi"))
            }
            checkIfSaved()
        }
    }

    fun reLog() {
        if (liveResult.value.isLoading) return
        val currentResult = liveResult.value.result ?: return
        val log = currentResult.toFoodLog().copy(
            logId = "",
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            liveResult.update { it.copy(isLoading = true) }
            FoodLogRepository.add(currentResult.photoUrl, log, java.util.Date())
            liveResult.update { it.copy(isLoading = false) }
            _event.send(AnalyzeResultEvent.Saved)
        }
    }

    fun deleteLog(onComplete: (Boolean) -> Unit) {
        if (liveResult.value.isLoading) return
        val logId = loadedLogId ?: return
        val date = loadedLogDate ?: return
        viewModelScope.launch {
            liveResult.update { it.copy(isLoading = true) }
            val success = FoodLogRepository.delete(logId, date, loadedUserId)
            liveResult.update { it.copy(isLoading = false) }
            onComplete(success)
        }
    }


    fun updateLog(newDescription: String) {
        if (liveResult.value.isLoading) return
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
                    FoodLogRepository.update(logId, updatedLog, date, loadedUserId)
                }
            } else {
                liveResult.update { it.copy(isLoading = false, isError = true) }
            }
        }
    }

    fun onSaveClick() {
        if (liveResult.value.isLoading) return
        val result = liveResult.value.result ?: return
        val photoUrl = result.photoUrl
        val score = result.result?.healthScore ?: 0
        val shouldShare = !photoUrl.isNullOrBlank() && photoUrl != "null" && score >= 8
        saveFood(shouldShare = shouldShare)
    }

    fun saveFood(shouldShare: Boolean) {
        if (liveResult.value.isLoading) return
        val result = liveResult.value.result ?: return
        liveResult.update { it.copy(isLoading = true) }
        CoroutineScope(Dispatchers.IO).launch {
            // 1. Upload image if needed
            val finalUrl = uploadJob?.await() ?: result.photoUrl
            if (finalUrl != result.photoUrl && finalUrl != null) {
                result.photoUrl = finalUrl
                FoodAnalyzer.updateCachedResult(result.id, finalUrl, result.mealType)
            }

            val log = result.toFoodLog()
            val targetDate = loadedLogDate ?: LogFragment.currentDate

            // 2. Prepare concurrent jobs
            val saveJob = async {
                FoodLogRepository.add(result.photoUrl, log, targetDate)
                StreakRepository.updateStreak()
                val score = result.result?.healthScore ?: 0
                val pointsDelta = when {
                    score >= 8 -> 10
                    score == 7 -> 5
                    score == 6 -> 0
                    score in 4..5 -> -5
                    else -> -10
                }
                if (pointsDelta != 0) {
                    uz.oybek.ozamiz.data.user.UserRepository.addPoints(pointsDelta)
                }
            }

            val shareJob = if (shouldShare) {
                async {
                    val user = uz.oybek.ozamiz.data.user.UserRepository.user
                    val userId = Firebase.auth.currentUser?.uid
                    if (user != null && userId != null) {
                        val post = uz.oybek.ozamiz.data.community.model.CommunityPost(
                            userId = userId,
                            userName = user.name.ifBlank { "Foydalanuvchi" },
                            photoUrl = result.photoUrl!!,
                            foodName = result.result!!.name,
                            calories = result.result!!.calories,
                            healthScore = result.result!!.healthScore ?: 0,
                            userScore = user.scorePoints
                        )
                        uz.oybek.ozamiz.data.community.CommunityPostRepository.addPost(post)
                    }
                }
            } else null

            // 3. Wait for both
            saveJob.await()
            shareJob?.await()

            // 4. Check limits and trigger notification
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
                    if (summary.consumedCalories > targetCalories) {
                        exceededTypes.add(DailyLimitFragment.TYPE_CALORIES)
                    }
                }

                if (exceededTypes.isNotEmpty()) {
                    uz.oybek.ozamiz.data.motivation.MotivationAlarmScheduler.scheduleNutrientWarning(
                        uz.oybek.ozamiz.appContext,
                        exceededTypes
                    )
                    _event.send(AnalyzeResultEvent.LimitExceeded(exceededTypes))
                } else {
                    liveResult.update { it.copy(isLoading = false) }
                    _event.send(AnalyzeResultEvent.Saved)
                }
            } else {
                liveResult.update { it.copy(isLoading = false) }
                _event.send(AnalyzeResultEvent.Saved)
            }
        }
    }

    fun submitAnswer(answer: String) {
        if (liveResult.value.isLoading) return
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
