package uz.oybek.ozamiz.data.ai.aiChat.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_chat_messages")
data class AiChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val dateString: String,
    val tag: String? = null
) {
    companion object {
        const val TAG_LATE_FOOD = "late_food"
        const val TAG_CALORIE_LIMIT = "calorie_limit"
        const val TAG_PROTEIN_LOW = "protein_low"
        const val TAG_FAT_EXCEEDED = "fat_exceeded"
        const val TAG_CARBS_EXCEEDED = "carbs_exceeded"
        const val TAG_PROTEIN_EXCEEDED = "protein_exceeded"
    }
}
