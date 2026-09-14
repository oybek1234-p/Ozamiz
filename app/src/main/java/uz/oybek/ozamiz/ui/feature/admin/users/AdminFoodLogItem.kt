package uz.oybek.ozamiz.ui.feature.admin.users

import androidx.annotation.Keep
import uz.oybek.ozamiz.data.logs.model.FoodLog

@Keep
data class AdminFoodLogItem(
    val dateString: String,
    val foodLog: FoodLog
)
