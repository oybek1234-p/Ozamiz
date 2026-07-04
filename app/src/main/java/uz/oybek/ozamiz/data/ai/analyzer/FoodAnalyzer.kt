package uz.oybek.ozamiz.data.ai.analyzer

import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.crashlytics.crashlytics
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.ai.analyzer.model.FoodAnalysis
import uz.oybek.ozamiz.data.ai.analyzer.model.FoodAnalysisResult
import uz.oybek.ozamiz.data.user.local.user

object FoodAnalyzer {

    private val gson = Gson()
    private val cache = mutableMapOf<Int, FoodAnalysis>()
    private val conversationCache = mutableMapOf<Int, List<Content>>()

    private suspend fun getModel(): GenerativeModel {
        val prompt = FoodAnalyzerConfig.buildSystemPrompt()
        return Firebase.ai.generativeModel(
            modelName = FoodAnalyzerConfig.MODEL_NAME,
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

    fun getHistory(id: Int) = conversationCache[id] ?: emptyList()

    suspend fun analyze(
        history: List<Content>
    ): FoodAnalysis? = withContext(Dispatchers.IO) {
        if (history.isEmpty()) {
            return@withContext null
        }
        runCatching {
            val correctHistory = history.dropLast(1)
            val lastMessage = history.last()

            val chat = getModel().startChat(correctHistory)
            val response = chat.sendMessage(lastMessage)

            response.text?.parseResult()?.let { result ->
                FoodAnalysis(
                    id = result.hashCode(),
                    photoUrl = null,
                    result = result,
                    mealType = ""
                ).also {
                    cache[it.id] = it
                    val modelContent = content(role = "model") {
                        text(response.text ?: "")
                    }
                    conversationCache[it.id] = history + modelContent
                }
            } ?: run {
                null
            }
        }.onFailure {
            Firebase.crashlytics.recordException(it)
        }.getOrNull()
    }

    suspend fun analyzeAudio(
        audioUri: Uri,
        meal: String,
        consumedCalories: Int,
        waterMl: Int,
        burnedCalories: Int,
        targetCalories: Int
    ): FoodAnalysis? = withContext(Dispatchers.IO) {
        val audioBytes = audioUri.toBytes() ?: return@withContext null
        val prompt = FoodAnalyzerConfig.buildFoodPrompt(
            "", meal, user.gender, consumedCalories, waterMl, burnedCalories, targetCalories
        )

        val content = content {
            inlineData(audioBytes, "audio/ogg")
            text(prompt)
        }

        analyze(listOf(content))
    }

    suspend fun createInitialContent(
        imageUrl: Uri?,
        description: String,
        meal: String,
        consumedCalories: Int,
        waterMl: Int,
        burnedCalories: Int,
        targetCalories: Int
    ): Content = withContext(Dispatchers.IO) {
        val prompt = FoodAnalyzerConfig.buildFoodPrompt(
            description,
            meal,
            user.gender,
            consumedCalories,
            waterMl,
            burnedCalories,
            targetCalories
        )
        content {
            imageUrl?.toBytes()?.let { inlineData(it, "image/jpeg") }
            text(prompt)
        }
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
                Log.e("FoodAnalyzer", "❌ No JSON object found in response: ${cleaned.take(200)}")
                return null
            }

            val jsonString = cleaned.substring(jsonStart, jsonEnd + 1)
            Log.d("FoodAnalyzer", "🔍 Extracted JSON: ${jsonString.take(200)}")

            gson.fromJson(jsonString, FoodAnalysisResult::class.java)
        } catch (e: Exception) {
            Log.e("FoodAnalyzer", "❌ Parsing error: ${e.message}")
            Firebase.crashlytics.recordException(e)
            null
        }
    }
}