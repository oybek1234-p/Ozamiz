package uz.oybek.ozamiz

import android.net.Uri
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.firebase.FirebaseInstances.storage
import java.util.UUID

object ImageUploader {

    private const val IMAGE_PATH = "food_images"

    private fun getStorageRef(): StorageReference {
        return storage.reference.child(IMAGE_PATH)
    }

    suspend fun uploadImage(uri: Uri): DataWrapper<String> {
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val imageRef = getStorageRef().child(fileName)

            val uploadTask = imageRef.putFile(uri).await()

            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

            DataWrapper.Success(downloadUrl)
        } catch (e: Exception) {
            handleException(e, "ImageUploadError")
            DataWrapper.Error(e.localizedMessage ?: "Rasm yuklashda noma'lum xatolik")
        }
    }

    suspend fun deleteImage(imageUrl: String?): DataWrapper<Unit> {
        if (imageUrl.isNullOrEmpty()) {
            return DataWrapper.Success(Unit) // URL bo'sh bo'lsa, hech narsa qilmaymiz
        }
        if (!imageUrl.startsWith("gs://") && !imageUrl.startsWith("https://firebasestorage.googleapis.com/")) {
            return DataWrapper.Success(Unit)
        }
        return try {
            val imageRef = storage.getReferenceFromUrl(imageUrl)
            imageRef.delete().await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "ImageDeleteError - URL: $imageUrl")
            DataWrapper.Success(Unit)
        }
    }
}
