package uz.oybek.ozamiz.data.ai

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import uz.oybek.ozamiz.data.ai.model.AiChat
import uz.oybek.ozamiz.data.ai.model.ChatMessage
import kotlinx.coroutines.tasks.await

object AiHistoryRepository {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private const val COLLECTION_AI_CHAT = "aiChat"
    private const val SUB_COLLECTION_CHATS = "chats"
    private const val SUB_COLLECTION_MESSAGES = "messages"

    private fun chatsRef(userId: String) =
        firestore.collection(COLLECTION_AI_CHAT)
            .document(userId)
            .collection(SUB_COLLECTION_CHATS)

    private fun messagesRef(userId: String, chatId: String) =
        chatsRef(userId).document(chatId).collection(SUB_COLLECTION_MESSAGES)

    suspend fun createNewChat(userId: String): String {
        val docRef = chatsRef(userId).document()
        val chat = AiChat(
            id = docRef.id,
            title = "Yangi suhbat",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        docRef.set(chat).await()
        return docRef.id
    }

    suspend fun saveMessage(userId: String, chatId: String, message: ChatMessage) {
        val docRef = messagesRef(userId, chatId).document()
        val messageWithId = message.copy(id = docRef.id)
        docRef.set(
            mapOf(
                "id" to messageWithId.id,
                "text" to messageWithId.text,
                "isUser" to messageWithId.isUser,
                "timestamp" to messageWithId.timestamp
            )
        ).await()

        chatsRef(userId).document(chatId).update(
            "updatedAt", System.currentTimeMillis()
        )
    }

    suspend fun updateChatTitle(userId: String, chatId: String, title: String) {
        chatsRef(userId).document(chatId).update("title", title).await()
    }
}
