package uz.oybek.ozamiz.data.ai

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.DataWrapper
import java.io.IOException

object GeminiSpeechRecognizer {

    private val speechModel by lazy {
        Firebase.ai.generativeModel("gemini-2.0-flash", systemInstruction = content {
            text(
                """
                Sen user nima yeganini o'zbek milliy taomlarini aniqlaydigan ekspert assistantsan.
                Sening asosiy maqsading - audio yozuvdagi oziq-ovqat nomlarini to'g'ri transkripsiya qilish.
                Muhim taomlar ro'yxati: [Somsa,Gumma, Honim, Osh, Manti, Sho'rva, Lag'mon, Shashlik, Kabob, Chuchvara, Dimlama, Norin, Mastava, Kartoshka].
                Nima ovqat yegani, qancha, nimalar borligini yoz. Boshqa gaplar kerak emas.
                1 kosa osh, 0.5 kola, pomidor bodring salat tarelka
            """.trimIndent()
            )
        })
    }

    suspend fun transcribeAudio(audioUri: Uri): DataWrapper<String> {
        return withContext(Dispatchers.IO) {
            try {
                val audioBytes = uriToByteArray(audioUri)
                if (audioBytes == null) {
                    return@withContext DataWrapper.Error("Audio faylni o'qishda xatolik.")
                }

                val inputContent = content {
                    inlineData(audioBytes, "audio/ogg")
                    text("Bu audio yozuvni matnga aylantir")
                }

                val response = speechModel.generateContent(inputContent)
                val transcribedText = response.text?.trim()

                if (transcribedText.isNullOrBlank()) {
                    DataWrapper.Error("AI ovozni aniqlay olmadi. Boshqa so'zlar bilan qayta urinib ko'ring.")
                } else {
                    DataWrapper.Success(transcribedText)
                }
            } catch (e: Exception) {
                DataWrapper.Error("Ovozni tahlil qilishda xatolik: ${e.localizedMessage}")
            }
        }
    }

    private fun uriToByteArray(uri: Uri): ByteArray? {
        return try {
            appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: IOException) {
            null
        }
    }
}