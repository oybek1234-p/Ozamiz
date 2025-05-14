// C:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\data\workouts\Exercise.kt
package uz.oybek.ozamiz.data.workouts

import android.os.Parcelable // Parcelable importi
import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize // Parcelize importi
import java.util.Locale

@Keep
@Parcelize
data class Exercise(
    var id: String = "",
    var name: String = "",
    var description: String? = null,
    var durationSeconds: Int? = null, // null bo'lishi mumkin
    var repetitions: Int? = null,    // null bo'lishi mumkin
    @get:PropertyName("mediaUrl") @set:PropertyName("mediaUrl")
    var mediaUrl: String? = null, // GIF/Image URL
    var mediaType: String = "gif", // "gif", "image", "video"
    var exerciseNameAudioUrl: String? = null, // Mashq nomi audiosi
    var exerciseAboutAudioUrl: String? = null, // Mashq haqida audio
    var voiceUrl: String? = null // Eski ovoz URL (balki keraksiz)
): Parcelable { // Parcelable interfeysini implement qilish
    constructor() : this(
        "", "", null, 30, null, null, "gif", null, null, null // Default qiymatlar
    )

    fun getDurationOrRepsText(): String {
        return when {
            durationSeconds != null && durationSeconds!! > 0 -> {
                val minutes = durationSeconds!! / 60
                val seconds = durationSeconds!! % 60
                if (minutes > 0) {
                    String.format(Locale.getDefault(), "%d:%02d", minutes, seconds) // MM:SS format
                } else {
                    String.format(Locale.getDefault(), "00:%02d", seconds) // SS format
                }
            }
            repetitions != null && repetitions!! > 0 -> "x$repetitions"
            else -> ""
        }
    }
}