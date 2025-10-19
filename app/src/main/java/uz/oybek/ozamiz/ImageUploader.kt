package uz.oybek.ozamiz

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.firebase.FirebaseInstances.storage
import java.io.ByteArrayOutputStream
import java.util.UUID

object ImageUploader {

    const val OZAMIZ_PATH = "ozamiz"

    private const val DEFAULT_IMAGE_PATH = "food_images"
    private const val MAX_IMAGE_SIZE_KB = 100

    private fun getStorageRef(path: String = DEFAULT_IMAGE_PATH): StorageReference {
        return storage.reference.child(path)
    }

    suspend fun uploadImage(uri: Uri, path: String = DEFAULT_IMAGE_PATH): DataWrapper<String> {
        return try {
            val compressedData = compressImage(uri, MAX_IMAGE_SIZE_KB)
            if (compressedData is DataWrapper.Error) return DataWrapper.Error("") // Propagate Error

            val byteArray = (compressedData as DataWrapper.Success).data

            val fileExtension =
                appContext.contentResolver.getType(uri)?.substringAfterLast('/') ?: "jpg"
            val fileName = "${UUID.randomUUID()}.$fileExtension"
            val imageRef = getStorageRef(path).child(fileName)

            val uploadTask =
                imageRef.putBytes(byteArray).await() // Changed from putFile to putBytes
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

            Log.d("ImageUploader", "Image uploaded successfully. URL: $downloadUrl")
            DataWrapper.Success(downloadUrl)
        } catch (e: Exception) {
            handleException(e, "ImageUploadError - Path: $path")
            DataWrapper.Error(e.localizedMessage ?: "Rasm yuklashda noma'lum xatolik")
        }
    }

    private suspend fun compressImage(uri: Uri, maxSizeKb: Int): DataWrapper<ByteArray> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val bitmap =
                    BitmapFactory.decodeStream(appContext.contentResolver.openInputStream(uri))
                        ?: return@withContext DataWrapper.Error("Rasmni yuklashda xatolik: Bitmapga o'tkazib bo'lmadi")

                var quality = 100
                var byteArray: ByteArray
                val baos = ByteArrayOutputStream()

                do {
                    baos.reset()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                    byteArray = baos.toByteArray()
                    quality -= 5

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

    suspend fun deleteImage(
        imageUrl: String?, path: String = DEFAULT_IMAGE_PATH
    ): DataWrapper<Unit> {
        if (imageUrl.isNullOrEmpty()) {
            return DataWrapper.Success(Unit)
        }
        if (!imageUrl.startsWith("gs://") && !imageUrl.startsWith("https://firebasestorage.googleapis.com/")) {
            return DataWrapper.Success(Unit)
        }
        return try {
            val imageRef = storage.getReferenceFromUrl(imageUrl)
            imageRef.delete().await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "ImageDeleteError - Path: $path, URL: $imageUrl")
            DataWrapper.Success(Unit)
        }
    }
}

const val firebasePrefix = "https://firebasestorage"