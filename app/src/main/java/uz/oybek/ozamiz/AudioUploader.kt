package uz.oybek.ozamiz

import android.net.Uri
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.firebase.FirebaseInstances.storage
import java.util.UUID

object AudioUploader {

    private const val AUDIO_PATH = "audios"

    private fun getStorageRef(): StorageReference {
        return storage.reference.child(AUDIO_PATH)
    }

    suspend fun uploadAudio(uri: Uri): DataWrapper<String> {
        return try {
            val fileExtension = appContext.contentResolver.getType(uri)?.substringAfterLast('/') ?: "m4a"
            val fileName = "${UUID.randomUUID()}.$fileExtension"
            val audioRef = getStorageRef().child(fileName)

            val uploadTask = audioRef.putFile(uri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

            DataWrapper.Success(downloadUrl)
        } catch (e: Exception) {
            handleException(e, "AudioUploadError")
            DataWrapper.Error(e.localizedMessage ?: "Audio yuklashda noma'lum xatolik")
        }
    }

    suspend fun deleteAudio(audioUrl: String?): DataWrapper<Unit> {
        if (audioUrl.isNullOrEmpty()) {
            return DataWrapper.Success(Unit)
        }
        // Basic check for Firebase Storage URLs
        if (!audioUrl.startsWith("gs://") && !audioUrl.startsWith("https://firebasestorage.googleapis.com/")) {
            return DataWrapper.Success(Unit) // Not a Firebase URL, nothing to delete here
        }
        return try {
            val audioRef = storage.getReferenceFromUrl(audioUrl)
            audioRef.delete().await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            // Log the error but treat deletion failure as non-critical usually
             handleException(e, "AudioDeleteError - URL: $audioUrl")
             // It's often okay to return Success even if deletion failed,
             // as the main operation (e.g., updating Firestore) might have succeeded.
             // Or return Error if deletion is critical:
             // DataWrapper.Error(e.localizedMessage ?: "Audioni o'chirishda xatolik")
            DataWrapper.Success(Unit) // For now, treat as success to not block other operations
        }
    }
}