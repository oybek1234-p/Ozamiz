package uz.oybek.ozamiz.ui.feature.log.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.community.CommunityShareHelper
import uz.oybek.ozamiz.data.food.model.FoodDatabase
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.streak.StreakRepository
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds

class SearchFoodViewModel : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _mealType = MutableStateFlow("")
    val mealType: StateFlow<String> = _mealType.asStateFlow()

    @OptIn(FlowPreview::class)
    private val debouncedQuery = _query.debounce(300.milliseconds)

    val searchResultCount: StateFlow<Int?> = debouncedQuery.map { searchInput ->
        val trimmed = searchInput.trim()
        if (trimmed.isBlank()) {
            null
        } else {
            val normQuery = SearchNormalizer.normalize(trimmed)
            val lowerQuery = trimmed.lowercase()

            FoodDatabase.allFoods.count { food ->
                val name = food.name
                val normName = SearchNormalizer.normalize(name)
                val lowerName = name.lowercase()

                normName.contains(normQuery) ||
                lowerName.contains(lowerQuery) ||
                food.searchAliases.any { SearchNormalizer.normalize(it).contains(normQuery) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val foodPagingFlow = debouncedQuery.flatMapLatest { searchInput ->
        Pager(
            config = PagingConfig(
                pageSize = 30,
                enablePlaceholders = false,
                initialLoadSize = 30
            ),
            pagingSourceFactory = { FoodPagingSource(searchInput) }
        ).flow
    }.cachedIn(viewModelScope)

    fun updateMealType(mealType: String) {
        _mealType.value = mealType
    }

    fun searchFoods(newQuery: String) {
        _query.value = newQuery
    }

    fun logFood(foodLog: FoodLog, date: Date) {
        viewModelScope.launch {
            FoodLogRepository.add(
                foodLog = foodLog,
                date = date)
            StreakRepository.updateStreak()
            CommunityShareHelper.shareFoodLogIfNeeded(foodLog)
        }
    }
}
