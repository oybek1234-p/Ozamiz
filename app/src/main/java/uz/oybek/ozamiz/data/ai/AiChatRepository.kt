package uz.oybek.ozamiz.data.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzerConfig
import uz.oybek.ozamiz.data.utils.DataWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import com.google.firebase.ai.type.GenerateContentResponse

object AiChatRepository {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = FoodAnalyzerConfig.MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
            }
        )
    }

    private var chatHistory = mutableListOf<com.google.firebase.ai.type.Content>()

    fun clearHistory() {
        chatHistory.clear()
    }

    suspend fun sendMessage(
        message: String,
        systemInstruction: String
    ): DataWrapper<String> = withContext(Dispatchers.IO) {
        try {
            ensureHistory(systemInstruction)

            val userContent = content(role = "user") { text(message) }
            val chat = model.startChat(chatHistory)
            val response = chat.sendMessage(userContent)
            
            val responseText = response.text ?: return@withContext DataWrapper.Error("AI javob bera olmadi")
            
            chatHistory.add(userContent)
            chatHistory.add(content(role = "model") { text(responseText) })
            
            DataWrapper.Success(responseText)
        } catch (e: Exception) {
            DataWrapper.Error(e.localizedMessage ?: "Xatolik yuz berdi")
        }
    }

    fun sendMessageStream(
        message: String,
        systemInstruction: String
    ): Flow<DataWrapper<String>> = flow {
        try {
            ensureHistory(systemInstruction)
            val userContent = content(role = "user") { text(message) }
            val chat = model.startChat(chatHistory)
            val stream = chat.sendMessageStream(userContent)
            
            var fullResponse = ""
            stream.collect { chunk ->
                val chunkText = chunk.text ?: ""
                fullResponse += chunkText
                emit(DataWrapper.Success(fullResponse))
            }
            
            chatHistory.add(userContent)
            chatHistory.add(content(role = "model") { text(fullResponse) })
        } catch (e: Exception) {
            emit(DataWrapper.Error(e.localizedMessage ?: "Xatolik yuz berdi"))
        }
    }

    private fun ensureHistory(systemInstruction: String) {
        if (chatHistory.isEmpty()) {
            chatHistory.add(content(role = "user") { text(systemInstruction) })
            chatHistory.add(content(role = "model") { text("Tushunarlu. Men tayyorman!") })
        }
    }
}
