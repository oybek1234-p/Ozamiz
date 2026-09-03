package uz.oybek.ozamiz.ui.feature.log.food

import androidx.paging.PagingSource
import androidx.paging.PagingState
import uz.oybek.ozamiz.data.food.model.FoodDatabase
import uz.oybek.ozamiz.data.food.model.FoodItem

class FoodPagingSource(
    private val query: String
) : PagingSource<Int, FoodItem>() {

    override fun getRefreshKey(state: PagingState<Int, FoodItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FoodItem> {
        val pageNumber = params.key ?: 0
        val pageSize = params.loadSize

        val allFoods = FoodDatabase.allFoods

        val filteredFoods = if (query.isBlank()) {
            allFoods.sortedWith(
                compareByDescending<FoodItem> { it.isPopular }
                    .thenBy { allFoods.indexOf(it) }
            )
        } else {
            val rawQuery = query.trim()
            val normQuery = SearchNormalizer.normalize(rawQuery)
            val lowerQuery = rawQuery.lowercase()

            allFoods
                .mapNotNull { food ->
                    val name = food.name
                    val normName = SearchNormalizer.normalize(name)
                    val lowerName = name.lowercase()
                    val words = SearchNormalizer.getWords(name)
                    
                    val aliases = food.searchAliases

                    val isExactNormalized = normName == normQuery
                    val isExactName = lowerName == lowerQuery
                    val isStartName = lowerName.startsWith(lowerQuery)
                    val normStartName = normName.startsWith(normQuery)
                    val firstWordExact = words.firstOrNull()?.lowercase() == lowerQuery
                    val anyWordExact = words.any { it.lowercase() == lowerQuery }
                    val anyWordStarts = words.any { it.lowercase().startsWith(lowerQuery) }
                    val containsQuery = normName.contains(normQuery) || lowerName.contains(lowerQuery)

                    // Check searchAliases matching
                    val aliasMatch = aliases.any { alias ->
                        val normAlias = SearchNormalizer.normalize(alias)
                        normAlias == normQuery || normAlias.startsWith(normQuery) || SearchNormalizer.getWords(alias).any { it.lowercase().startsWith(lowerQuery) }
                    }

                    val matchScore = when {
                        isExactName || isExactNormalized -> 3500
                        aliasMatch -> 3000
                        firstWordExact -> 2500
                        isStartName || normStartName -> 2000
                        anyWordExact -> 1500
                        anyWordStarts -> 1000
                        containsQuery -> 500
                        else -> 0
                    }

                    if (matchScore > 0) {
                        val popularBonus = if (food.isPopular) 500 else 0
                        val totalScore = matchScore + popularBonus
                        food to totalScore
                    } else {
                        null
                    }
                }
                .sortedWith(
                    compareByDescending<Pair<FoodItem, Int>> { it.second }
                        .thenByDescending { it.first.isPopular }
                        .thenBy { allFoods.indexOf(it.first) }
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

        return LoadResult.Page(
            data = pageData,
            prevKey = if (pageNumber == 0) null else pageNumber - 1,
            nextKey = if (toIndex >= filteredFoods.size) null else pageNumber + 1
        )
    }
}
