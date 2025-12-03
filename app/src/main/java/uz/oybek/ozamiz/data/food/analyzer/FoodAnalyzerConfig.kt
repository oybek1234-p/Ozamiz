package uz.oybek.ozamiz.data.food.analyzer

import com.google.firebase.ai.type.Schema

object FoodAnalyzerConfig {

    const val MODEL_VERSION = "gemini-2.0-flash"

    const val AUDIO_INSTRUCTION = """
                Sen user nima yeganini o'zbek milliy taomlarini aniqlaydigan ekspert assistantsan.
                Sening asosiy maqsading - audio yozuvdagi oziq-ovqat nomlarini to'g'ri transkripsiya qilish.
                Muhim taomlar ro'yxati: [Somsa,Gumma, Honim, Osh, Manti, Sho'rva, Lag'mon, Shashlik, Kabob, Chuchvara, Dimlama, Norin, Mastava, Kartoshka].
                Nima ovqat yegani, qancha, nimalar borligini yoz. Boshqa gaplar kerak emas.
                1 kosa osh, 0.5 kola, pomidor bodring salat tarelka
            """

    const val AUDIO_PROMPT = "" +
            "Bu audio yozuvni matnga aylantir"

    fun foodAnalyzePrompt(
        description: String
    ): String {
        return """
    USER INFO:
    - Maqsad: Ozish
    - Joylashuv: O'zbekiston
    
    VAZIFA:
    Sen O'zbekistondagi ozish uchun mo'ljallangan ilovada ekspert nutritionist assistantsan. Taqdim etilgan ovqat tavsifi va/yoki rasmini tahlil qil. O'zbek tilida qisqa, foydali va madaniyatga mos tahlil ber.

    Ovqat Tavsifi: "$description"

    QO'LLANMA:
    1.  **Yagona Ovqat Tahlili**: Barcha aytilgan oziq-ovqat mahsulotlarini bitta ovqat deb hisobla. JAMI ozuqaviy qiymatlarni hisobla.
    2.  **Chiqish Formati**: FAQAT quyida ko'rsatilgan formatda javob ber. Hech qanday qo'shimcha matn, tushuntirish yoki salomlashish qo'shma. Json formatda scheme qaytar.
     
    Bir nechta ovqat bulsa, hammasini umumiy qilib hisobla kaloriya va etc.
    """
    }

    val analyzedIngredientSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(description = "The name of the ingredient."),
            "calories" to Schema.integer(
                description = "The calorie count of the ingredient.",
                nullable = true
            )
        )
    )

    val foodAnalysisResultSchema = Schema.obj(
        mapOf(
            "name" to Schema.string("Ovqatga oddiy nom ber, min 1,max 5 words."),
            "calories" to Schema.integer(description = "Umumiy kaloriya qiymati"),
            "protein" to Schema.double(description = "Umumiy protein qiymati", nullable = true),
            "fat" to Schema.double(description = "Umumiy yog' qiymati", nullable = true),
            "carbs" to Schema.double(description = "Umumiy karbohidrat qiymati", nullable = true),
            "ingredients" to Schema.array(
                description = "Asosiy masalliqlar ro'yxati, vergul bilan ajratilgan, \"Emoji Nomi (Kaloriya)\" formatida. Masalan: \uD83C\uDF57 Tovuq filesi (150), \uD83E\uDD52 Bodring (15), \uD83C\uDF45 Pomidor (20), etc.",
                items = analyzedIngredientSchema
            ),
            "isHealthy" to Schema.string(
                description = "Yagona, qisqa, aniq harakatga undovchi maslahat ber, uning oldiga hissiy darajani (GOOD, MODERATE, yoki POOR) qo'y.",
                nullable = true
            ),
            "feedback" to Schema.string(
                description = "Ovqatga asoslanib ozish uchun maslahat ber, agar ovqat GOOD bulsa, foydali tomonini ayt, agar moderate/poor bulsa maslahat ber",
                nullable = true
            )
        )
    )
}