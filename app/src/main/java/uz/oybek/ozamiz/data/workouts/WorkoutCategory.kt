package uz.oybek.ozamiz.data.workouts

import androidx.annotation.Keep

@Keep
data class WorkoutCategory(
    var id: String = "",
    var name: String = "",
    var order: Int = 0
) {
     constructor() : this("", "", 0)
}