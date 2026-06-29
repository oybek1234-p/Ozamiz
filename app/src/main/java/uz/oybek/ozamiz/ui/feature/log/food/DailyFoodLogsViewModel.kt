package uz.oybek.ozamiz.ui.feature.log.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import uz.oybek.ozamiz.data.logs.FoodLogRepository
import uz.oybek.ozamiz.data.logs.model.FoodLog
import java.util.Date

class DailyFoodLogsViewModel : ViewModel() {
    fun getFoodLogsPaged(date: Date): Flow<PagingData<FoodLog>> {
        return FoodLogRepository.foodLogsPaged(date).cachedIn(viewModelScope)
    }
}
