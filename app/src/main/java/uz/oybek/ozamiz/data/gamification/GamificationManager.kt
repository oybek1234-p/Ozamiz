// C:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\data\gamification\GamificationManager.kt
package uz.oybek.ozamiz.data.gamification

import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.info.MealsType

class GamificationManager {

    /**
     * Ovqat kiritilgandan so'ng ballarni hisoblaydi va yangi nishon ochilganligini tekshiradi.
     * @param currentUser Foydalanuvchining joriy holati.
     * @param mealType Kiritilgan ovqat turi (masalan, "breakfast", "lunch").
     * @param isHealthy AI tomonidan ovqat sog'lom deb baholanganmi.
     * @return Pair<Int, Badge?>: (qo'shilgan/olingan ballar, agar ochilgan bo'lsa yangi nishon).
     */
    fun calculatePointsAndCheckBadges(currentUser: User, mealType: String, isHealthy: Boolean): Pair<Int, Badge?> {
        var pointsChange = 0

        if (isHealthy) {
            pointsChange += if (mealType == MealsType.Snack.id) {
                GamificationConstants.POINTS_HEALTHY_SNACK
            } else {
                GamificationConstants.POINTS_HEALTHY_MEAL
            }
        } else {
            pointsChange += GamificationConstants.POINTS_UNHEALTHY_MEAL
            // Kunlik nosog'lom ovqatlar sonini tekshirish uchun qo'shimcha logika kerak bo'ladi
            // Hozircha bu soddalashtirilgan
        }

        val newTotalPoints = currentUser.scorePoints + pointsChange
        var newBadgeUnlocked: Badge? = null

        // Nishonlarni tekshirish
        GamificationConstants.BADGES_LIST.forEach { badge ->
            if (newTotalPoints >= badge.requiredPoints && !currentUser.unlockedBadgeIds.contains(badge.id)) {
                // Foydalanuvchi bu nishonni hali ochmagan va yetarli ball to'plagan
                newBadgeUnlocked = badge.copy(isUnlocked = true)
                // Agar bir nechta nishon birdaniga ochilsa, oxirgisini qaytaramiz (yoki list qaytarish mumkin)
            }
        }
        return Pair(pointsChange, newBadgeUnlocked)
    }

    /**
     * Foydalanuvchining joriy darajasini va keyingi nishongacha qolgan ballarni aniqlaydi.
     * @param userScore Foydalanuvchining joriy ballari.
     * @param unlockedBadgeIds Ochilgan nishonlar ID lari.
     * @return Triple<Badge?, Int, Badge?>: (joriy nishon (daraja), keyingi nishongacha ball, keyingi nishon).
     *         Agar hech qanday nishon ochilmagan bo'lsa, joriy nishon null bo'ladi.
     *         Agar barcha nishonlar ochilgan bo'lsa, keyingi nishon va ball null/0 bo'ladi.
     */
    fun getCurrentLevelInfo(userScore: Int, unlockedBadgeIds: List<String>): Triple<Badge?, Int, Badge?> {
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

    fun getBadgeById(badgeId: String): Badge? {
        return GamificationConstants.BADGES_LIST.find { it.id == badgeId }
    }

    fun getAllBadgesWithUnlockStatus(unlockedIds: List<String>): List<Badge> {
        return GamificationConstants.BADGES_LIST.map { badge ->
            badge.copy(isUnlocked = unlockedIds.contains(badge.id))
        }.sortedBy { it.requiredPoints }
    }
}