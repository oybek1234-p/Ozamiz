package uz.oybek.ozamiz.data.firebase.uploader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.util.recordException
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUploader {

    private const val FIREBASE_STORAGE_API_PATH =
        "https://firebasestorage.googleapis.com/"
    private const val IMAGE_EXTENSION = "webp"

    suspend fun uploadImage(
        uri: Uri,
        folderRef: StorageReference,
        shouldBlurFaces: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        val originalFile = fromUri(uri) ?: return@withContext null

        // 1. Birinchi navbatda suratni to'liq compress qilamiz (500x500 WebP)
        val compressedFile = ImageCompressor.compressImageFileDirect(originalFile) ?: originalFile

        // 2. Keyin compress bo'lgan yengil suratni FaceBlurrer'ga beramiz
        if (shouldBlurFaces) {
            try {
                val bitmap = BitmapFactory.decodeFile(compressedFile.absolutePath)
                if (bitmap != null) {
                    val blurredBitmap = uz.oybek.ozamiz.util.FaceBlurrer.blurFaces(bitmap)
                    val out = FileOutputStream(compressedFile)
                    blurredBitmap.compress(Bitmap.CompressFormat.WEBP, 85, out)
                    out.close()
                    if (blurredBitmap != bitmap) {
                        bitmap.recycle()
                    }
                    blurredBitmap.recycle()
                }
            } catch (e: Exception) {
                recordException(e)
            }
        }

        val byteArray = compressedFile.readBytes()
        try {
            val fileName = "${UUID.randomUUID()}.$IMAGE_EXTENSION"
            val imageRef = folderRef.child(fileName)
            
            if (originalFile.exists() && originalFile != compressedFile) originalFile.delete()
            if (compressedFile.exists()) compressedFile.delete()
            
            val uploadTask = imageRef.putBytes(byteArray).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            downloadUrl
        } catch (e: Exception) {
            recordException(e)
            null
        }
    }

    suspend fun deleteImage(imageUrl: String?): BaseResult<Unit, Unit> = withContext(Dispatchers.IO) {
        if (imageUrl.isNullOrEmpty() || !imageUrl.startsWith(FIREBASE_STORAGE_API_PATH)) {
            return@withContext BaseResult.Success(Unit)
        }

        try {
            Firebase.storage.getReferenceFromUrl(imageUrl).delete().await()
            BaseResult.Success(Unit)
        } catch (e: Exception) {
            recordException(e)
            BaseResult.Success(Unit)
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
