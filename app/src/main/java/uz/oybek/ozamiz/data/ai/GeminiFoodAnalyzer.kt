package uz.oybek.ozamiz.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser
import java.util.regex.Pattern

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
        val userName = LocalUser.user.name
        val userAge = LocalUser.user.age
        val userHeight = LocalUser.user.height
        val currentWeight = LocalUser.user.weight
        val gender = LocalUser.user.gender
        val targetCalorie = LocalUser.user.targetCalories
        val activityLevel = LocalUser.user.activityLevel

        val prompt = ("""
    USER INFO:
    - Maqsad: Ozish
    - Joylashuv: O'zbekiston
    
    VAZIFA:
    Sen O'zbekistondagi ozish uchun mo'ljallangan ilovada ekspert nutritionist assistantsan. Taqdim etilgan ovqat tavsifi va/yoki rasmini tahlil qil. O'zbek tilida qisqa, foydali va madaniyatga mos tahlil ber.

    Ovqat Tavsifi/So'rov: "$description"

    QO'LLANMA:
    1.  **Yagona Ovqat Tahlili**: Barcha aytilgan oziq-ovqat mahsulotlarini bitta ovqat deb hisobla. JAMI ozuqaviy qiymatlarni hisobla.
    2.  **Chiqish Formati**: FAQAT quyida ko'rsatilgan formatda javob ber. Hech qanday qo'shimcha matn, tushuntirish yoki salomlashish qo'shma. Javobda "[]" qavslar bo'lmasin.
    
    Bir nechta ovqat bulsa, hammasini umumiy qilib hisobla kaloriya va etc. 
    JAVOB FORMATI:
    NUTRITION_INFO: Kaloriya: INTEGER, Oqsil: DOUBLE, Yog': DOUBLE, Uglevodlar: DOUBLE
    INGREDIENTS: Asosiy masalliqlar ro'yxati, vergul bilan ajratilgan, "Emoji Nomi (Kaloriya)" formatida. Masalan: 🍗 Tovuq filesi (150), 🥒 Bodring (15), 🍅 Pomidor (20), etc.
    HEALTH_ADVICE: Yagona, qisqa, aniq harakatga undovchi maslahat ber, uning oldiga hissiy darajani (GREEN, YELLOW, yoki RED) qo'y. Bu ENG MUHIM qism. Maslahat aniq, raqamli va o'rinbosar taklif qiladigan bo'lsin.
        - GREEN: Sog'lom ovqat uchun. Maslahat ijobiy bo'lishi kerak. Masalan: "GREEN: Vitaminga boy, ozish uchun ajoyib tanlov! Be'malol yeyishingiz mumkin."
        - YELLOW: O'rtacha sog'lom ovqat uchun. Maslahat me'yorni yoki sog'lomroq tayyorlashni taklif qilishi kerak. Masalan: "YELLOW: Me'yorida yesa bo'ladi. Keyingi safar yog'siz pishiring yoki yarim porsiya yeng."
        - RED: Nosog'lom ovqat uchun. Maslahat aniq iste'mol chegarasini (masalan, haftasiga necha marta) yoki aniq sog'lomroq alternativani (masalan, "Cola Zero") berishi SHART. Masalan: "RED: Haftasiga 1 martadan oshirmang, yoki 'Cola Zero' iching.", "RED: Keyingi safar yarim porsiya yeng.""
    """).trimIndent()

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

            fun extractSection(key: String, text: String): String {
                val keys = listOf(
                    "NUTRITION_INFO:", "INGREDIENTS:", "HEALTH_ADVICE:"
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
                return text.substring(contentStart, endIndex).trim().removeSurrounding("[", "]")
                    .trim()
            }

            val nutritionInfoText = extractSection("NUTRITION_INFO:", responseText)
            val ingredientsText = extractSection("INGREDIENTS:", responseText)
            val healthAdviceText = extractSection("HEALTH_ADVICE:", responseText)


            fun parseNutritionValue(key: String, text: String): String? {
                return Regex("""$key:\s*([\d.]+)""").find(text)?.groupValues?.get(1)
            }

            fun parseIngredients(text: String): List<AnalyzedIngredient> {
                val ingredientPattern = Pattern.compile("([^,(]+)(?:\\s*\\((\\d+)\\))?")
                val matcher = ingredientPattern.matcher(text)
                val ingredients = mutableListOf<AnalyzedIngredient>()
                while (matcher.find()) {
                    val name = matcher.group(1)?.trim() ?: continue
                    val calories = matcher.group(2)?.toIntOrNull()
                    ingredients.add(AnalyzedIngredient(name, calories))
                }
                return ingredients
            }

            fun parseHealthAdvice(text: String): Pair<String?, String?> {
                return when {
                    text.startsWith("GREEN:") -> "GOOD" to text.removePrefix("GREEN:").trim()
                    text.startsWith("YELLOW:") -> "MODERATE" to text.removePrefix("YELLOW:").trim()
                    text.startsWith("RED:") -> "POOR" to text.removePrefix("RED:").trim()
                    else -> null to text
                }
            }

            val finalEstimatedCalories =
                parseNutritionValue("Kaloriya", nutritionInfoText)?.toIntOrNull() ?: 0
            val protein = parseNutritionValue("Oqsil", nutritionInfoText)?.toDoubleOrNull()
            val fat = parseNutritionValue("Yog'", nutritionInfoText)?.toDoubleOrNull()
            val carbs = parseNutritionValue("Uglevodlar", nutritionInfoText)?.toDoubleOrNull()

            val recognizedIngredients = parseIngredients(ingredientsText)
            val (healthLevel, healthAdvice) = parseHealthAdvice(healthAdviceText)

            DataWrapper.Success(
                GeminiAnalysisResult(
                    estimatedCalories = finalEstimatedCalories,
                    estimatedProtein = protein,
                    estimatedFat = fat,
                    estimatedCarbs = carbs,
                    recognizedIngredients = recognizedIngredients,
                    alternativeFoodSuggestions = "",
                    isHealthyForWeightLoss = healthLevel,
                    healthFeedbackShort = healthAdvice
                )
            )
        } catch (e: Exception) {
            DataWrapper.Error("AI tahlilida xatolik: ${e.message}")
        }
    }
}