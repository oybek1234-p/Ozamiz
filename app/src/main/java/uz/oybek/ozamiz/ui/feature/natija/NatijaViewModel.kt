package uz.oybek.ozamiz.ui.feature.natija

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.common.util.orZero
import uz.oybek.ozamiz.data.profile.api.ProfileManagerRepo
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import uz.oybek.ozamiz.data.tracker.weight.api.WeightTrackerRepo
import uz.oybek.ozamiz.data.tracker.weight.model.WeightLog
import uz.oybek.ozamiz.data.tracker.weight.model.WeightTrackerSummary
import javax.inject.Inject

@Keep
enum class BmiCategory {
    UNDERWEIGHT, HEALTHY, OVERWEIGHT, OBESE, NONE
}

@Keep
data class NatijaWeightState(
    val startWeight: Double? = null,
    val currentWeight: Double? = null,
    val goalWeight: Double? = null
)

@Keep
data class NatijaPhotosState(
    val photoBefore: String? = null,
    val photoAfter: String? = null
)

@Keep
data class NatijaUiState(
    val isLoading: Boolean = true,
    val isGainGoal: Boolean = false,
    val weightState: NatijaWeightState = NatijaWeightState(),
    val photosState: NatijaPhotosState = NatijaPhotosState(),
    val weightSummary: WeightTrackerSummary? = null,
    val error: String? = null
) {
    val startWeight: Double? get() = weightState.startWeight
    val currentWeight: Double? get() = weightState.currentWeight
    val goalWeight: Double? get() = weightState.goalWeight
    val photoBefore: String? get() = photosState.photoBefore
    val photoAfter: String? get() = photosState.photoAfter
}

@HiltViewModel
class NatijaViewModel @Inject constructor(
    private val weightTrackerRepo: WeightTrackerRepo,
    private val profileRepo: ProfileRepo,
    private val profileManagerRepo: ProfileManagerRepo
) : ViewModel() {

    private fun getInitialIsGainGoal(): Boolean {
        val user = profileRepo.currentUser
        if (user.goal.isNotBlank()) {
            return user.goal == uz.oybek.ozamiz.data.profile.model.UserGoal.GAIN_WEIGHT.key
        }
        val targetW = user.targetWeight.orZero()
        val currentW = if (user.weight > 0) user.weight else 70.0
        return targetW > 0.0 && targetW > currentW
    }

    private val _uiState = MutableStateFlow(
        NatijaUiState(
            isGainGoal = getInitialIsGainGoal()
        )
    )
    val uiState = _uiState.asStateFlow()

    fun loadData(isRefresh: Boolean = false) {
        val user = profileRepo.currentUser
        if (!user.valid) return

        if (isRefresh) {
            _uiState.update { it.copy(isLoading = true, error = null) }
        }

        viewModelScope.launch {
            fetchFullSummary()
        }

        viewModelScope.launch {
            weightTrackerRepo.getWeightLogsFlow().collect { result ->
                if (result is BaseResult.Success) {
                    val logs = result.data.sortedBy { it.timestampLong }
                    val validLogs = logs.filter { it.weightKg > 0 }

                    if (validLogs.isNotEmpty()) {
                        val currentUser = profileRepo.currentUser
                        if (currentUser.startWeight == null || (currentUser.startWeight.orZero()) <= 0.0) {
                            val firstForStart = validLogs.first().weightKg
                            profileManagerRepo.updateUser {
                                it.apply { startWeight = firstForStart }
                            }
                        }
                    }

                    updateWeightAndPhotosState(logs)
                } else if (result is BaseResult.Failure) {
                    _uiState.update { it.copy(isLoading = false, error = result.error?.message ?: "Xatolik") }
                }
            }
        }
    }

    private suspend fun fetchFullSummary() {
        val user = profileRepo.currentUser
        when (val summaryResult = weightTrackerRepo.getWeightSummary(user.targetWeight)) {
            is BaseResult.Success -> {
                val summary = summaryResult.data
                val startW = if ((user.startWeight.orZero()) > 0) user.startWeight else summary.weight.startValue
                val currentW = summary.weight.currentValue ?: (if (user.weight > 0) user.weight else null)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        weightSummary = summary,
                        weightState = NatijaWeightState(
                            startWeight = startW,
                            currentWeight = currentW,
                            goalWeight = user.targetWeight
                        ),
                        photosState = NatijaPhotosState(
                            photoBefore = summary.photos.photoBeforeUrl ?: it.photosState.photoBefore,
                            photoAfter = summary.photos.photoAfterUrl ?: it.photosState.photoAfter
                        )
                    )
                }
            }
            is BaseResult.Failure -> {
                _uiState.update {
                    it.copy(isLoading = false, error = summaryResult.error?.message ?: "Xatolik")
                }
            }
        }
    }

    private fun updateWeightAndPhotosState(logs: List<WeightLog>) {
        val user = profileRepo.currentUser
        val validLogs = logs.filter { it.weightKg > 0 }

        val currentW = validLogs.lastOrNull()?.weightKg ?: (if (user.weight > 0) user.weight else null)
        val startW = (if ((user.startWeight.orZero()) > 0) user.startWeight else null) ?: validLogs.firstOrNull()?.weightKg

        val photoLogsWithUrl = logs.filter { it.photoUrl.isNotBlank() }
        val beforePhoto = photoLogsWithUrl.firstOrNull()?.photoUrl
        val afterPhoto = if (photoLogsWithUrl.size >= 2) {
            photoLogsWithUrl.lastOrNull()?.photoUrl
        } else {
            null
        }

        _uiState.update { current ->
            val fastSummary = current.weightSummary ?: weightTrackerRepo.calculateSummary(logs, user.targetWeight, emptyList())
            current.copy(
                isLoading = false,
                weightSummary = fastSummary,
                weightState = NatijaWeightState(
                    startWeight = startW,
                    currentWeight = currentW,
                    goalWeight = user.targetWeight
                ),
                photosState = NatijaPhotosState(
                    photoBefore = beforePhoto ?: current.photosState.photoBefore,
                    photoAfter = afterPhoto ?: current.photosState.photoAfter
                )
            )
        }
    }
}
