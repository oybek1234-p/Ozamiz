package uz.oybek.ozamiz.data.ai.aiChat

import com.google.firebase.firestore.Exclude

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    @get:Exclude
    val isThinking: Boolean = false
)