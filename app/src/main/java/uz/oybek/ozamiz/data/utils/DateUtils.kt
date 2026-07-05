package uz.oybek.ozamiz.data.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    const val YYYY_MM_DD = "yyyy-MM-dd"

    fun todayString(): String {
        return Date().dateApiFormat()
    }

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

    fun isSameDay(date1: Date?, date2: Date): Boolean {
        if (date1 == null) return false
        val cal1 = Calendar.getInstance().apply {
            time = date1
        }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun getCurrentTime(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val formattedTime = String.format("%02d:%02d:%02d", hour, minute, second)
        return formattedTime
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
            null -> System.currentTimeMillis()
            is Number -> ts.toLong()
            is com.google.firebase.Timestamp -> ts.toDate().time
            is Date -> ts.time
            is Map<*, *> -> {
                val timestampLong = ts["timestampLong"]
                val seconds = ts["seconds"]
                val nanoseconds = ts["nanoseconds"]
                when {
                    timestampLong is Number -> timestampLong.toLong()
                    seconds is Number -> {
                        val secs = seconds.toLong()
                        val nanos = (nanoseconds as? Number)?.toLong() ?: 0L
                        secs * 1000 + nanos / 1000000
                    }

                    else -> System.currentTimeMillis()
                }
            }

            else -> System.currentTimeMillis()
        }
    }

    fun daysBetween(registerDate: Long, currentTimeMillis: Long): Int {
        val registerCalendar = Calendar.getInstance().apply { timeInMillis = registerDate }
        val currentCalendar = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val registerYear = registerCalendar.get(Calendar.YEAR)
        val registerMonth = registerCalendar.get(Calendar.MONTH)
        val registerDay = registerCalendar.get(Calendar.DAY_OF_MONTH)
        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentMonth = currentCalendar.get(Calendar.MONTH)
        val currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH)
        val days = (currentYear - registerYear) * 365 +
                (currentMonth - registerMonth) * 30 +
                (currentDay - registerDay)
        return days
    }
}

fun Date.isSameDay(other: Date): Boolean {
    return DateUtils.isSameDay(this, other)
}

fun Date.isToday(): Boolean {
    return DateUtils.isSameDay(this, Date())
}

fun Long.toStartOfDayLong(): Long {
    return DateUtils.getStartOfDay(Date(this)).time
}

fun Date.toCalendar(): Calendar {
    return Calendar.getInstance().apply { time = this@toCalendar }
}

fun Long.isNewUserWelcomeEligible(): Boolean {
    val oneHourMs = 60 * 60 * 1000L
    return System.currentTimeMillis() - this <= oneHourMs
}

fun Date.formatToDdMmyyyy(): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(this)
}

fun Long.formatToDdMmyyyy(): String {
    return Date(this).formatToDdMmyyyy()
}

fun Date.formatToLogPhotoDateTime(): String {
    return SimpleDateFormat("dd-MMMM, yyyy HH:mm", Locale("uz", "UZ")).format(this)
}

fun Long.formatToLogPhotoDateTime(): String {
    return Date(this).formatToLogPhotoDateTime()
}

fun Date.formatToDdMmm(): String {
    return SimpleDateFormat("dd MMM", Locale("uz")).format(this)
}

fun Long.formatToDdMmm(): String {
    return Date(this).formatToDdMmm()
}

fun Date.formatToHhMm(): String {
    return SimpleDateFormat("HH:mm", Locale("uz")).format(this)
}

fun Long.formatToHhMm(): String {
    return Date(this).formatToHhMm()
}

fun Date.formatToHhMmA(): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(this)
}

fun Long.formatToHhMmA(): String {
    return Date(this).formatToHhMmA()
}

fun Date.formatToE(): String {
    return SimpleDateFormat("E", Locale("uz", "UZ")).format(this)
}

fun Date.formatToD(): String {
    return SimpleDateFormat("d", Locale.getDefault()).format(this)
}

fun Date.formatToPlanDate(): String {
    return SimpleDateFormat("dd-MMMM", Locale("uz")).format(this)
}

fun <T> List<T>.sortByRecent(timestampSelector: (T) -> Any?): List<T> {
    return this.sortedByDescending { DateUtils.toLongTimestamp(timestampSelector(it)) }
}

fun Date.dateApiFormat(): String =
    DateUtils.getFormattedDateString(
        this,
        DateUtils.YYYY_MM_DD
    )

fun formatLastSeen(lastSeen: Any?): String {
    val timestamp = DateUtils.toLongTimestamp(lastSeen)
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "yaqinda"
        diff < 3600000 -> "${diff / 60000}m oldin"
        diff < 86400000 -> "${diff / 3600000}s oldin"
        else -> {
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            sdf.format(timestamp)
        }
    }
}
