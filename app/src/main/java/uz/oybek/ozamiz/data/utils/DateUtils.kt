package uz.oybek.ozamiz.data.utils

import android.annotation.SuppressLint
import com.google.firebase.Timestamp
import okhttp3.internal.toLongOrDefault
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.appContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object DateUtils {

    fun formatDate(date: Any): String {
        if (date == 0L) return appContext.getString(R.string.yaqinda)
        return Date(parseDateMillis(date)).getTimeAgo()
    }

    @SuppressLint("SimpleDateFormat")
    fun getDayHour(date: Long): String {
        try {
            val dateObj = Date(date)
            val dateFormat = SimpleDateFormat("HH:mm")
            return dateFormat.format(dateObj)
        } catch (e: Exception) {

        }
        return formatDate(date)
    }

    fun parseDateMillis(any: Any): Long {
        when (any) {
            is Timestamp -> {
                return any.toDate().time
            }

            is String -> {
                return any.toLongOrDefault(0)
            }

            is Long -> {
                return any
            }
        }
        return 0
    }

    private val calendar = Calendar.getInstance()
    private val date = Date()

    fun getUserLastSeenTime(date: Long): String {
        if (date == 0L) return "Onlayn"
        return formatDate(date)
    }

    fun calculateDaysBetween(startMillis: Long, endMillis: Long): Long {
        val startDate = Date(startMillis)
        val endDate = Date(endMillis)
        val diffInMillis = endDate.time - startDate.time
        return TimeUnit.MILLISECONDS.toDays(diffInMillis)
    }

    fun getDay(long: Long): Int {
        date.time = long
        calendar.time = date
        val firstDay = calendar.get(Calendar.DAY_OF_YEAR)
        return firstDay
    }

    fun isSameDay(first: Long, second: Long): Boolean {
        date.time = first
        calendar.time = date
        val firstDay = calendar.get(Calendar.DAY_OF_YEAR)
        val firstYear = calendar.get(Calendar.YEAR)

        date.time = second
        calendar.time = date
        val secondDay = calendar.get(Calendar.DAY_OF_YEAR)
        val secondYear = calendar.get(Calendar.YEAR)

        return firstYear == secondYear && secondDay == firstDay
    }

    fun isToday(date: Long): Boolean {
        return isSameDay(date, System.currentTimeMillis())
    }

    val sdf = SimpleDateFormat("dd MMMM", Locale.getDefault())

    fun getDateDay(date: Long): String {
        // Define the format for hours and minutes
        // Set time zone to UTC
        sdf.timeZone = TimeZone.getDefault()
        // Convert the long date to a Date object and format it
        return sdf.format(Date(date))
    }

    private fun Date.getTimeAgo(): String {
        val now = Calendar.getInstance().time
        val differenceInMillis = now.time - this.time
        val differenceInMinutes = (differenceInMillis / (60 * 1000)).toInt()

        return when {
            differenceInMinutes < 2 -> "Yaqinda" // Customize for your needs
            differenceInMinutes < 60 -> "$differenceInMinutes ${getString(R.string.daqiqa_oldin)}"
            differenceInMinutes < 1440 -> "${differenceInMinutes / 60} ${getString(R.string.sohat_oldin)}"
            differenceInMinutes < 43200 -> "${differenceInMinutes / 1440} ${getString(R.string.kun_oldin)}"
            differenceInMinutes < 525600 -> "${differenceInMinutes / 43200} ${getString(R.string.oy_oldin)}"
            else -> "${differenceInMinutes / 525600} ${getString(R.string.yil_oldin)}"
        }
    }

    private fun getString(id: Int) = appContext.getString(id)
}