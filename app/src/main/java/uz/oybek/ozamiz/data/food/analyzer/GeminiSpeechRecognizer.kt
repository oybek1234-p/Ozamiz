package uz.oybek.ozamiz.data.food.analyzer

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

    private val speechModel by lazy {
        Firebase.ai.generativeModel(
            FoodAnalyzerConfig.MODEL_VERSION,
            systemInstruction = content {
                text(FoodAnalyzerConfig.AUDIO_INSTRUCTION)
            })
    }

    suspend fun transcribeAudio(audioUri: Uri): DataWrapper<String> {
        return withContext(Dispatchers.IO) {
            try {
                val audioBytes = uriToByteArray(audioUri)
                if (audioBytes == null) {
                    return@withContext DataWrapper.Error()
                }
                val inputContent = content {
                    inlineData(audioBytes, "audio/ogg")
                    text(FoodAnalyzerConfig.AUDIO_PROMPT)
                }
                val response = speechModel.generateContent(inputContent)
                val transcribedText = response.text?.trim()

                if (transcribedText.isNullOrBlank()) {
                    DataWrapper.Error()
                } else {
                    DataWrapper.Success(transcribedText)
                }
            } catch (e: Exception) {
                DataWrapper.Error()
            }
        }
    }

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