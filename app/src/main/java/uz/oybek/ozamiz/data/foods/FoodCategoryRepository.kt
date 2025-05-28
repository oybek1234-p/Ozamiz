// uz/oybek/ozamiz/data/foods/FoodCategoryRepository.kt
package uz.oybek.ozamiz.data.foods

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.ImageUploader
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.firebase.FirebaseInstances

class FoodCategoryRepository {

    private val firestore: FirebaseFirestore = FirebaseInstances.firestore
    private val categoryCollection = firestore.collection("food_categories")

    fun getAllCategories(): Flow<DataWrapper<List<FoodCategory>>> = callbackFlow {
        trySend(DataWrapper.Loading())
        val listenerRegistration = try {
            categoryCollection.orderBy("order", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(
                            DataWrapper.Error(
                                error.localizedMessage ?: "Kategoriyalarni yuklashda xatolik"
                            )
                        )
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        try {
                            val categories = snapshot.documents.mapNotNull { doc ->
                                doc.toObject<FoodCategory>()
                            }
                            trySend(DataWrapper.Success(categories))
                         } catch (e: Exception) {
                            trySend(DataWrapper.Error("Kategoriya ma'lumotlarini o'qishda xatolik"))
                        }
                    } else {
                        trySend(DataWrapper.Success(emptyList()))
                    }
                }
        } catch (e: Exception) {
            trySend(DataWrapper.Error(e.localizedMessage ?: "Kategoriya tinglash xatoligi"))
            close(e)
            return@callbackFlow
        }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    suspend fun addCategory(category: FoodCategory, imageUri: Uri?): DataWrapper<String> {
        var finalImageUrl: String? = null
        var uploadedImageForRollback: String? = null

        category.id = System.currentTimeMillis().toString()
        return try {
            if (imageUri != null) {
                when (val uploadResult = ImageUploader.uploadImage(imageUri)) {
                    is DataWrapper.Success -> {
                        finalImageUrl = uploadResult.data
                        uploadedImageForRollback = finalImageUrl
                    }

                    is DataWrapper.Error -> return DataWrapper.Error("Rasm yuklashda xatolik: ${uploadResult.error}")
                    is DataWrapper.Loading -> {}
                }
            }
            category.imageUrl = finalImageUrl
            categoryCollection.document(category.id).set(category).await()
            DataWrapper.Success(category.id)

        } catch (e: Exception) {
            uploadedImageForRollback?.let { ImageUploader.deleteImage(it) }
            DataWrapper.Error(e.localizedMessage ?: "Kategoriya qo'shishda xatolik")
        }
    }

    suspend fun updateCategory(category: FoodCategory, newImageUri: Uri?): DataWrapper<Unit> {
        if (category.id.isEmpty()) return DataWrapper.Error("Kategoriya ID si bo'sh")
        var finalImageUrl = category.imageUrl
        var oldImageUrlToDelete: String? = null
        var newImageUploadedUrl: String? = null

        return try {
            if (newImageUri != null) {
                when (val uploadResult = ImageUploader.uploadImage(newImageUri)) {
                    is DataWrapper.Success -> {
                        newImageUploadedUrl = uploadResult.data
                        if (!category.imageUrl.isNullOrEmpty() && category.imageUrl != newImageUploadedUrl) {
                            oldImageUrlToDelete = category.imageUrl
                        }
                        finalImageUrl = newImageUploadedUrl
                    }

                    is DataWrapper.Error -> return DataWrapper.Error("Yangi rasm yuklashda xatolik: ${uploadResult.error}")
                    is DataWrapper.Loading -> {}
                }
            }
            val categoryToUpdate = category.copy(imageUrl = finalImageUrl)
            categoryCollection.document(category.id).set(categoryToUpdate).await()

            oldImageUrlToDelete?.let { ImageUploader.deleteImage(it) }

            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            newImageUploadedUrl?.let { if (it != category.imageUrl) ImageUploader.deleteImage(it) }
            DataWrapper.Error(e.localizedMessage ?: "Kategoriyani yangilashda xatolik")
        }
    }

    suspend fun deleteCategory(categoryId: String): DataWrapper<Unit> {
        if (categoryId.isEmpty()) return DataWrapper.Error("Kategoriya ID si bo'sh")
        var imageUrlToDelete: String? = null
        return try {
            val categoryDoc = categoryCollection.document(categoryId).get().await()
            val categoryData = categoryDoc.toObject<FoodCategory>()
            imageUrlToDelete = categoryData?.imageUrl

            categoryCollection.document(categoryId).delete().await()
            imageUrlToDelete?.let { ImageUploader.deleteImage(it) }

            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            DataWrapper.Error(e.localizedMessage ?: "Kategoriyani o'chirishda xatolik")
        }
    }
}