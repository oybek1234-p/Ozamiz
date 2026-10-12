package uz.oybek.ozamiz.ui.feature.log.food

import uz.oybek.ozamiz.common.util.orZero
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.data.food.model.FoodDatabase
import uz.oybek.ozamiz.data.food.model.FoodItem
import uz.oybek.ozamiz.data.food.model.SearchableFood

class FoodPagingSource(
    private val query: String,
    private val dispatcher: CoroutineDispatcher
) : PagingSource<Int, FoodItem>() {

    override fun getRefreshKey(state: PagingState<Int, FoodItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FoodItem> =
        withContext(dispatcher) {
            val pageNumber = params.key.orZero()
            val pageSize = params.loadSize

            val searchableFoods = FoodDatabase.getSearchableFoods()

            val filteredFoods = if (query.isBlank()) {
                searchableFoods.sortedWith(
                    compareByDescending<SearchableFood> { it.food.isPopular }
                        .thenBy { searchableFoods.indexOf(it) }
                ).map { it.food }
            } else {
                val rawQuery = query.trim()
                val normQuery = SearchNormalizer.normalize(rawQuery)
                val lowerQuery = rawQuery.lowercase()

                searchableFoods
                    .mapNotNull { item ->
                        val score = item.calculateScore(rawQuery, normQuery, lowerQuery)
                        if (score > 0) item.food to score else null
                    }
                    .sortedWith(
                        compareByDescending<Pair<FoodItem, Int>> { it.second }
                            .thenByDescending { it.first.isPopular }
                            .thenBy { item -> searchableFoods.indexOfFirst { it.food.id == item.first.id } }
                    )
                    .map { it.first }
            }

            val fromIndex = pageNumber * pageSize
            val toIndex = minOf(fromIndex + pageSize, filteredFoods.size)

            val pageData = if (fromIndex < filteredFoods.size) {
                filteredFoods.subList(fromIndex, toIndex)
            } else {
                emptyList()
            }

            LoadResult.Page(
                data = pageData,
                prevKey = if (pageNumber == 0) null else pageNumber - 1,
                nextKey = if (toIndex >= filteredFoods.size) null else pageNumber + 1
            )
        }
}
