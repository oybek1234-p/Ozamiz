package uz.oybek.ozamiz.data.workouts

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
data class Workout(
    var id: String = "",
    var name: String = "",
    var description: String? = null,
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl")
    var imageUrl: String? = null,
    var categoryIds: List<String>? = null,
    var exerciseIds: List<String> = emptyList(),
    var durationMinutes: Int? = null,
    var estimatedCaloriesBurned: Int? = null, // Yangi qo'shilgan maydon
    var difficultyLevel: String? = null,
    var targetArea: List<String>? = null,
    var tags: List<String>? = null,
    var requiredPremium: Boolean = false,
    var adRequired: Boolean = false,

    @ServerTimestamp
    val createdAt: Date? = null,
    var order: Int = 0
) {
    constructor() : this(
        id = "",
        name = "",
        description = null,
        imageUrl = null,
        categoryIds = null,
        exerciseIds = emptyList(),
        durationMinutes = null,
        estimatedCaloriesBurned = null, // Konstruktorga ham qo'shildi
        difficultyLevel = null,
        targetArea = null,
        tags = null,
        requiredPremium = false,
        adRequired = false,
        createdAt = null,
        order = 0
    )
}