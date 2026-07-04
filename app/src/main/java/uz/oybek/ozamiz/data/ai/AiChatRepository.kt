package uz.oybek.ozamiz.data.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.data.ai.model.ChatMessage
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.aiChatsCollection
import uz.oybek.ozamiz.data.firebase.firestore
import uz.oybek.ozamiz.data.ai.analyzer.FoodAnalyzerConfig
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.utils.DataWrapper

object AiChatRepository {

    private var chatHistory = mutableListOf<Content>()

    suspend fun saveMessageToFirestore(
        chatId: String,
        text: String,
        isUser: Boolean,
        timestamp: Long
    ): Any? = withContext(Dispatchers.IO) {
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
            msgRef.set(
                mapOf(
                    ChatMessage::id.name to msgRef.id,
                    ChatMessage::text.name to text,
                    ChatMessage::isUser.name to isUser,
                    ChatMessage::timestamp.name to timestamp
                )
            ).await()

            chatRef.update("lastModified", timestamp).await()
        } catch (_: Exception) {
        }
    }

    private fun getModel(systemInstructionText: String) =
        Firebase.ai.generativeModel(
            modelName = FoodAnalyzerConfig.MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
            },
            systemInstruction = content { text(systemInstructionText) }
        )

    fun clearHistory() {
        chatHistory.clear()
    }

    fun syncHistory(messages: List<ChatMessage>) {
        chatHistory.clear()
        messages.forEach { msg ->
            val role = if (msg.isUser) "user" else "model"
            val safeText = msg.text.ifBlank { "..." }
            chatHistory.add(content(role = role) { text(safeText) })
        }
    }

    fun sendMessageStream(
        userMessage: String
    ): Flow<DataWrapper<String>> = flow {
        try {
            val systemPrompt = AiPromptBuilder.buildSystemPrompt()
            val userContent = content(role = "user") { text(userMessage) }
            val modelInstance = getModel(systemPrompt)
            val chat = modelInstance.startChat(chatHistory)
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
}