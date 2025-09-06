
package uz.oybek.ozamiz.ui.foods

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.admin
import uz.oybek.ozamiz.data.ai.GeminiFoodAnalyzer
import uz.oybek.ozamiz.data.foods.DailyUserMealPlan
import uz.oybek.ozamiz.data.foods.FoodCategory
import uz.oybek.ozamiz.data.foods.FoodCategoryRepository
import uz.oybek.ozamiz.data.foods.MealPlanRepository
import uz.oybek.ozamiz.data.foods.PlannedMealItem
import uz.oybek.ozamiz.data.foods.Recipe
import uz.oybek.ozamiz.data.foods.RecipeRepository
import uz.oybek.ozamiz.data.info.MealsType
import uz.oybek.ozamiz.data.upcomingMeal
import uz.oybek.ozamiz.ui.getinfo.Event
import java.util.Calendar
import java.util.Locale

data class FoodsUiState(
    val isLoadingPlan: Boolean = false,
    val isGeneratingPlan: Boolean = false,
    val isLoadingCategories: Boolean = false,
    val isLoadingPopularRecipes: Boolean = false,
    val errorMessage: String? = null,
    val selectedDay: String = FoodsViewModel.getCurrentDayOfWeekString(),
    val currentDayPlan: DailyUserMealPlan? = null,
    val categories: List<FoodCategory> = emptyList(),
    val popularRecipes: List<Recipe> = emptyList(),
    val isUserAdmin: Boolean = false,
    val isUserPremium: Boolean = false,
    val showPremiumPromptForDay: Boolean = false,
    val dayForPremiumPrompt: String? = null,
    // Kun va ovqat turi bo'yicha almashtirish holatini saqlaydi
    val replacingMealTags: Map<String, Set<String>> = emptyMap()
)

sealed class FoodsNavigationEvent {
    data class NavigateToFoodList(val categoryId: String? = null, val searchQuery: String? = null, val listTitle: String) : FoodsNavigationEvent()
    data class NavigateToRecipeDetail(val recipeJson: String) : FoodsNavigationEvent()
}

class FoodsViewModel(application: Application) : AndroidViewModel(application) {

    private val mealPlanRepository = MealPlanRepository()
    private val categoryRepository = FoodCategoryRepository()
    private val recipeRepository = RecipeRepository()
    private val geminiAnalyzer = GeminiFoodAnalyzer
    private val gson = Gson()

    private val _uiState = MutableStateFlow(FoodsUiState(isUserAdmin = LocalUser.user.admin(), isUserPremium = LocalUser.user.premium))
    val uiState: StateFlow<FoodsUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableLiveData<Event<FoodsNavigationEvent>>()
    val navigationEvent: LiveData<Event<FoodsNavigationEvent>> = _navigationEvent

    private var currentDataLoadJob: Job? = null

    companion object {
        fun getCurrentDayOfWeekString(): String {
            val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            return when (dayOfWeek) {
                Calendar.MONDAY -> "monday"; Calendar.TUESDAY -> "tuesday"; Calendar.WEDNESDAY -> "wednesday"
                Calendar.THURSDAY -> "thursday"; Calendar.FRIDAY -> "friday"; Calendar.SATURDAY -> "saturday"
                Calendar.SUNDAY -> "sunday"; else -> "monday"
            }
        }
    }

    init {
        loadInitialData(false)
        observeUserPremiumStatus()
    }

    private fun observeUserPremiumStatus() {
        viewModelScope.launch {
            LocalUser.userLive.asFlow().filterNotNull().map { it.premium }.distinctUntilChanged().collect { isPremium ->
                if (_uiState.value.isUserPremium != isPremium) {
                    _uiState.update { it.copy(isUserPremium = isPremium) }
                    onDaySelected(_uiState.value.selectedDay, forceReload = true)
                }
            }
        }
    }

