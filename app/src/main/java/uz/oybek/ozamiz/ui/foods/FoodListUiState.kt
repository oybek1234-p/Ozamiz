package uz.oybek.ozamiz.ui.foods

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import com.google.gson.Gson // Gson importi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.foods.FoodCategory
import uz.oybek.ozamiz.data.foods.FoodCategoryRepository
import uz.oybek.ozamiz.data.foods.Recipe
import uz.oybek.ozamiz.data.foods.RecipeRepository
import uz.oybek.ozamiz.ui.getinfo.Event

data class FoodListUiState(
    val isLoadingInitialList: Boolean = false,
    val isLoadingInitialCategories: Boolean = false,
    val isLoadingNextPage: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val recipes: List<Recipe> = emptyList(),
    val categories: List<FoodCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val canLoadMore: Boolean = true,
    val listTitle: String = "",
    val isFilterVisible: Boolean = true
) {
    val isOverallLoading: Boolean
        get() = (isLoadingInitialList || (isFilterVisible && isLoadingInitialCategories)) && !isRefreshing
}

class FoodListViewModel : ViewModel() {

    private val recipeRepository = RecipeRepository()
    private val categoryRepository = FoodCategoryRepository()
    private val gson = Gson() // Gson obyektini yaratish

    private val _uiState = MutableStateFlow(FoodListUiState())
    val uiState: StateFlow<FoodListUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableLiveData<Event<FoodListNavigationEvent>>()
    val navigationEvent: LiveData<Event<FoodListNavigationEvent>> = _navigationEvent

    private var lastVisibleDocument: DocumentSnapshot? = null
    private var currentRecipesLoadingJob: Job? = null
    private var currentCategoryLoadingJob: Job? = null
    private var currentCategoryIdArg: String? = null
    private var currentSearchQuery: String? = null

    fun initialize(categoryId: String?, searchQuery: String?, title: String) {
        val changed =
            this.currentCategoryIdArg != categoryId || this.currentSearchQuery != searchQuery
        if (!changed && _uiState.value.listTitle == title) return

        this.currentCategoryIdArg = categoryId
        this.currentSearchQuery = searchQuery
        val filterVisible = searchQuery == null

        _uiState.value = FoodListUiState(
            listTitle = title,
            selectedCategoryId = categoryId,
            isFilterVisible = filterVisible,
            isLoadingInitialList = changed,
            isLoadingInitialCategories = changed && filterVisible
        )

        cancelActiveLoads()
        loadData(isInitialLoad = true)
    }

