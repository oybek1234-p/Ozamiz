package uz.oybek.ozamiz.data.info

enum class ActivityLevel(val id: String, val title: String, val subtitle: String, val multiplier: Double) {
    SEDENTARY(
        "sedentary",
        "Kamharakat",
        "Ofis ishi yoki uyda",
        1.2
    ),
    LIGHTLY_ACTIVE(
        "lightly_active",
        "Yengil faol",
        "Yurish,uy ishlar",
        1.375
    ),
    MODERATELY_ACTIVE(
        "moderately_active",
        "O‘rtacha faol",
        "Kunlik yengil ish",
        1.55
    ),
    VERY_ACTIVE(
        "very_active",
        "Juda faol",
        "Doimiy jismoniy ish",
        1.725
    );

    companion object {
        fun fromId(id: String?) = entries.find { it.id == id } ?: SEDENTARY
    }
}
