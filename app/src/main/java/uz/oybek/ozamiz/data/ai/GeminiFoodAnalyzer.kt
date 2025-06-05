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
    private const val TAG = "ChatGptFoodAnalyzer"

    private val generativeModel: GenerativeModel? by lazy {
        if (API_KEY.isBlank()) {
            return@lazy null
        }
        try {
            val config = generationConfig {
                temperature = 0.6f
                maxOutputTokens = 2000
            }
            GenerativeModel(
                modelName = "gemini-2.0-flash", apiKey = API_KEY, generationConfig = config
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing GenerativeModel (Gemini)", e)
            null
        }
    }

    private fun uriToBitmap(context: Context, imageUri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(
                        context.contentResolver, imageUri
                    )
                )
            } else {
                @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(
                    context.contentResolver, imageUri
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI to Bitmap", e)
            null
        }
    }

    suspend fun analyzeFood(
        imageUri: Uri?, description: String, mealTime: String
    ): DataWrapper<GeminiAnalysisResult> {
        val model = generativeModel
            ?: return DataWrapper.Error("Gemini API sozlanmagan (kalit yetishmayapti yoki xato).")

        if (description.isBlank() && imageUri == null) {
            return DataWrapper.Error("Iltimos, ovqatni tavsiflang yoki rasm yuklang.")
        }

        val userAge = LocalUser.user.age
        val userWeight = LocalUser.user.weight
        val userGender = if (LocalUser.user.gender == 0) "Erkak" else "Ayol"

        val prompt = """
    Siz professional oziq-ovqat tahlilchisiz. Foydalanuvchining maqsadi ozish.
    Oziq-ovqat tarkibi haqida chuqur bilimga ega EMAS. Javobingiz juda qisqa, oddiy tilda va amaliy bo'lsin.
    "Uglevod", "oqsil", "yog'" kabi atamalarni ✅, ⚠️, 💡 qismlarida iloji boricha ishlatmang, o'rniga ularning ta'sirini oddiy tilda ("semirtiradi", "to'q tutadi") tushuntiring.
    Foydalanuvchining yoshi: $userAge yosh, vazni: $userWeight kg, jinsi: $userGender.
    Ovqatlanish vaqti: $mealTime

    Foydalanuvchi quyidagi ovqatni taqdim etdi:
    Tavsif: "$description"
    ${if (imageUri != null) "(Yuqoridagi rasmda ko'rsatilgan)" else ""}

    Bu ovqatni foydalanuvchining ozish maqsadiga qanchalik mos kelishini tahlil qiling. Tahlil natijasini FAQAT quyidagi QAT'IY FORMATDA yozing:
    ✅ [Ovqatning ozishga YORDAM BERADIGAN (juda qisqa, masalan: "Sabzi C vitaminiga boy", "Acchiqchuk kaloriyasi kam va foydali", "Tuxum to'q tutadi va energiya beradi"). Agar zararli bulsa shundagina bu qatorni yozmang]
    ⚠️ [Ovqat haqida ogoxlantirihsh agar ovqat miqdori kop bulsa yoki zararli bulsa(misol "Oshni faqat oyga bir marta yeyish mumkin"). Agar ogoxlantirish bulmasa yozma]
    💡 [Taviya va insighlar (juda qisqa, masalan: "Nonni kamaytiring, tuxum yoniga sabzavot oling."). Agar ovqat ozish uchun yaxshi bulsa yozma]
    Kichik ogoxlantirishlarga ahamiyat berma.
    Empty line
    Oqsil: taxminiy son
    Yog': taxminiy son g
    Uglevod: taxminiy son g

    Ko‘ringan Masalliqlar: [Masalliq(taxminiy kal), Masalliq (taxminiy kal), ...] (Masalan: "Qaynatilgan tuxum(155 kkal), "-" yozing.)

    FAQAT yuqoridagi formatda yozing. Boshqa hech qanday matn, kirish yoki yakuniy so'zlar BO'LMASIN. Har bir asosiy bo'lim (✅, ⚠️, 💡, Oqsil, Yog', Uglevod, Ko‘ringan Masalliqlar) yangi qatordan boshlansin.
    """.trimIndent()

        return try {
            val bitmap: Bitmap? = imageUri?.let {
                try {
                    withContext(Dispatchers.IO) { uriToBitmap(appContext, it) }
                } catch (e: IOException) {
                    Log.e(
                        "analyzeFood",
                        "Rasm yuklash/konvertatsiya qilishda xatolik: ${e.message}",
                        e
                    )
                    return DataWrapper.Error("Rasmni qayta ishlashda xatolik yuz berdi.")
                } catch (e: Exception) {
                    Log.e(
                        "analyzeFood",
                        "Rasm yuklash/konvertatsiya qilishda kutilmagan xatolik: ${e.message}",
                        e
                    )
                    return DataWrapper.Error("Rasmni yuklashda kutilmagan xatolik.")
                }
            }

            val inputContent = content {
                if (bitmap != null) {
                    image(bitmap)
                    if (description.isNotBlank()) {
                        text("Rasm va quyidagi tavsif asosida tahlil qiling:")
                    }
                } else {
                    text("Quyidagi tavsif asosida tahlil qiling:")
                }
                text(prompt)
            }

            val response = withContext(Dispatchers.IO) {
                model.generateContent(inputContent)
            }

            val responseText = response.text?.trim()
            if (responseText.isNullOrBlank()) {
                Log.w("analyzeFood", "AI dan bo'sh yoki null javob olindi. Prompt:\n$prompt")
                return DataWrapper.Error("AI dan bo'sh javob olindi. Qayta urinib ko'ring.")
            }

            val lines = responseText.lines().map { it.trim() }.filter { it.isNotEmpty() }

//            val mandatoryPrefixes =
//                listOf("⚠️", "💡", "Oqsil:", "Yog':", "Uglevod:", "Ko‘ringan Masalliqlar:")
//            val allMandatoryFound = mandatoryPrefixes.all { prefix ->
//                lines.any { it.startsWith(prefix) }
//            }
//
//            if (!allMandatoryFound) {
//                val missing =
//                    mandatoryPrefixes.filter { prefix -> !lines.any { it.startsWith(prefix) } }
//                Log.e(
//                    "analyzeFood",
//                    "AI javobi kutilgan formatda emas. Topilmadi: $missing. Javob:\n$responseText"
//                )
//                return DataWrapper.Error("AI javobi kutilgan formatda emas. Qayta urinib ko‘ring yoki tavsifni oydinlashtiring.")
//            }

            val insightsBuilder = StringBuilder()
            lines.firstOrNull { it.startsWith("✅") }
                ?.let { insightsBuilder.append(it).append("\n") }
            lines.firstOrNull { it.startsWith("⚠️") }
                ?.let { insightsBuilder.append(it).append("\n") }
            lines.firstOrNull { it.startsWith("💡") }
                ?.let { insightsBuilder.append(it).append("\n\n") }

            lines.firstOrNull { it.startsWith("Oqsil:") }
                ?.let { insightsBuilder.append(it).append("\n") }
            lines.firstOrNull { it.startsWith("Yog':") }
                ?.let { insightsBuilder.append(it).append("\n") }
            lines.firstOrNull { it.startsWith("Uglevod:") }
                ?.let { insightsBuilder.append(it).append("\n") }

            val fullInsightsText = insightsBuilder.toString().trim()

            fun parseMacroValue(lineContent: String?): Double? {
                return lineContent?.substringBefore("g")?.trim()
                    ?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull()
            }

            val proteinLineContent =
                lines.firstOrNull { it.startsWith("Oqsil:") }?.substringAfter("Oqsil:")
            val fatLineContent =
                lines.firstOrNull { it.startsWith("Yog':") }?.substringAfter("Yog':")
            val carbsLineContent =
                lines.firstOrNull { it.startsWith("Uglevod:") }?.substringAfter("Uglevod:")

            val protein = parseMacroValue(proteinLineContent)
            val fat = parseMacroValue(fatLineContent)
            val carbs = parseMacroValue(carbsLineContent)

            val ingredientsLineText =
                lines.firstOrNull { it.startsWith("Ko‘ringan Masalliqlar:") }?.substringAfter(":")
                    ?.trim() ?: "-"
            val recognizedIngredients =
                if (ingredientsLineText == "-" || ingredientsLineText.isBlank()) {
                    emptyList()
                } else {
                    ingredientsLineText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }

            var totalCaloriesFromIngredients = 0
            recognizedIngredients.forEach { item ->
                Regex("""\((\d+)\s*kkal\)""").find(item)?.groupValues?.get(1)?.toIntOrNull()?.let {
                    totalCaloriesFromIngredients += it
                }
            }
            val caloriesFromMacros =
                ((protein ?: 0.0) * 4 + (carbs ?: 0.0) * 4 + (fat ?: 0.0) * 9).toInt()
            val finalEstimatedCalories = when {
                totalCaloriesFromIngredients > 0 -> totalCaloriesFromIngredients
                caloriesFromMacros > 0 && totalCaloriesFromIngredients == 0 -> caloriesFromMacros // Use macro cals if ingredient cals are zero
                else -> 0 // Default if no info
            }

            return DataWrapper.Success(
                GeminiAnalysisResult(
                    estimatedCalories = finalEstimatedCalories,
                    estimatedProtein = protein,
                    estimatedFat = fat,
                    estimatedCarbs = carbs,
                    recognizedIngredients = recognizedIngredients,
                    insights = fullInsightsText,
                    ""
                )
            )

        } catch (e: Exception) {
            handleException(e, "GeminiAnalyzeError") // Assuming you have a global error handler
            Log.e("analyzeFood", "AI tahlilida kutilmagan xatolik: ${e.message}", e)
            return DataWrapper.Error("AI tahlilida kutilmagan xatolik yuz berdi.")
        }
    }

}