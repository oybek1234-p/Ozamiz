package uz.oybek.ozamiz.data.menu

import android.net.Uri
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.menu.model.MenuCategory
import uz.oybek.ozamiz.data.utils.DataWrapper
import java.util.UUID

object MenuRepository {

    fun getCategories(): Flow<List<MenuCategory>> = callbackFlow {
        val subscription = FirebasePaths.Firestore.menuCategories()
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val categories = snapshot?.toObjects(MenuCategory::class.java) ?: emptyList()
                trySend(categories)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addCategory(name: String, meal: Boolean = true, imageUri: Uri? = null): DataWrapper<Unit> {
        return try {
            val id = UUID.randomUUID().toString()
            var category = MenuCategory(id = id, name = name, meal = meal)
            if (imageUri != null) {
                val ref = FirebasePaths.Storage.categoryPhotos(id)
                ref.putFile(imageUri).await()
                val url = ref.downloadUrl.await().toString()
                category = category.copy(imageUrl = url)
            }
            FirebasePaths.Firestore.menuCategories().document(id).set(category).await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            DataWrapper.Error(e.message ?: "Noma'lum xato")
        }
    }

    suspend fun updateCategory(category: MenuCategory, imageUri: Uri? = null): DataWrapper<Unit> {
        return try {
            var categoryToSave = category
            if (imageUri != null) {
                val ref = FirebasePaths.Storage.categoryPhotos(category.id)
                ref.putFile(imageUri).await()
                val url = ref.downloadUrl.await().toString()
                categoryToSave = category.copy(imageUrl = url)
            }
            FirebasePaths.Firestore.menuCategories().document(category.id).set(categoryToSave).await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            DataWrapper.Error(e.message ?: "Noma'lum xato")
        }
    }

    suspend fun deleteCategory(categoryId: String): DataWrapper<Unit> {
        return try {
            FirebasePaths.Firestore.menuCategories().document(categoryId).delete().await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            DataWrapper.Error(e.message ?: "Noma'lum xato")
        }
    }

    fun getMenus(categoryId: String? = null): Flow<List<Menu>> = callbackFlow {
        var query = FirebasePaths.Firestore.menus() as Query
        if (categoryId != null) {
            query = query.whereArrayContains("categoryIds", categoryId)
        }
        
        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val menus = snapshot?.toObjects(Menu::class.java) ?: emptyList()
            trySend(menus)
        }
        awaitClose { subscription.remove() }
    }

    suspend fun searchMenus(queryText: String): DataWrapper<List<Menu>> {
        return try {
            val snapshot = FirebasePaths.Firestore.menus()
                .whereGreaterThanOrEqualTo("name", queryText)
                .whereLessThanOrEqualTo("name", queryText + "\uf8ff")
                .get()
                .await()
            val menus = snapshot.toObjects(Menu::class.java)
            DataWrapper.Success(menus)
        } catch (e: Exception) {
            DataWrapper.Error(e.message ?: "Noma'lum xato")
        }
    }

    suspend fun addMenu(menu: Menu, imageUri: Uri?): Boolean {
        return try {
            val id = menu.id.ifEmpty { UUID.randomUUID().toString() }
            val menuToSave = menu.copy(id = id)
            
            if (imageUri != null) {
                val ref = FirebasePaths.Storage.root.child("menus/$id.jpg")
                ref.putFile(imageUri).await()
                val url = ref.downloadUrl.await().toString()
                FirebasePaths.Firestore.menus().document(id).set(menuToSave.copy(imageUrl = url)).await()
            } else {
                FirebasePaths.Firestore.menus().document(id).set(menuToSave).await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteMenu(menuId: String): DataWrapper<Unit> {
        return try {
            FirebasePaths.Firestore.menus().document(menuId).delete().await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            DataWrapper.Error(e.message ?: "Noma'lum xato")
        }
    }

    fun getMeals(categories: List<MenuCategory>): List<MenuCategory> {
        return categories.filter { it.meal }
    }

    fun getDiets(categories: List<MenuCategory>): List<MenuCategory> {
        return categories.filter { !it.meal }
    }

    fun partitionCategories(categories: List<MenuCategory>): Pair<List<MenuCategory>, List<MenuCategory>> {
        return categories.partition { it.meal }
    }
}

