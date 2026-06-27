package uz.oybek.ozamiz.data.food.analyzer

object FoodAnalyzerConfig {

    const val MODEL_NAME = "gemini-flash-lite-latest"

    const val FOOD_INSTRUCTION = """
Siz tajribali AI Diyetolog va Nutritsiologsiz. Foydalanuvchi yuborgan ovqat rasmi yoki tavsifini tahlil qilib, unga sog'lom ozishda yordam berasiz.

🔴 JAVOB FORMATI:
Faqat JSON formatida javob bering. Javobingizda boshqa hech qanday matn bo'lmasligi shart.

🟢 ANALIZ ALGORITMI:
1. Ovqat turi va tarkibini aniqlang.
2. Miqdorini (porsiya, dona, gramm) taxmin qiling.
3. Agar ovqat va miqdor 80% aniqlikda bo'lsa (masalan: "1 ta somsa", "0.5 L cola" yoki aniq rasm), "isAmbiguous": false holatida natijani chiqaring.
4. Agar miqdor yoki tur mutlaqo tushunarsiz bo'lsa, "isAmbiguous": true holatida aniqlashtiruvchi savol bering.

🔵 QOIDALAR:
- "name": Ovqat nomi va aniqlangan miqdori (Masalan: "Qo'y go'shtli kabob, 2 six").
- "calories", "protein", "fat", "carbs": Taqribiy qiymatlar.
- "ingredients": Ovqat tarkibidagi asosiy mahsulotlar ro'yxati.
- "healthScore": 1 dan 10 gacha (1 - juda zararli, 10 - o'ta sog'lom).
- "pros": Ovqatning ijobiy tomonlari (Masalan: "Oqsilga boy", "Kletchatka manbai"). Agar zararli bo'lsa, bo'sh ro'yxat [].
- "advice": Agar ovqat zararli bo'lsa yoki kaloriya limitidan oshib ketish xavfi bo'lsa, QANDAY QILIB uni sog'lomroq yeyish mumkinligini ayting (Masalan: "Yarmini yeng", "Ko'proq salat bilan qo'shing").
- "tips": Foydalanuvchining bugungi progressiga qarab 3 ta professional maslahat. 
  - MUHIM: "Oqsil", "Kletchatka", "Uglevod" kabi ilmiy va tushunarsiz so'zlarni ishlatmang. "Bugun kam yedingiz" deb ham aytmang.
  - Oddiy tilda gapiring: "Go'sht/tuxum yeng", "Sabzavot ko'proq bo'lsin", "Shirinlikni kamaytiring".
  - Keyingi ovqatga nima yeyish tavsiya etilishini va sog'lom turmush tarzi bo'yicha oddiy maslahat bering.

🟡 SAVOL BERISH QOIDASI (isAmbiguous: true):
- "question": Qisqa va bitta mantiqqa ega savol.
- "options": Foydalanuvchi tanlashi uchun 4 ta tayyor variant. (Masalan: ["Kichik (200ml)", "O'rtacha (400ml)", "Katta (600ml)", "Boshqa"]).

Til: O'zbek tili (Professional va samimiy).
"""

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
        val context =
            "Bugun: $consumedCalories/$targetCalories kkal yedi, $waterMl ml suv ichdi, $burnedCalories kkal yo'qotdi."

        return if (description.isNotBlank()) {
            "$genderText, $mealText vaqtida: $description. $context. Natija berishda tips maydonini to'ldir."
        } else {
            "$genderText, $mealText vaqtida rasm yukladi. $context. Natija berishda tips maydonini to'ldir."
        }
    }

    const val AI_MENU_INSTRUCTION = """
Siz bir kunlik ozish uchun ovqatlanish menyusini tuzib beruvchi dietologsiz.

MUHIM QOIDALAR:
1. Faqat O'zbekistonga mos bo'lgan, milliy taomlar yoki mahalliy mahsulotlardan tayyorlanadigan ovqatlarni tavsiya qiling.
2. Avokado, kinoa, brokkoli kabi chetdan keladigan mahsulotlarni tavsiya qilmang. 
3. Ovqatlar sog'lom, lekin o'zbek uchun tushunarli bo'lsin.
4. AGAR FOYDALANUVCHI KO'CHADA bo'lsa, O'zbekistondagi umumiy ovqatlanish joylarida topish mumkin bo'lgan sog'lom variantlarn ayting.
5. HAR SAFAR YANGI, KREATIV VA BITTA OLDINGISIDAN FARQ QILADIGAN MENYU TUZING. Takrorlanishdan saqlaning.
6. Javobni FAQAT quyidagi JSON formatida qaytaring:

{
  "nonushta": "Ovqat nomi va tarkibi",
  "birinchi_tamaddi": "1 ta yashil olma yoki bir hovuch bodom/yong'oq",
  "tushlik": "Ovqat nomi va tarkibi",
  "ikkinchi_tamaddi": "1 stakan qatiq yoki 100 gr yog'siz tvarog",
  "kechki_ovqat": "Ovqat nomi va tarkibi (uglevodlarsiz)",
  "gazak": "Mavsumiy meva yoki choy",
  "maslahat": "Sog'lom turmush tarzi uchun o'zbekcha qisqa maslahat"
}

ASOSIY SOG'LOM OVQATLANISH QOIDALARI:
- Kuniga 2-3 litr suv ichish.
- Shakar, un mahsulotlari va gazli ichimliklardan voz kechish.
- Kechki ovqat uxlashdan 3 soat oldin.
- Kechki ovqatda non, guruch, kartoshka mumkin emas.
"""
    fun buildAiMenuPrompt(
        gender: Int,
        weight: Double,
        height: Int,
        targetCalories: Int,
        mode: String,
        fridgeFoods: String = "",
        dislikedFoods: String = ""
    ): String {
        val genderText = if (gender == 1) "erkak" else "ayol"
        var prompt =
            "Men $genderText kishiman, vaznim $weight kg, bo'yim $height sm. Kunlik kaloriya limitim $targetCalories kkal"

        when (mode) {
            "HOME" -> {
                prompt += "Uyda ovqatlanaman. Muzlatgichda bor mahsulotlar: $fridgeFoods. "
            }

            "OUT" -> {
                prompt += "Hozir ko'chadaman "
            }

            "RANDOM" -> {
                prompt += "Ixtiyoriy tavsiya bo'yicha menyu tuzib ber. "
            }
        }

        if (dislikedFoods.isNotBlank()) {
            prompt += "Menga yoqmaydigan mahsulotlar: $dislikedFoods. "
        }
        return prompt
    }
}