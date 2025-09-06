package uz.oybek.ozamiz.data.logs

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebasePagingSource<T : Any>(
    private val query: Query,
    private val valueType: Class<T>
) : PagingSource<String, T>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, T> {
        return try {
            val currentPageQuery = params.key?.let {
                // Firebase RealtimeDB da `startAfter` yo'q, shuning uchun biz
                // oxirgi elementning `timestamp` va `key`idan foydalanamiz.
                // Bu yerda soddalashtirilgan yondashuv - `limitToFirst` va `startAt` ishlatamiz.
                // Timestamp bo'yicha saralangan deb hisoblaymiz.
                query.startAt(null, it).limitToFirst(params.loadSize + 1)
            } ?: query.limitToFirst(params.loadSize)

            val snapshot = currentPageQuery.get().await()

            val dataList = mutableListOf<T>()
            snapshot.children.forEach { child ->
                child.getValue(valueType)?.let { dataList.add(it) }
            }

            // Ma'lumotlarni teskari tartibda qaytarish (eng yangilari birinchi)
            dataList.reverse()

            val nextKey = if (dataList.size > params.loadSize) {
                snapshot.children.last().key
            } else {
                null
            }

            LoadResult.Page(
                data = dataList.take(params.loadSize),
                prevKey = null, // Orqaga paging qo'llab-quvvatlanmaydi
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<String, T>): String? {
        // Yangilanishda birinchi sahifadan boshlash uchun null qaytaramiz
        return null
    }
}