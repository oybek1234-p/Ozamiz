package uz.oybek.ozamiz.data.firebase.uploader

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.data.firebase.FirebaseConstants
import uz.oybek.ozamiz.data.utils.DataWrapper
import java.util.UUID

object ImageUploader {

    private const val FIREBASE_STORAGE_API_PATH = "https://firebasestorage.googleapis.com/"
    private const val IMAGE_EXTENSION = "webp"
    private const val COMPRESSION_SIZE_KB = 100

    suspend fun uploadImage(
        uri: Uri,
        path: String = FirebaseConstants.STORAGE_MAIN
    ): DataWrapper<String> = withContext(Dispatchers.IO) {
        val compressedData = ImageCompressor.compressImage(
            uri = uri,
            maxSizeKb = COMPRESSION_SIZE_KB
        )

        val byteArray = when (compressedData) {
            is DataWrapper.Success -> compressedData.data
            is DataWrapper.Error -> return@withContext DataWrapper.Error()
        }

        try {
            val fileName = "${UUID.randomUUID()}.$IMAGE_EXTENSION"
            val imageRef = Firebase.storage.reference.child(
                path
            )
                .child(fileName)
            val uploadTask = imageRef.putBytes(byteArray).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            DataWrapper.Success(downloadUrl)
        } catch (e: Exception) {
            DataWrapper.Error()
        }
    }

    suspend fun deleteImage(imageUrl: String?): DataWrapper<Unit> = withContext(Dispatchers.IO) {
        if (imageUrl.isNullOrEmpty() || !imageUrl.startsWith(FIREBASE_STORAGE_API_PATH)) {
            return@withContext DataWrapper.Success(Unit)
        }

        try {
            Firebase.storage.getReferenceFromUrl(imageUrl).delete().await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            DataWrapper.Success(Unit)
        }
    }
}