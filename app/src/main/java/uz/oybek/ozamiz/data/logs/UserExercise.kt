package uz.oybek.ozamiz.data.logs

import uz.oybek.ozamiz.data.user.model.info.Gender

enum class UserExercise(
    val caloriesPerMin: Int,
    val exerciseName: String
) {
    PiyodaYurish(
        5,
        "Piyoda yurish"
    ),
    IdishTovoqYuvish(
        3,
        "Idish-tovoq yuvish"
    ),
    OvqatTayyorlash(
        2,
        "Ovqat pishirish"
    ),
    UyTozalash(
        4,
        "Uy yig'ishtirish"
    ),
    KirYuvishQolda(
        5,
        "Qo'lda kir yuvish"
    ),
    BozorlikQilish(
        4,
        "Bozorlik qilish"
    ),
    BolaParvarishi(
        4,
        "Bola parvarishi"
    ),
    Yugurish(
        10,
        "Yugurish"
    ),
    VelosipedUchish(
        7,
        "Velosiped haydash"
    ),
    ZinadanChiqish(
        9,
        "Zinadan ko'tarilish"
    ),
    Badantarbiya(
        5,
        "Ertalabki badantarbiya"
    ),
    RaqsgaTushish(
        6,
        "Raqsga tushish"
    ),
    ArqonSakrash(
        12,
        "Arqon sakrash"
    ),
    Futbol(
        9,
        "Futbol o'ynash"
    ),
    TurnikTortilish(
        8,
        "Turnik va brusda shug'ullanish"
    ),
    ZaldaShugullanish(
        6,
        "Trenajyor zalida shug'ullanish"
    ),
    OgirYukKotarish(
        8,
        "Og'ir yuk ko'tarish"
    ),
    BogIshlari(
        5,
        "Bog'dorchilik va tomorqa ishlari"
    ),
    Suzish(
        8,
        "Suzish"
    );

    fun getCalories(min: Int): Int {
        return min * caloriesPerMin
    }

    companion object {
        fun getExercisesList(gender: Int): List<UserExercise> {
            return if (gender == Gender.MALE) {
                listOf(
                    Futbol,
                    ZaldaShugullanish,
                    TurnikTortilish,
                    OgirYukKotarish,
                    Yugurish,
                    Suzish,
                    VelosipedUchish,
                    BogIshlari,
                    PiyodaYurish,
                    ZinadanChiqish,
                    Badantarbiya,
                    BozorlikQilish
                )
            } else {
                listOf(
                    UyTozalash,
                    OvqatTayyorlash,
                    IdishTovoqYuvish,
                    KirYuvishQolda,
                    BolaParvarishi,
                    RaqsgaTushish,
                    ArqonSakrash,
                    PiyodaYurish,
                    Yugurish,
                    Badantarbiya,
                    ZinadanChiqish,
                    BozorlikQilish,
                    VelosipedUchish
                )
            }
        }
    }
}