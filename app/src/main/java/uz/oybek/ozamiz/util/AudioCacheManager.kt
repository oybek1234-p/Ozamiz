package uz.oybek.ozamiz.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.appContext
import java.io.File
import java.net.URL

object AudioCacheManager {

    private val cacheDir = File(appContext.cacheDir, "audio_cache")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    private fun getFileForUrl(url: String): File {
        val fileName = url.substringAfterLast('/')
        return File(cacheDir, fileName)
    }

    suspend fun getAudioFile(url: String): File? {
        return withContext(Dispatchers.IO) {
            val file = getFileForUrl(url)
            if (file.exists()) {
                return@withContext file
            }

            try {
                URL(url).openStream().use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext file
            } catch (e: Exception) {
                // Agar yuklashda xato bo'lsa, faylni o'chirish
                if (file.exists()) {
                    file.delete()
                }
                return@withContext null
            }
        }
    }

    fun isCached(url: String): Boolean {
        return getFileForUrl(url).exists()
    }

    suspend fun preloadAudios(urls: List<String>) {
        withContext(Dispatchers.IO) {
            urls.forEach { url ->
                if (!isCached(url)) {
                    getAudioFile(url) // Bu funksiya yuklab oladi va saqlaydi
                }
            }
        }
    }
}