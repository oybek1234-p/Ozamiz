package uz.oybek.ozamiz.ui.feature.log.food

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.ai.analyzer.FoodAnalysis
import uz.oybek.ozamiz.data.ai.analyzer.FoodAnalysisResult
import uz.oybek.ozamiz.data.ai.analyzer.FoodAnalyzer
import uz.oybek.ozamiz.data.ai.analyzer.SingleIngredientResult
import uz.oybek.ozamiz.data.firebase.uploader.ImageCompressor
import uz.oybek.ozamiz.data.food.SavedFoodRepository
import uz.oybek.ozamiz.data.food.model.MealTime
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import uz.oybek.ozamiz.data.logs.model.FoodLog
import java.util.Date

sealed interface AnalyzeInput {
    data class Image(val uri: Uri?, val description: String, val meal: String) : AnalyzeInput
    data class Voice(val audioUri: Uri, val meal: String) : AnalyzeInput
}

data class AnalyzeResultState(
    val result: FoodAnalysis? = null,
    val logId: String? = null,
    val userId: String? = null,
    val logDate: Date = Date(),
    val input: AnalyzeInput? = null,
    val isAnalyzing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isFavorite: Boolean = false,
    val isError: Boolean = false,
    val isRecallMode: Boolean = false
) {
    val pendingDescription: String?
        get() = when (input) {
            is AnalyzeInput.Image -> input.description
            is AnalyzeInput.Voice -> "pending_description_voice"
            null -> null
        }

    val pendingImageUri: Uri?
        get() = (input as? AnalyzeInput.Image)?.uri
}

sealed interface AnalyzeResultEvent {
    data object Saved : AnalyzeResultEvent
    data class PromptShare(
        val name: String,
        val photoUrl: String?,
        val calories: Int,
        val protein: Double?,
        val fat: Double?,
        val carbs: Double?,
        val advice: String?,
        val mealType: String,
        val timestamp: Long
    ) : AnalyzeResultEvent
}

class AnalyzeResultViewModel : ViewModel() {

    private val _state = MutableStateFlow(AnalyzeResultState())
    val state: StateFlow<AnalyzeResultState> = _state.asStateFlow()

    private val _event = Channel<AnalyzeResultEvent>()
    val event = _event.receiveAsFlow()

    private var autoSaveJob: Job? = null
    private var syncUpdateJob: Job? = null
    private var baseFoodAnalysisResult: FoodAnalysisResult? = null
    private var lastSavedFoodLog: FoodLog? = null

    fun autoSaveFood(result: FoodAnalysis) {
        val currentState = _state.value
        if (currentState.isSaved || currentState.logId != null) {
            return
        }
        autoSaveJob?.cancel()

        _state.update { it.copy(isSaving = true) }
        val log = result.toFoodLog()
        val targetDate = currentState.logDate

        autoSaveJob = viewModelScope.launch(Dispatchers.IO) {
            val success = FoodLogRepository.add(
                foodLog = log,
                targetDate
            )
            _state.update {
                if (success) {
                    it.copy(
                        logId = log.logId,
                        isSaved = true,
                        isSaving = false
                    )
                } else {
                    it.copy(
                        isSaved = false,
                        isSaving = false
                    )
                }
            }
        }
    }

