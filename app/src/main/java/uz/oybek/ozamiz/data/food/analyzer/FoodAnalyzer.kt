package uz.oybek.ozamiz.data.food.analyzer

import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.food.analyzer.model.FoodAnalysis
import uz.oybek.ozamiz.data.food.analyzer.model.FoodAnalysisResult
import uz.oybek.ozamiz.data.user.local.user

object FoodAnalyzer {

    private val gson = Gson()
    private val cache = mutableMapOf<Int, FoodAnalysis>()
    private val conversationCache = mutableMapOf<Int, List<com.google.firebase.ai.type.Content>>()

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = FoodAnalyzerConfig.MODEL_NAME,
            systemInstruction = content { text(FoodAnalyzerConfig.FOOD_INSTRUCTION) }
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
        history: List<com.google.firebase.ai.type.Content>
    ): FoodAnalysis? = withContext(Dispatchers.IO) {
        if (history.isEmpty()) {
            Log.e("FoodAnalyzer", "❌ History is empty, cannot analyze")
            return@withContext null
        }

        Log.d("FoodAnalyzer", "🔍 Starting analysis with history size: ${history.size}")
        
        runCatching {
            val correctHistory = history.dropLast(1)
            val lastMessage = history.last()
            
            Log.d("FoodAnalyzer", "📝 History messages: ${correctHistory.size}, Last message role: ${lastMessage.role}")
            
            val chat = model.startChat(correctHistory)
            Log.d("FoodAnalyzer", "💬 Chat started, sending message...")
            
            val response = chat.sendMessage(lastMessage)
            Log.d("FoodAnalyzer", "✅ Response received: ${response.text?.take(100)}...")
            
            response.text?.parseResult()?.let { result ->
                Log.d("FoodAnalyzer", "✅ Parsed result: calories=${result.calories}, isAmbiguous=${result.isAmbiguous}")
                FoodAnalysis(
                    id = result.hashCode(),
                    photoUrl = null,
                    result = result,
                    mealType = ""
                ).also { 
                    cache[it.id] = it
                    val modelContent = com.google.firebase.ai.type.content(role = "model") {
                        text(response.text ?: "")
                    }
                    conversationCache[it.id] = history + modelContent
                }
            } ?: run {
                Log.e("FoodAnalyzer", "❌ Failed to parse response text: ${response.text}")
                null
            }
        }.onFailure { 
            Log.e("FoodAnalyzer", "❌ Analysis error: ${it.message}", it)
            Log.e("FoodAnalyzer", "❌ Error type: ${it.javaClass.simpleName}")
            Log.e("FoodAnalyzer", "❌ Stack trace: ${it.stackTraceToString()}")
        }
            .getOrNull()
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
    ): com.google.firebase.ai.type.Content = withContext(Dispatchers.IO) {
        val prompt = FoodAnalyzerConfig.buildFoodPrompt(
            description, meal, user.gender, consumedCalories, waterMl, burnedCalories, targetCalories
        )
        content {
            imageUrl?.toBytes()?.let { inlineData(it, "image/jpeg") }
            text(prompt)
        }
    }

    private fun Uri.toBytes(): ByteArray? = runCatching {
        appContext.contentResolver.openInputStream(this)?.readBytes()
    }.getOrNull()

    private fun String.parseResult(): FoodAnalysisResult? = runCatching {
        val cleaned = this.trim()
        
        val jsonString = when {
            cleaned.contains("```json") -> {
                cleaned.substringAfter("```json")
                    .substringBeforeLast("```")
                    .trim()
            }
            cleaned.startsWith("```") && cleaned.endsWith("```") -> {
                cleaned.removePrefix("```")
                    .removeSuffix("```")
                    .trim()
            }
            cleaned.startsWith("{") -> cleaned
            else -> {
                Log.e("FoodAnalyzer", "❌ Unexpected response format: ${cleaned.take(100)}")
                cleaned
            }
        }
        Log.d("FoodAnalyzer", "🔍 Extracted JSON: ${jsonString.take(200)}")
        gson.fromJson(jsonString, FoodAnalysisResult::class.java)
    }.getOrNull()
}