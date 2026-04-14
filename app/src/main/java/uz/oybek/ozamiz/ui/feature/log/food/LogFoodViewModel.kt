package uz.oybek.ozamiz.ui.feature.log.food

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import uz.oybek.ozamiz.data.firebase.uploader.ImageCompressor
import uz.oybek.ozamiz.data.firebase.uploader.ImagePaths
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader
import uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzer
import uz.oybek.ozamiz.data.food.analyzer.model.FoodAnalysis
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.food.SavedFoodRepository
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Date

data class LogFoodState(
    val isAnalyzing: Boolean = false,
    val description: String = "",
    val image: Uri? = null,
    val meal: String = MealTime.Breakfast.id,
    val result: FoodAnalysis? = null,
    val editing: FoodLog? = null,
    val uploadUrl: String? = null,
    val date: Date = Date()
)

sealed interface LogEvent {
    sealed class Error : LogEvent {
        object EmptyInput : Error()
        object AnalyzeError : Error()
    }

    data class Analyzed(
        val data: FoodAnalysis
    ) : LogEvent

    data class ClarificationNeeded(
        val data: FoodAnalysis
    ) : LogEvent

    data class Message(val text: String) : LogEvent
}

class LogFoodViewModel() : ViewModel() {

    private val _state = MutableStateFlow(LogFoodState())
    val state = _state.asStateFlow()

    private val _event = Channel<LogEvent>()
    val event = _event.receiveAsFlow()

    private val _loadingFoodIds = MutableStateFlow<Set<String>>(emptySet())
    val loadingFoodIds = _loadingFoodIds.asStateFlow()

