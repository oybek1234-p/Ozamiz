package uz.oybek.ozamiz.data.info

enum class ActivityLevel(val id: String, val description: String, val multiplier: Double) {
    SEDENTARY("sedentary", "Kamharakat (ofis ishi)", 1.2), LIGHTLY_ACTIVE(
        "lightly_active",
        "Yengil faol (haftasiga 1-3 marta yengil mashq)",
        1.375
    ),
    MODERATELY_ACTIVE(
        "moderately_active", "O'rtacha faol (haftasiga 3-5 marta o'rtacha mashq)", 1.55
    ),
    VERY_ACTIVE(
        "very_active",
        "Juda faol (haftasiga 6-7 marta intensiv mashq)",
        1.725
    ),
    EXTRA_ACTIVE("extra_active", "O'ta faol (juda og'ir mashqlar yoki jismoniy ish)", 1.9);

    companion object {
        fun fromId(id: String?) = entries.find { it.id == id } ?: SEDENTARY
        fun getAllDescriptions() = entries.map { it.description }
    }
}