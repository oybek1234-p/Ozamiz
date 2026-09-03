package uz.oybek.ozamiz.data.food.limit

import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.config.AppPreference
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.databinding.BottomSheetLimitReachedBinding
import uz.oybek.ozamiz.ui.feature.premium.isPremium

object FoodLimitManager {

    const val FREE_DAILY_AI_LIMIT = 3
    const val FREE_DAILY_AI_CHAT_LIMIT = 3
    const val PREMIUM_DAILY_AI_CHAT_LIMIT = 30

    /**
     * Base daily food limit for non-premium user:
     * - Free user: 3 foods per day.
     * - Premium user: Int.MAX_VALUE (unlimited)
     */
    fun getBaseDailyLimit(user: User = UserProfileRepository.user): Int {
        if (user.isPremium()) return Int.MAX_VALUE
        return FREE_DAILY_AI_LIMIT
    }

    /**
     * Today's logged food count.
     */
    fun getTodayLoggedCount(): Int {
        return AppPreference.getFoodLogLimitCount()
    }

    /**
     * Remaining food logs allowed for today.
     */
    fun getRemainingLogs(user: User = UserProfileRepository.user): Int {
        if (user.isPremium()) return Int.MAX_VALUE
        val logged = getTodayLoggedCount()
        return (FREE_DAILY_AI_LIMIT - logged).coerceAtLeast(0)
    }

    /**
     * Returns true if user can add food via AI right now.
     */
    fun canLogFood(user: User = UserProfileRepository.user): Boolean {
        if (user.isPremium()) return true
        return getRemainingLogs(user) > 0
    }

    /**
     * Base daily AI chat messages limit:
     * - Free user: 3 messages per day.
     * - Premium user: 30 messages per day.
     */
    fun getMaxDailyChatMessages(user: User = UserProfileRepository.user): Int {
        return if (user.isPremium()) PREMIUM_DAILY_AI_CHAT_LIMIT else FREE_DAILY_AI_CHAT_LIMIT
    }

    /**
     * Today's AI chat message count.
     */
    fun getTodayChatMessageCount(): Int {
        return AppPreference.getAiChatLimitCount()
    }

    /**
     * Remaining AI chat messages allowed for today.
     */
    fun getRemainingChatMessages(user: User = UserProfileRepository.user): Int {
        val max = getMaxDailyChatMessages(user)
        val used = getTodayChatMessageCount()
        return maxOf(0, max - used)
    }

    /**
     * Returns true if user can send AI chat message right now.
     */
    fun canSendChatMessage(user: User = UserProfileRepository.user): Boolean {
        return getRemainingChatMessages(user) > 0
    }

    /**
     * Centralized method to check limit before adding food.
     * If limit reached: shows limit reached BottomSheet with 2 options (Ro'yxatdan kiritish / Buy Premium).
     * If limit available: calls [onProceed].
     */
    fun checkLimitAndProceed(
        activity: FragmentActivity,
        onProceed: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        val user = UserProfileRepository.user
        val can = canLogFood(user)
        if (can) {
            onProceed()
        } else {
            showLimitReachedSheet(activity)
        }
    }

    /**
     * Displays the Limit Reached Bottom Sheet (2 options):
     * 1. Ro'yxatdan kiritish (Qidiruv orqali kiritish)
     * 2. Premium olish (Cheksiz imkoniyat)
     */
    fun showLimitReachedSheet(
        activity: FragmentActivity,
        title: String? = null,
        message: String? = null,
        onManualSearchProceed: (() -> Unit)? = null
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        val dialog = BottomSheetDialog(activity)
        val binding = BottomSheetLimitReachedBinding.inflate(activity.layoutInflater)
        dialog.setContentView(binding.root)

        binding.textLimitTitle.text = title ?: activity.getString(R.string.daily_food_limit_title)
        binding.textLimitMessage.text = message ?: activity.getString(R.string.daily_food_limit_msg)

        binding.btnManualSearch.setOnClickListener {
            try {
                if (dialog.isShowing) dialog.dismiss()
            } catch (e: Exception) {
                // ignore
            }
            if (activity.isFinishing || activity.isDestroyed) return@setOnClickListener

            if (onManualSearchProceed != null) {
                onManualSearchProceed()
            } else {
                val navHostFragment = activity.supportFragmentManager.findFragmentById(R.id.container) as? NavHostFragment
                try {
                    navHostFragment?.navController?.navigate(R.id.searchFoodFragment)
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        binding.btnBuyPremium.setOnClickListener {
            try {
                if (dialog.isShowing) dialog.dismiss()
            } catch (e: Exception) {
                // ignore
            }
            if (activity.isFinishing || activity.isDestroyed) return@setOnClickListener
            val navHostFragment = activity.supportFragmentManager.findFragmentById(R.id.container) as? NavHostFragment
            try {
                navHostFragment?.navController?.navigate(R.id.premiumFragment)
            } catch (e: Exception) {
                // ignore
            }
        }

        if (!activity.isFinishing && !activity.isDestroyed) {
            dialog.show()
        }
    }
}
