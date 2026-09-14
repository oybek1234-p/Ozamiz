package uz.oybek.ozamiz.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import androidx.annotation.Keep
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * Ozamiz Log Tahlilchisi:
 *
 * 1. TAOM TAHLILI (analyzeFood):
 *    - analyzeFood funksiyasiga oid barcha so'rovlar alohida.
 *    - User yuborgan matn, rasm URL (yoki fotosurat) va AIning qaytargan response'i.
 *
 * 2. AI CHAT:
 *    - Chat muloqotlari alohida (User savoli va AI javobi ketma-ket, takrorlarsiz).
 */
object VertexLogParser {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val urlPattern = Pattern.compile("(https?://[^\\s\"'<>]+|gs://[^\\s\"'<>]+)")

    @Keep
    enum class LogCategory {
        USER_MESSAGE,
        AI_RESPONSE,
        AI_STREAMING_CHUNK,
        SYSTEM_INSTRUCTION,
        HTTP_ANALYZE_FOOD,
        HTTP_OTHER_REQUEST,
        CALLABLE_VERIFICATION,
        APP_CHECK_WARNING,
        CLOUD_FUNCTION_ERROR,
        OTHER_SYSTEM_LOG
    }

    @Keep
    data class RawLogItem(
        val logIndex: Int = 0,
        val insertId: String = "",
        val timestamp: String = "",
        val trace: String = "",
        val logName: String = "",
        val eventName: String = "",
        val serviceName: String = "",
        val role: String = "",
        val text: String = "",
        val photoUrl: String = "",
        val httpMethod: String = "",
        val httpUrl: String = "",
        val httpStatus: Int = 0,
        val requestSize: Long = 0,
        val responseSize: Long = 0,
        val latency: String = "",
        val remoteIp: String = "",
        val hasImage: Boolean = false,
        val hasAudio: Boolean = false,
        val isChoiceChunk: Boolean = false,
        var category: LogCategory = LogCategory.OTHER_SYSTEM_LOG
    )

    @Keep
    data class UserSentData(
        val type: String,
        val text: String,
        val photoUrl: String,
        val sizeKb: Double
    )

    @Keep
    data class AiResponseData(
        val foodName: String,
        val healthScore: String,
        val calories: String,
        val macros: String,
        val ingredients: List<String>,
        val advice: String,
        val rawResponse: String
    )

    @Keep
    data class FoodAnalysisItem(
        val index: Int,
        val timestamp: String,
        val traceId: String,
        val status: Int,
        val userSent: UserSentData,
        val aiResponse: AiResponseData
    )

    @Keep
    data class ChatMessageEntry(
        val time: String,
        val role: String,
        val text: String
    )

    @Keep
    data class ChatSession(
        val sessionIndex: Int,
        val traceId: String,
        val startTime: String,
        val messages: List<ChatMessageEntry>
    )

