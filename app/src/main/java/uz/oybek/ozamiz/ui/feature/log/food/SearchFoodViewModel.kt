package uz.oybek.ozamiz.ui.feature.log.food

import uz.oybek.ozamiz.common.util.orZero
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.di.DefaultDispatcher
import uz.oybek.ozamiz.data.food.model.FoodDatabase
import uz.oybek.ozamiz.data.food.model.FoodItem
import uz.oybek.ozamiz.ui.modelUtils.MealTime
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import uz.oybek.ozamiz.data.tracker.api.FoodTrackerRepo
import uz.oybek.ozamiz.data.tracker.model.AddFoodRequest
import uz.oybek.ozamiz.data.tracker.session.SelectedDateManager
import uz.oybek.ozamiz.ui.utils.getArgs
import java.util.Date
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class SearchFoodViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedDateManager: SelectedDateManager,
    private val foodTrackerRepo: FoodTrackerRepo,
    private val profileRepo: ProfileRepo,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val args = savedStateHandle.getArgs<SearchFoodArgs>()

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<SearchFoodUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SearchFoodUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val _query = MutableStateFlow("")

    @OptIn(FlowPreview::class)
    private val debouncedQuery = _query
        .debounce { query -> if (query.isEmpty()) 0.milliseconds else 300.milliseconds }
        .distinctUntilChanged()
        .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val foodPagingFlow = debouncedQuery.flatMapLatest { searchInput ->
        Pager(
            config = PagingConfig(
                pageSize = 30,
                enablePlaceholders = false,
                initialLoadSize = 30
            ),
            pagingSourceFactory = { FoodPagingSource(searchInput, defaultDispatcher) }
        ).flow
    }.cachedIn(viewModelScope)

    init {
        viewModelScope.launch(defaultDispatcher) {
            debouncedQuery.collect { searchInput ->
                val trimmed = searchInput.trim()
                val count = if (trimmed.isBlank()) {
                    null
                } else {
                    val normQuery = SearchNormalizer.normalize(trimmed)
                    val lowerQuery = trimmed.lowercase()

                    val foods = FoodDatabase.getSearchableFoods()
                    foods.count { item ->
                        item.calculateScore(trimmed, normQuery, lowerQuery) > 0
                    }
                }
                _uiState.update { it.copy(searchResultCount = count) }
            }
        }

        viewModelScope.launch {
            profileRepo.user.collect { user ->
                _uiState.update { it.copy(isPremium = user.isPremiumActive) }
            }
        }
    }

    private fun createInitialState(): SearchFoodUiState {
        val selectedDate = selectedDateManager.getSelectedDate()
        val validMeal = MealTime.fromId(args?.mealType)?.id ?: MealTime.getMealByTime().id

        return SearchFoodUiState(
            mealType = validMeal,
            date = selectedDate
        )
    }

    fun searchFoods(newQuery: String) {
        _query.value = newQuery
        _uiState.update { it.copy(query = newQuery) }
    }

    fun logDirectFood(foodItem: FoodItem) {
        viewModelScope.launch {
            val weight = foodItem.defaultServing.grams
            val currentState = _uiState.value
            val request = AddFoodRequest(
                description = foodItem.defaultLoggedDescription(),
                calories = foodItem.caloriesForGrams(weight),
                protein = foodItem.proteinForGrams(weight),
                fat = foodItem.fatForGrams(weight),
                carbs = foodItem.carbsForGrams(weight),
                mealType = currentState.mealType,
                aiGenerated = false,
                date = selectedDateManager.getSelectedDate()
            )

            foodTrackerRepo.addFoodLog(request)
            _effects.send(SearchFoodUiEffect.ShowFoodAdded(foodItem.name))
        }
    }

    fun logFoodFromSelection(foodLog: FoodLog) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val request = AddFoodRequest(
                description = foodLog.description,
                calories = foodLog.calories,
                mealType = currentState.mealType,
                protein = foodLog.protein.orZero(),
                fat = foodLog.fat.orZero(),
                carbs = foodLog.carbs.orZero(),
                aiGenerated = foodLog.aiGenerated,
                date = selectedDateManager.getSelectedDate()
            )
            foodTrackerRepo.addFoodLog(request)
            _effects.send(SearchFoodUiEffect.ShowFoodAdded(foodLog.description))
        }
    }
}
