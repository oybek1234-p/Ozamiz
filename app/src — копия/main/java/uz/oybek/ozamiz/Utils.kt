package uz.oybek.ozamiz

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import androidx.core.view.isVisible
import com.google.gson.Gson
import com.stfalcon.imageviewer.StfalconImageViewer
import uz.oybek.ozamiz.ui.loadPhoto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

inline fun String?.ifNotNullOrEmpty(method: (string: String) -> Unit) {
    if (isNullOrEmpty().not()) {
        method.invoke(this!!)
    }
}

inline fun String?.ifEmptyOrNull(method: () -> String?): String? {
    if (isNullOrEmpty()) {
        return method.invoke()
    }
    return this
}

fun getTodayDate(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calendar = Calendar.getInstance()
    return dateFormat.format(calendar.time)
}

fun ImageView.openImageViewer(urls: List<String>, startPos: Int = 0, show: Boolean = true) {
    if (windowToken == null) return
    if (isVisible.not()) return
    if (isAttachedToWindow.not()) return
    StfalconImageViewer.Builder(context, urls) { view, image ->
        view.loadPhoto(image, show.not())
    }.withTransitionFrom(this).withStartPosition(startPos).withHiddenStatusBar(false).show()
}

var gson: Gson? = null
    get() {
        if (field == null) {
            field = Gson()
        }
        return field
    }

//fun AdView.loadAd() {
//    isVisible = true
//    val adRequest = AdRequest.Builder().build()
//    loadAd(adRequest)
//}

fun openPhoneCall(activity: Activity, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL)
    intent.data = Uri.parse("tel:$phone") // Create a URI with the phone number
    activity.startActivity(intent)
}

fun Activity.openLink(link: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(link)
        startActivity(intent)
    } catch (e: Exception) {
        //Ign
    }
}