    fun processLogFile(
        inputFilePath: String,
        outputDirectory: String? = null
    ) {
        val inputFile = File(inputFilePath)
        if (!inputFile.exists()) {
            println("❌ Xatolik: Fayl topilmadi -> $inputFilePath")
            return
        }

        val targetDirPath = outputDirectory ?: """C:\Users\samsu\OneDrive\Desktop\Ozamiz data"""
        val outDir = File(targetDirPath).apply { mkdirs() }

        println("==================================================")
        println("🚀 Ozamiz Log Tahlilchisi")
        println("📁 Kiruvchi fayl: ${inputFile.absolutePath}")
        println("📂 Natija papkasi: ${outDir.absolutePath}")
        println("==================================================")

        // 1. Loglarni o'qish
        val allLogs = streamParseLogs(inputFile)
        println("✅ Jami o'qilgan log yozuvlari: ${allLogs.size}\n")

        // 2. Tasniflash
        classifyAllLogs(allLogs)

        // 3. ANALYZE FOOD TAHLILLARINI AJRATISH
        val foodAnalyses = assembleFoodAnalyses(allLogs)
        println("🥗 --- 1. TAOM TAHLILI (analyzeFood funksiyasi) ---")
        println("  • Jami analyzeFood so'rovlari: ${foodAnalyses.size} ta")

        // 4. AI CHAT YOZISHMALARINI AJRATISH
        val rawChatSessions = assembleChatSessions(allLogs)
        val chatSessions = deduplicateChatSessions(rawChatSessions)
        println("\n💬 --- 2. AI CHAT YOZISHMALARI ---")
        println("  • Jami suhbatlar: ${chatSessions.size} ta")
        println("  • Jami savol-javoblar: ${chatSessions.sumOf { it.messages.size }} ta")

        // 5. FAYLLARGA YOZISH (Faqat ikkita asosiy yo'nalish)
        val foodTxtFile = File(outDir, "taom_tahlili_barcha_malumotlar.txt")
        val foodJsonFile = File(outDir, "taom_tahlili_barcha_malumotlar.json")
        val chatTxtFile = File(outDir, "ai_chat_yozishmalar_toliq.txt")
        val chatJsonFile = File(outDir, "ai_chat_yozishmalar_toliq.json")

        // Ortiqcha eski fayllarni tozalash
        File(outDir, "foydalanuvchi_yozgan_barcha_matnlar.txt").delete()
        File(outDir, "foydalanuvchi_yozgan_barcha_matnlar.json").delete()
        File(outDir, "backend_xatoliklar_hisoboti.txt").delete()
        File(outDir, "tokenlar_va_xarajatlar_tahlili.txt").delete()
        File(outDir, "barcha_loglar_aniq_toifasi_bilan.txt").delete()
        File(outDir, "umumiy_loglar_xulosasi.txt").delete()

        writeFoodAnalysisFile(foodAnalyses, foodTxtFile, foodJsonFile)
        writeChatFile(chatSessions, chatTxtFile, chatJsonFile)

        println("\n==================================================")
        println("🎉 TAYYOR BO'LDI! Faqat ikkita alohida fayl:")
        println("  1. 📄 taom_tahlili_barcha_malumotlar.txt (.json) -> analyzeFood (User yuborgan matn/rasm + AI response)")
        println("  2. 📄 ai_chat_yozishmalar_toliq.txt (.json)      -> AI Chat (User savoli + AI javobi)")
        println("==================================================")
    }

