package uz.oybek.ozamiz.data.menu

import android.net.Uri
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.uploader.ImageCompressor
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.utils.DataWrapper
import java.util.Date
import java.util.UUID

object MenuRepository {

    suspend fun loadMenus(
        categoryId: String? = null,
        lastMenuId: String? = null,
        lastTimestamp: Long? = null,
        pageSize: Int = 12
    ): DataWrapper<List<Menu>> {
        return try {
            var query = FirebasePaths.Firestore.menus()
                .orderBy(Menu::createdAt.name, Query.Direction.DESCENDING)
                .limit(pageSize.toLong())

            if (categoryId != null) {
                query = query.whereArrayContains(Menu::categoryIds.name, categoryId)
            }

            if (lastTimestamp != null && lastMenuId != null) {
                val lastDate = Date(lastTimestamp)
                query = query.startAfter(lastDate)
            }
            val snapshot = query.get().await()
            val menus = snapshot.toObjects(Menu::class.java)
            DataWrapper.Success(menus)
        } catch (e: Exception) {
            e.printStackTrace()
            DataWrapper.Error(e.message)
        }
    }

    suspend fun searchMenus(
        queryText: String,
        lastName: String? = null,
        pageSize: Int = 12
    ): DataWrapper<List<Menu>> {
        return try {
            var query = FirebasePaths.Firestore.menus()
                .orderBy(Menu::name.name, Query.Direction.ASCENDING)
                .startAt(queryText)
                .endAt(queryText + "\uf8ff")
                .limit(pageSize.toLong())

            if (lastName != null) {
                query = query.startAfter(lastName)
            }

            val snapshot = query.get().await()
            val menus = snapshot.toObjects(Menu::class.java)
            DataWrapper.Success(menus)
        } catch (e: Exception) {
            DataWrapper.Error(e.message)
        }
    }

    suspend fun addMenu(menu: Menu, imageUri: Uri?): Boolean {
        return try {
            val id = menu.id.ifEmpty { UUID.randomUUID().toString() }
            var menuToSave = menu.copy(
                id = id,
                createdAt = menu.createdAt ?: Date()
            )

            if (imageUri != null) {
                val compressedUri = ImageCompressor.compressImageToUri(imageUri) ?: imageUri
                val photoUrl =
                    ImageUploader.uploadImage(compressedUri, FirebasePaths.Storage.menuPhotos(id))
                menuToSave = menuToSave.copy(imageUrl = photoUrl)
            }

            FirebasePaths.Firestore.menus().document(id).set(menuToSave).await()
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
            DataWrapper.Error(e.message)
        }
    }
}