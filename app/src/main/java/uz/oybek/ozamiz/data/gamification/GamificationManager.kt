package uz.oybek.ozamiz.data.gamification

class GamificationManager {

    fun getCurrentLevelInfo(
        userScore: Int, unlockedBadgeIds: List<String>
    ): Triple<Badge?, Int, Badge?> {
        val badges = GamificationConstants.BADGES_LIST.sortedBy { it.requiredPoints }
        var currentBadge: Badge? = null
        var nextBadge: Badge? = null
        var pointsToNextBadge = 0

        for (badge in badges) {
            if (unlockedBadgeIds.contains(badge.id)) {
                currentBadge = badge
            } else {
                // Bu birinchi ochilmagan nishon
                nextBadge = badge
                pointsToNextBadge = (badge.requiredPoints - userScore).coerceAtLeast(0)
                break
            }
        }
        // Agar currentBadge null bo'lsa (hech qanday nishon ochilmagan) va birinchi nishon mavjud bo'lsa
        if (currentBadge == null && badges.isNotEmpty()) {
            nextBadge = badges.first()
            pointsToNextBadge = (nextBadge.requiredPoints - userScore).coerceAtLeast(0)
        }


        return Triple(currentBadge, pointsToNextBadge, nextBadge)
    }

    fun getAllBadgesWithUnlockStatus(unlockedIds: List<String>): List<Badge> {
        return GamificationConstants.BADGES_LIST.map { badge ->
            badge.copy(isUnlocked = unlockedIds.contains(badge.id))
        }.sortedBy { it.requiredPoints }
    }
}