    fun loadData(isInitialLoad: Boolean, isRefresh: Boolean = false) {
        if (isRefresh) {
            if (_uiState.value.isRefreshing) return
            lastVisibleDocument = null
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    isLoadingInitialList = true,
                    isLoadingInitialCategories = it.isFilterVisible,
                    canLoadMore = true,
                    recipes = emptyList()
                )
            }
        } else if (isInitialLoad) {
            lastVisibleDocument = null
        }

        val jobs = mutableListOf<Job>()

        if (_uiState.value.categories.isEmpty() && _uiState.value.isFilterVisible) { // Faqat filter ko'rinadigan bo'lsa kategoriyalarni yuklash
            currentCategoryLoadingJob = loadCategories(isInitialLoad || isRefresh)
            jobs.add(currentCategoryLoadingJob!!)
        }

        currentRecipesLoadingJob = loadRecipes(isInitialLoad = true)
        jobs.add(currentRecipesLoadingJob!!)


        viewModelScope.launch {
            try {
                jobs.joinAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Xatolik: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }

    }


    private fun loadCategories(isInitialOrRefresh: Boolean): Job = viewModelScope.launch {
        categoryRepository.getAllCategories()
            .catch { e ->
                _uiState.update {
                    it.copy(
                        isLoadingInitialCategories = false,
                        isRefreshing = it.isLoadingInitialList, // Ensure refreshing stops correctly
                        errorMessage = (it.errorMessage
                            ?: "") + "\nKategoriya filtrlari yuklanmadi: ${e.localizedMessage}"
                    )
                }
            }
            .onCompletion {
                if (_uiState.value.isLoadingInitialCategories) { // Check specific flag
                    _uiState.update {
                        it.copy(
                            isLoadingInitialCategories = false,
                            isRefreshing = it.isLoadingInitialList // Check if recipes are still loading
                        )
                    }
                }
            }
            .collectLatest { result ->
                when (result) {
                    is DataWrapper.Success -> {
                        val categories = result.data
                        var effectiveSelectedId = _uiState.value.selectedCategoryId

                        if (isInitialOrRefresh && currentCategoryIdArg == null && categories.isNotEmpty()) {
                            effectiveSelectedId = categories.first().id
                            _uiState.update {
                                it.copy(
                                    categories = categories,
                                    selectedCategoryId = effectiveSelectedId,
                                    isLoadingInitialCategories = false,
                                    isRefreshing = it.isLoadingInitialList
                                )
                            }
                            cancelActiveLoads(recipesOnly = true)
                            currentRecipesLoadingJob = loadRecipes(isInitialLoad = true)
                        } else {
                            _uiState.update {
                                it.copy(
                                    categories = categories,
                                    isLoadingInitialCategories = false,
                                    isRefreshing = it.isLoadingInitialList
                                )
                            }
                        }
                    }

                    is DataWrapper.Error -> _uiState.update {
                        it.copy(
                            categories = emptyList(),
                            isLoadingInitialCategories = false,
                            isRefreshing = it.isLoadingInitialList,
                            errorMessage = (it.errorMessage ?: "") + "\n" + result.error
                        )
                    }

                    is DataWrapper.Loading -> {}
                }
            }
    }

    private fun loadRecipes(isInitialLoad: Boolean): Job {
        if (!isInitialLoad && _uiState.value.isLoadingNextPage) {
            return Job().apply { complete() }
        }
        if (!isInitialLoad && !_uiState.value.canLoadMore) {
            return Job().apply { complete() }
        }

        val categoryFilter = _uiState.value.selectedCategoryId
        val searchFilter = currentSearchQuery

        if (isInitialLoad) {
            _uiState.update { it.copy(isLoadingInitialList = true, isLoadingNextPage = false) }
        } else {
            _uiState.update { it.copy(isLoadingNextPage = true) }
        }

        return viewModelScope.launch {
            val repositoryFlow = if (!searchFilter.isNullOrBlank()) {
                _uiState.update { it.copy(canLoadMore = false) } // Search is usually not paginated this way
                recipeRepository.searchRecipes(searchFilter)
            } else {
                recipeRepository.getRecipes(
                    categoryId = categoryFilter,
                    lastVisible = if (isInitialLoad) null else lastVisibleDocument
                )
            }

            repositoryFlow
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingInitialList = false,
                            isLoadingNextPage = false,
                            isRefreshing = false,
                            errorMessage = "Retseptlarni yuklashda xatolik: ${e.localizedMessage}"
                        )
                    }
                }
                .onCompletion {
                    // Ensure loading flags are reset if the flow completes or is cancelled
                    if (_uiState.value.isLoadingInitialList || _uiState.value.isLoadingNextPage) {
                        _uiState.update {
                            it.copy(
                                isLoadingInitialList = false,
                                isLoadingNextPage = false,
                                isRefreshing = false // Also reset refresh here
                            )
                        }
                    }
                }
                .collectLatest { result ->
                    when (result) {
                        is DataWrapper.Success -> {
                            val currentList =
                                if (isInitialLoad) mutableListOf() else _uiState.value.recipes.toMutableList()
                            val newRecipes: List<Recipe>
                            var newLastVisible: DocumentSnapshot? = null
                            var canLoadMoreResult = true

                            when (result.data) {
                                is Pair<*, *> -> {
                                    @Suppress("UNCHECKED_CAST")
                                    val pairResult =
                                        result.data as? Pair<List<Recipe>, DocumentSnapshot?>
                                    newRecipes = pairResult?.first ?: emptyList()
                                    newLastVisible = pairResult?.second
                                    canLoadMoreResult =
                                        newLastVisible != null && newRecipes.isNotEmpty()
                                }

                                is List<*> -> {
                                    @Suppress("UNCHECKED_CAST")
                                    newRecipes = (result.data as? List<Recipe>) ?: emptyList()
                                    newLastVisible = null // No pagination info for simple list
                                    canLoadMoreResult = false // Assume no more pages
                                }

                                else -> {
                                    newRecipes = emptyList()
                                    canLoadMoreResult = false
                                    _uiState.update { it.copy(errorMessage = "Kutilmagan ma'lumot formati") }
                                }
                            }

                            currentList.addAll(newRecipes)
                            lastVisibleDocument = newLastVisible

                            _uiState.update {
                                it.copy(
                                    isLoadingInitialList = false,
                                    isLoadingNextPage = false,
                                    recipes = currentList.toList(),
                                    canLoadMore = canLoadMoreResult,
                                    isRefreshing = false
                                )
                            }
                        }

                        is DataWrapper.Error -> _uiState.update {
                            it.copy(
                                isLoadingInitialList = false,
                                isLoadingNextPage = false,
                                isRefreshing = false,
                                errorMessage = result.error
                            )
                        }

                        is DataWrapper.Loading -> { /* This state means data is coming */ }
                    }
                }
        }
    }

    fun onCategorySelected(categoryId: String?) {
        val currentState = _uiState.value
        if (currentState.selectedCategoryId == categoryId || currentState.isLoadingInitialList || currentState.isRefreshing || currentState.isLoadingNextPage) {
            return
        }
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        cancelActiveLoads(recipesOnly = true)
        loadData(isInitialLoad = true) // Reload data for new category
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.canLoadMore && !currentState.isLoadingNextPage && !currentState.isLoadingInitialList && !currentState.isRefreshing) {
            currentRecipesLoadingJob = loadRecipes(isInitialLoad = false)
        }
    }

    private fun cancelActiveLoads(recipesOnly: Boolean = false) {
        currentRecipesLoadingJob?.cancel()
        currentRecipesLoadingJob = null
        if (!recipesOnly) {
            currentCategoryLoadingJob?.cancel()
            currentCategoryLoadingJob = null
        }
    }

    fun onRecipeClick(recipe: Recipe) { // O'ZGARISH: recipeId o'rniga Recipe
        _navigationEvent.value = Event(FoodListNavigationEvent.NavigateToRecipeDetail(recipeJson = gson.toJson(recipe)))
    }

    fun onAddRecipeClick() {
        _navigationEvent.value = Event(FoodListNavigationEvent.NavigateToAddRecipe)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

sealed class FoodListNavigationEvent {
    data class NavigateToRecipeDetail(val recipeJson: String) : FoodListNavigationEvent() // O'ZGARISH
    object NavigateToAddRecipe : FoodListNavigationEvent()
}