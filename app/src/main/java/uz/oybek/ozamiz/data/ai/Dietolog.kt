package uz.oybek.ozamiz.data.ai

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.ai.model.AiMealRecommendation
import uz.oybek.ozamiz.data.config.LocalPreferencesRepository
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzerConfig
import uz.oybek.ozamiz.data.logs.model.DailySummary
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.utils.DateUtils
import uz.oybek.ozamiz.data.utils.dateApiFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AiEventType(val description: String) {
    APP_OPENED("Foydalanuvchi hozirgina ilovaga kirdi."),
    FOOD_ADDED("Foydalanuvchi hozirgina qandaydir ovqat kiritdi."),
    EXERCISE_ADDED("Foydalanuvchi hozirgina mashq qilganini kiritdi."),
    CALORIE_LIMIT_REACHED("Foydalanuvchi kunlik kaloriya limitiga yetdi yoki oshib ketdi! Endi ertagacha nima yeyishi mumkinligini, sog'lom o'zbek taomlarini (sabzavotli sho'rva, qatiq, bodring, pomidor, yashil salat, qaynatilgan go'sht) tavsiya qiling. Agar yana ortiqcha yesa nima bo'lishini qisqacha tushuntiring."),
    MACRO_EXCEEDED("Foydalanuvchining makro elementlardan biri (oqsil, yog' yoki uglevod) me'yoridan 20 dan ortiq oshib ketdi! Qaysi makro oshganini va uning tanaga ta'sirini qisqacha tushuntiring, sog'lom alternativalar tavsiya qiling.")
}

object Dietolog {
    private const val PREFS_NAME = "dietolog_prefs"
    private const val KEY_ADVICE_TEXT = "advice_text"
    private const val KEY_LAST_TRIGGER_TIME = "last_trigger_time"
    private const val KEY_TRIGGER_COUNT_TODAY = "trigger_count_today"
    private const val KEY_LAST_DATE = "last_date"

    private val _latestMessage = MutableStateFlow<String?>(null)
    val latestMessage: StateFlow<String?> = _latestMessage.asStateFlow()

    init {
        val prefs = appContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
        _latestMessage.value = prefs.getString(KEY_ADVICE_TEXT, null)
    }

    private val generativeModel by lazy {
        Firebase.ai.generativeModel(
            FoodAnalyzerConfig.MODEL_NAME,
            systemInstruction = content {
                text(
                    """
                    Siz inson-dietologsiz.
                    Juda sodda, qisqa va aniq maslahat beradigan insondek gapirasiz. Har doim "Siz" deb murojaat qiling.
                    Qoidalar: 
                    1. Asosiy e'tiborni mijozning AYNI DAMDA qilgan harakatiga qarating. 
                    2. "Obbo", "Nega unday qildingiz", "Qayerdan chiqdi" kabi ortiqcha hayrat yoki kinoya, kesatiqlarni UMUMAN ISHLATMANG! Ehtiyotkor, sipo va juda oddiy muomalada bo'ling.
                    3. "Assalomu aleykum", "Yaxshi natija", "Motivatsiya" degan shablon so'zlarni aslo ishlatmang.
                    4. Gap qisqa (1 yoki 2 ta qisqacha gap) bo'lsin. Falsafiy qistirmalar aralashtirmang.
                    
                    Faqat faktlarga asoslanib, juda oddiy tartibda qisqa eslatma bering.
                """.trimIndent()
                )
            }
        )
    }

