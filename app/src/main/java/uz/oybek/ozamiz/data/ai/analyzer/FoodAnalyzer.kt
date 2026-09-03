package uz.oybek.ozamiz.data.ai.analyzer

import android.net.Uri
import android.util.Base64
import com.google.firebase.Firebase
import com.google.firebase.functions.functions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.utils.gson
import uz.oybek.ozamiz.data.utils.recordException

object FoodAnalyzer {

    private suspend fun callAnalyzeFoodFunction(
        description: String? = null,
        imageBase64: String? = null,
        mimeType: String? = null,
        audioBase64: String? = null,
        audioMimeType: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val data = hashMapOf<String, Any?>().apply {
            if (!description.isNullOrBlank()) put("description", description.trim())
            if (!imageBase64.isNullOrBlank()) {
                put("imageBase64", imageBase64)
                put("mimeType", mimeType ?: "image/jpeg")
            }
            if (!audioBase64.isNullOrBlank()) {
                put("audioBase64", audioBase64)
                put("audioMimeType", audioMimeType ?: "audio/ogg")
            }
        }

        val result = Firebase.functions("europe-west1")
            .getHttpsCallable("analyzeFood")
            .call(data)
            .await()

        val resultMap = result.data as? Map<*, *>
        resultMap?.get("text") as? String
    }

    suspend fun analyze(
        description: String?,
        meal: String,
        image: Uri?
    ): FoodAnalysis? = withContext(Dispatchers.IO) {
        runCatching {
            val imageBytes = image?.toBytes()
            val imageBase64 = imageBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
            val mimeType = if (image?.toString()?.endsWith(".webp", ignoreCase = true) == true) {
                "image/webp"
            } else {
                "image/jpeg"
            }

            val textResponse = callAnalyzeFoodFunction(
                description = description,
                imageBase64 = imageBase64,
                mimeType = mimeType
            )

            textResponse?.parseResult()?.let { result ->
                FoodAnalysis(
                    id = result.hashCode(),
                    photoUrl = image?.toString(),
                    result = result,
                    mealType = meal
                )
            }
        }.onFailure {
            android.util.Log.e("FoodAnalyzer", "Error in FoodAnalyzer.analyze: ${it.message}", it)
            recordException(it)
        }.getOrNull()
    }

    suspend fun analyzeAudio(
        audioUri: Uri,
        meal: String
    ): FoodAnalysis? = withContext(Dispatchers.IO) {
        val audioBytes = audioUri.toBytes() ?: return@withContext null
        runCatching {
            val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            val textResponse = callAnalyzeFoodFunction(
                audioBase64 = audioBase64,
                audioMimeType = "audio/ogg"
            )

            textResponse?.parseResult()?.let { result ->
                FoodAnalysis(
                    id = result.hashCode(),
                    photoUrl = null,
                    result = result,
                    mealType = meal
                )
            }
        }.onFailure {
            android.util.Log.e(
                "FoodAnalyzer",
                "Error in FoodAnalyzer.analyzeAudio: ${it.message}",
                it
            )
            recordException(it)
        }.getOrNull()
    }

    suspend fun analyzeSingleIngredient(input: String): SingleIngredientResult? =
        withContext(Dispatchers.IO) {
            runCatching {
                val textResponse = callAnalyzeFoodFunction(description = input.trim())
                textResponse?.parseSingleIngredientResult()
            }.onFailure {
                recordException(it)
            }.getOrNull()
        }

    private fun Uri.toBytes(): ByteArray? = runCatching {
        appContext.contentResolver.openInputStream(this)?.readBytes()
    }.getOrNull()

    private fun String.parseResult(): FoodAnalysisResult? {
        return try {
            val cleaned = this.trim()
            val jsonStart = cleaned.indexOf("{")
            val jsonEnd = cleaned.lastIndexOf("}")

            if (jsonStart == -1 || jsonEnd == -1) {
                return null
            }

            val jsonString = cleaned.substring(jsonStart, jsonEnd + 1)
            gson?.fromJson(jsonString, FoodAnalysisResult::class.java)
        } catch (e: Exception) {
            recordException(e)
            null
        }
    }

    private fun String.parseSingleIngredientResult(): SingleIngredientResult? {
        return try {
            val cleaned = this.trim()
            val jsonStart = cleaned.indexOf("{")
            val jsonEnd = cleaned.lastIndexOf("}")

            if (jsonStart == -1 || jsonEnd == -1) {
                return null
            }

            val jsonString = cleaned.substring(jsonStart, jsonEnd + 1)
            val fullResult = runCatching {
                gson?.fromJson(
                    jsonString,
                    FoodAnalysisResult::class.java
                )
            }.getOrNull()
            val firstItem = fullResult?.foodItems?.firstOrNull()
            if (firstItem != null) {
                return SingleIngredientResult(
                    name = firstItem.name,
                    calories = firstItem.calories ?: fullResult.getEffectiveCalories(),
                    amount = firstItem.amount,
                    unit = firstItem.unit ?: "g",
                    protein = firstItem.protein,
                    fat = firstItem.fat,
                    carbs = firstItem.carbs
                )
            }

            gson?.fromJson(jsonString, SingleIngredientResult::class.java)
        } catch (e: Exception) {
            recordException(e)
            null
        }
    }
}