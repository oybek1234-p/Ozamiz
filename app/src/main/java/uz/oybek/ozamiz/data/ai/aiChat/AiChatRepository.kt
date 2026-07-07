package uz.oybek.ozamiz.data.ai.aiChat

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
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

    private var chatHistory = mutableListOf<Content>()

    private val _newMessagesEvent = MutableSharedFlow<String>()
    val newMessagesEvent = _newMessagesEvent.asSharedFlow()

    val isGenerating = MutableStateFlow(false)
    private val _suggestedReplies = MutableStateFlow<List<String>>(emptyList())
    val suggestedReplies: StateFlow<List<String>> = _suggestedReplies.asStateFlow()

    private val aiChatDao by lazy {
        AiChatDatabase.getDatabase(appContext).aiChatDao()
    }

    suspend fun getCachedMessages(): List<AiChatMessageEntity>? {
        return aiChatDao.getMessagesForDateFlow(DateUtils.todayString()).firstOrNull()
    }

    private fun getModel(systemInstructionText: String) = Firebase.ai.generativeModel(
        modelName = AiPromptBuilder.MODEL_NAME,
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
        },
        systemInstruction = content { text(systemInstructionText) })

    suspend fun onFoodLogAdded(foodLog: FoodLog? = null) {
        isGenerating.value = true
        try {
            val date = Date()
            val summary = LogRepository.getDailySummarySync(date)

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val todayStr = DateUtils.todayString()

            if (foodLog != null) {
                checkLateFood(foodLog, hour, todayStr)
            }

            if (checkCalorieLimit(summary, user, todayStr)) {
                return
            }

            if (checkLowProteinHighCarbFat(summary, user, todayStr)) {
                return
            }

            if (checkMacroLimits(summary, user, todayStr)) {
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isGenerating.value = false
        }
    }

    private suspend fun checkLateFood(foodLog: FoodLog, hour: Int, todayStr: String) {
        if (foodLog.calories > 200 && (hour !in 4..20)) {
            val tag = AiChatMessageEntity.TAG_LATE_FOOD
            if (!aiChatDao.hasMessageWithTag(todayStr, tag)) {
                appContext.resources.getStringArray(R.array.late_food_warnings).randomOrNull()
                    ?.let {
                        saveCachedMessage(it, false, tag)
                    }
            }
        }
    }

    private suspend fun checkCalorieLimit(
        summary: DailySummary, user: User, todayStr: String
    ): Boolean {
        val netCalories = summary.consumedCalories
        if (netCalories >= user.targetCalories) {
            val tag = AiChatMessageEntity.TAG_CALORIE_LIMIT
            if (!aiChatDao.hasMessageWithTag(todayStr, tag)) {
                val parsedMessage = DietologAnswers.calorieLimitExceededMessages.random()
                saveCachedMessage(parsedMessage, false, tag)
                _newMessagesEvent.emit(parsedMessage)
                return true
            }
        }
        return false
    }

    private suspend fun checkLowProteinHighCarbFat(
        summary: DailySummary, user: User, todayStr: String
    ): Boolean {
        if (summary.consumedFat > user.targetFat && summary.consumedCarbs > user.targetCarbs && summary.consumedProtein < user.targetProtein * 0.5) {
            val tag = AiChatMessageEntity.TAG_PROTEIN_LOW
            if (!aiChatDao.hasMessageWithTag(todayStr, tag)) {
                val warning = DietologAnswers.lowProteinHighCarbFatMessages.random()
                saveCachedMessage(warning, false, tag)
                _newMessagesEvent.emit(warning)
                return true
            }
        }
        return false
    }

    private suspend fun checkMacroLimits(
        summary: DailySummary, user: User, todayStr: String
    ): Boolean {
        if (user.targetFat > 0 && summary.consumedFat > user.targetFat) {
            val tag = AiChatMessageEntity.TAG_FAT_EXCEEDED
            if (!aiChatDao.hasMessageWithTag(todayStr, tag)) {
                val parsedMessage = DietologAnswers.fatExceededMessages.random()
                saveCachedMessage(parsedMessage, false, tag)
                _newMessagesEvent.emit(parsedMessage)
                return true
            }
        }
        if (user.targetCarbs > 0 && summary.consumedCarbs > user.targetCarbs) {
            val tag = AiChatMessageEntity.TAG_CARBS_EXCEEDED
            if (!aiChatDao.hasMessageWithTag(todayStr, tag)) {
                val parsedMessage = DietologAnswers.carbExceededMessages.random()
                saveCachedMessage(parsedMessage, false, tag)
                _newMessagesEvent.emit(parsedMessage)
                return true
            }
        }
        if (user.targetProtein > 0 && summary.consumedProtein > user.targetProtein) {
            val tag = AiChatMessageEntity.TAG_PROTEIN_EXCEEDED
            if (!aiChatDao.hasMessageWithTag(todayStr, tag)) {
                val parsedMessage = DietologAnswers.proteinExceededMessages.random()
                saveCachedMessage(parsedMessage, false, tag)
                _newMessagesEvent.emit(parsedMessage)
                return true
            }
        }
        return false
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

    suspend fun clearHistory() {
        aiChatDao.clearAllMessages()
        chatHistory.clear()
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

    var suggestedShownCount = 0

    fun sendMessage(
        text: String, chatId: String?
    ): Flow<DataWrapper<String>> = flow {

        saveCachedMessage(text, true)

        if (chatId != null) {
            saveMessageToFirestore(chatId, text, true, System.currentTimeMillis())
        }
        var fullResponse = ""
        try {
            val systemPrompt = AiPromptBuilder.buildDietologInstruction(suggestedShownCount < 3)
            val userContent = content(role = "user") { text(text) }
            val modelInstance = getModel(systemPrompt)
            val chat = modelInstance.startChat(chatHistory)
            val stream = chat.sendMessageStream(userContent)

            stream.collect { chunk ->
                val chunkText = chunk.text ?: ""
                fullResponse += chunkText
                val parsed = extractStreamingJsonMessage(fullResponse)

                emit(DataWrapper.Success(parsed))
            }

            chatHistory.add(userContent)
            chatHistory.add(content(role = "model") { text(fullResponse) })

            val finalParsed =
                extractJsonMessage(fullResponse) ?: extractStreamingJsonMessage(fullResponse)

            saveCachedMessage(finalParsed, false)

            val parsedQuestions = extractJsonReplies(fullResponse)

            if (suggestedShownCount < 3) {
                _suggestedReplies.value = parsedQuestions
                suggestedShownCount++
            }
            if (chatId != null) {
                if (user.isPremium().not()) {
                    AppPreference.incrementDietologMessageCount()
                }
                saveMessageToFirestore(chatId, finalParsed, false, System.currentTimeMillis())
            }
        } catch (e: Exception) {
            emit(DataWrapper.Error(e.localizedMessage))
        }
    }

    private fun extractStreamingJsonMessage(text: String): String {
        val keyIndex = text.indexOf("\"message\"")
        if (keyIndex != -1) {
            val colonIndex = text.indexOf(":", keyIndex + 9)
            if (colonIndex != -1) {
                val quoteStart = text.indexOf("\"", colonIndex + 1)
                if (quoteStart != -1) {
                    val builder = StringBuilder()
                    var escaped = false
                    var i = quoteStart + 1
                    while (i < text.length) {
                        val c = text[i]
                        if (escaped) {
                            builder.append(c)
                            escaped = false
                        } else if (c == '\\') {
                            escaped = true
                        } else if (c == '"') {
                            break
                        } else {
                            builder.append(c)
                        }
                        i++
                    }
                    return builder.toString()
                }
            }
        }
        val trimmed = text.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return ""
        }
        return text
    }

    private fun extractJsonMessage(text: String): String? {
        return try {
            val jsonStart = text.indexOf("{")
            val jsonEnd = text.lastIndexOf("}")
            if (jsonStart != -1 && jsonEnd != -1) {
                val jsonString = text.substring(jsonStart, jsonEnd + 1)
                val obj = org.json.JSONObject(jsonString)
                obj.getString("message")
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractJsonReplies(text: String): List<String> {
        return try {
            val jsonStart = text.indexOf("{")
            val jsonEnd = text.lastIndexOf("}")
            if (jsonStart != -1 && jsonEnd != -1) {
                val jsonString = text.substring(jsonStart, jsonEnd + 1)
                val obj = org.json.JSONObject(jsonString)
                val arr = obj.getJSONArray("replies")
                val result = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    result.add(arr.getString(i))
                }
                result
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}