package uz.oybek.ozamiz.ui.getinfo

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import java.util.Locale

enum class ReminderType {
    MEAL, EXERCISE
}

@Keep
data class ReminderItem(
    var id: String = "",
    var name: String = "",
    @DrawableRes var iconRes: Int = 0,
    var hour: Int? = null,
    var minute: Int? = null,
    var type: ReminderType = ReminderType.MEAL,
    var description: String? = null,
    var imageUrl: String? = null
) {

    constructor() : this("", "", 0, null, null, ReminderType.MEAL, null, null)

    fun getTimeAsString(): String {
        return if (isValidTimeSet()) {
            String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        } else {
            "vaqtni kiritish"
        }
    }

    fun isValidTimeSet(): Boolean {
        return hour != null && minute != null
    }
}