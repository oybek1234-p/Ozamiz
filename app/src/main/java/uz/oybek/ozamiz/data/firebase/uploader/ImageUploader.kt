package uz.oybek.ozamiz.data.firebase.uploader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.utils.DataWrapper
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUploader {

    private const val FIREBASE_STORAGE_API_PATH =
        "https://firebasestorage.googleapis.com/"
    private const val IMAGE_EXTENSION = "webp"

    suspend fun uploadImage(
        uri: Uri,
        path: String,
        userId: String = LocalUser.user.uid,
        shouldBlurFaces: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        val originalFile = fromUri(uri) ?: return@withContext null

        val bitmap = if (shouldBlurFaces) {
            val originalBitmap = BitmapFactory.decodeFile(originalFile.absolutePath)
            uz.oybek.ozamiz.util.FaceBlurrer.blurFaces(originalBitmap)
        } else {
            null
        }

        val fileToCompress = if (bitmap != null) {
            val blurredFile = File(appContext.cacheDir, "blurred_${System.currentTimeMillis()}.webp")
            val out = FileOutputStream(blurredFile)
            bitmap.compress(Bitmap.CompressFormat.WEBP, 90, out)
            out.close()
            blurredFile
        } else {
            originalFile
        }
        val compressedFile = ImageCompressor.compressImageFileDirect(fileToCompress)
        val byteArray = compressedFile?.readBytes() ?: return@withContext null
        try {
            val fileName = "${UUID.randomUUID()}.$IMAGE_EXTENSION"
            var imageRef = Firebase.storage.reference
                .child(path)
            if (path != ImagePaths.main) {
                imageRef = imageRef.child(userId)
            }
            
            if (originalFile.exists()) originalFile.delete()
            if (fileToCompress != originalFile && fileToCompress.exists()) fileToCompress.delete()
            
            imageRef = imageRef.child(fileName)
            val uploadTask = imageRef.putBytes(byteArray).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            downloadUrl
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            return@withContext null
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
            Firebase.crashlytics.recordException(e)
            DataWrapper.Success(Unit)
        }
    }

    private fun fromUri(uri: Uri): File? {
        return try {
            val inputStream = appContext.contentResolver.openInputStream(uri)
                ?: return null
            val file = File(
                appContext.cacheDir,
                "temp_image_${System.currentTimeMillis()}.webp"
            )
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            if (file.exists() && file.length() > 0) {
                file
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}