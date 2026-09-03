package uz.oybek.ozamiz.data.premium

import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.database
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.recordException

object PremiumRepository {

    private val rootReference = database.reference

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
            recordException(e)
            DataWrapper.Error("")
        }

    }
}