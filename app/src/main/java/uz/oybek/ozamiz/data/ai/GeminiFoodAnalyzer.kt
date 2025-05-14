package uz.oybek.ozamiz.data.ai

import android.graphics.Bitmap
import android.net.Uri
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.DataWrapper

object GeminiFoodAnalyzer {

    private const val API_KEY = "AIzaSyAfAJjwFvNBO7QFUssoFPo0-s_yMfoUsAY"

    private val generativeModel by lazy {
        val apiKey = API_KEY
        if (apiKey.isEmpty()) {
            null
        } else {
            val config =
                generationConfig { temperature = 0.7f; topK = 1; topP = 1f; maxOutputTokens = 4000 }
            GenerativeModel("gemini-2.0-flash", apiKey, config)
        }
    }

    suspend fun analyzeFood(
        imageUri: Uri?, description: String
    ): DataWrapper<GeminiAnalysisResult> {
        val model = generativeModel ?: return DataWrapper.Error("Gemini API sozlanmagan")
        if (description.isBlank()) return DataWrapper.Error("Iltimos, ovqatni tavsiflang.")


        val bitmap = if (imageUri != null) {
            withContext(Dispatchers.IO) { uriToBitmap(imageUri) }
        } else null

        val prompt = """
            Analyze the food:"$description".
            Provide the estimated nutritional content according about and "visible ingredients on image(if exist)" on separate lines in this exact order:
            1. Calories (integer, kcal)
            2. Protein (decimal, grams, use '.' as decimal separator)
            3. Fat (decimal, grams, use '.' as decimal separator)
            4. Carbohydrates (decimal, grams, use '.' as decimal separator)
            5. Recognized visible ingredients (comma-separated list, e.g., Tovuq filesi, borokoli, guruch)
            6. Brief nutritional insights
            
            If a value or list cannot be reasonably estimated, write "-". Do not add any extra text before or after these 6 lines.
            Example output:
            480
            35.1
            18.5
            42.0
            Tovuq filesi, grechka, pomidor, bodring
            ✅Oqsilga boy, to'yimli.
            ✅Sabzavotlar vitamin manbai.
            ❗️Juda yog'li.
            ️❗️Non kop istemol qilmang.
        """.trimIndent()

        return try {
            val inputContent = content {
                bitmap?.let {
                    image(it);
                }
                text(prompt)
            }
            val response = withContext(Dispatchers.IO) { model.generateContent(inputContent) }
            val responseText = response.text?.trim() ?: ""

            if (responseText.isBlank()) return DataWrapper.Error("AI dan bo'sh javob olindi.")

            val lines = responseText.lines().map { it.trim() }
            if (lines.size < 6) return DataWrapper.Error("AI dan to'liq javob olinmadi.")

            val calories = lines[0].filter { it.isDigit() }.toIntOrNull() ?: 0
            val protein = lines[1].toDoubleOrNull()
            val fat = lines[2].toDoubleOrNull()
            val carbs = lines[3].toDoubleOrNull()
            val ingredientsString = lines[4]
            val ingredients = if (ingredientsString.equals(
                    "N/A", ignoreCase = true
                ) || ingredientsString.isBlank()
            ) {
                emptyList()
            } else {
                ingredientsString.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            }
            val insights = lines.drop(5).joinToString("\n").trim()

            if (calories <= 0) return DataWrapper.Error("Kaloriyani aniqlab bo'lmadi.")

            DataWrapper.Success(
                GeminiAnalysisResult(calories, protein, fat, carbs, ingredients, insights)
            )

        } catch (e: Exception) {
            e.printStackTrace()
            DataWrapper.Error("AI bilan bog'lanishda xatolik yuz berdi.")
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            appContext.contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            null
        }
    }
}