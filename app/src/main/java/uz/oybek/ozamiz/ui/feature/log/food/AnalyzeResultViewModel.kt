package uz.oybek.ozamiz.ui.feature.log.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.food.analyzer.model.FoodAnalysis
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import uz.oybek.ozamiz.ui.feature.home.LogFragment

data class AnalyzeResultState(
    val result: FoodAnalysis? = null, val isLoading: Boolean = false, val isError: Boolean = false
)

class AnalyzeResultViewModel : ViewModel() {

    private val liveResult = MutableStateFlow(AnalyzeResultState())
    val resultLive: StateFlow<AnalyzeResultState?> = liveResult

    private var loadedLogDate: java.util.Date? = null
    private var loadedLogId: String? = null

    fun setResult(result: FoodAnalysis?) {
        liveResult.update {
            it.copy(
                result = result
            )
        }
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
                    photoUrl = log.photoUrl ?: "",
                    result = uz.oybek.ozamiz.data.food.analyzer.model.FoodAnalysisResult(
                        name = log.description,
                        calories = log.calories,
                        protein = log.protein,
                        fat = log.fat,
                        carbs = log.carbs,
                        ingredients = emptyList(),
                        healthScore = log.healthLevel,
                        aiInsight = log.aiInsights,
                        timestamp = (log.timestamp as? Number)?.toLong() ?: System.currentTimeMillis()
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
        val logId = loadedLogId // Can be null
        val date = loadedLogDate // Can be null

        viewModelScope.launch {
            liveResult.update { it.copy(isLoading = true) }
            
            // Re-analyze
            val prompt = "$newDescription. (Do not ask questions, provide best possible estimate)"
            val initialContent = uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzer.createInitialContent(
                null, // Use null for image as we are text-correcting
                prompt,
                currentResult.mealType
            )
            val history = listOf(initialContent)
            
            val analysis = uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzer.analyze(history)

            if (analysis != null) {
                // Keep original interactions/photo
                analysis.photoUrl = currentResult.photoUrl
                analysis.mealType = currentResult.mealType
                
                // Update local state - This triggers UI update
                liveResult.update { it.copy(result = analysis, isLoading = false) }
                
                // If existing log, update remote
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
            val log = result.toFoodLog()
            // If we have a loaded log ID, we are likely editing, not saving a new one, 
            // but the button says "Save to Log", which usually implies a new entry or committing the analysis.
            // However, based on the flow, this fragment is shown after analysis.
            
            FoodLogRepository.add(
                 result.photoUrl,
                 log,
                 loadedLogDate ?: LogFragment.currentDate // Use loaded date or current global date
            )
             uz.oybek.ozamiz.data.streak.StreakRepository.updateStreak()
             liveResult.update { it.copy(isLoading = false) }
        }
    }
    fun submitAnswer(answer: String) {
        val currentAnalysis = liveResult.value.result ?: return
        val id = currentAnalysis.id
        
        viewModelScope.launch {
            liveResult.update { it.copy(isLoading = true) }
            
            // Get history from cache
            val history = uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzer.getHistory(id)
            if (history.isEmpty()) {
                liveResult.update { it.copy(isLoading = false, isError = true) }
                return@launch
            }
            
            // Append user answer
            val answerContent = com.google.firebase.ai.type.content(role = "user") {
                text(answer)
            }
            val newHistory = history + answerContent
            
            // Re-analyze
            val newAnalysis = uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzer.analyze(newHistory)
            
            if (newAnalysis != null) {
                // Keep the photoUrl from original since re-analysis might lose it if not careful 
                // (though FoodAnalyzer analyze sets empty string currently, we should fix that or carry it over)
                newAnalysis.photoUrl = currentAnalysis.photoUrl
                newAnalysis.mealType = currentAnalysis.mealType // Carry over meal type
                
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