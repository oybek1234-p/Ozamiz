// C:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\data\gamification\Badge.kt
package uz.oybek.ozamiz.data.gamification

import androidx.annotation.DrawableRes

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    @DrawableRes val iconResId: Int, // Nishon rasmi uchun resurs ID
    val requiredPoints: Int,
    val isUnlocked: Boolean = false // Foydalanuvchi uchun ochilganmi?
)