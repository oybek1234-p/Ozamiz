package uz.oybek.ozamiz.data.info

enum class GenderEnum(val value: Int, val description: String) {
    MALE(0, "Erkak"),
    FEMALE(1, "Ayol"),
    UNKNOWN(-1, "Noma'lum");

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: UNKNOWN
    }
}