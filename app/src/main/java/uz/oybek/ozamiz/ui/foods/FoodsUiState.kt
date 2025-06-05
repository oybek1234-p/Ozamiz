package uz.oybek.ozamiz.ui.foods

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.admin
import uz.oybek.ozamiz.data.foods.FoodAdvice
import uz.oybek.ozamiz.data.foods.FoodCategory
import uz.oybek.ozamiz.data.foods.FoodCategoryRepository
import uz.oybek.ozamiz.data.foods.FoodsRepository
import uz.oybek.ozamiz.data.foods.Recipe
import uz.oybek.ozamiz.data.foods.RecipeRepository
import uz.oybek.ozamiz.data.foods.SimpleMealIdea
import uz.oybek.ozamiz.data.info.MealsType
import uz.oybek.ozamiz.data.upcomingMeal
import uz.oybek.ozamiz.ui.getinfo.Event
import java.util.Calendar

data class FoodsUiState(
    val isLoadingCategories: Boolean = false,
    val isLoadingSimpleMealIdeas: Boolean = false,
    // val isLoadingAdvice: Boolean = false, // Maslahatlar hozircha kerak emas
    val isLoadingPopularRecipes: Boolean = false,
    val errorMessage: String? = null,
    val categories: List<FoodCategory> = emptyList(),
    val simpleMealIdeasByMealType: Map<String, List<SimpleMealIdea>> = emptyMap(),
    // val foodAdviceList: List<FoodAdvice> = emptyList(), // Maslahatlar hozircha kerak emas
    val popularRecipes: List<Recipe> = emptyList(),
    val isUserAdmin: Boolean = false // Admin funksiyalari uchun qoldiramiz, lekin FoodsFragmentda ishlatmaymiz
) {
    val isOverallLoading: Boolean
        get() = isLoadingCategories || isLoadingSimpleMealIdeas /*|| isLoadingAdvice*/ || isLoadingPopularRecipes
}

sealed class FoodsNavigationEvent {
    data class NavigateToFoodList(
        val categoryId: String? = null,
        val searchQuery: String? = null,
        val listTitle: String = "Retseptlar"
    ) : FoodsNavigationEvent()

    data class NavigateToRecipeDetail(val recipeJson: String) : FoodsNavigationEvent()
    // object ShowAdminAddOptions : FoodsNavigationEvent() // Admin uchun hozircha kerak emas
    data class ShowSimpleMealDetail(val mealIdea: SimpleMealIdea) : FoodsNavigationEvent()
    data class NavigateToSimpleMealList(val mealType: String, val listTitle: String) : FoodsNavigationEvent()
    object NavigateToMonthlyMealPlan : FoodsNavigationEvent()
    object NavigateToHeavyFoods : FoodsNavigationEvent() // Semirtiradigan ovqatlar uchun
    object NavigateToFoodAdvice : FoodsNavigationEvent()
}

class FoodsViewModel : ViewModel() {

    private val categoryRepository = FoodCategoryRepository()
    private val foodsRepository = FoodsRepository()
    private val recipeRepository = RecipeRepository()
    private val gson = Gson()

    private val _uiState = MutableStateFlow(FoodsUiState())
    val uiState: StateFlow<FoodsUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableLiveData<Event<FoodsNavigationEvent>>()
    val navigationEvent: LiveData<Event<FoodsNavigationEvent>> = _navigationEvent

    private var categoriesJob: Job? = null
    private var simpleMealIdeasJob: Job? = null
    // private var adviceJob: Job? = null // Maslahatlar hozircha kerak emas
    private var popularRecipesJob: Job? = null
    private var dailyPlanJob: Job? = null

    init {
        loadInitialData()
        _uiState.update { it.copy(isUserAdmin = LocalUser.user.admin()) } // Admin statusini tekshirish
    }

