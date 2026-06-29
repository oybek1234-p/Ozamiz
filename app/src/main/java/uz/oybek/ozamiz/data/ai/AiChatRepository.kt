package uz.oybek.ozamiz.data.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzerConfig
import uz.oybek.ozamiz.data.utils.DataWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import uz.oybek.ozamiz.data.logs.model.DailyLogs
import uz.oybek.ozamiz.data.logs.model.DailySummary
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.ai.model.ChatMessage
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.firestore
import uz.oybek.ozamiz.data.firebase.aiChatsCollection
import uz.oybek.ozamiz.data.user.UserRepository

object AiChatRepository {

    suspend fun saveMessageToFirestore(chatId: String, text: String, isUser: Boolean, timestamp: Long): Any? = withContext(Dispatchers.IO) {
        try {
            val chatRef = firestore.aiChatsCollection.document(chatId)
            val msgRef = chatRef.collection(FirebasePaths.Firestore.MESSAGES).document()

            chatRef.set(
                mapOf(
                    "userId" to UserRepository.user.uid,
                    "userName" to UserRepository.user.name,
                    "lastModified" to timestamp
                ),
                SetOptions.merge()
            ).await()
            msgRef.set(mapOf(
                ChatMessage::id.name to msgRef.id,
                ChatMessage::text.name to text,
                ChatMessage::isUser.name to isUser,
                ChatMessage::timestamp.name to timestamp
            )).await()
            
            chatRef.update("lastModified", timestamp).await()
        } catch (_: Exception) {}
    }

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

    private var chatHistory = mutableListOf<Content>()

    fun clearHistory() {
        chatHistory.clear()
    }

    fun syncHistory(messages: List<ChatMessage>) {
        chatHistory.clear()
        messages.forEach { msg ->
            val role = if (msg.isUser) "user" else "model"
            // To prevent empty strings from throwing exception
            val safeText = if (msg.text.isBlank()) "..." else msg.text
            chatHistory.add(content(role = role) { text(safeText) })
        }
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
