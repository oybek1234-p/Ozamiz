package uz.oybek.ozamiz.ui.feature.log.food

import uz.oybek.ozamiz.data.community.CommunityShareHelper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.food.model.FoodDatabase
import uz.oybek.ozamiz.data.food.model.FoodItem
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.streak.StreakRepository
import java.util.Date

data class SearchFoodState(
    val searchResults: List<FoodItem> = emptyList(),
    val isSearching: Boolean = false,
    val query: String = "",
    val mealType: String = ""
)


class SearchFoodViewModel : ViewModel() {

    private val _state = MutableStateFlow(SearchFoodState())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null

    fun updateMealType(mealType: String) {
        _state.update { it.copy(mealType = mealType) }
        if (state.value.query.isBlank()) {
            loadInitialFoods()
        }
    }

    private fun loadInitialFoods() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            val results = FoodDatabase.allFoods.take(15)
            _state.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun searchFoods(query: String) {
        searchJob?.cancel()
        _state.update { it.copy(query = query) }
        if (query.isBlank()) {
            loadInitialFoods()
            return
        }
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            val results = FoodDatabase.allFoods.filter { 
                it.name.contains(query, ignoreCase = true) 
            }.take(30)
            _state.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun logFood(foodLog: FoodLog, date: Date) {
        viewModelScope.launch {
            FoodLogRepository.add(
                image = null,
                foodLog = foodLog,
                date = date
            )
            StreakRepository.updateStreak()
            CommunityShareHelper.shareFoodLogIfNeeded(foodLog)
        }
    }
}
