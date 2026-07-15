package uz.oybek.ozamiz.ui.feature.menu.list

enum class MenuFilterType(val value: String) {
    MEAL("meal"),
    DIFFICULTY("difficulty"),
    DIET("diet"),
    CALORIE("calorie");

    companion object {
        fun fromValue(value: String): MenuFilterType? {
            return entries.find { it.value.equals(value, ignoreCase = true) }
        }
    }
}
