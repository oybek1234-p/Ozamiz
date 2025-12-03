package uz.oybek.ozamiz.ui.feature.onboarding

enum class MultipleAnswersQuestion(
    val title: String,
    val answers: List<String>
) {
    GOAL(
        title = "Nima uchun ozmoqchisiz?",
        answers = listOf(
            "O'zimga ishonchni oshirish uchun",
            "Salomatligimni yaxshilash uchun",
            "Jismoniy holatimni yaxshilash uchun",
            "Muhim tadbirga tayyorgarlik uchun",
            "Boshqa sabab"
        )
    ),
    HISTORY(
        title = "Ozish bo'yicha tajribangiz qanday?",
        answers = listOf(
            "Avval ozganman va yana ozmoqchiman",
            "Harakat qilib ko'rganman, o'xshamagan",
            "Ozganman, lekin vaznim yana qaytgan",
            "Avval ozishga harakat qilmaganman"
        )
    ),

    STRUGGLES(
        title = "Qanday qiyinchiliklarga duch keldingiz?",
        answers = listOf(
            "Ishtahani jilovlash",
            "Motivatsiyani saqlash",
            "Ovqat miqdorini kamaytirish",
            "Nima yeyishni bilmaslik",
            "Vaqt yo’q"
        )
    ),
    CALORIES(
        title = "Avval kaloriyalarni sanaganmisiz?",
        answers = listOf(
            "Ha",
            "Yo'q"
        )
    ),
    FASTING(
        title = "Hech interval ochlik usulini sinab ko'rganmisiz?",
        answers = listOf(
            "Ha, menga yoqdi",
            "Ha, lekin bu men uchun emas ekan",
            "Yo'q, lekin sinab ko'rmoqchiman",
            "Yo'q, menga qiziq emas",
            "Interval ochlik nima degani?"
        )
    ),
    CHALLENGE(
        title = "O'zingizni sinab ko'ring! Necha kun uzluksiz davom eta olasiz?",
        answers = listOf(
            "50 kun (Afsona 🔥)",
            "30 kun (Qoyilmaqom 🚀)",
            "14 kun (Ajoyib 👌)",
            "7 kun (Yaxshi 👍)"
        )
    )
}