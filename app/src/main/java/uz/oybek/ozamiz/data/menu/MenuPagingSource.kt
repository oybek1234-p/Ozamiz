package uz.oybek.ozamiz.data.menu

import androidx.paging.PagingSource
import androidx.paging.PagingState
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.utils.DataWrapper

class MenuPagingSource(
    private val categoryId: String?,
    private val query: String? = null
) : PagingSource<Menu, Menu>() {

    override fun getRefreshKey(state: PagingState<Menu, Menu>): Menu? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestItemToPosition(anchorPosition)
        }
    }

    override suspend fun load(params: LoadParams<Menu>): LoadResult<Menu, Menu> {
        return try {
            val lastMenu = params.key
            
            val result = if (!query.isNullOrBlank()) {
                MenuRepository.searchMenus(
                    queryText = query,
                    lastName = lastMenu?.name,
                    pageSize = params.loadSize
                )
            } else {
                MenuRepository.loadMenus(
                    categoryId = categoryId,
                    lastMenuId = lastMenu?.id,
                    lastTimestamp = lastMenu?.createdAt?.time,
                    pageSize = params.loadSize
                )
            }

            when (result) {
                is DataWrapper.Success -> {
                    val menus = result.data
                    LoadResult.Page(
                        data = menus,
                        prevKey = null,
                        nextKey = if (menus.size < params.loadSize) null else menus.lastOrNull()
                    )
                }
                is DataWrapper.Error -> {
                    LoadResult.Error(Exception(result.error ?: "Unknown error"))
                }
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
