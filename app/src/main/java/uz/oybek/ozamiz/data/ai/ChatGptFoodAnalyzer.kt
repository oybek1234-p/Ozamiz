package uz.oybek.ozamiz.data.ai

import android.net.Uri
import android.util.Log
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.foods.FoodItem
import uz.oybek.ozamiz.data.info.FoodLog
import uz.oybek.ozamiz.handleException
import java.time.LocalDate

object ChatGptFoodAnalyzer {

    private const val API_KEY =
        "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"
    private const val TAG = "ChatGptFoodAnalyzer"

    private val openAI: OpenAI? by lazy {
        OpenAI(API_KEY)
    }

    suspend fun analyzeFood(
        imageUri: Uri?, description: String
    ): DataWrapper<GeminiAnalysisResult> {
        val client =
            openAI ?: return DataWrapper.Error("OpenAI API sozlanmagan (kalit yetishmayapti)")
        if (description.isBlank()) return DataWrapper.Error("Iltimos, ovqatni tavsiflang.")

        val systemPrompt = """
        Siz professional oziq-ovqat tahlilchisiz va foydalanuvchining maqsadi ozish. Foydalanuvchi: ${LocalUser.user.age} yosh, ${LocalUser.user.weight} kg, jinsi: ${if (LocalUser.user.gender == 0) "Erkak" else "Ayol"}.

        Faqat quyidagi 7 qatorlik formatda javob yozing (agar qiymat noma'lum bo'lsa '-' qo'ying):
        🔥 Kaloriyalar: [taxminiy_son] kkal
        🥩 Protein: [son.son] g
        🧈 Yog': [son.son] g
        🍚 Uglevodlar: [son.son] g
        🥗 Tarkib: [ingredient1, ingredient2]
        ✅/❗️ Foyda/Kamchilik: [qisqa_fikr_1]
        💡 Tavsiya: [ozish_uchun_qisqa_maslahat]

        Misol:
        🔥 Kaloriyalar: 450 kkal
        🥩 Protein: 25.5 g
        🧈 Yog': 18.2 g
        🍚 Uglevodlar: 45.0 g
        🥗 Tarkib: guruch, sabzi, go'sht, yog'
        ❗️ Osh yog'li bo'lishi mumkin.
        💡 Tushlikda kichik porsiyada, non bilan emas. Haftada 1 marta.

        Faqat shu formatda, boshqa matnsiz javob ber. Kaloriyani taxmin qilishga harakat qil.
        """.trimIndent()

        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId("gpt-4o"), // Yoki "gpt-3.5-turbo"
            messages = listOf(
                ChatMessage(ChatRole.System, systemPrompt),
                ChatMessage(ChatRole.User, description) // Foydalanuvchi tavsifi
            ), temperature = 0.6, maxTokens = 200 // Javob qisqa bo'lishi kerak
            // Bu yerda JSON response format so'ralmagan
        )

        return try {
            Log.d(TAG, "analyzeFood uchun AI ga so'rov yuborilmoqda...")
            val completion = withContext(Dispatchers.IO) {
                client.chatCompletion(chatCompletionRequest)
            }

            val responseText = completion.choices.firstOrNull()?.message?.content?.trim()
            Log.d(TAG, "analyzeFood uchun AI javobi: $responseText")

            if (responseText.isNullOrBlank()) {
                Log.e(TAG, "analyzeFood: AI dan bo'sh javob olindi.")
                return DataWrapper.Error("AI dan bo'sh javob olindi.")
            }

            // Javobni qatorlarga bo'lib parse qilish (avvalgi mantig'ingiz)
            val lines = responseText.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size < 7) { // Kamida 7 qator kutiladi
                Log.e(
                    TAG,
                    "analyzeFood: AI dan yetarli javob olinmadi (7 qatordan kam). Javob: $responseText"
                )
                return DataWrapper.Error("AI dan to'liq javob olinmadi.")
            }

            // Parsing logikasi (avvalgi kodingizdan)
            val calories = lines[0].filter { it.isDigit() }.toIntOrNull() ?: 0
            val protein =
                lines[1].substringAfter(':').filter { it.isDigit() || it == '.' }.toDoubleOrNull()
            val fat =
                lines[2].substringAfter(':').filter { it.isDigit() || it == '.' }.toDoubleOrNull()
            val carbs =
                lines[3].substringAfter(':').filter { it.isDigit() || it == '.' }.toDoubleOrNull()
            val ingredientsString = lines[4].substringAfter(':').trim()
            val ingredients =
                if (ingredientsString == "-") emptyList() else ingredientsString.split(',')
                    .map { it.trim() }
            // Qolgan qatorlarni insights sifatida birlashtirish
            val insights = lines.drop(5).joinToString("\n")

            if (calories <= 0 && protein == null && fat == null && carbs == null) {
                Log.w(TAG, "analyzeFood: AI hech qanday ozuqaviy qiymat qaytarmadi.")
                //return DataWrapper.Error("AI ovqatni tahlil qila olmadi.") // Yoki Success bilan davom etish
            }

            DataWrapper.Success(
                GeminiAnalysisResult(calories, protein, fat, carbs, ingredients, insights)
            )

        } catch (e: Exception) {
            handleException(e, "ChatGptAnalysisError")
            Log.e(TAG, "analyzeFood so'rovida xatolik", e)
            // Xatolikni qaytarish
            DataWrapper.Error("AI bilan tahlil qilishda xatolik: ${e.message}")
        }
    }

}