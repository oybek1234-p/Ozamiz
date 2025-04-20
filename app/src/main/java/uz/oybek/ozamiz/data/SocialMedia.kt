package uz.oybek.ozamiz.data

import android.app.Activity
import android.content.Intent
import android.net.Uri

open class SocialMedia {

    companion object {
        private const val TELEGRAM_SUFFIX = "https://t.me/"

        fun openLink(activity: Activity, link: String) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(parseTelegramLink(link)))
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