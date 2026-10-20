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
                val prefs = context.getSharedPreferences("room_db_versions", Context.MODE_PRIVATE)
                var currentSuffix = prefs.getInt("ai_chat_database_suffix", 0)

                fun createInstance(name: String): AiChatDatabase {
                    return Room.databaseBuilder(
                        context.applicationContext,
                        AiChatDatabase::class.java,
                        name
                    )
                    .fallbackToDestructiveMigration(true)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                }

                repeat(3) {
                    val activeName = if (currentSuffix == 0) "ai_chat_database" else "ai_chat_database_$currentSuffix"
                    try {
                        val db = createInstance(activeName)
                        db.openHelper.writableDatabase
                        if (currentSuffix != prefs.getInt("ai_chat_database_suffix", 0)) {
                            prefs.edit().putInt("ai_chat_database_suffix", currentSuffix).apply()
                        }
                        INSTANCE = db
                        return@synchronized db
                    } catch (_: Throwable) {
                        runCatching { context.deleteDatabase(activeName) }
                        currentSuffix++
                        prefs.edit().putInt("ai_chat_database_suffix", currentSuffix).apply()
                    }
                }

                val finalInstance = try {
                    val fallbackDb = createInstance("ai_chat_database_fallback_${System.currentTimeMillis()}")
                    fallbackDb.openHelper.writableDatabase
                    fallbackDb
                } catch (_: Throwable) {
                    Room.inMemoryDatabaseBuilder(
                        context.applicationContext,
                        AiChatDatabase::class.java
                    )
                    .fallbackToDestructiveMigration(true)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                }

                INSTANCE = finalInstance
                finalInstance
            }
        }
    }
}
