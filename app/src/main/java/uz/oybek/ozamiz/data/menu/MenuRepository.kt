package uz.oybek.ozamiz.data.menu

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.menu.model.MenuCategory
import uz.oybek.ozamiz.data.utils.DataWrapper

object MenuRepository {

    fun getCategories(): Flow<List<MenuCategory>> = flowOf(emptyList())

    suspend fun addCategory(name: String, isSecondary: Boolean = false): DataWrapper<Unit> {
        return DataWrapper.Success(Unit)
    }

    suspend fun updateCategory(category: MenuCategory): DataWrapper<Unit> {
        return DataWrapper.Success(Unit)
    }

    suspend fun deleteCategory(categoryId: String): DataWrapper<Unit> {
        return DataWrapper.Success(Unit)
    }

    fun getMenus(categoryId: String? = null): Flow<List<Menu>> = flowOf(emptyList())

    suspend fun searchMenus(queryText: String): DataWrapper<List<Menu>> {
        return DataWrapper.Success(emptyList())
    }

    suspend fun addMenu(menu: Menu, imageUri: Uri?): Boolean {
        return false
    }

    suspend fun deleteMenu(menuId: String): DataWrapper<Unit> {
        return DataWrapper.Success(Unit)
    }
}