    fun loadInitialData(isRefresh: Boolean = false) {
        if (currentDataLoadJob?.isActive == true && !isRefresh) return

        currentDataLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCategories = true, isLoadingPopularRecipes = true, errorMessage = null, showPremiumPromptForDay = false) }
            loadOrGeneratePlanForDay(_uiState.value.selectedDay, isRefresh)
            launch { loadCategories() }
            launch { loadPopularRecipes() }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories()
                .catch { e -> _uiState.update { it.copy(isLoadingCategories = false, errorMessage = (it.errorMessage ?: "") + "\nKategoriya xatosi: " + e.localizedMessage) } }
                .collectLatest { result ->
                    when (result) {
                        is DataWrapper.Success -> _uiState.update { it.copy(isLoadingCategories = false, categories = result.data) }
                        is DataWrapper.Error -> _uiState.update { it.copy(isLoadingCategories = false, errorMessage = (it.errorMessage ?: "") + "\n" + result.error) }
                        is DataWrapper.Loading -> _uiState.update { it.copy(isLoadingCategories = true) }
                    }
                }
        }
    }

    private fun loadPopularRecipes() {
        viewModelScope.launch {
            recipeRepository.getLatestRecipes(LocalUser.user.upcomingMeal(), limit = 10)
                .catch { e -> _uiState.update { it.copy(isLoadingPopularRecipes = false, errorMessage = (it.errorMessage ?: "") + "\nRetseptlar xatosi: " + e.localizedMessage) } }
                .collectLatest { result ->
                    when (result) {
                        is DataWrapper.Success -> _uiState.update { it.copy(isLoadingPopularRecipes = false, popularRecipes = result.data) }
                        is DataWrapper.Error -> _uiState.update { it.copy(isLoadingPopularRecipes = false, errorMessage = (it.errorMessage ?: "") + "\n" + result.error) }
                        is DataWrapper.Loading -> _uiState.update { it.copy(isLoadingPopularRecipes = true) }
                    }
                }
        }
    }

    fun onDaySelected(dayKey: String, forceReload: Boolean = false) {
        if ((_uiState.value.selectedDay == dayKey && !forceReload)) return
        if (_uiState.value.isLoadingPlan || _uiState.value.isGeneratingPlan) return

        _uiState.update { it.copy(selectedDay = dayKey, showPremiumPromptForDay = false, dayForPremiumPrompt = null, currentDayPlan = null) }
        loadOrGeneratePlanForDay(dayKey, forceReload)
    }

    private fun loadOrGeneratePlanForDay(dayKey: String, isRefresh: Boolean = false) {
        val user = LocalUser.user
        if (!user.valid) {
            _uiState.update { it.copy(errorMessage = "Foydalanuvchi ma'lumotlari topilmadi.") }
            return
        }

        if (!user.premium && !isDayAccessibleForFreeUser(dayKey)) {
            _uiState.update { it.copy(currentDayPlan = null, showPremiumPromptForDay = true, dayForPremiumPrompt = dayKey, isLoadingPlan = false, isGeneratingPlan = false) }
            return
        }

        _uiState.update { it.copy(isLoadingPlan = true, currentDayPlan = null) }

        viewModelScope.launch {
            if (isRefresh) {
                replaceAllMealsForCurrentDay(dayKey)
                return@launch
            }

            when (val planResult = mealPlanRepository.getPlanForDayOnce(user.uid, dayKey)) {
                is DataWrapper.Success -> {
                    if (planResult.data != null && !planResult.data.isEmpty()) {
                        _uiState.update { it.copy(isLoadingPlan = false, currentDayPlan = planResult.data) }
                    } else {
                        generatePlanForDay(user, dayKey)
                    }
                }
                is DataWrapper.Error -> {
                    _uiState.update { it.copy(isLoadingPlan = false, errorMessage = planResult.error) }
                }
                is DataWrapper.Loading -> { /* Ignored */ }
            }
        }
    }

    private suspend fun generatePlanForDay(user: User, dayKey: String) {
        _uiState.update { it.copy(isLoadingPlan = false, isGeneratingPlan = true) }
//        val generatedPlanResult = geminiAnalyzer.generateMealPlanForDay(user, dayKey)
//
//        when (generatedPlanResult) {
//            is DataWrapper.Success -> {
//                val newPlan = generatedPlanResult.data
//                mealPlanRepository.savePlanForDay(user.uid, dayKey, newPlan)
//                _uiState.update { it.copy(isGeneratingPlan = false, currentDayPlan = newPlan) }
//            }
//            is DataWrapper.Error -> {
//                _uiState.update { it.copy(isGeneratingPlan = false, errorMessage = generatedPlanResult.error) }
//            }
//            else -> {}
//        }
    }

    fun replaceMeal(mealTypeTag: String) {
        if (!_uiState.value.isUserPremium) {
            _uiState.update { it.copy(errorMessage = "Ovqat almashtirish uchun Premium kerak.") }
            return
        }
        val currentDay = _uiState.value.selectedDay
        val currentPlan = _uiState.value.currentDayPlan ?: return

        // Mark this specific meal on this specific day as "replacing"
        _uiState.update { state ->
            val dayReplacements = state.replacingMealTags[currentDay]?.toMutableSet() ?: mutableSetOf()
            dayReplacements.add(mealTypeTag)
            state.copy(replacingMealTags = state.replacingMealTags + (currentDay to dayReplacements))
        }

//        viewModelScope.launch {
//            val replacedMealResult = geminiAnalyzer.replaceSingleMeal(LocalUser.user, currentDay, mealTypeTag, currentPlan)
//
//            val updatedPlan = if (replacedMealResult is DataWrapper.Success) {
//                currentPlan.copy().apply { updateMealByType(mealTypeTag, replacedMealResult.data) }
//            } else {
//                null
//            }
//
//            if (updatedPlan != null) {
//                updatedPlan.timestamp = System.currentTimeMillis()
//                mealPlanRepository.savePlanForDay(LocalUser.user.uid, currentDay, updatedPlan)
//                _uiState.update { it.copy(currentDayPlan = updatedPlan) }
//            }
//
//            if (replacedMealResult is DataWrapper.Error) {
//                _uiState.update { it.copy(errorMessage = replacedMealResult.error) }
//            }
//
//            // Always unmark the meal as "replacing" when the operation finishes
//            _uiState.update { state ->
//                val dayReplacements = state.replacingMealTags[currentDay]?.toMutableSet() ?: mutableSetOf()
//                dayReplacements.remove(mealTypeTag)
//                state.copy(replacingMealTags = state.replacingMealTags + (currentDay to dayReplacements))
//            }
//        }
    }


    fun replaceAllMealsForCurrentDay(dayKey: String = _uiState.value.selectedDay) {
        if (!_uiState.value.isUserPremium) {
            _uiState.update { it.copy(errorMessage = "Rejani to'liq almashtirish uchun Premium kerak.") }; return
        }
        viewModelScope.launch {
            mealPlanRepository.deletePlanForDay(LocalUser.user.uid, dayKey)
            generatePlanForDay(LocalUser.user, dayKey)
        }
    }

    private fun isDayAccessibleForFreeUser(dayKey: String): Boolean {
        val calendar = Calendar.getInstance()
        val todayIndex = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val dayKeys = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
        val selectedDayIndex = dayKeys.indexOf(dayKey.lowercase())
        if (selectedDayIndex == -1) return false
        val accessibleIndices = listOf(todayIndex, (todayIndex + 1) % 7, (todayIndex + 2) % 7)
        return accessibleIndices.contains(selectedDayIndex)
    }

    fun onCategoryClick(category: FoodCategory) { _navigationEvent.value = Event(FoodsNavigationEvent.NavigateToFoodList(categoryId = category.id, listTitle = category.name)) }
    fun onRecipeClick(recipe: Recipe) { _navigationEvent.value = Event(FoodsNavigationEvent.NavigateToRecipeDetail(recipeJson = gson.toJson(recipe))) }
    fun onSeeAllRecipesClick() { _navigationEvent.value = Event(FoodsNavigationEvent.NavigateToFoodList(listTitle = "Barcha Retseptlar")) }
    fun onSearchRequested(query: String) { if (query.isNotBlank()) _navigationEvent.value = Event(FoodsNavigationEvent.NavigateToFoodList(searchQuery = query, listTitle = "Qidiruv: '$query'")) }
    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }

    override fun onCleared() {
        super.onCleared()
        currentDataLoadJob?.cancel()
    }
}