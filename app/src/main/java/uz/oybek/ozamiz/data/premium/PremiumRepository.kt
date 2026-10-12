package uz.oybek.ozamiz.data.premium

import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.firebase.database
import uz.oybek.ozamiz.util.recordException

object PremiumRepository {

    private val rootReference = database.reference

    suspend fun getPremiumPrices(): BaseResult<Pair<Long, Long>, String> {
        return try {
            val monthlySnapshot = rootReference.child("premiumPrice").get().await()

            val yearlySnapshot = rootReference.child("premiumYearPrice").get().await()

            val monthly = monthlySnapshot.value?.toString()?.replace(" ", "")?.toLongOrNull()
            val yearly = yearlySnapshot.value?.toString()?.replace(" ", "")?.toLongOrNull()

            if (monthly != null && yearly != null) {
                BaseResult.Success(Pair(monthly, yearly))
            } else {
                BaseResult.Failure("Narx ma'lumotlari topilmadi")
            }
        } catch (e: Exception) {
            recordException(e)
            BaseResult.Failure(e.message ?: "")
        }

    }
}