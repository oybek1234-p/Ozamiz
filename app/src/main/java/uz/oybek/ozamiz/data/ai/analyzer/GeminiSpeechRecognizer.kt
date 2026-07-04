package uz.oybek.ozamiz.data.ai.analyzer

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.utils.DataWrapper
import java.io.IOException

object GeminiSpeechRecognizer {

    private fun uriToByteArray(uri: Uri): ByteArray? {
        return try {
            val inputStream = appContext.contentResolver.openInputStream(uri)
            inputStream?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: IOException) {
            null
        }
    }
}