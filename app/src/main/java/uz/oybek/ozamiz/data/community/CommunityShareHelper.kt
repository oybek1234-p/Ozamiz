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
        // Disabled per user directive: Food logs are no longer posted to community feed
        return
    }
}
