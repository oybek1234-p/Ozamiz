package uz.oybek.ozamiz.ui.feature.menu.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import uz.oybek.ozamiz.data.ai.TodayAiMenuRepository
import uz.oybek.ozamiz.data.config.AppPreference
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import uz.oybek.ozamiz.data.streak.StreakRepository
import uz.oybek.ozamiz.data.food.model.MealTime

sealed class AiMenuUiState {
    object Empty : AiMenuUiState()
    object Loading : AiMenuUiState()
    data class Success(val menuResponse: AiMenuResponse) : AiMenuUiState()
    data class Error(val message: String) : AiMenuUiState()
}

sealed interface TodayAiMenuEvent {
    data class FoodLogged(val name: String) : TodayAiMenuEvent
    object Error : TodayAiMenuEvent
}

data class AiMenuResponse(
    val meals: List<AiMeal>,
    val tips: List<AiTip>,
    val mainRule: String
)

data class AiMeal(
    val type: String,
    val title: String,
    val time: String,
    val totalCalories: Int,
    val items: List<AiMealItem>,
    val tip: String
)

data class AiMealItem(
    val name: String,
    val calories: Int
)

data class AiTip(
    val title: String,
    val description: String,
    val icon: String
)

class TodayAiMenuViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AiMenuUiState>(AiMenuUiState.Empty)
    val uiState: StateFlow<AiMenuUiState> = _uiState.asStateFlow()

    private val _event = Channel<TodayAiMenuEvent>()
    val event = _event.receiveAsFlow()

    init {
        checkSavedMenu()
    }

    private fun checkSavedMenu() {
        viewModelScope.launch {
            val settings = AppPreference.settingsFlow.first()
            if (settings.todayAiMenuDate == DateUtils.todayString() && settings.todayAiMenuJson.isNotEmpty()) {
                parseAndSetState(settings.todayAiMenuJson)
            } else {
                _uiState.value = AiMenuUiState.Empty
            }
        }
    }

    fun generateMenu(mealsCount: Int, preferences: String) {
        _uiState.value = AiMenuUiState.Loading
        viewModelScope.launch {
            TodayAiMenuRepository.generateTodayMenu(mealsCount, preferences).collect { result ->
                when (result) {
                    is DataWrapper.Error -> _uiState.value = AiMenuUiState.Error(result.error.orEmpty())
                    is DataWrapper.Success -> parseAndSetState(result.data)
                }
            }
        }
    }
    
    fun setEmptyState() {
        _uiState.value = AiMenuUiState.Empty
    }

    fun addProductToLog(meal: AiMeal, item: AiMealItem) {
        viewModelScope.launch {
            val mealType = getMealTypeId(meal.type)
            val log = FoodLog(
                logId = "",
                timestamp = System.currentTimeMillis(),
                mealType = mealType,
                description = item.name,
                calories = item.calories,
                protein = 0.0,
                carbs = 0.0,
                fat = 0.0
            )
            val success = FoodLogRepository.add(null, log, java.util.Date(), shouldShare = false)
            if (success) {
                StreakRepository.updateStreak()
                _event.send(TodayAiMenuEvent.FoodLogged(item.name))
            } else {
                _event.send(TodayAiMenuEvent.Error)
            }
        }
    }

    private fun getMealTypeId(type: String): String {
        val lower = type.lowercase()
        return when {
            lower.contains("nonushta") || lower.contains("breakfast") || lower.contains("ertalab") -> MealTime.Breakfast.id
            lower.contains("tush") || lower.contains("lunch") || lower.contains("kunorta") -> MealTime.Lunch.id
            lower.contains("kech") || lower.contains("dinner") || lower.contains("oqshom") || lower.contains("shom") -> MealTime.Dinner.id
            else -> MealTime.Snack.id
        }
    }

    private fun parseAndSetState(jsonString: String) {
        try {
            val obj = JSONObject(jsonString)
            val mainRule = obj.optString("mainRule", "")
            
            val tips = LocalDietTips.getAll()
            
            val mealsArray = obj.optJSONArray("meals")
            val meals = mutableListOf<AiMeal>()
            if (mealsArray != null) {
                for (i in 0 until mealsArray.length()) {
                     val mealObj = mealsArray.getJSONObject(i)
                     val itemsArray = mealObj.optJSONArray("items")
                     val items = mutableListOf<AiMealItem>()
                     if (itemsArray != null) {
                         for (j in 0 until itemsArray.length()) {
                             val itemObj = itemsArray.getJSONObject(j)
                             items.add(
                                 AiMealItem(
                                     name = itemObj.optString("name", ""),
                                     calories = itemObj.optInt("calories", 0)
                                 )
                             )
                         }
                     }
                     meals.add(
                         AiMeal(
                             type = mealObj.optString("type", ""),
                             title = mealObj.optString("title", ""),
                             time = mealObj.optString("time", ""),
                             totalCalories = mealObj.optInt("totalCalories", 0),
                             items = items,
                             tip = mealObj.optString("tip", "")
                         )
                     )
                }
            }
            
            _uiState.value = AiMenuUiState.Success(AiMenuResponse(meals, tips, mainRule))
        } catch (e: Exception) {
            _uiState.value = AiMenuUiState.Error("JSON parse xatosi: ${e.message}")
        }
    }
}
