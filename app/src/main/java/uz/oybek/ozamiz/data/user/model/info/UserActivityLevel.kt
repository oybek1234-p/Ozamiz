package uz.oybek.ozamiz.data.user.model.info

enum class UserActivityLevel(
    val title: String,
    val subtitle: String,
    val multiplier: Double
) {
    SEDENTARY(
        "Kamharakat",
        "Ofis ishi yoki uyda",
        1.2
    ),
    LIGHTLY_ACTIVE(
        "Yengil faol",
        "Yurish,uy ishlar",
        1.375
    ),
    MODERATELY_ACTIVE(
        "O‘rtacha faol",
        "Kunlik yengil ish",
        1.55
    ),
    VERY_ACTIVE(
        "Juda faol",
        "Doimiy jismoniy ish",
        1.725
    );
}