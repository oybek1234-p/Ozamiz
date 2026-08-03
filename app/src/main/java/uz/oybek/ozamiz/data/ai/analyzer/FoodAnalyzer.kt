package uz.oybek.ozamiz.data.ai.analyzer

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.crashlytics.crashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.ai.AiPromptBuilder
import uz.oybek.ozamiz.data.ai.AiPromptBuilder.MODEL_NAME
import uz.oybek.ozamiz.data.utils.gson

object FoodAnalyzer {

    private val cache = mutableMapOf<Int, FoodAnalysis>()

    private suspend fun getModel(): GenerativeModel {
        val prompt = AiPromptBuilder.buildFoodInstruction()
        return Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content { text(prompt) }
        )
    }

    fun getLastAnalyzed(id: Int) = cache[id]

    fun updateCachedResult(id: Int, photoUrl: String?, mealType: String) {
        cache[id]?.let {
            it.photoUrl = photoUrl
            it.mealType = mealType
        }
    }

    suspend fun analyze(
        description: String?,
        meal: String,
        image: Uri?
    ): FoodAnalysis? = withContext(Dispatchers.IO) {
        runCatching {
            val userPrompt = AiPromptBuilder.buildFoodAnalyzerPrompt(
                mealTime = meal,
                userDescription = description.orEmpty(),
                photo = image != null
            )
            val model = getModel()
            val userContent = content {
                image?.toBytes()?.let { inlineData(it, "image/jpeg") }
                text(userPrompt)
            }
            val response = model.generateContent(userContent)

            response.text?.parseResult()?.let { result ->
                FoodAnalysis(
                    id = result.hashCode(),
                    photoUrl = image?.toString(),
                    result = result,
                    mealType = meal
                ).also {
                    cache[it.id] = it
                }
            }
        }.onFailure {
            Firebase.crashlytics.recordException(it)
        }.getOrNull()
    }

    suspend fun analyzeAudio(
        audioUri: Uri,
        meal: String
    ): FoodAnalysis? = withContext(Dispatchers.IO) {
        val audioBytes = audioUri.toBytes() ?: return@withContext null
        runCatching {
            val model = getModel()
            val userContent = content {
                inlineData(audioBytes, "audio/ogg")
                text(
                    AiPromptBuilder.buildFoodAnalyzerPrompt(
                        mealTime = meal,
                        audio = true
                    )
                )
            }
            val response = model.generateContent(userContent)
            response.text?.parseResult()?.let { result ->
                FoodAnalysis(
                    id = result.hashCode(),
                    photoUrl = null,
                    result = result,
                    mealType = meal
                ).also {
                    cache[it.id] = it
                }
            }
        }.onFailure {
            Firebase.crashlytics.recordException(it)
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
            Firebase.crashlytics.recordException(e)
            null
        }
    }
}