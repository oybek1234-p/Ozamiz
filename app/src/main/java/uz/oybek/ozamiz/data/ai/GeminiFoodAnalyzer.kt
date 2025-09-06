package uz.oybek.ozamiz.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser

object GeminiFoodAnalyzer {

    private val foodAnalysisModel by lazy {
        Firebase.ai.generativeModel("gemini-2.0-flash")
    }

    private fun uriToBitmap(context: Context, imageUri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, imageUri)
                )
            } else {
                @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(
                    context.contentResolver, imageUri
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun analyzeFood(
        imageUri: Uri?, description: String, mealTime: String
    ): DataWrapper<GeminiAnalysisResult> {
        if (description.isBlank() && imageUri == null) {
            return DataWrapper.Error("Iltimos, ovqatni tavsiflang yoki rasm yuklang.")
        }

        val user = LocalUser.user
        val prompt = """
    Siz vaznni nazorat qilishga yordam beruvchi professional dietolog-analitiksiz.
    Foydalanuvchi ma'lumotlarini hisobga olib, quyidagi ovqat(lar)ni prompt yoki image,yoki ikallasini analiz qiling.
    MUHIM: Agar bir nechta ovqat nomi berilgan bo'lsa, ularning barchasini YAGONA TAOM SIFATIDA ko'rib chiqing va ozuqaviy qiymatlarni (kaloriya, oqsil va hokazo) UMUMIY YIG'INDISINI hisoblang.
    Javobni FAQAT KO'RSATILGAN BO'LIMLAR BO'YICHA, o'zbek tilida va hech qanday ortiqcha matnsiz qaytaring.

    Foydalanuvchi ma'lumotlari:
    Yoshi: ${user.age}
    Vazni: ${user.weight} kg
    Jinsi: ${if (user.gender == 0) "Erkak" else "Ayol"}
    Tahlil qilinadigan taom(lar): "$description"
    
    Javob formati:
    NUTRITION_INFO: [UMUMIY ozuqaviy qiymatni ANIQ SHU FORMATDA yozing ":" dan keyin faqat raqam mumkin : Kaloriya: 0, Oqsil: 0.0, Yog': 0.0, Uglevodlar: 0.0]
    INGREDIENTS: [Barcha taomlardagi asosiy masalliqlar, vergul bilan ajratib]
    ALTERNATIVES: [Ushbu taomlar majmuasiga sog'lomroq alternativ takliflar]
    IS_HEALTHY_FOR_WEIGHT_LOSS: [Ushbu taomlar majmuasi vazn tashlash uchun foydalimi? Faqat "Ha" yoki "Yo'q" deb javob bering]
    HEALTH_FEEDBACK_SHORT: [Nima uchun vazn tashlash uchun foydali yoki emasligi haqida qisqa gap, yoki tavsiya]
    """.trimIndent()

        return try {
            val imageBitmap = imageUri?.let {
                withContext(Dispatchers.IO) { uriToBitmap(appContext, it) }
            }

            val inputContent = content {
                imageBitmap?.let { image(it) }
                if (prompt.isNotEmpty()) {
                    text(prompt)
                }
            }

            val response = withContext(Dispatchers.IO) {
                foodAnalysisModel.generateContent(inputContent)
            }

            val responseText = response.text?.trim()

            if (responseText.isNullOrBlank()) {
                return DataWrapper.Error("AI dan bo'sh javob olindi.")
            }

            // --- PARSING MANTIG'I O'ZGARISHSIZ QOLADI ---
            fun extractSection(key: String, text: String): String {
                val keys = listOf(
                    "NUTRITION_INFO:", "INGREDIENTS:",
                    "ALTERNATIVES:", "IS_HEALTHY_FOR_WEIGHT_LOSS:", "HEALTH_FEEDBACK_SHORT:"
                )
                val startIndex = text.indexOf(key)
                if (startIndex == -1) return ""

                val contentStart = startIndex + key.length
                var endIndex = text.length

                val nextKeyIndex = keys.map { text.indexOf(it, contentStart) }
                    .filter { it != -1 }
                    .minOrNull()

                if (nextKeyIndex != null) {
                    endIndex = nextKeyIndex
                }
                return text.substring(contentStart, endIndex).trim()
            }

            val nutritionInfoText = extractSection("NUTRITION_INFO:", responseText)
            val ingredientsText = extractSection("INGREDIENTS:", responseText)
            val alternatives = extractSection("ALTERNATIVES:", responseText)
            val isHealthyText = extractSection("IS_HEALTHY_FOR_WEIGHT_LOSS:", responseText)
            val healthFeedbackShort = extractSection("HEALTH_FEEDBACK_SHORT:", responseText)

            fun parseNutritionValue(key: String, text: String): String? {
                return Regex("""$key:\s*([\d.]+)""").find(text)?.groupValues?.get(1)
            }

            val finalEstimatedCalories =
                parseNutritionValue("Kaloriya", nutritionInfoText)?.toIntOrNull() ?: 0
            val protein = parseNutritionValue("Oqsil", nutritionInfoText)?.toDoubleOrNull()
            val fat = parseNutritionValue("Yog'", nutritionInfoText)?.toDoubleOrNull()
            val carbs = parseNutritionValue("Uglevodlar", nutritionInfoText)?.toDoubleOrNull()

            val recognizedIngredients = ingredientsText.split(",")
                .map { it.trim().removePrefix("*").trim() }
                .filter { it.isNotEmpty() }

            val isHealthyForWeightLoss = isHealthyText.equals("Ha", ignoreCase = true)

            DataWrapper.Success(
                GeminiAnalysisResult(
                    estimatedCalories = finalEstimatedCalories,
                    estimatedProtein = protein,
                    estimatedFat = fat,
                    estimatedCarbs = carbs,
                    recognizedIngredients = recognizedIngredients,
                    alternativeFoodSuggestions = alternatives,
                    isHealthyForWeightLoss = isHealthyForWeightLoss,
                    healthFeedbackShort = healthFeedbackShort.ifBlank { null }
                )
            )
        } catch (e: Exception) {
            DataWrapper.Error("AI tahlilida xatolik: ${e.message}")
        }
    }
}