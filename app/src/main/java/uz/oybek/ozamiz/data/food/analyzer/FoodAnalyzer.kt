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

/**
 * Firebase Gemini orqali ovqatni tahlil qiladi
 */
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
            // Using generateContent with history is manually managing chat
            // Essentially we pass the whole conversation as input or use startChat if we want stateful
            // But since we want to be stateless in repository, simple generation with context is often sufficient
            // However, Gemini API 'generateContent' takes a single Content.
            // For chat, we should use model.startChat(history).sendMessage(lastMessage)
            
            // Let's assume the last message is the new input, and previous are history.
            val correctHistory = history.dropLast(1)
            val lastMessage = history.last()
            
            Log.d("FoodAnalyzer", "📝 History messages: ${correctHistory.size}, Last message role: ${lastMessage.role}")
            
            val chat = model.startChat(correctHistory)
            Log.d("FoodAnalyzer", "💬 Chat started, sending message...")
            
            val response = chat.sendMessage(lastMessage)
            Log.d("FoodAnalyzer", "✅ Response received: ${response.text?.take(100)}...")
            
            response.text?.parseResult()?.let { result ->
                Log.d("FoodAnalyzer", "✅ Parsed result: calories=${result.calories}, isAmbiguous=${result.isAmbiguous}")
                // Make sure to preserve ID if possible, or just generate new hash
                FoodAnalysis(
                    id = result.hashCode(),
                    photoUrl = "", // Set by ViewModel
                    result = result,
                    mealType = "" // Set by ViewModel
                ).also { 
                    cache[it.id] = it
                    // Append the model response to history and save
                    // We need to reconstruct the model content because 'response' is a GenerateContentResponse
                    // But for the next turn, we need the model's reply in the history.
                    // The easiest way is to add the generated text as a model part.
                    val modelContent = com.google.firebase.ai.type.content(role = "model") {
                        text(response.text ?: "")
                    }
                    conversationCache[it.id] = history + modelContent
                    Log.d("FoodAnalyzer", "💾 Cached result with id: ${it.id}")
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

    suspend fun createInitialContent(
       imageUrl: Uri?,
       description: String,
       meal: String
    ): com.google.firebase.ai.type.Content = withContext(Dispatchers.IO) {
        val prompt = FoodAnalyzerConfig.buildFoodPrompt(description, meal, user.gender)
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