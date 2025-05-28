package uz.oybek.ozamiz.data.workouts

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Keep
@Parcelize
data class Video(
    var id: String = "",
    var name: String = "",
    var description: String? = null,
    var youtubeUrl: String = "",
    var youtubeVideoId: String? = null, // Avtomatik yoki qo'lda kiritiladi
    @get:PropertyName("thumbnailUrl") @set:PropertyName("thumbnailUrl")
    var thumbnailUrl: String? = null,
    var durationMinutes: Int? = null,
    var categoryIds: List<String>? = null, // O'ZGARISH: WorkoutCategory ID'lari
    @ServerTimestamp
    val createdAt: Date? = null,
    var order: Int = 0 // Kategoriya ichida tartiblash uchun
) : Parcelable {
    constructor() : this(
        id = "",
        name = "",
        description = null,
        youtubeUrl = "",
        youtubeVideoId = null,
        thumbnailUrl = null,
        durationMinutes = null,
        categoryIds = null, // O'ZGARISH
        createdAt = null,
        order = 0
    )
}

// YouTube URL'dan ID ajratib olish uchun yordamchi funksiya (o'zgarishsiz)
fun extractYoutubeVideoId(youtubeUrl: String?): String? {
    if (youtubeUrl.isNullOrBlank()) return null
    val patterns = arrayOf(
        "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*",
        "(?<=\\/v\\/|\\/videos\\/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v=|&v=|\u200C\u200B?feature=player_embedded&v=|\u200C\u200B?v=|\u200C\u200B?list=)[^#\\&\\?\\n]*"
    )
    for (pattern in patterns) {
        val compiledPattern = Regex(pattern)
        val matcher = compiledPattern.find(youtubeUrl)
        if (matcher != null && (matcher.value.length == 11 || matcher.value.contains("shorts"))) {
            if (matcher.value.contains("shorts/")) {
                return matcher.value.substringAfter("shorts/")
            }
            return matcher.value
        }
    }
    return null
}