    fun startAnalyze(imageUri: Uri?, description: String, meal: String, date: Date) {
        val normalizedMeal = MealTime.getValidMeal(meal, date.time)
        val input = AnalyzeInput.Image(imageUri, description, normalizedMeal)

        _state.update {
            it.copy(
                logDate = date,
                input = input,
                isAnalyzing = true,
                isError = false,
                isSaved = false,
                isSaving = false,
                logId = null
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val uri = imageUri?.let {
                ImageCompressor.compressImageToUri(it, 512, 512)
            }

            var result =
                FoodAnalyzer.analyze(description = description, meal = normalizedMeal, image = uri)
            if (result == null) {
                kotlinx.coroutines.delay(1500)
                result = FoodAnalyzer.analyze(
                    description = description,
                    meal = normalizedMeal,
                    image = uri
                )
            }
            if (result == null) {
                kotlinx.coroutines.delay(2000)
                result = FoodAnalyzer.analyze(
                    description = description,
                    meal = normalizedMeal,
                    image = uri
                )
            }

            if (result != null) {
                result.photoUrl = uri?.toString()
                result.mealType = normalizedMeal
                autoSaveFood(result)
            }

            _state.update {
                it.copy(
                    result = result,
                    isAnalyzing = false,
                    isError = result == null
                )
            }
            if (result != null) checkIfSaved()
        }
    }

    fun analyzeAudio(audioUri: Uri, meal: String, date: Date) {
        val normalizedMeal = MealTime.getValidMeal(meal, date.time)
        val input = AnalyzeInput.Voice(audioUri, normalizedMeal)

        _state.update {
            it.copy(
                logDate = date,
                input = input,
                isAnalyzing = true,
                isError = false,
                isSaved = false,
                isSaving = false,
                logId = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            var result = FoodAnalyzer.analyzeAudio(audioUri, normalizedMeal)
            if (result == null) {
                kotlinx.coroutines.delay(1500)
                result = FoodAnalyzer.analyzeAudio(audioUri, normalizedMeal)
            }
            if (result == null) {
                kotlinx.coroutines.delay(2000)
                result = FoodAnalyzer.analyzeAudio(audioUri, normalizedMeal)
            }

            if (result != null) {
                result.mealType = normalizedMeal
                autoSaveFood(result)
            }
            _state.update {
                it.copy(
                    result = result,
                    isAnalyzing = false,
                    isError = result == null
                )
            }
            if (result != null) checkIfSaved()
        }
    }

    fun retryAnalyze() {
        val currentState = _state.value
        if (currentState.isAnalyzing) return
        val date = currentState.logDate
        val oldLogId = currentState.logId
        val oldFoodLog = currentState.result?.toFoodLog()?.apply { logId = oldLogId ?: "" }

        if (oldFoodLog != null && !oldFoodLog.logId.isNullOrEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                FoodLogRepository.delete(oldFoodLog, date, currentState.userId)
            }
        }

        when (val input = currentState.input) {
            is AnalyzeInput.Image -> startAnalyze(input.uri, input.description, input.meal, date)
            is AnalyzeInput.Voice -> analyzeAudio(input.audioUri, input.meal, date)
            null -> Unit
        }
    }

    fun setRecallResult(result: FoodAnalysis) {
        _state.update {
            it.copy(
                result = result,
                isRecallMode = true,
                isSaved = true
            )
        }
        checkIfSaved()
    }

    fun setLogDate(date: Date) {
        _state.update { it.copy(logDate = date) }
    }

    fun loadLog(
        logId: String, date: Date, userId: String? = null
    ) {
        _state.update {
            it.copy(
                logId = logId,
                logDate = date,
                userId = userId,
                isLoading = true,
                isSaved = true,
                isRecallMode = true
            )
        }

        viewModelScope.launch {
            val log = FoodLogRepository.getFoodLog(logId, date, userId)
                ?: SavedFoodRepository.getSavedFood(logId)

            if (log != null) {
                val parsedResult = if (!log.rawAnalysisJson.isNullOrEmpty()) {
                    runCatching {
                        uz.oybek.ozamiz.data.utils.gson?.fromJson(
                            log.rawAnalysisJson,
                            FoodAnalysisResult::class.java
                        )
                    }.getOrNull()
                } else null

                val savedMultiplier = parsedResult?.portionMultiplier ?: 1.0
                val finalResult = parsedResult?.copy(
                    name = log.description,
                    calories = log.calories,
                    protein = log.protein,
                    fat = log.fat,
                    carbs = log.carbs,
                    healthScore = log.healthLevel ?: parsedResult.healthScore,
                    timestamp = (log.timestamp as? Number)?.toLong() ?: parsedResult.timestamp,
                    portionMultiplier = savedMultiplier
                ) ?: FoodAnalysisResult(
                    name = log.description,
                    calories = log.calories,
                    protein = log.protein,
                    fat = log.fat,
                    carbs = log.carbs,
                    foodItems = log.foodItems,
                    healthScore = log.healthLevel,
                    advice = log.advice ?: log.aiInsights,
                    tags = log.tags,
                    timestamp = (log.timestamp as? Number)?.toLong() ?: System.currentTimeMillis(),
                    portionMultiplier = 1.0
                )

                val analysis = FoodAnalysis(
                    id = log.logId.hashCode(),
                    photoUrl = log.photoUrl?.takeIf { it.isNotBlank() },
                    result = finalResult,
                    mealType = log.mealType
                )
                _state.update {
                    it.copy(
                        result = analysis,
                        isLoading = false
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isError = true,
                        isLoading = false,
                        isRecallMode = false
                    )
                }
            }

            checkIfSaved()
        }
    }

    private fun checkIfSaved() {
        val currentResult = _state.value.result ?: return
        viewModelScope.launch {
            val isFav = SavedFoodRepository.isFoodSavedByDescription(
                currentResult.result?.name ?: ""
            )
            _state.update { it.copy(isFavorite = isFav) }
        }
    }

    fun toggleFavorite() {
        if (_state.value.isLoading) return
        val currentResult = _state.value.result ?: return
        val foodLog = currentResult.toFoodLog()
        viewModelScope.launch {
            val isFav = SavedFoodRepository.isFoodSavedByDescription(foodLog.description)
            if (isFav) {
                SavedFoodRepository.removeSavedFoodByDescription(foodLog.description)
            } else {
                SavedFoodRepository.saveFood(foodLog)
            }
            checkIfSaved()
        }
    }

    fun shareFood() {
        val currentState = _state.value
        val result = currentState.result ?: return
        val analysis = result.result ?: return
        viewModelScope.launch {
            _event.send(
                AnalyzeResultEvent.PromptShare(
                    name = analysis.name,
                    photoUrl = result.photoUrl ?: currentState.pendingImageUri?.toString(),
                    calories = analysis.getEffectiveCalories(),
                    protein = analysis.getEffectiveProtein(),
                    fat = analysis.getEffectiveFat(),
                    carbs = analysis.getEffectiveCarbs(),
                    advice = analysis.advice,
                    mealType = result.mealType,
                    timestamp = analysis.timestamp
                )
            )
        }
    }

    fun reLog() {
        if (_state.value.isLoading) return
        val currentResult = _state.value.result ?: return
        val log = currentResult.toFoodLog().copy(
            logId = "", timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val targetDate = _state.value.logDate
            FoodLogRepository.add(
                log, targetDate
            )
            _state.update { it.copy(isLoading = false) }
            _event.send(AnalyzeResultEvent.Saved)
        }
    }

    fun deleteLog(onComplete: (Boolean) -> Unit) {
        if (_state.value.isLoading) return
        val currentState = _state.value
        val logId = currentState.logId ?: return
        val date = currentState.logDate
        val foodLog = currentState.result?.toFoodLog()?.apply { this.logId = logId } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true) }
            val success = FoodLogRepository.delete(foodLog, date, currentState.userId)
            _state.update { it.copy(isLoading = false, isSaved = false, logId = null) }
            onComplete(success)
        }
    }

    fun cancelAndExit() {
        val currentState = _state.value
        val saveJob = autoSaveJob
        val logId = currentState.logId
        val date = currentState.logDate
        val userId = currentState.userId
        val foodLog = currentState.result?.toFoodLog()

        viewModelScope.launch(Dispatchers.IO) {
            saveJob?.join()
            val finalLogId = logId ?: _state.value.logId
            if (finalLogId != null && foodLog != null) {
                foodLog.logId = finalLogId
                FoodLogRepository.delete(foodLog, date, userId)
            }
        }
        _state.update { it.copy(logId = null, isSaved = false) }
    }

    fun onSaveClick() {
        val currentState = _state.value
        if (currentState.isLoading || currentState.isSaving) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            autoSaveJob?.join()
            _state.update { it.copy(isSaving = false, isSaved = true) }
            _event.send(AnalyzeResultEvent.Saved)
        }
    }

    private fun syncLogUpdate(oldLog: FoodLog? = null) {
        val currentState = _state.value
        val currentResult = currentState.result ?: return
        val date = currentState.logDate
        val userId = currentState.userId
        val previousLog = oldLog ?: lastSavedFoodLog

        syncUpdateJob?.cancel()
        syncUpdateJob = viewModelScope.launch(Dispatchers.IO) {
            autoSaveJob?.join()
            val logId = _state.value.logId ?: return@launch
            val updatedLog = currentResult.toFoodLog()
            updatedLog.logId = logId
            lastSavedFoodLog = updatedLog
            FoodLogRepository.update(previousLog, updatedLog, date, userId)
        }
    }

    fun updateName(name: String) {
        val oldLog = _state.value.result?.toFoodLog()?.apply { logId = _state.value.logId ?: "" }
        _state.update { state ->
            val currentResult = state.result ?: return@update state
            val analysis = currentResult.result ?: return@update state
            state.copy(
                result = currentResult.copy(
                    result = analysis.copy(name = name)
                )
            )
        }
        syncLogUpdate(oldLog)
    }

    fun updateMeal(meal: String) {
        val oldLog = _state.value.result?.toFoodLog()?.apply { logId = _state.value.logId ?: "" }
        _state.update { state ->
            val currentResult = state.result ?: return@update state
            val analysis = currentResult.result ?: return@update state
            val normalized = MealTime.getValidMeal(meal, analysis.timestamp)
            state.copy(
                result = currentResult.copy(
                    mealType = normalized
                )
            )
        }
        syncLogUpdate(oldLog)
    }

    fun updateTime(timestamp: Long) {
        val oldLog = _state.value.result?.toFoodLog()?.apply { logId = _state.value.logId ?: "" }
        _state.update { state ->
            val currentResult = state.result ?: return@update state
            val analysis = currentResult.result ?: return@update state
            val normalizedMeal = MealTime.getValidMeal(currentResult.mealType, timestamp)
            state.copy(
                result = currentResult.copy(
                    mealType = normalizedMeal,
                    result = analysis.copy(timestamp = timestamp)
                )
            )
        }
        syncLogUpdate(oldLog)
    }

    fun updateCalories(calories: Double) {
        val oldLog = _state.value.result?.toFoodLog()?.apply { logId = _state.value.logId ?: "" }
        _state.update { state ->
            val currentResult = state.result ?: return@update state
            val analysis = currentResult.result ?: return@update state
            state.copy(
                result = currentResult.copy(
                    result = analysis.copy(calories = calories.toInt(), foodItems = null)
                )
            )
        }
        syncLogUpdate(oldLog)
    }

    fun updateProtein(protein: Double) {
        val oldLog = _state.value.result?.toFoodLog()?.apply { logId = _state.value.logId ?: "" }
        _state.update { state ->
            val currentResult = state.result ?: return@update state
            val analysis = currentResult.result ?: return@update state
            state.copy(
                result = currentResult.copy(
                    result = analysis.copy(protein = protein, foodItems = null)
                )
            )
        }
        syncLogUpdate(oldLog)
    }

    fun updateFat(fat: Double) {
        val oldLog = _state.value.result?.toFoodLog()?.apply { logId = _state.value.logId ?: "" }
        _state.update { state ->
            val currentResult = state.result ?: return@update state
            val analysis = currentResult.result ?: return@update state
            state.copy(
                result = currentResult.copy(
                    result = analysis.copy(fat = fat, foodItems = null)
                )
            )
        }
        syncLogUpdate(oldLog)
    }

    fun updateCarbs(carbs: Double) {
        val oldLog = _state.value.result?.toFoodLog()?.apply { logId = _state.value.logId ?: "" }
        _state.update { state ->
            val currentResult = state.result ?: return@update state
            val analysis = currentResult.result ?: return@update state
            state.copy(
                result = currentResult.copy(
                    result = analysis.copy(carbs = carbs, foodItems = null)
                )
            )
        }
        syncLogUpdate(oldLog)
    }

    fun updateFoodItem(
        position: Int,
        updatedItem: uz.oybek.ozamiz.data.ai.analyzer.AnalyzedIngredient
    ) {
        val oldLog = _state.value.result?.toFoodLog()?.apply { logId = _state.value.logId ?: "" }
        val currentResult = _state.value.result ?: return
        val resultObj = currentResult.result ?: return
        val currentItems = resultObj.getNormalizedFoodItems().toMutableList()
        if (position in currentItems.indices) {
            currentItems[position] = updatedItem
        } else return

        val newTotalCalories = currentItems.sumOf { it.calories ?: 0 }
        val newTotalProtein = currentItems.mapNotNull { it.protein }.sum()
        val newTotalFat = currentItems.mapNotNull { it.fat }.sum()
        val newTotalCarbs = currentItems.mapNotNull { it.carbs }.sum()

        val updatedResultObj = resultObj.copy(
            calories = if (newTotalCalories > 0) newTotalCalories else resultObj.calories,
            protein = if (newTotalProtein > 0.0) newTotalProtein else resultObj.protein,
            fat = if (newTotalFat > 0.0) newTotalFat else resultObj.fat,
            carbs = if (newTotalCarbs > 0.0) newTotalCarbs else resultObj.carbs,
            foodItems = currentItems
        )

        if (baseFoodAnalysisResult != null) {
            val baseObj = baseFoodAnalysisResult!!
            val baseItems = baseObj.getNormalizedFoodItems().toMutableList()
            val multiplier = resultObj.portionMultiplier ?: 1.0
            val inv = if (multiplier > 0) 1.0 / multiplier else 1.0
            if (position in baseItems.indices) {
                baseItems[position] = updatedItem.copy(
                    calories = updatedItem.calories?.let { (it * inv).toInt() },
                    protein = updatedItem.protein?.let { it * inv },
                    fat = updatedItem.fat?.let { it * inv },
                    carbs = updatedItem.carbs?.let { it * inv },
                    amount = updatedItem.amount?.let { it * inv }
                )
            }
            baseFoodAnalysisResult = baseObj.copy(foodItems = baseItems)
        }

        _state.update { state ->
            state.copy(result = currentResult.copy(result = updatedResultObj))
        }
        syncLogUpdate(oldLog)
    }

    fun scalePortion(portion: Double) {
        val oldLog = _state.value.result?.toFoodLog()?.apply { logId = _state.value.logId ?: "" }
        val currentResult = _state.value.result ?: return
        val currentResultObj = currentResult.result ?: return

        if (baseFoodAnalysisResult == null) {
            val currentMultiplier = currentResultObj.portionMultiplier ?: 1.0
            baseFoodAnalysisResult = if (currentMultiplier != 1.0 && currentMultiplier > 0) {
                val inv = 1.0 / currentMultiplier
                val unscaledItems = currentResultObj.getNormalizedFoodItems().map { item ->
                    item.copy(
                        calories = item.calories?.let { (it * inv).toInt() },
                        protein = item.protein?.let { it * inv },
                        fat = item.fat?.let { it * inv },
                        carbs = item.carbs?.let { it * inv },
                        amount = item.amount?.let { it * inv }
                    )
                }
                currentResultObj.copy(
                    calories = (currentResultObj.getEffectiveCalories() * inv).toInt(),
                    protein = currentResultObj.getEffectiveProtein()?.let { it * inv },
                    fat = currentResultObj.getEffectiveFat()?.let { it * inv },
                    carbs = currentResultObj.getEffectiveCarbs()?.let { it * inv },
                    foodItems = unscaledItems,
                    portionMultiplier = 1.0
                )
            } else {
                currentResultObj
            }
        }

        val base = baseFoodAnalysisResult!!
        val baseItems = base.getNormalizedFoodItems()

        val scaledItems = baseItems.map { item ->
            item.copy(
                calories = item.calories?.let { (it * portion).toInt() },
                protein = item.protein?.let { it * portion },
                fat = item.fat?.let { it * portion },
                carbs = item.carbs?.let { it * portion },
                amount = item.amount?.let { it * portion }
            )
        }

        val baseCalories = (base.getEffectiveCalories() * portion).toInt()
        val baseProtein = base.getEffectiveProtein()?.let { it * portion }
        val baseFat = base.getEffectiveFat()?.let { it * portion }
        val baseCarbs = base.getEffectiveCarbs()?.let { it * portion }

        val updatedResultObj = base.copy(
            calories = baseCalories,
            protein = baseProtein,
            fat = baseFat,
            carbs = baseCarbs,
            foodItems = scaledItems,
            portionMultiplier = portion
        )

        _state.update { state ->
            state.copy(result = currentResult.copy(result = updatedResultObj))
        }
        syncLogUpdate(oldLog)
    }

    fun deleteFoodItem(position: Int) {
        val oldLog = _state.value.result?.toFoodLog()?.apply { logId = _state.value.logId ?: "" }
        val currentResult = _state.value.result ?: return
        val resultObj = currentResult.result ?: return
        val currentItems = resultObj.getNormalizedFoodItems().toMutableList()
        if (position in currentItems.indices) {
            currentItems.removeAt(position)
        } else return

        val newTotalCalories = currentItems.sumOf { it.calories ?: 0 }
        val newTotalProtein = currentItems.mapNotNull { it.protein }.sum()
        val newTotalFat = currentItems.mapNotNull { it.fat }.sum()
        val newTotalCarbs = currentItems.mapNotNull { it.carbs }.sum()

        val updatedResultObj = resultObj.copy(
            calories = if (newTotalCalories > 0) newTotalCalories else resultObj.calories,
            protein = if (newTotalProtein > 0.0) newTotalProtein else resultObj.protein,
            fat = if (newTotalFat > 0.0) newTotalFat else resultObj.fat,
            carbs = if (newTotalCarbs > 0.0) newTotalCarbs else resultObj.carbs,
            foodItems = currentItems
        )

        if (baseFoodAnalysisResult != null) {
            val baseObj = baseFoodAnalysisResult!!
            val baseItems = baseObj.getNormalizedFoodItems().toMutableList()
            if (position in baseItems.indices) {
                baseItems.removeAt(position)
            }
            baseFoodAnalysisResult = baseObj.copy(foodItems = baseItems)
        }

        _state.update { state ->
            state.copy(result = currentResult.copy(result = updatedResultObj))
        }
        syncLogUpdate(oldLog)
    }

    fun addIngredientWithAi(inputStr: String, singleResultParam: SingleIngredientResult? = null) {
        if (inputStr.isBlank()) return
        val oldLog = _state.value.result?.toFoodLog()?.apply { logId = _state.value.logId ?: "" }
        viewModelScope.launch {
            val singleResult = singleResultParam ?: FoodAnalyzer.analyzeSingleIngredient(inputStr)
            val currentResult = _state.value.result
            val resultObj = currentResult?.result

            val newIngredient = singleResult?.toAnalyzedIngredient()
                ?: uz.oybek.ozamiz.data.ai.analyzer.AnalyzedIngredient(
                    name = inputStr,
                    calories = 0,
                    amount = 100.0,
                    unit = "g"
                )

            if (currentResult != null && resultObj != null) {
                val currentItems = resultObj.getNormalizedFoodItems().toMutableList()
                currentItems.add(newIngredient)

                val newTotalCalories = currentItems.sumOf { it.calories ?: 0 }
                val newTotalProtein = currentItems.mapNotNull { it.protein }.sum()
                val newTotalFat = currentItems.mapNotNull { it.fat }.sum()
                val newTotalCarbs = currentItems.mapNotNull { it.carbs }.sum()

                val updatedResultObj = resultObj.copy(
                    calories = newTotalCalories,
                    protein = newTotalProtein,
                    fat = newTotalFat,
                    carbs = newTotalCarbs,
                    foodItems = currentItems
                )

                if (baseFoodAnalysisResult != null) {
                    val baseObj = baseFoodAnalysisResult!!
                    val baseItems = baseObj.getNormalizedFoodItems().toMutableList()
                    val multiplier = resultObj.portionMultiplier ?: 1.0
                    val inv = if (multiplier > 0) 1.0 / multiplier else 1.0
                    val baseNewItem = newIngredient.copy(
                        calories = newIngredient.calories?.let { (it * inv).toInt() },
                        protein = newIngredient.protein?.let { it * inv },
                        fat = newIngredient.fat?.let { it * inv },
                        carbs = newIngredient.carbs?.let { it * inv },
                        amount = newIngredient.amount?.let { it * inv }
                    )
                    baseItems.add(baseNewItem)
                    baseFoodAnalysisResult = baseObj.copy(foodItems = baseItems)
                }

                _state.update { state ->
                    state.copy(
                        result = currentResult.copy(result = updatedResultObj),
                        isAnalyzing = false,
                        isLoading = false
                    )
                }
                syncLogUpdate(oldLog)
            }
        }
    }
}