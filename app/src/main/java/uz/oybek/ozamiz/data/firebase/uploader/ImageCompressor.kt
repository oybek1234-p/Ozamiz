package uz.oybek.ozamiz.data.firebase.uploader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.util.handleException
import java.io.ByteArrayOutputStream

object ImageCompressor {

    suspend fun compressImage(
        uri: Uri,
        maxSizeKb: Int
    ): DataWrapper<ByteArray> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val bitmap = BitmapFactory.decodeStream(
                    appContext.contentResolver.openInputStream(uri)
                ) ?: return@withContext DataWrapper.Error()

                var quality = 100
                var byteArray: ByteArray
                val baos = ByteArrayOutputStream()

                do {
                    baos.reset()
                    bitmap.compress(Bitmap.CompressFormat.WEBP, quality, baos)
                    byteArray = baos.toByteArray()
                    quality -= 5

                    if (quality < 0) {
                        return@withContext DataWrapper.Error()
                    }
                } while (byteArray.size > maxSizeKb * 1024)
                bitmap.recycle()
                DataWrapper.Success(byteArray)
            } catch (e: Exception) {
                handleException(e)
                DataWrapper.Error()
            }
        }
}