    private fun streamParseLogs(file: File): List<RawLogItem> {
        val items = mutableListOf<RawLogItem>()
        var index = 0

        BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8), 65536).use { reader ->
            reader.mark(1024)
            var firstChar = ' '
            while (true) {
                val c = reader.read()
                if (c == -1) break
                val ch = c.toChar()
                if (!ch.isWhitespace()) {
                    firstChar = ch
                    break
                }
            }
            reader.reset()

            if (firstChar == '[') {
                val jsonReader = JsonReader(reader)
                jsonReader.isLenient = true
                jsonReader.beginArray()
                while (jsonReader.hasNext() && jsonReader.peek() != JsonToken.END_ARRAY) {
                    try {
                        val element = JsonParser.parseReader(jsonReader)
                        if (element.isJsonObject) {
                            index++
                            parseSingleLogEntry(index, element.asJsonObject)?.let { items.add(it) }
                        }
                    } catch (_: Exception) {}
                }
                jsonReader.endArray()
            } else {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line!!.trim()
                    if (trimmed.isEmpty() || trimmed == "[" || trimmed == "]" || trimmed == ",") continue
                    val cleanJson = if (trimmed.endsWith(",")) trimmed.dropLast(1) else trimmed
                    try {
                        val element = JsonParser.parseString(cleanJson)
                        if (element.isJsonObject) {
                            index++
                            parseSingleLogEntry(index, element.asJsonObject)?.let { items.add(it) }
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        return items
    }

    private fun JsonObject.optObject(key: String): JsonObject? =
        get(key)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.optArray(key: String): JsonArray? =
        get(key)?.takeIf { it.isJsonArray }?.asJsonArray

    private fun JsonObject.optString(key: String): String =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString ?: ""

    private fun deepFindKey(elem: JsonElement, targetKey: String): String? {
        if (elem.isJsonObject) {
            val obj = elem.asJsonObject
            for ((k, v) in obj.entrySet()) {
                if (k.equals(targetKey, ignoreCase = true)) {
                    if (v.isJsonPrimitive && v.asString.isNotBlank()) return v.asString
                }
                val found = deepFindKey(v, targetKey)
                if (found != null) return found
            }
        } else if (elem.isJsonArray) {
            for (item in elem.asJsonArray) {
                val found = deepFindKey(item, targetKey)
                if (found != null) return found
            }
        }
        return null
    }

    private fun isSystemOrErrorText(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return true
        val lower = trimmed.lowercase()
        return lower.startsWith("at ") ||
                lower.startsWith("post http") ||
                lower.startsWith("get http") ||
                lower.startsWith("callable") ||
                lower.startsWith("function execution") ||
                lower.startsWith("{") ||
                lower.contains("at json.parse") ||
                lower.contains("node:internal") ||
                lower.contains("syntaxerror") ||
                lower.contains("typeerror") ||
                lower.contains("clienterror") ||
                lower.contains("detailed error") ||
                lower.contains("publisher model") ||
                lower.contains("resourcename")
    }

    private fun parseSingleLogEntry(index: Int, obj: JsonObject): RawLogItem? {
        return try {
            val insertId = obj.optString("insertId")
            val timestamp = obj.optString("timestamp")
            val trace = obj.optString("trace").ifEmpty {
                obj.optObject("labels")?.optString("trace") ?: ""
            }
            val logName = obj.optString("logName")

            val labels = obj.optObject("labels")
            val eventName = labels?.optString("event.name")?.ifEmpty { null }
                ?: labels?.optString("firebasevertexai.googleapis.com/event.name")
                ?: labels?.optString("firebase-log-type") ?: ""

            val resource = obj.optObject("resource")
            val serviceName = resource?.optObject("labels")?.optString("service_name")
                ?: labels?.optString("goog-drz-cloudfunctions-id") ?: ""

            val httpRequest = obj.optObject("httpRequest")
            val httpMethod = httpRequest?.optString("requestMethod") ?: ""
            val httpUrl = httpRequest?.optString("requestUrl") ?: ""
            val httpStatus = httpRequest?.get("status")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
            val requestSize = httpRequest?.optString("requestSize")?.toLongOrNull() ?: 0L
            val responseSize = httpRequest?.optString("responseSize")?.toLongOrNull() ?: 0L
            val latency = httpRequest?.optString("latency") ?: ""
            val remoteIp = httpRequest?.optString("remoteIp") ?: ""

            val jsonPayload = obj.optObject("jsonPayload")
            val textPayload = obj.optString("textPayload")

            var role = jsonPayload?.optString("role") ?: ""

            val textBuilder = StringBuilder()
            var photoUrl = listOf("photoUrl", "photo_url", "imageUrl", "image_url", "fileUri", "imageUri")
                .firstNotNullOfOrNull { deepFindKey(obj, it) } ?: ""

            var hasImage = photoUrl.isNotEmpty()
            var hasAudio = false
            var isChoiceChunk = false

            // Chuqur user text qidirish
            val deepUserText = listOf("description", "meal")
                .firstNotNullOfOrNull { deepFindKey(obj, it) } ?: ""

            if (deepUserText.isNotBlank() && !isSystemOrErrorText(deepUserText)) {
                textBuilder.append(deepUserText)
                if (role.isEmpty()) role = "user"
            }

            // HTTP Request
            if (httpRequest != null) {
                if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                textBuilder.append("$httpMethod $httpUrl -> Status $httpStatus")
                if (requestSize > 10000) hasImage = true
            }

            // TextPayload
            if (textPayload.isNotBlank()) {
                if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                textBuilder.append(textPayload)
                val matcher = urlPattern.matcher(textPayload)
                if (matcher.find() && photoUrl.isEmpty()) {
                    photoUrl = matcher.group(1)
                    hasImage = true
                }
            }

            // JsonPayload
            if (jsonPayload != null) {
                val directText = jsonPayload.optString("text")
                if (directText.isNotEmpty() && !textBuilder.contains(directText)) {
                    if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                    textBuilder.append(directText)
                }

                jsonPayload.optArray("content")?.forEach { elem ->
                    extractFromElement(elem, textBuilder, { hasImage = true }, { hasAudio = true }, { url ->
                        if (photoUrl.isEmpty()) photoUrl = url
                    })
                }

                jsonPayload.optArray("contents")?.forEach { elem ->
                    if (elem.isJsonObject) {
                        val cObj = elem.asJsonObject
                        if (role.isEmpty()) role = cObj.optString("role")
                        cObj.optArray("parts")?.forEach { p ->
                            extractFromElement(p, textBuilder, { hasImage = true }, { hasAudio = true }, { url ->
                                if (photoUrl.isEmpty()) photoUrl = url
                            })
                        }
                    }
                }

                val msgElem = jsonPayload.get("message")
                if (msgElem != null) {
                    if (msgElem.isJsonObject) {
                        isChoiceChunk = true
                        val mObj = msgElem.asJsonObject
                        if (role.isEmpty()) role = mObj.optString("role").ifEmpty { "model" }
                        mObj.optArray("content")?.forEach { elem ->
                            extractFromElement(elem, textBuilder, { hasImage = true }, { hasAudio = true }, { url ->
                                if (photoUrl.isEmpty()) photoUrl = url
                            })
                        }
                        mObj.optArray("parts")?.forEach { elem ->
                            extractFromElement(elem, textBuilder, { hasImage = true }, { hasAudio = true }, { url ->
                                if (photoUrl.isEmpty()) photoUrl = url
                            })
                        }
                    } else if (msgElem.isJsonPrimitive) {
                        val msgStr = msgElem.asString
                        if (!textBuilder.contains(msgStr)) {
                            if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                            textBuilder.append(msgStr)
                        }
                    }
                }

                jsonPayload.optArray("candidates")?.forEach { cand ->
                    if (cand.isJsonObject) {
                        isChoiceChunk = true
                        if (role.isEmpty()) role = "model"
                        cand.asJsonObject.optObject("content")?.optArray("parts")?.forEach { p ->
                            extractFromElement(p, textBuilder, { hasImage = true }, { hasAudio = true }, { url ->
                                if (photoUrl.isEmpty()) photoUrl = url
                            })
                        }
                    }
                }
            }

            if (role.isEmpty()) {
                role = when {
                    eventName.contains("user.message", ignoreCase = true) -> "user"
                    eventName.contains("system.message", ignoreCase = true) -> "system"
                    eventName.contains("assistant.message", ignoreCase = true) -> "model"
                    eventName.contains("choice", ignoreCase = true) -> "model"
                    else -> ""
                }
            }

            val text = textBuilder.toString().trim()
            if (text.isEmpty() && !hasImage && !hasAudio && httpRequest == null) {
                return null
            }

            RawLogItem(
                logIndex = index,
                insertId = insertId,
                timestamp = timestamp,
                trace = trace,
                logName = logName,
                eventName = eventName,
                serviceName = serviceName,
                role = role,
                text = text,
                photoUrl = photoUrl,
                httpMethod = httpMethod,
                httpUrl = httpUrl,
                httpStatus = httpStatus,
                requestSize = requestSize,
                responseSize = responseSize,
                latency = latency,
                remoteIp = remoteIp,
                hasImage = hasImage,
                hasAudio = hasAudio,
                isChoiceChunk = isChoiceChunk || eventName.contains("choice", ignoreCase = true)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractFromElement(
        elem: JsonElement,
        sb: StringBuilder,
        onImage: () -> Unit,
        onAudio: () -> Unit,
        onUrl: (String) -> Unit
    ) {
        if (elem.isJsonObject) {
            val obj = elem.asJsonObject
            val t = obj.optString("text")
            if (t.isNotEmpty()) {
                if (sb.isNotEmpty()) sb.append(" ")
                sb.append(t)
            }

            val fileData = obj.optObject("fileData") ?: obj.optObject("file_data")
            if (fileData != null) {
                val uri = fileData.optString("fileUri").ifEmpty { fileData.optString("file_uri") }
                if (uri.isNotBlank()) {
                    onImage()
                    onUrl(uri)
                }
            }

            val inlineData = obj.optObject("inlineData") ?: obj.optObject("inline_data")
            if (inlineData != null) {
                val mime = inlineData.optString("mimeType")
                if (mime.startsWith("image/")) onImage()
                if (mime.startsWith("audio/")) onAudio()
            }

            val photoCandidate = obj.optString("photoUrl").ifEmpty { obj.optString("imageUrl") }
            if (photoCandidate.isNotBlank()) {
                onImage()
                onUrl(photoCandidate)
            }
        } else if (elem.isJsonPrimitive) {
            if (sb.isNotEmpty()) sb.append(" ")
            sb.append(elem.asString)
        }
    }

    private fun classifyAllLogs(logs: List<RawLogItem>) {
        for (log in logs) {
            val text = log.text
            val url = log.httpUrl.lowercase()
            val logName = log.logName.lowercase()

            log.category = when {
                url.contains("analyzefood") || log.serviceName.contains("analyzefood", ignoreCase = true) -> LogCategory.HTTP_ANALYZE_FOOD
                log.httpMethod.isNotEmpty() || logName.contains("requests") -> LogCategory.HTTP_OTHER_REQUEST
                text.contains("DETAILED Error", ignoreCase = true) ||
                        text.contains("ClientError: [VertexAI.ClientError]", ignoreCase = true) ||
                        text.contains("Publisher model", ignoreCase = true) ||
                        text.contains("SyntaxError", ignoreCase = true) ||
                        (logName.contains("stderr") && text.contains("Error", ignoreCase = true)) -> LogCategory.CLOUD_FUNCTION_ERROR
                text.contains("AppCheck", ignoreCase = true) || text.contains("App Check", ignoreCase = true) -> LogCategory.APP_CHECK_WARNING
                text.contains("Callable request verification", ignoreCase = true) ||
                        log.eventName == "callable-request-verification" -> LogCategory.CALLABLE_VERIFICATION
                log.role == "user" || log.eventName.contains("user.message", ignoreCase = true) -> LogCategory.USER_MESSAGE
                log.isChoiceChunk || log.eventName.contains("choice", ignoreCase = true) -> LogCategory.AI_STREAMING_CHUNK
                log.role == "model" || log.eventName.contains("assistant.message", ignoreCase = true) -> LogCategory.AI_RESPONSE
                log.role == "system" || log.eventName.contains("system.message", ignoreCase = true) -> LogCategory.SYSTEM_INSTRUCTION
                else -> LogCategory.OTHER_SYSTEM_LOG
            }
        }
    }

    /**
     * 1. TAOM TAHLILI:
     * analyzeFood funksiyasi ishlatilgan har bir amal bo'yicha:
     * - User jo'natgan matn, rasm URL (yoki fotosurat hajmi)
     * - AI qaytargan response
     */
    private fun assembleFoodAnalyses(logs: List<RawLogItem>): List<FoodAnalysisItem> {
        val result = mutableListOf<FoodAnalysisItem>()
        val groupedByTrace = logs.groupBy { it.trace.ifEmpty { it.insertId } }
        var count = 0

        for ((trace, traceLogs) in groupedByTrace) {
            val sorted = traceLogs.sortedBy { it.logIndex }
            val isAnalyzeFood = sorted.any {
                it.category == LogCategory.HTTP_ANALYZE_FOOD ||
                        it.serviceName.contains("analyzefood", ignoreCase = true) ||
                        it.httpUrl.contains("analyzefood", ignoreCase = true)
            }

            if (!isAnalyzeFood) continue

            val httpReq = sorted.firstOrNull { it.category == LogCategory.HTTP_ANALYZE_FOOD }
            val userLogs = sorted.filter { it.category == LogCategory.USER_MESSAGE }
            val modelLogs = sorted.filter { it.category == LogCategory.AI_RESPONSE }
            val errorLogs = sorted.filter { it.category == LogCategory.CLOUD_FUNCTION_ERROR }
            val streamingChunks = sorted.filter { it.category == LogCategory.AI_STREAMING_CHUNK }.joinToString("") { it.text }

            val status = httpReq?.httpStatus ?: if (errorLogs.isNotEmpty()) 500 else 200

            var photoUrl = sorted.firstOrNull { it.photoUrl.isNotBlank() }?.photoUrl ?: ""
            val reqSize = httpReq?.requestSize ?: 0L
            val sizeKb = (reqSize / 1024.0 * 10).toInt() / 10.0
            val hasImage = photoUrl.isNotEmpty() || sorted.any { it.hasImage } || reqSize > 10000

            // 1. User jo'natgan matn
            var userText = ""
            for (u in userLogs) {
                val clean = u.text.trim()
                if (clean.isNotBlank() && !isSystemOrErrorText(clean)) {
                    userText = clean
                    break
                }
            }

            if (userText.isEmpty()) {
                for (item in sorted) {
                    val clean = item.text.trim()
                    if (clean.isNotBlank() && !isSystemOrErrorText(clean) && !clean.startsWith("[")) {
                        userText = clean
                        break
                    }
                }
            }

            val requestType = when {
                photoUrl.isNotBlank() -> "📸 Rasm (URL mavjud)"
                hasImage -> "📸 Rasm (Kamera/Galereya)"
                else -> "📝 Matnli tavsif"
            }

            val finalUserText = when {
                userText.isNotBlank() -> userText
                photoUrl.isNotBlank() -> "[Foydalanuvchi taom fotosuratini yuklagan: $photoUrl]"
                hasImage -> "[Foydalanuvchi taom fotosuratini yuklagan (${sizeKb} KB)]"
                else -> "[Foydalanuvchi taom tavsifi so'rovi]"
            }

            // 2. AI response
            val fullAiText = when {
                streamingChunks.isNotBlank() -> streamingChunks
                modelLogs.isNotEmpty() -> modelLogs.last().text
                else -> ""
            }

            val parsedFood = parseFoodJson(fullAiText)

            val effectiveAdvice = when {
                parsedFood.advice.isNotBlank() -> parsedFood.advice
                status == 200 -> "✅ Tahlil muvaffaqiyatli bajarildi (Status 200 OK). Natija telefonga yetkazildi."
                else -> {
                    val errText = errorLogs.firstOrNull()?.text ?: "Server Error (Status $status)"
                    when {
                        errText.contains("gemini-3.1-flash-lite", ignoreCase = true) -> "404 Xatolik: 'gemini-3.1-flash-lite' modeli mavjud emas"
                        errText.contains("SyntaxError", ignoreCase = true) -> "500 Xatolik: JSON parse xatosi"
                        else -> "Server xatosi (Status $status)"
                    }
                }
            }

            count++
            result.add(
                FoodAnalysisItem(
                    index = count,
                    timestamp = httpReq?.timestamp ?: sorted.firstOrNull()?.timestamp ?: "",
                    traceId = trace.substringAfterLast("/"),
                    status = status,
                    userSent = UserSentData(
                        type = requestType,
                        text = finalUserText,
                        photoUrl = photoUrl,
                        sizeKb = sizeKb
                    ),
                    aiResponse = AiResponseData(
                        foodName = parsedFood.name,
                        healthScore = parsedFood.healthScore,
                        calories = parsedFood.calories,
                        macros = parsedFood.macros,
                        ingredients = parsedFood.ingredients,
                        advice = effectiveAdvice,
                        rawResponse = fullAiText
                    )
                )
            )
        }

        return result
    }

    @Keep
    private data class ParsedFoodData(
        val name: String = "",
        val healthScore: String = "",
        val ingredients: List<String> = emptyList(),
        val calories: String = "",
        val macros: String = "",
        val advice: String = ""
    )

    private fun parseFoodJson(rawText: String): ParsedFoodData {
        if (rawText.isBlank()) return ParsedFoodData()
        val trimmed = rawText.trim()
        val jsonStart = trimmed.indexOf("{")
        val jsonEnd = trimmed.lastIndexOf("}")

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            val jsonCandidate = trimmed.substring(jsonStart, jsonEnd + 1)
            try {
                val element = JsonParser.parseString(jsonCandidate)
                if (element.isJsonObject) {
                    val obj = element.asJsonObject
                    val name = obj.optString("name")
                    val healthScore = obj.get("healthScore")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                    val advice = obj.optString("advice")

                    val ingList = mutableListOf<String>()
                    var totalCal = 0
                    var totalP = 0.0
                    var totalF = 0.0
                    var totalC = 0.0

                    obj.optArray("foodItems")?.forEach { itemElem ->
                        if (itemElem.isJsonObject) {
                            val item = itemElem.asJsonObject
                            val itemName = item.optString("name")
                            val amount = item.get("amount")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                            val unit = item.optString("unit").ifEmpty { "g" }
                            val cal = item.get("calories")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
                            val p = item.get("protein")?.takeIf { it.isJsonPrimitive }?.asDouble ?: 0.0
                            val f = item.get("fat")?.takeIf { it.isJsonPrimitive }?.asDouble ?: 0.0
                            val c = item.get("carbs")?.takeIf { it.isJsonPrimitive }?.asDouble ?: 0.0

                            totalCal += cal
                            totalP += p
                            totalF += f
                            totalC += c
                            ingList.add("$itemName: $amount$unit | $cal kkal (Oqsil: ${p}g, Yog': ${f}g, Uglevod: ${c}g)")
                        }
                    }

                    val calStr = if (totalCal > 0) "$totalCal kkal" else ""
                    val macroStr = if (totalCal > 0) "Oqsil: %.1fg, Yog': %.1fg, Uglevod: %.1fg".format(totalP, totalF, totalC) else ""

                    return ParsedFoodData(
                        name = name,
                        healthScore = healthScore,
                        ingredients = ingList,
                        calories = calStr,
                        macros = macroStr,
                        advice = advice
                    )
                }
            } catch (_: Exception) {}
        }
        return ParsedFoodData(advice = trimmed)
    }

    /**
     * 2. AI CHAT:
     * User savoli va AI javobi (ketma-ket suhbat tartibida)
     */
    private fun assembleChatSessions(logs: List<RawLogItem>): List<ChatSession> {
        val sessions = mutableListOf<ChatSession>()
        val groupedByTrace = logs.groupBy { it.trace.ifEmpty { it.insertId } }
        var sessionIndex = 0

        for ((trace, traceLogs) in groupedByTrace) {
            val sorted = traceLogs.sortedBy { it.logIndex }
            val isAnalyzeFood = sorted.any {
                it.category == LogCategory.HTTP_ANALYZE_FOOD ||
                        it.serviceName.contains("analyzefood", ignoreCase = true) ||
                        it.httpUrl.contains("analyzefood", ignoreCase = true)
            }

            // analyzeFood amallarini chatga qo'shmaymiz!
            if (isAnalyzeFood) continue

            val userItems = mutableListOf<RawLogItem>()
            val directModelItems = mutableListOf<RawLogItem>()
            val streamingChunks = StringBuilder()

            for (item in sorted) {
                when (item.category) {
                    LogCategory.USER_MESSAGE -> {
                        if (item.text.isNotBlank()) userItems.add(item)
                    }
                    LogCategory.AI_RESPONSE -> {
                        if (item.text.isNotBlank()) directModelItems.add(item)
                    }
                    LogCategory.AI_STREAMING_CHUNK -> {
                        streamingChunks.append(item.text)
                    }
                    else -> {}
                }
            }

            val chatEntries = mutableListOf<ChatMessageEntry>()
            val minPairs = minOf(userItems.size, directModelItems.size)

            for (i in 0 until minPairs) {
                val u = userItems[i]
                val m = directModelItems[i]
                val cleanUser = u.text.trim()
                val cleanAi = cleanAiTextMessage(m.text)

                if (!isSystemOrErrorText(cleanUser)) {
                    chatEntries.add(ChatMessageEntry(u.timestamp, "USER", cleanUser))
                    chatEntries.add(ChatMessageEntry(m.timestamp, "AI", cleanAi))
                }
            }

            val streamedAi = streamingChunks.toString().trim()
            if (streamedAi.isNotEmpty() && userItems.size > directModelItems.size) {
                val lastUser = userItems.last()
                val cleanUser = lastUser.text.trim()
                val cleanAi = cleanAiTextMessage(streamedAi)

                if (!isSystemOrErrorText(cleanUser)) {
                    chatEntries.add(ChatMessageEntry(lastUser.timestamp, "USER", cleanUser))
                    chatEntries.add(ChatMessageEntry(sorted.lastOrNull()?.timestamp ?: "", "AI", cleanAi))
                }
            }

            if (chatEntries.isNotEmpty()) {
                sessionIndex++
                sessions.add(
                    ChatSession(
                        sessionIndex = sessionIndex,
                        traceId = trace.substringAfterLast("/"),
                        startTime = sorted.firstOrNull()?.timestamp ?: "",
                        messages = chatEntries
                    )
                )
            }
        }

        return sessions.sortedBy { it.startTime }
    }

    private fun deduplicateChatSessions(sessions: List<ChatSession>): List<ChatSession> {
        val sortedByLengthDesc = sessions.sortedByDescending { it.messages.size }
        val uniqueSessions = mutableListOf<ChatSession>()

        for (candidate in sortedByLengthDesc) {
            val candTexts = candidate.messages.map { "${it.role}:${it.text.trim()}" }
            val isSubSession = uniqueSessions.any { existing ->
                val existTexts = existing.messages.map { "${it.role}:${it.text.trim()}" }
                candTexts.size < existTexts.size && existTexts.containsAll(candTexts)
            }
            if (!isSubSession) {
                uniqueSessions.add(candidate)
            }
        }

        return uniqueSessions.sortedBy { it.startTime }.mapIndexed { idx, s ->
            s.copy(sessionIndex = idx + 1)
        }
    }

    private fun cleanAiTextMessage(raw: String): String {
        val trimmed = raw.trim()
        val jsonStart = trimmed.indexOf("{")
        val jsonEnd = trimmed.lastIndexOf("}")

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            val jsonCandidate = trimmed.substring(jsonStart, jsonEnd + 1)
            try {
                val element = JsonParser.parseString(jsonCandidate)
                if (element.isJsonObject) {
                    val obj = element.asJsonObject
                    if (obj.has("message") && obj.get("message").isJsonPrimitive) {
                        return obj.get("message").asString
                    }
                }
            } catch (_: Exception) {}
        }
        return trimmed
    }

    /**
     * TAOM TAHLILI FAYLIGA YOZISH (User sent + AI response)
     */
    private fun writeFoodAnalysisFile(items: List<FoodAnalysisItem>, textFile: File, jsonFile: File) {
        PrintWriter(textFile.bufferedWriter(StandardCharsets.UTF_8)).use { writer ->
            writer.println("================================================================================")
            writer.println("   OZAMIZ — TAOM TAHLILI (analyzeFood) SO'ROVLARI VA AI JAVOBLARI (JAMI ${items.size} TA)")
            writer.println("================================================================================\n")

            for (item in items) {
                writer.println("--------------------------------------------------------------------------------")
                val statusStr = if (item.status == 200) "✅ 200 OK" else "❌ Status ${item.status}"
                writer.println("TAOM TAHLILI #${item.index} | Vaqt: ${item.timestamp} | Status: $statusStr")
                writer.println("Trace ID: ${item.traceId}")

                writer.println("\n👤 [FOYDALANUVCHI JO'NATGAN — REQUEST]:")
                writer.println("   • Turi: ${item.userSent.type}")
                writer.println("   • Matn: ${item.userSent.text}")
                if (item.userSent.photoUrl.isNotBlank()) {
                    writer.println("   • 📸 Rasm URL: ${item.userSent.photoUrl}")
                }

                writer.println("\n🤖 [AI QAYTORGAN NATIJA — RESPONSE]:")
                if (item.aiResponse.foodName.isNotEmpty()) {
                    val scoreInfo = if (item.aiResponse.healthScore.isNotEmpty()) " (Sog'lomlik darajasi: ${item.aiResponse.healthScore}/10)" else ""
                    writer.println("   🍽 Aniqlangan taom: ${item.aiResponse.foodName}$scoreInfo")
                    if (item.aiResponse.calories.isNotEmpty()) writer.println("   ⚡ Kaloriya: ${item.aiResponse.calories} | ${item.aiResponse.macros}")
                    if (item.aiResponse.ingredients.isNotEmpty()) {
                        writer.println("   📋 Tarkibi:")
                        item.aiResponse.ingredients.forEach { ing ->
                            writer.println("      - $ing")
                        }
                    }
                    if (item.aiResponse.advice.isNotEmpty()) {
                        writer.println("   💡 Maslahat: ${item.aiResponse.advice}")
                    }
                } else if (item.aiResponse.advice.isNotBlank()) {
                    writer.println("   💡 Natija: ${item.aiResponse.advice}")
                } else if (item.aiResponse.rawResponse.isNotBlank()) {
                    writer.println("   Javob: ${item.aiResponse.rawResponse}")
                }

                writer.println("--------------------------------------------------------------------------------\n")
            }
        }

        val jsonList = items.map {
            mapOf(
                "index" to it.index,
                "timestamp" to it.timestamp,
                "status" to it.status,
                "traceId" to it.traceId,
                "userSent" to mapOf(
                    "type" to it.userSent.type,
                    "text" to it.userSent.text,
                    "photoUrl" to it.userSent.photoUrl,
                    "sizeKb" to it.userSent.sizeKb
                ),
                "aiResponse" to mapOf(
                    "foodName" to it.aiResponse.foodName,
                    "healthScore" to it.aiResponse.healthScore,
                    "calories" to it.aiResponse.calories,
                    "macros" to it.aiResponse.macros,
                    "ingredients" to it.aiResponse.ingredients,
                    "advice" to it.aiResponse.advice
                )
            )
        }
        jsonFile.writeText(gson.toJson(jsonList), StandardCharsets.UTF_8)
    }

    /**
     * AI CHAT FAYLIGA YOZISH (User savoli + AI javobi)
     */
    private fun writeChatFile(sessions: List<ChatSession>, textFile: File, jsonFile: File) {
        PrintWriter(textFile.bufferedWriter(StandardCharsets.UTF_8)).use { writer ->
            writer.println("================================================================================")
            writer.println("   OZAMIZ — AI CHAT YOZISHMALARI (${sessions.size} TA SUHBAT)")
            writer.println("================================================================================\n")

            for (session in sessions) {
                writer.println("================================================================================")
                writer.println("💬 SUHBAT #${session.sessionIndex} | Boshlangan vaqti: ${session.startTime}")
                writer.println("Trace ID: ${session.traceId}")
                writer.println("================================================================================\n")

                for (msg in session.messages) {
                    val timeStr = if (msg.time.isNotEmpty()) "[${msg.time}] " else ""
                    if (msg.role == "USER") {
                        writer.println("👤 USER $timeStr:")
                        writer.println("   ${msg.text}\n")
                    } else {
                        writer.println("🤖 AI $timeStr:")
                        writer.println("   ${msg.text}\n")
                        writer.println("   · · · · · · · · · · · · · · · · · · · · · · · · · · · · · · · · · · · · · · ·\n")
                    }
                }
                writer.println("\n")
            }
        }

        val jsonList = sessions.map {
            mapOf(
                "sessionIndex" to it.sessionIndex,
                "startTime" to it.startTime,
                "traceId" to it.traceId,
                "messages" to it.messages.map { m ->
                    mapOf(
                        "role" to m.role,
                        "time" to m.time,
                        "text" to m.text
                    )
                }
            )
        }
        jsonFile.writeText(gson.toJson(jsonList), StandardCharsets.UTF_8)
    }
}
