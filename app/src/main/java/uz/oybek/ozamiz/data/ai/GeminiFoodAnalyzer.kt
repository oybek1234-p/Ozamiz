// C:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\data\ai\GeminiFoodAnalyzer.kt
package uz.oybek.ozamiz.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.handleException
import java.io.IOException

object GeminiFoodAnalyzer {

    private const val API_KEY = "AIzaSyC0wvLIiXMkIx-LosviSlXSJKLrbpXdgfI"
    private const val TAG = "GeminiFoodAnalyzer"

    private val generativeModel: GenerativeModel? by lazy {
        if (API_KEY.isBlank()) {
            return@lazy null
        }
        try {
            val config = generationConfig {
                temperature = 0.6f
                maxOutputTokens = 500
            }
            GenerativeModel(
                modelName = "gemini-2.0-flash", // Hozircha shu modelda qolamiz
                apiKey = API_KEY,
                generationConfig = config
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing GenerativeModel (Gemini)", e)
            null
        }
    }

    private fun uriToBitmap(context: Context, imageUri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI to Bitmap", e)
            null
        }
    }

    suspend fun analyzeFood(
        imageUri: Uri?,
        description: String,
        mealTime: String
    ): DataWrapper<GeminiAnalysisResult> {
        val model = generativeModel ?: return DataWrapper.Error("Gemini API sozlanmagan.")

        if (description.isBlank() && imageUri == null) {
            return DataWrapper.Error("Iltimos, ovqatni tavsiflang yoki rasm yuklang.")
        }

        val userAge = LocalUser.user.age
        val userWeight = LocalUser.user.weight
        val userGender = if (LocalUser.user.gender == 0) "Erkak" else "Ayol"

        val prompt = """
        Sen ozishga yordam beruvchi do'stona va amaliy oziq-ovqat tahlilchisiz.
        Foydalanuvchining maqsadi ozish va u ovqatlanish bilmaydi, hattoki protein, oqsil , yog nimaligini ham bilmaydi. Lox foydalanuvchi. 
        Javobingiz foydalanuvchini xursand qilishi, motivatsiya berishi va aniq, qisqa yo'l-yo'riq berishi kerak - shunga etibor ber!
        Ozingni huddi usha lox ozmoqchi odamdek tasavur qil birinchi va keyin javob chiqar.
        Javobing userni hursand qilishi va yul kursatishi shart, va ogoxlantirishi kerak.
        O'rniga nima ovqat yeyish mumkin ligi ayt. 4-5 ta misol bilan.
        Hamma javoblar user ovqatiga aloqador bulsin, sizlab yoz
        Bazi bir ovqatlarni qancha muddatda bir yeyishni ham aytib o't: Masalan: osh 2 haftada bir marta yeyish mumkin
        Foydalanuvchining yoshi: $userAge, vazni: $userWeight kg, jinsi: $userGender.
        Ovqatlanish vaqti: $mealTime
        Ovqat: "$description" ${if (imageUri != null) "(rasmda ko'rsatilgan)" else ""}

        Javobingiz O'TA QISQA, oddiy, amaliy va motivatsion bo'lsin.
        Natijani FAQAT quyidagi QAT'IY FORMATDA yozing. Har bir bo'lim yangi qatordan boshlansin:

        BENEFITS_ADVICE:
        ✅ [Ozishga ijobiy tomoni (1-2 jumla). Agar yo'q bo'lsa, bu qismni yozmang.]
        💡 [1 ta amaliy TAVSIYA (1-2 jumla). Agar yo'q bo'lsa, bu qismni yozmang.]
        WARNINGS_ONLY:
        ⚠️ [1 ta OGOHLANTIRISH (1-2 jumla). Har doim "⚠️" belgisini qo'ying. Agar yo'q bo'lsa, bu qismni yozmang(Juda qattiq bulma)]
        NUTRITION_INFO:
        Oqsil: [taxminiy son]g
        Yog': [taxminiy son]g
        Uglevod: [taxminiy son]g
        INGREDIENTS:
        Ko‘ringan Masalliqlar: [Icon(If available) Masalliq - taxminiy kal, Icon(If available)Masalliq - taxminiy kal, ...] (Noma'lum bo'lsa "-")
        ALTERNATIVES:
        Alternativ takliflar: [Ozish uchun mosroq boshqa ovqat (1 ta). Agar hozirgisi zo'r bo'lsa, "-".]
        """.trimIndent()

        return try {
            val bitmap: Bitmap? = imageUri?.let {
                withContext(Dispatchers.IO) { uriToBitmap(appContext, it) }
            }

            val inputContent = content {
                if (bitmap != null) {
                    image(bitmap)
                    if (description.isNotBlank()) text("Rasm va quyidagi tavsif asosida tahlil qiling:")
                } else {
                    text("Quyidagi tavsif asosida tahlil qiling:")
                }
                text(prompt)
            }

            val response = withContext(Dispatchers.IO) { model.generateContent(inputContent) }
            val responseText = response.text?.trim()

            if (responseText.isNullOrBlank()) {
                return DataWrapper.Error("AI dan bo'sh javob olindi. Qayta urinib ko'ring.")
            }

            val lines = responseText.lines().map { it.trim() }

            val healthAssessmentStr = lines.firstOrNull { it.startsWith("HEALTH_ASSESSMENT:") }?.substringAfter(":")?.trim()
            val isHealthy = when (healthAssessmentStr?.uppercase()) {
                "YES" -> true
                "NO" -> false
                else -> null
            }
            val healthFeedbackShort = lines.firstOrNull { it.startsWith("HEALTH_FEEDBACK_SHORT:") }?.substringAfter(":")?.trim()

            val benefitsStartIndex = lines.indexOfFirst { it.startsWith("BENEFITS_ADVICE:") } + 1
            val warningsStartIndex = lines.indexOfFirst { it.startsWith("WARNINGS_ONLY:") }
            val nutritionInfoStartIndex = lines.indexOfFirst { it.startsWith("NUTRITION_INFO:") }

            // benefitsAndAdviceLines, warningLines ni to'g'ri ajratishga e'tibor berish kerak
            val generalInsightsText = if (benefitsStartIndex > 0 && warningsStartIndex > benefitsStartIndex) {
                lines.subList(benefitsStartIndex, warningsStartIndex).joinToString("\n").trim()
            } else if (benefitsStartIndex > 0 && nutritionInfoStartIndex > benefitsStartIndex && warningsStartIndex == -1) {
                lines.subList(benefitsStartIndex, nutritionInfoStartIndex).joinToString("\n").trim()
            } else ""

            val onlyWarningsText = if (warningsStartIndex > 0 && nutritionInfoStartIndex > warningsStartIndex) {
                lines.subList(warningsStartIndex + 1, nutritionInfoStartIndex).joinToString("\n").trim()
            } else ""


            fun parseMacroValue(linePrefix: String): Double? {
                return lines.firstOrNull { it.startsWith(linePrefix) }
                    ?.substringAfter(":")?.substringBefore("g")?.trim()
                    ?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull()
            }

            val protein = parseMacroValue("Oqsil:")
            val fat = parseMacroValue("Yog':")
            val carbs = parseMacroValue("Uglevod:")

            val ingredientsLineText = lines.firstOrNull { it.startsWith("Ko‘ringan Masalliqlar:") }?.substringAfter(":")?.trim() ?: "-"
            val recognizedIngredients = if (ingredientsLineText == "-" || ingredientsLineText.isBlank()) {
                emptyList()
            } else {
                ingredientsLineText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }

            var finalEstimatedCalories = 0
            val caloriesRegex = Regex("""(\d+)\s*kal""")
            val match = caloriesRegex.find(healthFeedbackShort ?: "")
            if (match != null) {
                finalEstimatedCalories = match.groupValues[1].toIntOrNull() ?: 0
            } else {
                val caloriesFromMacros = ((protein ?: 0.0) * 4 + (carbs ?: 0.0) * 4 + (fat ?: 0.0) * 9).toInt()
                val totalCaloriesFromIngredients = recognizedIngredients.sumOf { item ->
                    Regex("""\((\d+)\s*kal\)""").find(item)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                }
                finalEstimatedCalories = when {
                    totalCaloriesFromIngredients > 0 -> totalCaloriesFromIngredients
                    caloriesFromMacros > 0 -> caloriesFromMacros
                    else -> 0
                }
            }

            val alternatives = lines.firstOrNull { it.startsWith("Alternativ takliflar:") }?.substringAfter(":")?.trim() ?: ""

            DataWrapper.Success(
                GeminiAnalysisResult(
                    estimatedCalories = finalEstimatedCalories,
                    estimatedProtein = protein,
                    estimatedFat = fat,
                    estimatedCarbs = carbs,
                    recognizedIngredients = recognizedIngredients,
                    insights = generalInsightsText,
                    alternativeFoodSuggestions = alternatives,
                    isHealthyForWeightLoss = isHealthy,
                    healthFeedbackShort = healthFeedbackShort,
                    warningsText = onlyWarningsText
                )
            )
        } catch (e: Exception) {
            handleException(e, "GeminiAnalyzeFoodError")
            DataWrapper.Error("AI tahlilida kutilmagan xatolik: ${e.message}")
        }
    }
}