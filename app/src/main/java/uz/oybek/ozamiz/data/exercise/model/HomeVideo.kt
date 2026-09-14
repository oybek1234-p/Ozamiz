package uz.oybek.ozamiz.data.exercise.model

import androidx.annotation.Keep

@Keep
data class HomeVideo(
    val id: String,
    val name: String,
    val calories: Int,
    val benefits: List<String>,
    val youtubeVideoId: String,
    val durationSeconds: Int
) {
    val thumbnailUrl: String
        get() = "https://img.youtube.com/vi/$youtubeVideoId/hqdefault.jpg"
}
