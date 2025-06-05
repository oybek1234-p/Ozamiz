package uz.oybek.ozamiz

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.firebase.FirebaseInstances.storage
import java.io.ByteArrayOutputStream
import java.util.UUID

object ImageUploader {

    private const val DEFAULT_IMAGE_PATH = "food_images" // Default path
    private const val MAX_IMAGE_SIZE_KB = 200 // Maximum allowed image size in KB

    private fun getStorageRef(path: String = DEFAULT_IMAGE_PATH): StorageReference {
        return storage.reference.child(path)
    }

    suspend fun uploadImage(uri: Uri, path: String = DEFAULT_IMAGE_PATH): DataWrapper<String> {
        return try {
            val compressedData = compressImage(uri, MAX_IMAGE_SIZE_KB)
            if (compressedData is DataWrapper.Error) return DataWrapper.Error("") // Propagate Error

            val byteArray = (compressedData as DataWrapper.Success).data

            val fileExtension = appContext.contentResolver.getType(uri)?.substringAfterLast('/') ?: "jpg"
            val fileName = "${UUID.randomUUID()}.$fileExtension"
            val imageRef = getStorageRef(path).child(fileName)

            val uploadTask = imageRef.putBytes(byteArray).await() // Changed from putFile to putBytes
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

            DataWrapper.Success(downloadUrl)
        } catch (e: Exception) {
            handleException(e, "ImageUploadError - Path: $path")
            DataWrapper.Error(e.localizedMessage ?: "Rasm yuklashda noma'lum xatolik")
        }
    }

    private suspend fun compressImage(uri: Uri, maxSizeKb: Int): DataWrapper<ByteArray> = withContext(Dispatchers.IO) {
        return@withContext try {
            val bitmap = BitmapFactory.decodeStream(appContext.contentResolver.openInputStream(uri))
                ?: return@withContext DataWrapper.Error("Rasmni yuklashda xatolik: Bitmapga o'tkazib bo'lmadi")

            var quality = 100
            var byteArray: ByteArray
            val baos = ByteArrayOutputStream()

            do {
                baos.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                byteArray = baos.toByteArray()
                quality -= 5 // Reduce quality by 5 each time

                if (quality < 0) {
                    return@withContext DataWrapper.Error("Rasmni siqishda xatolik: Qayta ishlash mumkin bo'lmagan rasm.")
                }
            } while (byteArray.size > maxSizeKb * 1024) // Check if image size exceeds limit

            bitmap.recycle() // Free memory
            DataWrapper.Success(byteArray)
        } catch (e: Exception) {
            handleException(e, "ImageCompressionError")
            DataWrapper.Error(e.localizedMessage ?: "Rasmni siqishda noma'lum xatolik")
        }
    }

    suspend fun deleteImage(imageUrl: String?, path: String = DEFAULT_IMAGE_PATH): DataWrapper<Unit> {
        if (imageUrl.isNullOrEmpty()) {
            return DataWrapper.Success(Unit) // URL bo'sh bo'lsa, hech narsa qilmaymiz
        }
        // Basic check for Firebase Storage URLs
        if (!imageUrl.startsWith("gs://") && !imageUrl.startsWith("https://firebasestorage.googleapis.com/")) {
            return DataWrapper.Success(Unit) // Not a Firebase URL, nothing to delete here
        }
        return try {
            // Attempt to get reference from URL (path parameter is not strictly needed for deletion if full URL is given)
            // However, if only a file name was stored and path is needed to reconstruct, then use path.
            // For simplicity, assuming full URL is always passed for deletion.
            val imageRef = storage.getReferenceFromUrl(imageUrl)
            imageRef.delete().await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "ImageDeleteError - Path: $path, URL: $imageUrl")
            // It's often okay to return Success even if deletion failed,
            // as the main operation (e.g., updating Firestore) might have succeeded.
            // Or return Error if deletion is critical.
            DataWrapper.Success(Unit) // For now, treat as success to not block other operations
        }
    }
}