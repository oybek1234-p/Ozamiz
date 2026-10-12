package uz.oybek.ozamiz.ui.feature.log.food

import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.data.analyzer.api.FoodAnalyzerRepo
import uz.oybek.ozamiz.data.analyzer.model.AnalyzedIngredient
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.savedFood.api.SavedFoodRepo
import uz.oybek.ozamiz.data.tracker.api.FoodTrackerRepo
import uz.oybek.ozamiz.data.tracker.mapper.toSaveFoodRequest
import uz.oybek.ozamiz.data.tracker.model.FoodModel
import uz.oybek.ozamiz.data.tracker.model.toRequest
import uz.oybek.ozamiz.data.tracker.model.withIngredientAdded
import uz.oybek.ozamiz.data.tracker.model.withIngredientRemoved
import uz.oybek.ozamiz.data.tracker.model.withIngredientReplaced
import uz.oybek.ozamiz.data.tracker.model.withPortionStep
import uz.oybek.ozamiz.data.tracker.model.withScaledPortion
import uz.oybek.ozamiz.common.util.currentTime
import uz.oybek.ozamiz.ui.utils.getArgs
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AnalyzeResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodTrackerRepo: FoodTrackerRepo,
    private val savedFoodRepo: SavedFoodRepo,
    private val foodAnalyzerRepo: FoodAnalyzerRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyzeResultUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<AnalyzeResultEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var analyzeJob: Job? = null
    private var syncJob: Job? = null

    private val args = savedStateHandle.getArgs<AnalyzeResultArgs>()

    init {
        when (args) {
            is AnalyzeResultArgs.ExistingLog -> {
                loadLog(
                    logId = args.logId,
                    date = Date(args.timestamp),
                    userId = args.userId
                )
            }

            is AnalyzeResultArgs.NewAnalysis -> {
                startAnalyze(args)
            }

            null -> Unit
        }
    }

    private fun loadLog(
        logId: String,
        date: Date,
        userId: String?
    ) {
        _uiState.update {
            it.copy(
                logDate = date,
                userId = userId,
                isLoading = true,
                isError = false,
                isSaved = true
            )
        }
        viewModelScope.launch {
            val model = foodTrackerRepo.getFoodLog(logId, date, userId)
            if (model != null) {
                val isFav = savedFoodRepo.isFoodSaved(model.description)
                _uiState.update {
                    it.copy(
                        food = model,
                        isLoading = false,
                        isFavorite = isFav
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isError = true
                    )
                }
            }
        }
    }

    private fun startAnalyze(
        newAnalysis: AnalyzeResultArgs.NewAnalysis
    ) {
        val imageUri = newAnalysis.imageUri?.toUri()
        val description = newAnalysis.description.orEmpty()
        val mealType = newAnalysis.mealType

        analyzeJob?.cancel()
        val now = currentTime()
        _uiState.update {
            it.copy(
                logDate = now,
                isAnalyzing = true,
                isError = false,
                isSaved = false,
                initialDescription = description,
                initialImageUri = newAnalysis.imageUri
            )
        }
        analyzeJob = viewModelScope.launch {
            val model = foodAnalyzerRepo.analyzeFood(
                imageUri = imageUri,
                description = description,
                meal = mealType,
                date = now
            )
            if (model != null) {
                _uiState.update {
                    it.copy(
                        food = model,
                        isAnalyzing = false
                    )
                }
                if (model.healthLevel >= 8) {
                    _events.send(
                        AnalyzeResultEvent.PlayCelebration(model.healthLevel)
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        food = null,
                        isAnalyzing = false,
                        isError = true
                    )
                }
            }
        }
    }

    fun retryAnalyze() {
        if (_uiState.value.isAnalyzing) return
        val newAnalysis = args as? AnalyzeResultArgs.NewAnalysis ?: return
        startAnalyze(newAnalysis)
    }

    fun onSaveClick() {
        val state = _uiState.value
        val food = state.food ?: return
        if (state.isSaving) return

        syncJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val res = if (food.isNew) {
                val request = food.toRequest(
                    imageUri = state.initialImageUri?.toUri(),
                    date = state.logDate
                )
                foodTrackerRepo.addFoodLog(request)
            } else {
                foodTrackerRepo.updateFoodLog(
                    foodLog = food,
                    targetUid = state.userId
                )
            }
            if (res is BaseResult.Success) {
                _events.send(AnalyzeResultEvent.Saved)
            } else {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun reLog() {
        val food = _uiState.value.food ?: return
        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val res = foodTrackerRepo.reLogFood(food)
            if (res is BaseResult.Success) {
                _events.send(AnalyzeResultEvent.ReLogged)
            } else {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteLog() {
        val state = _uiState.value
        val food = state.food?.takeIf { !it.isNew } ?: return
        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val res = foodTrackerRepo.deleteFoodLog(
                foodLog = food,
                targetUid = state.userId
            )
            if (res is BaseResult.Success) {
                _events.send(AnalyzeResultEvent.Deleted)
            } else {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun cancelAndExit() {
        analyzeJob?.cancel()
        syncJob?.cancel()
        _uiState.update { it.copy(isAnalyzing = false, isSaving = false) }
        viewModelScope.launch {
            _events.send(AnalyzeResultEvent.NavigateBack)
        }
    }

    fun toggleFavorite() {
        val food = _uiState.value.food ?: return
        viewModelScope.launch {
            val isFav = _uiState.value.isFavorite
            if (isFav) {
                savedFoodRepo.deleteSavedFood(food.description)
            } else {
                savedFoodRepo.saveFood(food.toSaveFoodRequest())
                _events.send(AnalyzeResultEvent.FoodSavedToFavorites)
            }
            _uiState.update { it.copy(isFavorite = !isFav) }
        }
    }

    fun shareFood() {
        val food = _uiState.value.food ?: return
        viewModelScope.launch {
            _events.send(AnalyzeResultEvent.PromptShare(food))
        }
    }

    private inline fun mutateFood(crossinline transform: (FoodModel) -> FoodModel) {
        val current = _uiState.value.food ?: return
        val updated = transform(current)
        if (current.isNew) {
            _uiState.update { it.copy(food = updated, isSaved = false) }
        } else {
            _uiState.update { it.copy(food = updated) }
            syncJob?.cancel()
            syncJob = viewModelScope.launch {
                val state = _uiState.value
                withContext(NonCancellable) {
                    foodTrackerRepo.updateFoodLog(
                        foodLog = updated,
                        targetUid = state.userId
                    )
                }
            }
        }
    }

    fun increasePortion() = mutateFood { it.withPortionStep(increase = true) }
    fun decreasePortion() = mutateFood { it.withPortionStep(increase = false) }
    fun scalePortion(portion: Double) = mutateFood { it.withScaledPortion(portion) }
    fun updateCalories(calories: Double) = mutateFood { it.copy(calories = calories.toInt()) }
    fun updateProtein(protein: Double) = mutateFood { it.copy(protein = protein) }
    fun updateFat(fat: Double) = mutateFood { it.copy(fat = fat) }
    fun updateCarbs(carbs: Double) = mutateFood { it.copy(carbs = carbs) }
    fun updateFoodItem(position: Int, item: AnalyzedIngredient) =
        mutateFood { it.withIngredientReplaced(position, item) }

    fun deleteFoodItem(position: Int) = mutateFood { it.withIngredientRemoved(position) }
    fun addIngredient(ingredient: AnalyzedIngredient) =
        mutateFood { it.withIngredientAdded(ingredient) }
}