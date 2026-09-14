package uz.oybek.ozamiz.data.logs.model

import androidx.annotation.Keep
import uz.oybek.ozamiz.data.user.model.info.Gender

@Keep
enum class UserExercise(
    val caloriesPerMin: Int,
    val exerciseName: String,
    val emoji: String,
    val priority: Int,
    val benefit: String
) {
    PiyodaYurish(5, "Piyoda yurish", "🚶", 1, "Yurak salomatligi va kayfiyatni yaxshilaydi"),
    Yugurish(10, "Yugurish", "🏃", 2, "Chidamlilikni oshiradi va yog' yoqadi"),
    UyTozalash(4, "Uy yig'ishtirish", "🧹", 3, "Harakatda bo'lib kaloriya sarflaysiz"),
    Badantarbiya(5, "Ertalabki badantarbiya", "🤸", 4, "Kun bo'yi tetik va faol bo'lasiz"),
    ZaldaShugullanish(6, "Trenajyor zalida shug'ullanish", "🏋️", 5, "Mushaklarni mustahkamlaydi"),
    VelosipedUchish(7, "Velosiped haydash", "🚴", 6, "Oyoq mushaklari va yurakni chiniqtiradi"),
    RaqsgaTushish(6, "Raqsga tushish", "💃", 7, "Koordinatsiya va kayfiyatni oshiradi"),
    Suzish(8, "Suzish", "🏊", 8, "Barcha mushaklarni bir vaqtda ishlaydi"),
    IdishTovoqYuvish(3, "Idish-tovoq yuvish", "🧼", 10, "Kundalik harakat va kaloriya sarfi"),
    OvqatTayyorlash(2, "Ovqat pishirish", "🍳", 11, "Sog'lom ovqatlanish odatini shakllantiradi"),
    KirYuvishQolda(5, "Qo'lda kir yuvish", "🧺", 12, "Qo'l va yelka mushaklarini kuchaytiradi"),
    BozorlikQilish(4, "Bozorlik qilish", "🛒", 13, "Piyoda yurish va yuk ko'tarishdan foyda"),
    BolaParvarishi(4, "Bola parvarishi", "👶", 14, "Faol harakat va chidamlilik oshadi"),
    ZinadanChiqish(9, "Zinadan ko'tarilish", "🪜", 15, "Oyoq kuchini oshiradi va yog' yoqadi"),
    Futbol(9, "Futbol o'ynash", "⚽", 17, "Jamoaviy sport va chidamlilik"),
    TurnikTortilish(8, "Turnik va brusda shug'ullanish", "💪", 18, "Yuqori tana mushaklarini rivojlantiradi"),
    OgirYukKotarish(8, "Og'ir yuk ko'tarish", "🧱", 19, "Kuch va chidamlilikni oshiradi"),
    BogIshlari(5, "Bog'dorchilik va tomorqa ishlari", "🧑‍🌾", 20, "Tabiatda ishlash ruhiy salomatlikka foydali");

    fun getCalories(min: Int): Int {
        return min * caloriesPerMin
    }

    companion object {

        fun getCommonExercises(): List<UserExercise> {
            return listOf(
                PiyodaYurish,
                Yugurish,
                UyTozalash,
                Badantarbiya,
                ZaldaShugullanish,
                RaqsgaTushish
            )
        }

        fun getExercisesList(gender: Int): List<UserExercise> {
            val allExercises = entries.toList()
            val genderSpecificExclusion = if (gender == Gender.MALE) {
                setOf(
                    OvqatTayyorlash,
                    IdishTovoqYuvish,
                    KirYuvishQolda,
                    BolaParvarishi,
                    RaqsgaTushish,
                    UyTozalash
                )
            } else {
                setOf(
                    Futbol,
                    TurnikTortilish,
                    OgirYukKotarish
                )
            }

            return allExercises
                .filterNot { it in genderSpecificExclusion }
                .sortedBy { it.priority }
        }
    }
}