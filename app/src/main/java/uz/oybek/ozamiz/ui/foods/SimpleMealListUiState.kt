package uz.oybek.ozamiz.ui.foods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.foods.FoodsRepository
import uz.oybek.ozamiz.data.foods.SimpleMealIdea

data class SimpleMealListUiState(
    val isLoading: Boolean = false,
    val isLoadingNextPage: Boolean = false,
    val errorMessage: String? = null,
    val meals: List<SimpleMealIdea> = emptyList(),
    val listTitle: String = "",
    val canLoadMore: Boolean = true
)

class SimpleMealListViewModel : ViewModel() {

    private val foodsRepository = FoodsRepository()

    private val _uiState = MutableStateFlow(SimpleMealListUiState())
    val uiState: StateFlow<SimpleMealListUiState> = _uiState.asStateFlow()

    private var lastVisibleDocument: DocumentSnapshot? = null
    private var loadMealsJob: Job? = null
    private val PAGE_LIMIT = 15L

    fun loadInitialMeals(mealType: String, title: String, isRefresh: Boolean = false) {
        if (_uiState.value.isLoading && !isRefresh) return

        loadMealsJob?.cancel()
        lastVisibleDocument = null 
        _uiState.update {
            it.copy(
                isLoading = true,
                meals = if (isRefresh || it.meals.isEmpty()) emptyList() else it.meals, 
                listTitle = title,
                canLoadMore = true, 
                errorMessage = null
            )
        }
        fetchMeals(mealType, true)
    }

    fun loadNextPage(mealType: String) {
        if (!_uiState.value.canLoadMore || _uiState.value.isLoadingNextPage || _uiState.value.isLoading) return
        _uiState.update { it.copy(isLoadingNextPage = true) }
        fetchMeals(mealType, false)
    }

    private fun fetchMeals(mealType: String, isInitialLoad: Boolean) {
        loadMealsJob = viewModelScope.launch {
            foodsRepository.getPaginatedSimpleMealIdeasByMealType(
                mealType = mealType,
                limit = PAGE_LIMIT,
                lastVisible = if (isInitialLoad) null else lastVisibleDocument
            )
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingNextPage = false,
                            errorMessage = e.localizedMessage ?: "Oddiy ovqatlarni yuklashda xatolik"
                        )
                    }
                }
                .collectLatest { result ->
                    when (result) {
                        is DataWrapper.Success -> {
                            val newMeals = result.data.first
                            lastVisibleDocument = result.data.second
                            val currentMeals = if (isInitialLoad) emptyList() else _uiState.value.meals
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoadingNextPage = false,
                                    meals = currentMeals + newMeals,
                                    canLoadMore = newMeals.size >= PAGE_LIMIT && lastVisibleDocument != null
                                )
                            }
                        }
                        is DataWrapper.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoadingNextPage = false,
                                    errorMessage = result.error
                                )
                            }
                        }
                        is DataWrapper.Loading -> {
                        }
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        loadMealsJob?.cancel()
    }
}