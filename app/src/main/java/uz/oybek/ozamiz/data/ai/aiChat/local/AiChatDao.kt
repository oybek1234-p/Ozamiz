package uz.oybek.ozamiz.data.ai.aiChat.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiChatDao {
    @Query("SELECT * FROM ai_chat_messages WHERE dateString = :date ORDER BY timestamp ASC")
    fun getMessagesForDateFlow(date: String): Flow<List<AiChatMessageEntity>>

    @Insert
    suspend fun insertMessage(message: AiChatMessageEntity)
    
    @Query("SELECT EXISTS(SELECT 1 FROM ai_chat_messages WHERE dateString = :date AND tag = :tag)")
    suspend fun hasMessageWithTag(date: String, tag: String): Boolean
    
    @Query("DELETE FROM ai_chat_messages WHERE dateString != :currentDate")
    suspend fun clearOldMessages(currentDate: String)

    @Query("DELETE FROM ai_chat_messages")
    suspend fun clearAllMessages()
}