    val savedFoods = SavedFoodRepository.getSavedFoods()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val savedFoodIds = savedFoods.map { it.map { food -> food.logId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    fun updateMeal(
        meal: String
    ) {
        _state.update { it.copy(meal = meal) }
    }

    fun logFood(foodLog: FoodLog, date: Date) {
        viewModelScope.launch {
            FoodLogRepository.add(
                image = null,
                foodLog = foodLog,
                date = date
            )
        }
    }

    fun reLogFood(foodLog: FoodLog) {
        viewModelScope.launch {
            val duplicate = foodLog.copy(
                logId = "",
                timestamp = System.currentTimeMillis()
            )
            FoodLogRepository.add(null, duplicate, state.value.date)
            _event.send(LogEvent.Message("Qayta qo'shildi!"))
        }
    }

    fun toggleFavorite(foodLog: FoodLog) {
        viewModelScope.launch {
            val isSaved = SavedFoodRepository.isFoodSaved(foodLog.logId)
            if (isSaved) {
                SavedFoodRepository.removeSavedFood(foodLog.logId)
                _event.send(LogEvent.Message("Sevimlilardan olib tashlandi"))
            } else {
                SavedFoodRepository.saveFood(foodLog)
                _event.send(LogEvent.Message("Sevimlilarga qo'shildi!"))
            }
        }
    }

    fun removeFavorite(foodLog: FoodLog) {
        viewModelScope.launch {
            SavedFoodRepository.removeSavedFood(foodLog.logId)
            _event.send(LogEvent.Message("Sevimlilardan olib tashlandi"))
        }
    }

    fun logSavedFood(foodLog: FoodLog) {
        viewModelScope.launch {
            _loadingFoodIds.update { it + foodLog.logId }
            val log = foodLog.copy(
                logId = "",
                timestamp = System.currentTimeMillis(),
                mealType = state.value.meal
            )
            kotlinx.coroutines.delay(1000)
            FoodLogRepository.add(null, log, state.value.date)
            _loadingFoodIds.update { it - foodLog.logId }
            _event.send(LogEvent.Message("Qo'shildi!"))
        }
    }

    fun updateDescription(text: String?) {
        _state.update { it.copy(description = text.toString()) }

    }

    fun analyzePhoto(
        image: Uri
    ) {
        _state.update { it.copy(image = image) }
        analyze()
    }

    private var analyzeJob: Job? = null

    fun submitAnswer(answer: String) {
        val currentAnalysis = state.value.result ?: run {
            android.util.Log.e("LogFoodViewModel", "❌ submitAnswer: currentAnalysis is null")
            return
        }
        val id = currentAnalysis.id

        android.util.Log.d("LogFoodViewModel", "🔍 submitAnswer called with: '$answer', id=$id")

        viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true) }

            val history = uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzer.getHistory(id)
            android.util.Log.d("LogFoodViewModel", "📚 History size: ${history.size}")

            if (history.isEmpty()) {
                android.util.Log.e("LogFoodViewModel", "❌ History is empty for id=$id")
                _state.update { it.copy(isAnalyzing = false) }
                _event.send(LogEvent.Error.AnalyzeError)
                return@launch
            }

            val answerContent = com.google.firebase.ai.type.content(role = "user") {
                text(answer)
            }
            val newHistory = history + answerContent
            android.util.Log.d("LogFoodViewModel", "📝 New history size: ${newHistory.size}")

            val result = uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzer.analyze(newHistory)

            android.util.Log.d(
                "LogFoodViewModel",
                "📊 Analysis result: ${if (result != null) "SUCCESS" else "NULL"}"
            )

            _state.update { it.copy(result = result, isAnalyzing = false) }

            if (result != null) {
                result.photoUrl = currentAnalysis.photoUrl
                result.mealType = currentAnalysis.mealType
                FoodAnalyzer.updateCachedResult(result.id, currentAnalysis.photoUrl, currentAnalysis.mealType)

                android.util.Log.d(
                    "LogFoodViewModel",
                    "✅ Result preserved with photoUrl=${result.photoUrl}, mealType=${result.mealType}"
                )
                android.util.Log.d(
                    "LogFoodViewModel",
                    "🔍 isAmbiguous=${result.result?.isAmbiguous}"
                )

                if (result.result?.isAmbiguous == true) {
                    android.util.Log.d("LogFoodViewModel", "❓ Sending ClarificationNeeded event")
                    _event.send(LogEvent.ClarificationNeeded(result))
                } else {
                    android.util.Log.d("LogFoodViewModel", "✅ Sending Analyzed event")
                    _event.send(LogEvent.Analyzed(result))
                }
            } else {
                android.util.Log.e("LogFoodViewModel", "❌ Result is null, sending AnalyzeError")
                _event.send(LogEvent.Error.AnalyzeError)
            }
        }
    }

    fun analyze() {
        analyzeJob?.cancel()
        analyzeJob = viewModelScope.launch {
            val state = this@LogFoodViewModel.state.value

            if (state.description.isBlank()
                && state.image == null
            ) {
                _event.send(LogEvent.Error.EmptyInput)
                return@launch
            }

            _state.update { it.copy(isAnalyzing = true) }

            val compressor = state.image?.let {
                ImageCompressor.compressImageFile(
                    it
                )
            }
            
            android.util.Log.d("LogFoodViewModel", "AI Analyze: Image compressed, creating initial content to send to FoodAnalyzer...")

            val initialContent = FoodAnalyzer.createInitialContent(
                compressor?.toUri(),
                state.description,
                state.meal
            )

            // Initial history contains just the user's prompt
            val history = listOf(initialContent)

            android.util.Log.d("LogFoodViewModel", "AI Analyze: Sending request to Gemini...")
            val result = FoodAnalyzer.analyze(history)
            android.util.Log.d("LogFoodViewModel", "AI Analyze: Request complete. Result success=${result != null}")

            _state.update {
                it.copy(
                    result = result,
                    isAnalyzing = false
                )
            }
            if (result != null) {
                // Set the local image path for later upload and meal type
                result.photoUrl = state.image?.toString()
                result.mealType = state.meal
                FoodAnalyzer.updateCachedResult(result.id, state.image?.toString(), state.meal)
                
                if (result.result?.isAmbiguous == true) {
                    _event.send(LogEvent.ClarificationNeeded(result))
                } else {
                    _event.send(LogEvent.Analyzed(result))
                }
            } else {
                _event.send(LogEvent.Error.AnalyzeError)
            }
        }
    }

    fun analyzeAudio(
        audioUri: Uri
    ) {
        analyzeJob?.cancel()
        analyzeJob = viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true) }
            val result = FoodAnalyzer.analyzeAudio(audioUri, state.value.meal)
            
            _state.update { it.copy(result = result, isAnalyzing = false) }
            
            if (result != null) {
                result.mealType = state.value.meal
                FoodAnalyzer.updateCachedResult(result.id, null, state.value.meal)
                
                if (result.result?.isAmbiguous == true) {
                    _event.send(LogEvent.ClarificationNeeded(result))
                } else {
                    _event.send(LogEvent.Analyzed(result))
                }
            } else {
                _event.send(LogEvent.Error.AnalyzeError)
            }
        }
    }

    fun cancelAnalyze() {
        analyzeJob?.cancel()
        _state.update { it.copy(isAnalyzing = false) }
    }

}
