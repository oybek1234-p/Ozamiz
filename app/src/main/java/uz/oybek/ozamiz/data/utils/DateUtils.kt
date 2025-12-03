package uz.oybek.ozamiz.data.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    const val YYYY_MM_DD = "yyyy-MM-dd"

    private val dateFormatter = SimpleDateFormat(
        YYYY_MM_DD, Locale.getDefault()
    )

    fun getFormattedDateString(date: Date = Date(), pattern: String = YYYY_MM_DD): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }

    fun getDateFromKey(key: String): Date? {
        return try {
            dateFormatter.parse(key)
        } catch (e: Exception) {
            null
        }
    }

    fun getTodayKey(): String = getFormattedDateString(Date())

    fun getStartOfDay(date: Date = Date()): Date {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.time
    }

    fun getEndOfDay(date: Date = Date()): Date {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.time
    }
}