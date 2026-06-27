package uz.oybek.ozamiz.data.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    const val YYYY_MM_DD = "yyyy-MM-dd"

    fun getFormattedDateString(date: Date, pattern: String): String {
        return SimpleDateFormat(
            pattern,
            Locale("uz", "UZ")
        ).format(date)
    }

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

    fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun generateDatesForScroller(): List<Date> {
        val calendar = Calendar.getInstance()
        return (-3..3).map { dayOffset ->
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
            getStartOfDay(calendar.time)
        }
    }

    fun toLongTimestamp(ts: Any?): Long {
        return when (ts) {
            is Long -> ts
            is com.google.firebase.Timestamp -> ts.toDate().time
            is java.util.Date -> ts.time
            is Map<*, *> -> {
                (ts["timestampLong"] as? Long) ?: (ts["seconds"] as? Long)?.let { it * 1000 } ?: System.currentTimeMillis()
            }
            else -> System.currentTimeMillis()
        }
    }
}

fun <T> List<T>.sortByRecent(timestampSelector: (T) -> Any?): List<T> {
    return this.sortedByDescending { DateUtils.toLongTimestamp(timestampSelector(it)) }
}

fun Date.dateApiFormat(): String =
    DateUtils.getFormattedDateString(
        this,
        DateUtils.YYYY_MM_DD
    )