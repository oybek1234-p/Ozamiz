package uz.oybek.ozamiz.data.utils

import android.app.Activity
import android.content.Intent
import androidx.core.net.toUri

class CompanyLinks {

    companion object {
        const val PHONE_NUMBER = "+998971871415"
        const val TELEGRAM = "@oybek1415"

        private const val TELEGRAM_SUFFIX = "https://t.me/"

        fun openLink(activity: Activity, link: String) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                parseTelegramLink(link).toUri()
            )
            activity.startActivity(intent)
        }

        fun telegramUserName(link: String): String {
            if (link.startsWith(TELEGRAM_SUFFIX)) {
                return link.removePrefix(TELEGRAM_SUFFIX)
            } else if (link.startsWith("@")) {
                return link
            }
            return ""
        }

        fun parseTelegramLink(link: String): String {
            return if (link.startsWith(TELEGRAM_SUFFIX)) {
                link
            } else {
                val l = link.removePrefix("@")
                "$TELEGRAM_SUFFIX$l"
            }
        }
    }
}