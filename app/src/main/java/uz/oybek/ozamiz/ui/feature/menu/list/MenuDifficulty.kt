package uz.oybek.ozamiz.ui.feature.menu.list

import androidx.annotation.StringRes
import uz.oybek.ozamiz.R

enum class MenuDifficulty(
    val id: String,
    @StringRes val textResId: Int,
    val emoji: String
) {
    EASY("Oson", R.string.oson, "🍳"),
    MEDIUM("O'rtacha", R.string.ortacha, "🥘"),
    HARD("Qiyin", R.string.qiyin, "🥩");

    companion object {
        fun fromId(id: String): MenuDifficulty? {
            return entries.find { it.id.equals(id, ignoreCase = true) }
        }
    }
}
