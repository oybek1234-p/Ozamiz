package uz.oybek.ozamiz.data.exercise.model

import androidx.annotation.Keep

@Keep
data class HomeExercise(
    val id: String,
    val name: String,
    val difficulty: String,
    val category: String,
    val calories: Int,
    val durationSeconds: Int,
    val benefits: List<String>,
    val instructions: List<String>,
    val imageRes: Int
)
