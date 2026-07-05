package uz.oybek.ozamiz.data.ai

import uz.oybek.ozamiz.data.logs.LogRepository
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.ui.feature.home.LogFragment

object AiPromptBuilder {

    const val MODEL_NAME = "gemini-flash-lite-latest"

    suspend fun buildDietologInstruction(needReplies: Boolean): String {
        val currentDate = LogFragment.currentDate
        val dailyLogs = LogRepository.getDailyLogsSync(currentDate)
        val dailySummary = LogRepository.getDailySummarySync(currentDate)

        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        val tzFormat = java.text.SimpleDateFormat("XXX", java.util.Locale.getDefault())
            .format(java.util.Date())
        val currentTime = "$timeFormat (GMT$tzFormat)"

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

        val foodNames = dailyLogs?.foodLogs?.values?.map { it.description } ?: emptyList()
        val foodsJoined =
            if (foodNames.isEmpty()) "Hali hech narsa yeyilmagan" else foodNames.joinToString(", ")

        val dailyContext = """
            BUGUNGI HOLAT (Daily Info):
            - Kaloriya: ${dailySummary.consumedCalories} / ${user.targetCalories} kkal.
            - Suv: ${dailySummary.consumedWaterMl} / ${user.targetWaterMl} ml.
            - Oqsil: ${dailySummary.consumedProtein}g / ${user.targetProtein}g.
            - Uglevod: ${dailySummary.consumedCarbs}g / ${user.targetCarbs}g.
            - Yog': ${dailySummary.consumedFat}g / ${user.targetFat}g.
            - Jismoniy faollikda yoqilgan kaloriya: ${dailySummary.burnedCalories} kkal.
            - BUGUN YEGANLARI (Faqat nomlari): $foodsJoined
            - Hozirgi vaqt $currentTime.
        """.trimIndent()

        return """
            Sening isming Ozamiz. O'zbekistonliklar uchun Vaznim ilovasining maxsus dietolog-trener va psixologsan.

            $userContext

            $dailyContext
            Asosan userga javob qaytarishga qarat.
            QAT'IY QOIDALAR (BUZISH TAQIQLANADI):
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
            10. 
            Javobing oxirida har doim mijozga mavzuga mos 3 ta qisqa tayyor savol variantlarini taqdim et.
            Javobingni FAQAT quyidagi JSON formatida qaytar:
            {
              "message": "Sening toza matndagi javobing shu yerda bo'ladi.",
              ${if (needReplies) "\"replies\": [\"Savol 1\", \"Savol 2\", \"Savol 3\"]" else ""}
            }
        """.trimIndent()
    }


    suspend fun buildFoodInstruction(): String {
        val currentDate = LogFragment.currentDate
        val dailySummary = LogRepository.getDailySummarySync(currentDate)

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

            ${userContext}

            ${dailyContext}

            🔴 JAVOB FORMATI:
            Faqat JSON formatida javob bering. Javobingizda boshqa hech qanday matn bo'lmasligi shart.

            🟢 ANALIZ ALGORITMI:
            1. Ovqat turi va tarkibini aniqlang, taqribiy kaloriya qiymatini hisoblang.
            2. Miqdorini (porsiya, dona, gramm) taxmin qiling.

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
         
        """.trimIndent()
    }

    fun buildDietologPrompt(
        message: String
    ) {

    }

    fun buildFoodAnalyzerPrompt(
        mealTime: String,
        foodName: String = "",
        photo: Boolean = false,
        audio: Boolean = false
    ): String {
        return StringBuilder().apply {
            if (foodName.isNotEmpty()) {
                append("Ovqat: $foodName")
            }
            append(" Vaqti: $mealTime")
            if (photo) {
                append("Rasmni tahlil qil")
            }
            if (audio) {
                append("Audioni tahlil qil")
            }

        }.toString()
    }
}