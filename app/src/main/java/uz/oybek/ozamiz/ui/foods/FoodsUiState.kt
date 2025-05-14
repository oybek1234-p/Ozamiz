package uz.oybek.ozamiz.ui.foods

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson // Gson importi
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
import uz.oybek.ozamiz.ui.getinfo.Event

data class FoodsUiState(
    val isLoadingCategories: Boolean = false,
    val isLoadingSimpleMealIdeas: Boolean = false, // Bitta umumiy flag
    val isLoadingAdvice: Boolean = false,
    val isLoadingPopularRecipes: Boolean = false,
    val errorMessage: String? = null,
    val categories: List<FoodCategory> = emptyList(),
    val simpleMealIdeasByMealType: Map<String, List<SimpleMealIdea>> = emptyMap(),
    val foodAdviceList: List<FoodAdvice> = emptyList(),
    val popularRecipes: List<Recipe> = emptyList(),
    val isUserAdmin: Boolean = false
) {
    val isOverallLoading: Boolean // Endi simple meal idea uchun alohida emas, umumiy yuklanishni tekshiramiz
        get() = isLoadingCategories || isLoadingSimpleMealIdeas || isLoadingAdvice || isLoadingPopularRecipes
}

sealed class FoodsNavigationEvent {
    data class NavigateToFoodList(
        val categoryId: String? = null,
        val searchQuery: String? = null,
        val listTitle: String = "Retseptlar"
    ) : FoodsNavigationEvent()

    data class NavigateToRecipeDetail(val recipeJson: String) : FoodsNavigationEvent() // O'ZGARISH: recipeId o'rniga recipeJson
    object ShowAdminAddOptions : FoodsNavigationEvent()
    data class ShowSimpleMealDetail(val mealIdea: SimpleMealIdea) : FoodsNavigationEvent()
    data class NavigateToSimpleMealList(val mealType: String, val listTitle: String) : FoodsNavigationEvent()
}

class FoodsViewModel : ViewModel() {

    private val categoryRepository = FoodCategoryRepository()
    private val foodsRepository = FoodsRepository()
    private val recipeRepository = RecipeRepository()
    private val gson = Gson() // Gson obyektini yaratish

    private val _uiState = MutableStateFlow(FoodsUiState())
    val uiState: StateFlow<FoodsUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableLiveData<Event<FoodsNavigationEvent>>()
    val navigationEvent: LiveData<Event<FoodsNavigationEvent>> = _navigationEvent

    private var categoriesJob: Job? = null
    private var simpleMealIdeasJob: Job? = null
    private var adviceJob: Job? = null
    private var popularRecipesJob: Job? = null


    init {
        loadInitialData()
        _uiState.update { it.copy(isUserAdmin = LocalUser.user.admin()) }
    }

    fun loadInitialData(isRefresh: Boolean = false) {
        if (_uiState.value.isOverallLoading && !isRefresh) return

        _uiState.update {
            it.copy(
                isLoadingCategories = true,
                isLoadingSimpleMealIdeas = true,
                isLoadingAdvice = true,
                isLoadingPopularRecipes = true,
                errorMessage = null
            )
        }

        categoriesJob?.cancel()
        simpleMealIdeasJob?.cancel()
        adviceJob?.cancel()
        popularRecipesJob?.cancel()

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

        popularRecipesJob = viewModelScope.launch {
            recipeRepository.getLatestRecipes(limit = 6)
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

    fun onCategoryClick(category: FoodCategory) {
        _navigationEvent.value = Event(
            FoodsNavigationEvent.NavigateToFoodList(
                categoryId = category.id, listTitle = category.name
            )
        )
    }

    fun onPopularRecipeClick(recipe: Recipe) {
        // O'ZGARISH: Recipe obyektini JSON ga o'tkazish
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

    fun onAdminAddClick() {
        _navigationEvent.value = Event(FoodsNavigationEvent.ShowAdminAddOptions)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        categoriesJob?.cancel()
        simpleMealIdeasJob?.cancel()
        adviceJob?.cancel()
        popularRecipesJob?.cancel()
    }
}