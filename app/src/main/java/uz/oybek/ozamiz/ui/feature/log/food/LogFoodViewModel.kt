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
}

class LogFoodViewModel() : ViewModel() {

    private val _state = MutableStateFlow(LogFoodState())
    val state = _state.asStateFlow()

    private val _event = Channel<LogEvent>()
    val event = _event.receiveAsFlow()

    fun updateMeal(
        meal: String
    ) {
        _state.update { it.copy(meal = meal) }
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

    fun cancelAnalyze() {
        analyzeJob?.cancel()
        _state.update { it.copy(isAnalyzing = false) }
    }

}
