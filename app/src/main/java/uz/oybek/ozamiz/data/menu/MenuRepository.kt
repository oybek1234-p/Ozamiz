package uz.oybek.ozamiz.data.menu

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.uploader.ImagePaths
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.menu.model.MenuCategory
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.util.handleException

object MenuRepository {

    private val db by lazy { Firebase.firestore }
    private val menuCollection = db.collection("menus")
    private val categoryCollection = db.collection("menu_categories")

    fun getCategories(): Flow<List<MenuCategory>> = callbackFlow {
        val listener = categoryCollection.addSnapshotListener { value, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(value?.toObjects(MenuCategory::class.java) ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    suspend fun addCategory(name: String, isSecondary: Boolean = false): DataWrapper<Unit> {
        return try {
            val id = categoryCollection.document().id
            categoryCollection.document(id).set(MenuCategory(id = id, name = name, isSecondary = isSecondary)).await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error()
        }
    }

    suspend fun updateCategory(category: MenuCategory): DataWrapper<Unit> {
        return try {
            require(category.id.isNotEmpty())
            categoryCollection.document(category.id).set(category).await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error()
        }
    }

    suspend fun deleteCategory(categoryId: String): DataWrapper<Unit> {
        return try {
            categoryCollection.document(categoryId).delete().await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error()
        }
    }

    fun getMenus(categoryId: String? = null): Flow<List<Menu>> = callbackFlow {
        var query: Query = menuCollection.orderBy(
            "createdAt",
            Query.Direction.DESCENDING
        )
        if (!categoryId.isNullOrEmpty() && categoryId != "all") {
            query = query.whereArrayContains("categoryIds", categoryId)
        }
        val listener = query.addSnapshotListener { value, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(value?.toObjects(Menu::class.java) ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    suspend fun searchMenus(queryText: String): DataWrapper<List<Menu>> {
        return try {
            val normalized = queryText.trim().lowercase()
            if (normalized.isEmpty()) return DataWrapper.Success(emptyList())

            val snapshot = menuCollection
                .whereArrayContains("searchKeywords", normalized)
                .get()
                .await()

            DataWrapper.Success(snapshot.toObjects(Menu::class.java))
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error()
        }
    }

    suspend fun addMenu(menu: Menu, imageUri: Uri?): Boolean {
        return try {
            val imageUrl = if (imageUri != null) {
                ImageUploader.uploadImage(imageUri, ImagePaths.main)
            } else menu.imageUrl
            if (imageUrl.isNullOrEmpty()) return false
            val id = menu.id.ifEmpty { menuCollection.document().id }
            val finalMenu = menu.copy(
                id = id,
                imageUrl = imageUrl,
                searchKeywords = generateKeywords(menu.name)
            )
            menuCollection.document(id).set(finalMenu).await()
            true
        } catch (e: Exception) {
            handleException(e)
            false
        }
    }

    suspend fun deleteMenu(menuId: String): DataWrapper<Unit> {
        return try {
            menuCollection.document(menuId).delete().await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error()
        }
    }

    private fun generateKeywords(name: String): List<String> {
        val keywords = mutableListOf<String>()
        name.lowercase().split(" ").forEach { word ->
            var temp = ""
            word.forEach { char ->
                temp += char
                keywords.add(temp)
            }
        }
        return keywords.distinct()
    }
}
