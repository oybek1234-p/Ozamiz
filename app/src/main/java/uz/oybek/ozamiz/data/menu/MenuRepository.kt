package uz.oybek.ozamiz.data.menu

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import menuList
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.uploader.ImageCompressor
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.utils.DataWrapper
import java.util.Date
import java.util.UUID

object MenuRepository {

    init {
        addMenus()
    }

    private fun addMenus() {
//        menuList.forEach {
//            it.createdAt = Date()
//            FirebasePaths.Firestore.menus().add(it)
//        }
    }

    suspend fun loadMenus(
        categoryId: String? = null,
        lastMenuId: String? = null,
        lastTimestamp: Long? = null,
        pageSize: Int = 12
    ): DataWrapper<List<Menu>> {
        return try {
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
            val menus = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Menu::class.java)?.apply {
                    id = doc.id
                }
            }
            DataWrapper.Success(menus)
        } catch (e: Exception) {
            e.printStackTrace()
            DataWrapper.Error(e.message)
        }
    }

    suspend fun searchMenus(
        queryText: String, lastName: String? = null, pageSize: Int = 12
    ): DataWrapper<List<Menu>> {
        return try {
            var query =
                FirebasePaths.Firestore.menus().orderBy(Menu::name.name, Query.Direction.ASCENDING)
                    .startAt(queryText).endAt(queryText + "\uf8ff").limit(pageSize.toLong())

            if (lastName != null) {
                query = query.startAfter(lastName)
            }

            val snapshot = query.get().await()
            val menus = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Menu::class.java)?.apply {
                    id = doc.id
                }
            }
            DataWrapper.Success(menus)
        } catch (e: Exception) {
            DataWrapper.Error(e.message)
        }
    }

    suspend fun addMenu(menu: Menu, imageUri: Uri?): Boolean {
        return try {
            val id = menu.id.ifEmpty { UUID.randomUUID().toString() }
            var menuToSave = menu.copy(
                id = id, createdAt = menu.createdAt ?: Date()
            )

            if (imageUri != null) {
                val compressedUri = ImageCompressor.compressImageToUri(imageUri, maxSizeKb = 100) ?: imageUri
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
            val task = FirebasePaths.Firestore.menus().document(menuId).delete()
            task.addOnFailureListener {
                throw it
            }.addOnSuccessListener {
                    Log.d("MenuRepository", "deleteMenu success: $menuId")
                }
            task.await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            DataWrapper.Error(e.message)
        }
    }
}