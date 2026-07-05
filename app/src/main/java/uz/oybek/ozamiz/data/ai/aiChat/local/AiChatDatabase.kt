package uz.oybek.ozamiz.data.ai.aiChat.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AiChatMessageEntity::class], version = 2, exportSchema = false)
abstract class AiChatDatabase : RoomDatabase() {
    abstract fun aiChatDao(): AiChatDao

    companion object {
        @Volatile
        private var INSTANCE: AiChatDatabase? = null

        fun getDatabase(context: Context): AiChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AiChatDatabase::class.java,
                    "ai_chat_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
