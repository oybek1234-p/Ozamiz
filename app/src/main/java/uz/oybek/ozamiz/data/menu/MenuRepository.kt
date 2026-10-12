package uz.oybek.ozamiz.data.menu

import android.net.Uri
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.menu.model.Menu
import java.util.Date

object MenuRepository {

    suspend fun loadMenus(
        categoryId: String? = null,
        lastMenuId: String? = null,
        lastTimestamp: Long? = null,
        pageSize: Int = 12
    ): BaseResult<List<Menu>, String> = withContext(Dispatchers.IO) {
        try {
            var query = FirebasePaths.Firestore.menus()
                .orderBy(Menu::createdAt.name, Query.Direction.DESCENDING).limit(pageSize.toLong())

            if (categoryId != null) {
                query = query.whereArrayContains(Menu::categoryIds.name, categoryId)
            }

            if (lastTimestamp != null && lastMenuId != null) {
                val lastDate = Date(lastTimestamp)
                query = query.startAfter(lastDate)
            }

            val snapshot = query.get().await()
            val list = snapshot.toObjects(Menu::class.java)
            BaseResult.Success(list)
        } catch (e: Exception) {
            BaseResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun searchMenus(
        queryText: String = "",
        lastName: String? = null,
        pageSize: Int = 12
    ): BaseResult<List<Menu>, String> = withContext(Dispatchers.IO) {
        try {
            val snapshot = FirebasePaths.Firestore.menus().get().await()
            val allMenus = snapshot.toObjects(Menu::class.java)
            val filtered = allMenus.filter {
                it.name.contains(queryText, ignoreCase = true) ||
                        it.ingredients.any { ing ->
                            ing.contains(queryText, ignoreCase = true)
                        }
            }
            val paged = if (lastName != null) {
                val startIndex = filtered.indexOfFirst { it.name == lastName }
                if (startIndex != -1 && startIndex + 1 < filtered.size) {
                    filtered.subList(
                        startIndex + 1,
                        (startIndex + 1 + pageSize).coerceAtMost(filtered.size)
                    )
                } else {
                    emptyList()
                }
            } else {
                filtered.take(pageSize)
            }
            BaseResult.Success(paged)
        } catch (e: Exception) {
            BaseResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun searchMenus(query: String): BaseResult<List<Menu>, String> =
        searchMenus(queryText = query)

    suspend fun getMenuById(menuId: String): BaseResult<Menu, String> =
        withContext(Dispatchers.IO) {
            try {
                val doc = FirebasePaths.Firestore.menus().document(menuId).get().await()
                val menu = doc.toObject(Menu::class.java)
                if (menu != null) {
                    BaseResult.Success(menu)
                } else {
                    BaseResult.Failure("Menu not found")
                }
            } catch (e: Exception) {
                BaseResult.Failure(e.message ?: "Unknown error")
            }
        }

    suspend fun addMenu(
        menu: Menu,
        imageUri: Uri? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = if (menu.id.isNotBlank()) {
                FirebasePaths.Firestore.menus().document(menu.id)
            } else {
                FirebasePaths.Firestore.menus().document()
            }
            val menuToSave = if (menu.id.isBlank()) menu.copy(id = docRef.id) else menu
            docRef.set(menuToSave).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteMenu(menuId: String): BaseResult<Unit, String> = withContext(Dispatchers.IO) {
        try {
            FirebasePaths.Firestore.menus().document(menuId).delete().await()
            BaseResult.Success(Unit)
        } catch (e: Exception) {
            BaseResult.Failure(e.message ?: "Unknown error")
        }
    }
}