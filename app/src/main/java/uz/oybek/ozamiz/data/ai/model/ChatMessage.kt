package uz.oybek.ozamiz.data.ai.model

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    @get:com.google.firebase.firestore.Exclude
    val isThinking: Boolean = false
)
