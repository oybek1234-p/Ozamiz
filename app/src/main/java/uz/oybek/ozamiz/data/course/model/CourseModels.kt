package uz.oybek.ozamiz.data.course.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class CourseLevel(
    val id: String,
    val levelNumber: Int,
    val title: String,
    val description: String,
    val isLocked: Boolean,
    val lessons: List<CourseLesson>
) : Parcelable

@Parcelize
data class CourseLesson(
    val id: String,
    val dayNumber: Int,
    val title: String,
    val durationMinutes: Int,
    val isCompleted: Boolean,
    val isLocked: Boolean,
    val screens: List<CourseScreen>,
    val task: CourseTask
) : Parcelable

@Parcelize
data class CourseScreen(
    val id: String,
    val title: String,
    val text: String,
    val imageUrl: String? = null,
    @DrawableRes val localImageRes: Int? = null
) : Parcelable

@Parcelize
data class CourseTask(
    val id: String,
    val title: String,
    val description: String,
    val benefits: String
) : Parcelable
