package uz.oybek.ozamiz.data.firebase

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.database.Query
import kotlinx.coroutines.tasks.await

class FirebasePagingSource<T : Any>(
    private val query: Query,
    private val valueType: Class<T>
) : PagingSource<String, T>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, T> {
        return try {
            val currentPageQuery = params.key?.let {
                query.startAt(null, it).limitToFirst(params.loadSize + 1)
            } ?: query.limitToFirst(params.loadSize)

            val snapshot = currentPageQuery.get().await()

            val dataList = mutableListOf<T>()
            snapshot.children.forEach { child ->
                child.getValue(valueType)?.let { dataList.add(it) }
            }
            dataList.reverse()

            val nextKey = if (dataList.size > params.loadSize) {
                snapshot.children.last().key
            } else {
                null
            }

            LoadResult.Page(
                data = dataList.take(params.loadSize),
                prevKey = null,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, T>): String? {
        return null
    }
}