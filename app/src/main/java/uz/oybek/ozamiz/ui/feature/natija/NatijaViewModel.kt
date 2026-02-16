package uz.oybek.ozamiz.ui.feature.natija

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.logs.WeightLogRepository
import uz.oybek.ozamiz.data.logs.model.WeightLog
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.utils.DataWrapper

data class NatijaUiState(
    val isLoading: Boolean = false,
    val logs: List<WeightLog> = emptyList(),
    val startWeight: Double? = null,
    val currentWeight: Double? = null,
    val goalWeight: Double? = null,
    val weightDiff: Double = 0.0,
    val weeklyRate: Double = 0.0,
    val photoBefore: String? = null,
    val photoAfter: String? = null,
    val showTopBanner: Boolean = false,
    val showBottomBanner: Boolean = false,
    val estimatedGoalDate: Long? = null,
    val error: String? = null,
    val bmi: Double? = null,
    val bmiCategory: BmiCategory = BmiCategory.NONE
)

enum class BmiCategory {
    UNDERWEIGHT, HEALTHY, OVERWEIGHT, OBESE, NONE
}

class NatijaViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NatijaUiState())
    val uiState = _uiState.asStateFlow()

    fun loadData() {
        val user = LocalUser.user
        if (!user.valid) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            WeightLogRepository.getLogsFlow(user.uid).collect { result ->
                when (result) {
                    is DataWrapper.Success -> {
                        val logs = result.data.sortedBy { it.timestampLong }
                        val validLogs = logs.filter { it.weightKg > 0 }

                        if (validLogs.isNotEmpty()) {
                            val currentUser = LocalUser.user
                            if (currentUser.startWeight == null || (currentUser.startWeight
                                    ?: 0.0) <= 0.0
                            ) {
                                val firstForStart = validLogs.first().weightKg
                                LocalUser.update { startWeight = firstForStart }
                            }
                        }

                        processLogs(logs)
                    }

                    is DataWrapper.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.error) }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun processLogs(logs: List<WeightLog>) {
        val user = LocalUser.user
        val validLogs = logs.filter { it.weightKg > 0 }

        val currentW = if (user.weight > 0) user.weight else validLogs.lastOrNull()?.weightKg
        val startW = if ((user.startWeight
                ?: 0.0) > 0
        ) user.startWeight else validLogs.firstOrNull()?.weightKg

        // Calculate diff (overall progress)
        val diff = if (currentW != null && startW != null) currentW - startW else 0.0

        val targetWeight = user.targetWeight

        // BMI Calculation
        val bmiData = if (currentW != null && user.height > 0) {
            calculateBmi(currentW, user.height)
        } else {
            0.0 to BmiCategory.NONE
        }

        if (validLogs.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    logs = emptyList(),
                    startWeight = startW,
                    currentWeight = currentW,
                    weightDiff = diff,
                    goalWeight = targetWeight,
                    showTopBanner = true,
                    showBottomBanner = false,
                    photoBefore = null,
                    photoAfter = null,
                    bmi = bmiData.first,
                    bmiCategory = bmiData.second
                )
            }
            return
        }

        val lastLog = validLogs.last()

        val photoLogs = logs.filter { it.photoUrl.isNotEmpty() }

        // User wants to compare the last two added photos.
        val lastPhoto = photoLogs.lastOrNull()?.photoUrl
        val firstPhoto = if (photoLogs.size >= 2) photoLogs[photoLogs.size - 2].photoUrl else null

        // Weekly Rate and Prediction
        var weeklyRate = 0.0
        if (validLogs.size >= 2) {
            val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            val logWeekAgo = validLogs.lastOrNull { it.timestampLong < oneWeekAgo }

            if (logWeekAgo != null) {
                weeklyRate = (currentW ?: lastLog.weightKg) - logWeekAgo.weightKg
            } else {
                weeklyRate = (currentW ?: lastLog.weightKg) - validLogs.first().weightKg
            }
        }

        // Prediction Calculation
        // Safe rate: 0.5 - 1 kg per week. Let's use 0.75 as average/optimistic strict plan.
        var predictionDate: Long? = null
        if (currentW != null && targetWeight != null && targetWeight < currentW) {
            val remain = currentW - targetWeight
            val ratePerWeek = 0.75
            val weeksNeeded = remain / ratePerWeek
            val millisNeeded = (weeksNeeded * 7 * 24 * 60 * 60 * 1000).toLong()
            predictionDate = System.currentTimeMillis() + millisNeeded
        }

        // Banner Logic
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        val lastLogTime = lastLog.timestampLong
        val timeSinceLastLog = System.currentTimeMillis() - lastLogTime
        val showTop = timeSinceLastLog > sevenDaysMs
        val showBottom = !showTop

        _uiState.update {
            it.copy(
                isLoading = false,
                logs = logs,
                startWeight = startW,
                currentWeight = currentW,
                goalWeight = targetWeight,
                weightDiff = diff,
                weeklyRate = weeklyRate,
                photoBefore = firstPhoto,
                photoAfter = lastPhoto,
                showTopBanner = showTop,
                showBottomBanner = showBottom,
                estimatedGoalDate = predictionDate,
                error = null,
                bmi = bmiData.first,
                bmiCategory = bmiData.second
            )
        }
    }

    private fun calculateBmi(weight: Double, heightCm: Int): Pair<Double, BmiCategory> {
        if (weight <= 0 || heightCm <= 0) return 0.0 to BmiCategory.NONE

        val heightM = heightCm / 100.0
        val bmi = weight / (heightM * heightM)

        val category = when {
            bmi < 18.5 -> BmiCategory.UNDERWEIGHT
            bmi < 25.0 -> BmiCategory.HEALTHY
            bmi < 30.0 -> BmiCategory.OVERWEIGHT
            else -> BmiCategory.OBESE
        }

        return bmi to category
    }
}

