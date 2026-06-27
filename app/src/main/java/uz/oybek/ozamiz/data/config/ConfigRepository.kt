package uz.oybek.ozamiz.data.config

import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.database.database
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.utils.DataWrapper

object ConfigRepository {

    private val rootReference = Firebase.database.reference

    suspend fun getPremiumPrices(): DataWrapper<Pair<Long, Long>> {
        return try {
            val monthlySnapshot = rootReference.child("premiumPrice").get().await()

            val yearlySnapshot = rootReference.child("premiumYearPrice").get().await()

            val monthly = monthlySnapshot.value?.toString()?.replace(" ", "")?.toLongOrNull()
            val yearly = yearlySnapshot.value?.toString()?.replace(" ", "")?.toLongOrNull()

            if (monthly != null && yearly != null) {
                DataWrapper.Success(Pair(monthly, yearly))
            } else {
                DataWrapper.Error("Narx ma'lumotlari topilmadi")
            }
        } catch (e: Exception) {
            Firebase.crashlytics.recordException(e)
            DataWrapper.Error(e.localizedMessage ?: "Xatolik yuz berdi")
        }
    }
}
