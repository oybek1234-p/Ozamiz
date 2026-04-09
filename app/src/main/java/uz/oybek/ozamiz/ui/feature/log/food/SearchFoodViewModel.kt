package uz.oybek.ozamiz.ui.feature.log.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.food.FoodRepository
import uz.oybek.ozamiz.data.food.model.FoodItem
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import uz.oybek.ozamiz.data.logs.model.FoodLog
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
    }

    fun searchFoods(query: String) {
        searchJob?.cancel()
        _state.update { it.copy(query = query) }
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            val results = FoodRepository.searchFoods(query)
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
        }
    }
}