    suspend fun sendUserEvent(event: AiEventType, itemName: String? = null) {
        val user = UserRepository.user
        if (!user.valid || user.uid.isBlank()) return

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")
        val savedDate = prefs.getString(KEY_LAST_DATE, "")

        if (savedDate != todayStr) {
            prefs.edit()
                .putString(KEY_LAST_DATE, todayStr)
                .putInt(KEY_TRIGGER_COUNT_TODAY, 0)
                .putLong(KEY_LAST_TRIGGER_TIME, 0L)
                .apply()
        }

        val triggerCount = prefs.getInt(KEY_TRIGGER_COUNT_TODAY, 0)
        val lastTriggerTime = prefs.getLong(KEY_LAST_TRIGGER_TIME, 0L)
        val currentTime = System.currentTimeMillis()
        val cooldownPassed = (currentTime - lastTriggerTime) > (1 * 60 * 60 * 1000L)

        if (event == AiEventType.APP_OPENED && !cooldownPassed && triggerCount > 0) return

        try {
            val summaryRef = FirebasePaths.dailySummaryReference(user.uid, todayStr)
            val summarySnapshot = summaryRef.get().await()
            val summary = summarySnapshot.getValue(DailySummary::class.java) ?: DailySummary()

            val foodsSnapshot = FirebasePaths.foodLogsReference(user.uid, todayStr).get().await()
            val todayFoods = foodsSnapshot.children.mapNotNull {
                it.child("description").getValue(String::class.java)
            }.joinToString(", ")

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeString = timeFormat.format(Date())
            val extraItemInfo = if (itemName != null) "Hozirgi voqea (narsa): $itemName" else ""

            val prompt = """
                DIETOLOG UCHUN KONTEKST:
                - ASOSIY VOQEA: ${event.description}. $extraItemInfo
                - Mijozning yeganlari (bugun): ${if (todayFoods.isBlank()) "Hali hech nima." else todayFoods}
                - Darcha (Soat): $timeString
                - Maqsad: ${if (user.goal == "gain_weight") "Semirish" else "Ozish"}
                - Tananing umumiy ahvoli: Quvvat(${summary.consumedCalories} kkal), Suv(${summary.consumedWaterMl} ml), Mashq(${summary.burnedCalories} kkal).
                
                VAZIFA: Kontekstdagi ASOSIY VOQEAga yoki joriy ahvolga qarab darhol 1-2 ta QISQA, ODDDIY gap yozing. Hech qanday "Obbo", "Nega unday qildingiz" deb xiralik va sarkazm qilmang. Agar hozir suv kiritgani kelsa yoki umuman vaziyat silliq bo'lsa javobni oddiylashtiring. Zararini oddiygina qilib tushuntiring, tamom. Ortiqcha gap va hayajon yasash mutlaqo taqiqlanadi!
                DIQQAT: Hech qanday maxsus formatlar (masalan: * yoki # belgilar) ishlata ko'rmang, faqat oddiy matnda yozing! Markdown taqiqlanadi!
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            val text = response.text ?: return

            prefs.edit()
                .putString(KEY_ADVICE_TEXT, text)
                .putInt(KEY_TRIGGER_COUNT_TODAY, triggerCount + 1)
                .putLong(KEY_LAST_TRIGGER_TIME, currentTime)
                .apply()

            appendMessageToChatCache(text)
            _latestMessage.value = text

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendUserEventAsync(event: AiEventType, itemName: String? = null) {
        kotlinx.coroutines.GlobalScope.launch {
            sendUserEvent(event, itemName)
        }
    }

    suspend fun getMealRecommendations(
        mealType: String,
        remainingCalories: Int,
        goal: String,
        eatenFoods: List<String>
    ): List<AiMealRecommendation>? {
        return try {
            val eatenStr =
                if (eatenFoods.isEmpty()) "Hali hech narsa yeyilmagan" else eatenFoods.joinToString(
                    ", "
                )
            val prompt = """
                Mijozning maqsadi: ${if (goal == "gain_weight") "Semirish" else "Ozish"}.
                Mijozning bugungi normasidan qolgan kaloriya: $remainingCalories kkal.
                Bugun yegan ovqatlari: $eatenStr.
                Joriy ovqatlanish vaqti (mahal): $mealType.
                
                Vazifa: Mijozning joriy ovqatlanish vaqti ($mealType) uchun shaxsiy dietolog sifatida vaziyatdan kelib chiqib chiroyli shakllantirilgan 1 ta amaliy maslahat yozing.
                
                Qoidalar:
                1. Qandaydir taomlar ro'yxatini yoki retseptlarni aslo tavsiya etmang!
                2. Nomi (name) juda qisqa (2-3 so'z), foydali, aniq va qat'iy amaliy chaqiriq bo'lsin (masalan: "Suv bilan boshlang", "Sabzavot birinchi", "Kichkina idish oling", "Miyani aldaymiz", "Yaxshilab chaynang"). "Toyinganlik sari qadam" kabi ortiqcha, umumiy, ma'nosiz gaplarni aslo ishlatmang! Nomda "tavsiya", "$mealType" kabi zerikarli va takrorlanuvchi so'zlarni aslo ishlatmang!
                3. Tavsifi (description) bitta butun va chiroyli shakllantirilgan matn bo'lsin. Ushbu matn o'z ichiga aynan 3 yoki 4 ta har xil qisqa amaliy tavsiyani (tips) olishi shart. Har bir tavsiyani yangi qatordan (\n), emoji belgilari va qisqa sarlavhasi (masalan, 💧 Avval suv: ..., 🍽️ Kichik idish: ..., 🧠 Miyani aldaymiz: ...) bilan boshlab chiroyli shaklda yozing.
                4. Har safar chaqirilganda, so'ralgan mahalga mos ravishda har xil qiziqarli maslahatlar qaytaring.
                
                Javobni FAQAT quyidagi formatdagi roppa-rosa 1 ta elementdan iborat JSON Array ko'rinishida qaytaring (boshqa hech qanday izoh va matn qo'shmang):
                
                [
                  {
                    "description": "[Yangi qatorlar, sarlavhacha va emojilar bilan bezatilgan 3-4 ta maslahatdan iborat chiroyli tavsif matni]"
                  }
                ]
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            val text = response.text ?: return null

            val jsonStart = text.indexOf("[")
            val jsonEnd = text.lastIndexOf("]")
            if (jsonStart != -1 && jsonEnd != -1) {
                val jsonString = text.substring(jsonStart, jsonEnd + 1)
                val type = object : TypeToken<List<AiMealRecommendation>>() {}.type
                Gson().fromJson(jsonString, type)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun generateContextualGreeting(
        goal: String,
        targetWeight: Float?,
        targetCalories: Int,
        consumedCalories: Int,
        targetWater: Int,
        consumedWater: Int,
        targetExercise: Int,
        burnedExercise: Int,
        currentHour: Int
    ): Pair<String, List<String>>? {
        return try {
            val timeOfDay = when (currentHour) {
                in 4..11 -> "Ertalab"
                in 12..16 -> "Tushlik (Kunduzi)"
                in 17..21 -> "Kechqurun"
                else -> "Tunda"
            }

            val prompt = """
                Mijozning maqsadi: $goal (Maqsad vazni: ${targetWeight ?: "Noma'lum"} kg).
                Hozirgi vaqt: $currentHour:00 ($timeOfDay).
                Bugungi natijalar:
                - Ovqat: $consumedCalories kkal yeyildi (Me'yor: $targetCalories kkal)
                - Suv: $consumedWater ml ichildi (Me'yor: $targetWater ml)
                - Mashq: $burnedExercise kkal yoqildi (Me'yor: $targetExercise kkal)
                
                Vazifangiz: Mijozga shaxsiy dietolog sifatida qisqa va qat'iy maslahat/eslatma berish va mijoz sizga oson javob berishi uchun 3 ta qisqa tayyor javob variantini taqdim etish.
                Qoidalar:
                - Hozirgi vaqt va kamchilikka e'tibor qiling. Agar ertalab bo'lsa, suv ichishni eslatin. Agar kechqurun bo'lsayu, mashq qilinmagan bo'lsa, piyoda yurishni taklif qiling. Agar me'yordan oshgan bo'lsa uni chora ko'rishga undating.
                - Har safar bitta eng muhim narsaga urg'u bering.
                - Hech qanday "Salom", "Qandaysiz" so'zlarini ISHLATMANG!
                
                Javobingizni FAQAT QUYIDAGI JSON FORMATIDA qaytaring (Boshqa so'z qo'shish taqiqlanadi):
                {
                  "message": "Maslahatingiz yoki eslatmangiz (matn).",
                  "replies": ["Variant 1", "Variant 2", "Variant 3"]
                }
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            val text = response.text ?: return null

            val jsonStart = text.indexOf("{")
            val jsonEnd = text.lastIndexOf("}")
            if (jsonStart != -1 && jsonEnd != -1) {
                val jsonString = text.substring(jsonStart, jsonEnd + 1)
                val obj = org.json.JSONObject(jsonString)

                val message = obj.getString("message")
                val repliesArray = obj.getJSONArray("replies")
                val repliesList = mutableListOf<String>()
                for (i in 0 until repliesArray.length()) {
                    repliesList.add(repliesArray.getString(i))
                }
                Pair(message, repliesList)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun appendMessageToChatCache(messageText: String) {
        val todayStr = DateUtils.getFormattedDateString(Date(), "yyyy-MM-dd")
        val existingJson = LocalPreferencesRepository.getAiChatData(todayStr)
        try {
            val jsonArray = if (existingJson.isNullOrBlank()) {
                JSONArray()
            } else {
                JSONArray(existingJson)
            }

            if (jsonArray.length() == 0) {
                val greetings = listOf(
                    "Ozish haqida xohlagan savolingizni bering, javob berishga tayyorman!",
                    "Bugun o'zingizni qanday his qilyapsiz? Yordamim kerakmi?",
                    "Men sizning shaxsiy dietologingizman, tortinmay savol beravering.",
                    "Nima yesam ozaman deb o'ylayapsizmi? Darhol so'rang!",
                    "Vazn tashlash qiyinmi? Qayerda xato qilyapsiz, keling topamiz.",
                    "Men doim shu yerdaman. Qaysi ovqatni yeyishga ikkilanyapsiz?"
                )
                val greetObj = JSONObject()
                greetObj.put("text", greetings.random())
                greetObj.put("isUser", false)
                jsonArray.put(greetObj)
            }

            var found = false
            for (i in 0 until jsonArray.length()) {
                val text = jsonArray.getJSONObject(i).optString("text")
                if (text == messageText) {
                    found = true
                    break
                }
            }
            if (!found) {
                val obj = JSONObject()
                obj.put("text", messageText)
                obj.put("isUser", false)
                jsonArray.put(obj)
                LocalPreferencesRepository.setAiChatData(todayStr, jsonArray.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getAndCacheMealRecommendations(
        mealType: String,
        remainingCalories: Int,
        goal: String,
        eatenFoods: List<String>
    ): List<AiMealRecommendation>? {
        val cacheKey = "ai_rec_${Date().dateApiFormat()}_$mealType"
        val cachedJson = LocalPreferencesRepository.getAiMealCache(cacheKey)
        if (cachedJson != null) {
            try {
                val type = object : TypeToken<List<AiMealRecommendation>>() {}.type
                val list = Gson().fromJson<List<AiMealRecommendation>>(cachedJson, type)
                if (list.isNotEmpty()) {
                    val item = list.firstOrNull()
                    if (item != null) {
                        appendMessageToChatCache(item.description)
                    }
                    return list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val result = getMealRecommendations(mealType, remainingCalories, goal, eatenFoods)
        if (!result.isNullOrEmpty()) {
            val json = Gson().toJson(result)
            LocalPreferencesRepository.setAiMealCache(cacheKey, json)

            val item = result.firstOrNull()
            if (item != null) {
                val recText = item.description
                appendMessageToChatCache(recText)
            }
        }
        return result
    }
}