    fun loadInitialData(isRefresh: Boolean = false) {
        if (_uiState.value.isOverallLoading && !isRefresh) return

        _uiState.update {
            it.copy(
                isLoadingCategories = true,
                isLoadingSimpleMealIdeas = true,
                // isLoadingAdvice = true, // Maslahatlar hozircha kerak emas
                isLoadingPopularRecipes = true,
                errorMessage = null
            )
        }

        categoriesJob?.cancel()
        simpleMealIdeasJob?.cancel()
        // adviceJob?.cancel() // Maslahatlar hozircha kerak emas
        popularRecipesJob?.cancel()
        dailyPlanJob?.cancel()

        loadDailyMealPlan()

        categoriesJob = viewModelScope.launch {
            categoryRepository.getAllCategories()
                .catch { e -> _uiState.update { it.copy(isLoadingCategories = false, errorMessage = (it.errorMessage ?: "") + "\nKategoriya xatosi: " + e.localizedMessage) } }
                .collectLatest { result ->
                    when (result) {
                        is DataWrapper.Success -> _uiState.update {
                            it.copy(isLoadingCategories = false, categories = result.data)
                        }
                        is DataWrapper.Error -> _uiState.update {
                            it.copy(isLoadingCategories = false, errorMessage = (it.errorMessage ?: "") + "\n" + result.error)
                        }
                        is DataWrapper.Loading -> _uiState.update { it.copy(isLoadingCategories = true) }
                    }
                }
        }

        simpleMealIdeasJob = viewModelScope.launch {
            foodsRepository.getAllSimpleMealIdeasFlow()
                .catch { e -> _uiState.update { it.copy(isLoadingSimpleMealIdeas = false, errorMessage = (it.errorMessage ?: "") + "\nOddiy ovqatlar xatosi: " + e.localizedMessage) } }
                .collectLatest { result ->
                    when (result) {
                        is DataWrapper.Success -> {
                            val ideas = result.data
                            val ideasByMealType = ideas.groupBy { it.mealTypeTag ?: "other" }
                            _uiState.update {
                                it.copy(isLoadingSimpleMealIdeas = false, simpleMealIdeasByMealType = ideasByMealType)
                            }
                        }
                        is DataWrapper.Error -> _uiState.update {
                            it.copy(isLoadingSimpleMealIdeas = false, errorMessage = (it.errorMessage ?: "") + "\n" + result.error)
                        }
                        is DataWrapper.Loading -> _uiState.update { it.copy(isLoadingSimpleMealIdeas = true) }
                    }
                }
        }

        /* Maslahatlar hozircha kerak emas
        adviceJob = viewModelScope.launch {
            foodsRepository.getAllFoodAdviceFlow()
                .catch { e -> _uiState.update { it.copy(isLoadingAdvice = false, errorMessage = (it.errorMessage ?: "") + "\nMaslahatlar xatosi: " + e.localizedMessage) } }
                .collectLatest { result ->
                    when (result) {
                        is DataWrapper.Success -> _uiState.update {
                            it.copy(isLoadingAdvice = false, foodAdviceList = result.data)
                        }
                        is DataWrapper.Error -> _uiState.update {
                            it.copy(isLoadingAdvice = false, errorMessage = (it.errorMessage ?: "") + "\n" + result.error)
                        }
                        is DataWrapper.Loading -> _uiState.update { it.copy(isLoadingAdvice = true) }
                    }
                }
        }
        */

        popularRecipesJob = viewModelScope.launch {
            recipeRepository.getLatestRecipes(MealsType.fromId(LocalUser.user.upcomingMeal())!!.name.trim(),limit = 6)
                .catch { e -> _uiState.update { it.copy(isLoadingPopularRecipes = false, errorMessage = (it.errorMessage ?: "") + "\nMashhur retseptlar xatosi: " + e.localizedMessage) } }
                .collectLatest { result ->
                    when (result) {
                        is DataWrapper.Success -> _uiState.update {
                            it.copy(isLoadingPopularRecipes = false, popularRecipes = result.data)
                        }
                        is DataWrapper.Error -> _uiState.update {
                            it.copy(isLoadingPopularRecipes = false, errorMessage = (it.errorMessage ?: "") + "\n" + result.error)
                        }
                        is DataWrapper.Loading -> _uiState.update { it.copy(isLoadingPopularRecipes = true) }
                    }
                }
        }
    }

    private fun loadDailyMealPlan() {
        val userId = LocalUser.user.uid
        if (userId.isEmpty()) {
            _uiState.update { it.copy(errorMessage = (it.errorMessage ?: "") + "\nKunlik reja uchun foydalanuvchi topilmadi.") }
            return
        }
    }

    fun onCategoryClick(category: FoodCategory) {
        _navigationEvent.value = Event(
            FoodsNavigationEvent.NavigateToFoodList(
                categoryId = category.id, listTitle = category.name
            )
        )
    }

    fun onPopularRecipeClick(recipe: Recipe) {
        _navigationEvent.value = Event(FoodsNavigationEvent.NavigateToRecipeDetail(recipeJson = gson.toJson(recipe)))
    }

    fun onSeeAllRecipesClick() {
        _navigationEvent.value =
            Event(FoodsNavigationEvent.NavigateToFoodList(listTitle = "Barcha Retseptlar"))
    }

    fun onSearchRequested(query: String) {
        if (query.isBlank()) return
        _navigationEvent.value = Event(
            FoodsNavigationEvent.NavigateToFoodList(
                searchQuery = query, listTitle = "Qidiruv: '${query}'"
            )
        )
    }

    fun onSeeAllSimpleMealsClick(mealType: String, listTitle: String) {
        _navigationEvent.value = Event(FoodsNavigationEvent.NavigateToSimpleMealList(mealType, listTitle))
    }

    fun onSimpleMealIdeaClick(mealIdea: SimpleMealIdea) {
        _navigationEvent.value = Event(FoodsNavigationEvent.ShowSimpleMealDetail(mealIdea))
    }

    fun onAdminAddClick() { // Admin funksiyasi chaqirilishi mumkin, lekin FoodsFragmentda menyu itemi bo'lmaydi
        // _navigationEvent.value = Event(FoodsNavigationEvent.ShowAdminAddOptions)
    }

    fun onMonthlyPlanClick() {
        _navigationEvent.value = Event(FoodsNavigationEvent.NavigateToMonthlyMealPlan)
    }

    fun onHeavyFoodsClick() { // Yangi funksiya
        _navigationEvent.value = Event(FoodsNavigationEvent.NavigateToHeavyFoods)
    }
    fun onFoodAdviceClick() { // Yangi funksiya
        _navigationEvent.value = Event(FoodsNavigationEvent.NavigateToFoodAdvice)
    }


    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        categoriesJob?.cancel()
        simpleMealIdeasJob?.cancel()
        // adviceJob?.cancel() // Maslahatlar hozircha kerak emas
        popularRecipesJob?.cancel()
        dailyPlanJob?.cancel()
    }
}