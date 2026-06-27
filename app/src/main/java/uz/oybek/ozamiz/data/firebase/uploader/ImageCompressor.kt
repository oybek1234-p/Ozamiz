package uz.oybek.ozamiz.data.firebase.uploader

import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.util.handleException
import java.io.File
import java.io.FileOutputStream

object ImageCompressor {

    suspend fun compressImageFile(
        uri: Uri,
        maxSizeKb: Int = 150
    ): File? = withContext(Dispatchers.IO) {
        try {
            val file = fromUri(uri) ?: return@withContext null

            val compressedFile = Compressor.compress(appContext, file) {
                resolution(500, 500)
                quality(85)
                format(Bitmap.CompressFormat.WEBP)
                size(maxSizeKb * 1024L)
            }

            launch {
                if (file.exists()) file.delete()
            }
            compressedFile
        } catch (e: Exception) {
            handleException(e)
            null
        }
    }

    suspend fun compressImage(
        uri: Uri,
        maxSizeKb: Int = 150
    ): DataWrapper<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val file = fromUri(uri) ?: return@withContext DataWrapper.Error()

            val compressedFile = Compressor.compress(appContext, file) {
                resolution(500, 500)
                quality(85)
                format(Bitmap.CompressFormat.WEBP)
                size(maxSizeKb * 1024L)
            }

            val byteArray = compressedFile.readBytes()

            launch {
                if (file.exists()) file.delete()
                if (compressedFile.exists() && compressedFile != file) compressedFile.delete()
            }
            DataWrapper.Success(byteArray)
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error()
        }
    }

    suspend fun compressImageToUri(
        uri: Uri,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024,
        quality: Int = 80,
        maxSizeKb: Int = 50
    ): Uri? = withContext(Dispatchers.IO) {
        val originalFile = fromUri(uri) ?: return@withContext null

        try {
            val compressedFile = Compressor.compress(appContext, originalFile) {
                resolution(maxWidth, maxHeight)
                quality(quality)
                format(Bitmap.CompressFormat.WEBP)
                size(maxSizeKb * 1024L)
            }

            launch {
                if (originalFile.exists()) originalFile.delete()
            }
            compressedFile.toUri()
        } catch (e: Exception) {
            handleException(e)
            null
        }
    }

    suspend fun compressImageToFile(
        uri: Uri,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024,
        quality: Int = 80,
        maxSizeKb: Int = 50
    ): File? = withContext(Dispatchers.IO) {
        val originalFile = fromUri(uri) ?: return@withContext null

        try {
            val compressedFile = Compressor.compress(appContext, originalFile) {
                resolution(maxWidth, maxHeight)
                quality(quality)
                format(Bitmap.CompressFormat.WEBP)
                size(maxSizeKb * 1024L)
            }

            launch {
                if (originalFile.exists()) originalFile.delete()
            }
            compressedFile
        } catch (e: Exception) {
            handleException(e)
            null
        }
    }

    suspend fun compressImageFileDirect(
        file: File,
        maxSizeKb: Int = 150
    ): File? = withContext(Dispatchers.IO) {
        try {
            val compressedFile = Compressor.compress(appContext, file) {
                resolution(500, 500)
                quality(85)
                format(Bitmap.CompressFormat.WEBP)
                size(maxSizeKb * 1024L)
            }
            compressedFile
        } catch (e: Exception) {
            handleException(e)
            null
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