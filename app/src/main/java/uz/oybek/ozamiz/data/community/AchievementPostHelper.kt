package uz.oybek.ozamiz.data.community

import uz.oybek.ozamiz.data.community.model.PostType.MANUAL
import uz.oybek.ozamiz.data.community.model.PostType.WEIGHT_LOSS
import uz.oybek.ozamiz.data.firebase.firestore
import uz.oybek.ozamiz.data.firebase.auth
import uz.oybek.ozamiz.data.firebase.userWeightLogsCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.ui.feature.premium.PremiumUtils
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.logs.model.WeightLog
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.utils.roundTo1Decimal
import uz.oybek.ozamiz.data.utils.sortByRecent

object AchievementPostHelper {

    private fun isUserVerified(): Boolean {
        val user = UserProfileRepository.user
        return user.valid && !user.uid.isNullOrBlank()
    }

    private fun buildPost(
        title: String,
        description: String,
        type: String = MANUAL,
        photos: List<String> = emptyList(),
        weightLoss: Double? = null
    ): CommunityPost? {
        val uid = auth.currentUser?.uid ?: return null
        val user = UserProfileRepository.user
        return CommunityPost(
            userId = uid,
            userName = user.name.ifBlank { "Foydalanuvchi" },
            userAvatarUrl = user.avatarUrl,
            title = title,
            description = description,
            type = type,
            photos = photos,
            photoUrl = photos.firstOrNull() ?: "",
            weightLoss = weightLoss,
            verified = true,
            isVisible = true,
            isPremiumUser = PremiumUtils.isPremium(user)
        )
    }

    fun onWeightDropped(previousWeightKg: Double, newWeightKg: Double) {
        val dropped = (previousWeightKg - newWeightKg).roundTo1Decimal()
        if (dropped <= 0.0) return
        if (!isUserVerified()) return

        val uid = auth.currentUser?.uid ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            val logsSnapshot = firestore.userWeightLogsCollection(uid)
                .orderBy(WeightLog::timestamp.name, com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(2)
                .get()
                .await()
            val logs = logsSnapshot.toObjects(WeightLog::class.java).sortByRecent { it.timestamp }
            
            val photos = if (logs.size >= 2) {
                listOf(logs[1].photoUrl, logs[0].photoUrl).filter { it.isNotEmpty() }
            } else if (logs.isNotEmpty()) {
                listOf(logs[0].photoUrl).filter { it.isNotEmpty() }
            } else {
                emptyList()
            }

            fun formatWeight(w: Double): String =
                if (w % 1.0 == 0.0) w.toInt().toString() else String.format(java.util.Locale.US, "%.1f", w)

            val prevStr = formatWeight(previousWeightKg)
            val newStr = formatWeight(newWeightKg)
            val droppedStr = formatWeight(dropped)
            
            val title = "${droppedStr} kg vazn tashlandi! ${prevStr} kg dan ${newStr} kg ga."
            val description = "-${droppedStr} kg ozildi"

            val post = buildPost(
                title = title,
                description = description,
                type = WEIGHT_LOSS,
                photos = photos,
                weightLoss = dropped
            ) ?: return@launch

            CommunityPostRepository.addPost(post)
        }
    }
}
