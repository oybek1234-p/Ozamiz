package uz.oybek.ozamiz.data.community

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.community.model.PostType
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.ui.feature.premium.PremiumUtils

object CommunityShareHelper {

    fun shareFoodLogIfNeeded(foodLog: FoodLog) {
        val score = foodLog.healthLevel ?: 0
        if (score < 9) return

        val user = UserProfileRepository.user
        val userId = Firebase.auth.currentUser?.uid ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val post = CommunityPost(
                userId = userId,
                userName = user.name.ifBlank { "Foydalanuvchi" },
                userAvatarUrl = user.avatarUrl,
                photoUrl = foodLog.photoUrl ?: "",
                photos = if (foodLog.photoUrl != null) listOf(foodLog.photoUrl!!) else emptyList(),
                title = foodLog.description,
                description = foodLog.aiInsights ?: "",
                foodName = foodLog.description,
                calories = foodLog.calories,
                protein = foodLog.protein,
                fat = foodLog.fat,
                carbs = foodLog.carbs,
                healthScore = score,
                userScore = user.scorePoints,
                verified = true,
                isVisible = true,
                type = PostType.FOOD,
                isPremiumUser = PremiumUtils.isPremium(user)
            )
            CommunityPostRepository.addPost(post)
        }
    }
}
