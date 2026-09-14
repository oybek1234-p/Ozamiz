package uz.oybek.ozamiz.data.ai.aiChat

import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.functions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.ai.AiPromptBuilder
import uz.oybek.ozamiz.data.ai.aiChat.local.AiChatDatabase
import uz.oybek.ozamiz.data.ai.aiChat.local.AiChatMessageEntity
import uz.oybek.ozamiz.data.config.AppPreference
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.logs.LogRepository
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.user.UserProfileRepository.user
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import java.util.Calendar
import java.util.Date

object AiChatRepository {

    private val _newMessagesEvent = MutableSharedFlow<String>()
    val newMessagesEvent = _newMessagesEvent.asSharedFlow()

    private val _suggestedReplies = MutableStateFlow<List<String>>(emptyList())

    private val aiChatDao by lazy {
        AiChatDatabase.getDatabase(appContext).aiChatDao()
    }

    suspend fun getCachedMessages(): List<AiChatMessageEntity>? {
        return aiChatDao.getMessagesForDateFlow(DateUtils.todayString()).firstOrNull()
    }

    suspend fun saveMessageToFirestore(
        chatId: String, text: String, isUser: Boolean, timestamp: Long
    ): Any? = withContext(Dispatchers.IO) {
        try {
            val chatRef = FirebasePaths.Firestore.aiChat(chatId)
            val msgRef = FirebasePaths.Firestore.aiMessages(chatId).document()

            chatRef.set(
                mapOf(
                    "userId" to user.uid,
                    "userName" to user.name,
                    "lastModified" to timestamp
                ), SetOptions.merge()
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

    private suspend fun callChatFunction(
        message: String,
        history: List<Map<String, String>>,
        userContext: String
    ): String? = withContext(Dispatchers.IO) {
        val data = hashMapOf(
            "message" to message.trim(),
            "history" to history,
            "userContext" to userContext,
            "systemInstruction" to userContext
        )

        val result = Firebase.functions("europe-west1")
            .getHttpsCallable("chatWithDietolog")
            .call(data)
            .await()

        val resultMap = result.data as? Map<*, *>
        resultMap?.get("text") as? String
    }

    suspend fun clearHistory() {
        android.util.Log.d("DietologAI", "[CLEAR HISTORY]")
        aiChatDao.clearAllMessages()
        clearReplies()
    }

    suspend fun saveCachedMessage(text: String, isUser: Boolean, tag: String? = null) {
        val entity = AiChatMessageEntity(
            text = text,
            isUser = isUser,
            timestamp = System.currentTimeMillis(),
            dateString = DateUtils.todayString(),
            tag = tag
        )
        aiChatDao.insertMessage(entity)
    }

    fun clearReplies() {
        _suggestedReplies.value = emptyList()
    }

    fun sendMessage(
        text: String,
        chatId: String?,
        scope: CoroutineScope
    ): Flow<DataWrapper<String>> = flow {

        val delimiterRegex = Regex("---+\\s*suggestions?\\s*---+", RegexOption.IGNORE_CASE)
        val now = System.currentTimeMillis()

        try {
            // 1. Avvalgi xabarlar tarixini o'qib olish (maksimum 4 ta xabar)
            val cached = getCachedMessages().orEmpty()
            val recentCached = cached.takeLast(4)
            val historyList = recentCached.map { msg ->
                mapOf(
                    "role" to if (msg.isUser) "user" else "model",
                    "text" to msg.text
                )
            }

            // 2. Foydalanuvchi xabarini saqlash
            scope.launch(Dispatchers.IO) {
                try {
                    saveCachedMessage(text, true)
                } catch (e: Exception) {
                    android.util.Log.e("DietologAI", "Error caching user message", e)
                }

                if (chatId != null) {
                    try {
                        saveMessageToFirestore(chatId, text, true, now)
                    } catch (e: Exception) {
                        android.util.Log.e("DietologAI", "Error saving to Firestore", e)
                    }
                }
            }

            val userContext = AiPromptBuilder.buildUserContext()
            val fullResponse = callChatFunction(text, historyList, userContext) ?: ""

            val finalCleanText =
                fullResponse.split(delimiterRegex).firstOrNull()?.trim() ?: fullResponse.trim()
            android.util.Log.d("DietologAI", "[AI RESPONSE FINAL]:\n$finalCleanText")

            if (finalCleanText.isNotBlank()) {
                // 2. DARHOL xabarni chiqarish (hech qanday kutishsiz)
                emit(DataWrapper.Success(finalCleanText))

                extractAndEmitSuggestions(fullResponse, delimiterRegex)

                // 3. Tashqi scope'da bazaga yozish (UI ni ushlamaydi)
                scope.launch(Dispatchers.IO) {
                    try {
                        saveCachedMessage(finalCleanText, false)
                    } catch (e: Exception) {
                        android.util.Log.e("DietologAI", "Error caching final AI response", e)
                    }

                    if (chatId != null) {
                        try {
                            saveMessageToFirestore(
                                chatId,
                                finalCleanText,
                                false,
                                System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "DietologAI",
                                "Error saving final AI response to Firestore",
                                e
                            )
                        }
                    }
                }
            } else {
                throw IllegalStateException("Bo'sh javob qaytdi")
            }
            android.util.Log.d("DietologAI", "==================================================")
        } catch (e: Exception) {
            android.util.Log.e("DietologAI", "[ERROR OCCURRED]: ${e.message}", e)
            val errorMsg =
                "Kechirasiz, javob tayyorlanishida xatolik yuz berdi. Qaytadan so'rashingiz mumkin."
            scope.launch(Dispatchers.IO) {
                try {
                    saveCachedMessage(errorMsg, false)
                } catch (_: Exception) {
                }
            }
            emit(DataWrapper.Success(errorMsg))
        }
    }

    private fun extractAndEmitSuggestions(fullResponse: String, delimiterRegex: Regex) {
        val parts = fullResponse.split(delimiterRegex)
        if (parts.size > 1) {
            val suggestionsSection = parts[1].trim()
            val questions = suggestionsSection.lines()
                .map { it.replace(Regex("^[0-9•\\-\\.\\s]+"), "").trim() }
                .filter { it.isNotBlank() && it.length in 4..45 }
                .take(3)
            if (questions.isNotEmpty()) {
                _suggestedReplies.value = questions
                android.util.Log.d("DietologAI", "[SUGGESTIONS EXTRACTED]: $questions")
            }
        }
    }

    suspend fun saveFeedback(
        userQuestion: String,
        aiResponse: String,
        isLiked: Boolean
    ) = withContext(Dispatchers.IO) {
        try {
            val map = hashMapOf(
                "userId" to user.uid,
                "userName" to user.name,
                "userQuestion" to userQuestion,
                "aiResponse" to aiResponse,
                "feedbackType" to if (isLiked) "like" else "dislike",
                "isLiked" to isLiked,
                "timestamp" to System.currentTimeMillis()
            )
            FirebasePaths.Firestore.aiChatFeedback().document().set(map).await()
            android.util.Log.d("DietologAI", "Saved feedback to root aiChatFeedback collection: isLiked=$isLiked")
        } catch (e: Exception) {
            android.util.Log.e("DietologAI", "Error saving feedback to Firestore", e)
        }
    }
}