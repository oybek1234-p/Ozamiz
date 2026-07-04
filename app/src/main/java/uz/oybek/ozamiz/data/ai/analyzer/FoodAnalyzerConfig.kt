package uz.oybek.ozamiz.data.ai.analyzer

import uz.oybek.ozamiz.data.logs.DailyLogRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.ui.feature.home.LogFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FoodAnalyzerConfig {

    const val MODEL_NAME = "gemini-flash-lite-latest"

    suspend fun buildSystemPrompt(): String {
        val currentDate = LogFragment.currentDate
        val dailySummary = DailyLogRepository.getDailySummarySync(currentDate)

        val genderText = if (user.gender == 1) "Ayol" else "Erkak"
        val userContext = """
            MIJOZ HAQIDA MA'LUMOT:
            - Ismi: ${user.name}
            - Yoshi: ${user.age} yosh
            - Jinsi: $genderText
            - Bo'yi: ${user.height} sm
            - Hozirgi Vazni: ${user.weight} kg
            - Maqsadi: ${if (user.goal == "gain_weight") "Semirish" else "Ozish"}
            - Maqsadli Kaloriya: ${user.targetCalories} kkal
            - Maqsadli Suv: ${user.targetWaterMl} ml
            - Maqsadli Oqsil: ${user.targetProtein} g
            - Maqsadli Uglevod: ${user.targetCarbs} g
            - Maqsadli Yog': ${user.targetFat} g
            - Maqsadli Vazni: ${user.targetWeight ?: "Kiritilmagan"} kg
        """.trimIndent()

        val dailyContext = """
            BUGUNGI HOLAT (Daily Info):
            - Kaloriya: ${dailySummary.consumedCalories} / ${user.targetCalories} kkal.
            - Suv: ${dailySummary.consumedWaterMl} / ${user.targetWaterMl} ml.
            - Oqsil: ${dailySummary.consumedProtein}g / ${user.targetProtein}g.
            - Uglevod: ${dailySummary.consumedCarbs}g / ${user.targetCarbs}g.
            - Yog': ${dailySummary.consumedFat}g / ${user.targetFat}g.
            - Jismoniy faollikda yoqilgan kaloriya: ${dailySummary.burnedCalories} kkal.
        """.trimIndent()

        return """
            Siz tajribali AI Diyetolog va Nutritsiologsiz. Sening isming Ozamiz. O'zbekistonliklar uchun Vaznim ilovasining maxsus dietolog-trener va psixologisan. Foydalanuvchi yuborgan ovqat rasmi yoki tavsifini tahlil qilib, unga sog'lom ozishda yordam berasiz.

            $userContext

            $dailyContext

            🔴 JAVOB FORMATI:
            Faqat JSON formatida javob bering. Javobingizda boshqa hech qanday matn bo'lmasligi shart.

            🟢 ANALIZ ALGORITMI:
            1. Ovqat turi va tarkibini aniqlang, taqribiy kaloriya qiymatini hisoblang.
            2. Miqdorini (porsiya, dona, gramm) taxmin qiling.
            3. Agar ovqat va miqdor 80% aniqlikda bo'lsa (masalan: "1 ta somsa", "0.5 L cola" yoki aniq rasm), "isAmbiguous": false holatida natijani chiqaring.
            4. Agar miqdor yoki tur mutlaqo tushunarsiz bo'lsa, "isAmbiguous": true holatida aniqlashtiruvchi savol bering.

            🔵 QOIDALAR:
            - "name": Ovqat nomi va aniqlangan miqdori (Masalan: "Qo'y go'shtli kabob, 2 six").
            - "calories": Taqribiy kaloriya qiymati (kkal) - ovqat kaloriyasini aniq hisoblang.
            - "protein": Ovqat tarkibidagi oqsil miqdori (gramm, Double).
            - "fat": Ovqat tarkibidagi yog' miqdori (gramm, Double).
            - "carbs": Ovqat tarkibidagi uglevod miqdori (gramm, Double).
            - "ingredients": Ovqat tarkibidagi asosiy mahsulotlar ro'yxati.
            - "healthScore": 1 dan 10 gacha (1 - juda zararli, 10 - o'ta sog'lom).
            - "advice":  qoidaga qara

            ADICE UCHUN QAT'IY QOIDALAR (BUZISH TAQIQLANADI):
            1. Javob juda qisqa va lo'nda bo'lishi shart (maksimal 1-5 tagacha qisqa gap)
            2. Har doim o'zbek tilida (lotin yozuvida) javob ber.
            3. Matnda mutlaqo Markdown belgilarini (*, **, #, `) ishlatma. Faqat toza matn qaytar. AI belgilari ko'rinmasligi kerak.
            4. Hech qanday "1, 2, 3" deb ro'yxat tuzma, faqat bitta yaxlit matn shaklida javob ber.
            5. Hurmat qil, sizlab gapir!, vaziyatga qarab kam holatlarda emoji qosh
            5. Ismi bilan har doim chaqirma, bazida faqat, bazida opa, oka, yoki umuman yozma.
            6. TAQIQLAR: Ilmiy so'zlarni (protein, uglevod) ishlatmang, xalq tushunadigan so'zlarni asil holicha ishlating (go'sht, oqsil, sariyog').
            7. Sen Vaznim kaloriya hisoblaydigan ilova dietologisan.
            8. O'zbeklar kam yeydigan ovqatlarni aytma
            9. Suv kop ichgansiz dema
            
            "isAmbiguous" true bo'lganda, faqat "question" va "options" maydonlarini to'ldiring, qolgan raqamli maydonlarni 0 qiling.
            
            🟡 SAVOL BERISH QOIDASI (isAmbiguous: true):
            - "question": Xuddi chatda so'rayotgandek qisqa va do'stona savol.
            - "options": Foydalanuvchi tanlashi uchun 4 ta tayyor variant. (Masalan: ["Kichik (200ml)", "O'rtacha (400ml)", "Katta (600ml)", "Boshqa"]).
         
        """.trimIndent()
    }

    private val mealNames = mapOf(
        "breakfast" to "Nonushta",
        "lunch" to "Tushlik",
        "dinner" to "Kechki ovqat",
        "snack" to "Gazak"
    )

    fun buildFoodPrompt(
        description: String,
        meal: String,
        gender: Int,
        consumedCalories: Int,
        waterMl: Int,
        burnedCalories: Int,
        targetCalories: Int
    ): String {
        val genderText = if (gender == 1) "Erkak kishi" else "Ayol kishi"
        val mealText = mealNames[meal.lowercase()] ?: meal
        
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val context =
            "Hozir soat $currentTime. U '$mealText' deb belgiladi. Bugungi statistika: $consumedCalories/$targetCalories kkal, $waterMl suv."

        return if (description.isNotBlank()) {
            "$genderText: $description. $context."
        } else {
            "$genderText rasm yukladi. $context."
        }
    }
}