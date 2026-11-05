package uz.oybek.ozamiz.ui.feature.log.photo

import android.app.Application
import android.net.Uri
import androidx.annotation.Keep
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.common.util.orZero
import uz.oybek.ozamiz.data.community.api.CommunityPostRepository
import uz.oybek.ozamiz.data.community.model.PostType
import uz.oybek.ozamiz.data.community.model.request.CreateGeneralPostRequest
import uz.oybek.ozamiz.data.profile.api.ProfileManagerRepo
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import uz.oybek.ozamiz.data.tracker.weight.api.WeightTrackerRepo
import uz.oybek.ozamiz.data.tracker.weight.model.AddWeightRequest
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter
import uz.oybek.ozamiz.ui.utils.FaceBlurer
import javax.inject.Inject

@Keep
data class AddWeightPhotoUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedImageUri: Uri? = null,
    val logAddedSuccessfully: Boolean = false,
    val lastWeightChangeKg: Double = 0.0,
    val lastMeasurementChangeCm: Double = 0.0,
    val photoBeforeUrl: String? = null,
    val autoPostSubmitted: Boolean = false,
    val startWeightKg: Double = 0.0,
    val currentWeightKg: Double = 0.0,
    val daysCount: Int = 1
)

@HiltViewModel
class AddWeightPhotoViewModel @Inject constructor(
    application: Application,
    private val weightTrackerRepo: WeightTrackerRepo,
    private val profileRepo: ProfileRepo,
    private val profileManagerRepo: ProfileManagerRepo,
    private val communityPostRepository: CommunityPostRepository,
    private val faceBlurer: FaceBlurer
) : AndroidViewModel(application) {

    val isGainGoal: Boolean
        get() {
            val user = profileRepo.currentUser
            if (user.userGoal.isGain) return true
            val targetW = user.targetWeight ?: 0.0
            val currentW = if (user.weight > 0) user.weight else 70.0
            return targetW > 0.0 && targetW > currentW
        }

    val totalWeightChangeKg: Double
        get() {
            val user = profileRepo.currentUser
            val current = if (user.weight > 0) user.weight else 70.0
            val start = user.startWeight ?: current
            return current - start
        }

    private val _uiState = MutableStateFlow(AddWeightPhotoUiState())
    val uiState: StateFlow<AddWeightPhotoUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    fun saveWeightPhotoLog(
        weightStr: String,
        bellyStr: String = "",
        hipsStr: String = "",
        armStr: String = ""
    ) {
        val weightKg = weightStr.toDoubleOrNull()
        val bellyCm = bellyStr.toDoubleOrNull()
        val hipsCm = hipsStr.toDoubleOrNull()
        val armCm = armStr.toDoubleOrNull()
        val imageUri = _uiState.value.selectedImageUri
        val userId = profileRepo.userId

        if (userId.isBlank()) {
            _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.error_user_not_found)) }
            return
        }
        if (weightKg == null || weightKg < 20.0 || weightKg > 500.0) {
            _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.error_invalid_weight)) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val userBefore = profileRepo.currentUser
            val oldWeight = if (userBefore.weight > 0) userBefore.weight else (userBefore.startWeight ?: weightKg)

            val previousLogsResult = weightTrackerRepo.getWeightLogs(limit = 2)
            val previousLogs = if (previousLogsResult is BaseResult.Success) previousLogsResult.data else emptyList()
            val previousLog = previousLogs.firstOrNull()

            val baseWeight = if (previousLog != null && previousLog.weightKg > 0) previousLog.weightKg else oldWeight

            val weightLogsForPhotoResult = weightTrackerRepo.getWeightLogs(limit = 100)
            val allLogs = (weightLogsForPhotoResult as? BaseResult.Success)?.data.orEmpty()
            val priorLogsWithPhoto = allLogs
                .filter { it.photoUrl.isNotBlank() }
                .sortedBy { it.timestampLong }

            // Genuine before photo: the first (eng boshidagi) photo log, or user's existing avatar
            val genuineBeforePhoto = priorLogsWithPhoto.firstOrNull()?.photoUrl
                ?: userBefore.avatarUrl?.takeIf { it.isNotBlank() }

            val request = AddWeightRequest(
                weightKg = weightKg,
                bellyCm = bellyCm,
                hipsCm = hipsCm,
                armCm = armCm,
                imageUri = imageUri,
                shouldBlur = true
            )
            val result = weightTrackerRepo.addWeightLog(request)

            when (result) {
                is BaseResult.Success -> {
                    val weightDiff = weightKg - baseWeight

                    val prevBelly = previousLog?.bellyCm ?: 0.0
                    val prevHips = previousLog?.hipsCm ?: 0.0
                    val prevArm = previousLog?.armCm ?: 0.0
                    val prevSum = prevBelly + prevHips + prevArm

                    val currBelly = bellyCm ?: 0.0
                    val currHips = hipsCm ?: 0.0
                    val currArm = armCm ?: 0.0
                    val currSum = currBelly + currHips + currArm

                    val measurementDiff = if (prevSum > 0 && currSum > 0) currSum - prevSum else 0.0

                    profileManagerRepo.updateUser { user ->
                        user.apply {
                            lastWeightLogTimestamp = System.currentTimeMillis()
                            weight = weightKg
                            if (startWeight == null || startWeight.orZero() <= 0.0) {
                                startWeight = weightKg
                            }
                        }
                    }

                    val startW = userBefore.startWeight?.takeIf { it > 0 } ?: baseWeight
                    val currentW = weightKg
                    val totalDiff = currentW - startW
                    val startWeightStr = NutritionFormatter.kg(startW)
                    val currentWeightStr = NutritionFormatter.kg(currentW)
                    val totalChangeStr = NutritionFormatter.kgChange(totalDiff)

                    val firstLogTime = priorLogsWithPhoto.firstOrNull()?.timestampLong
                        ?: allLogs.minOfOrNull { it.timestampLong }
                        ?: userBefore.registerDate
                    val daysCount = (((System.currentTimeMillis() - firstLogTime) / (1000L * 60 * 60 * 24)).toInt() + 1).coerceAtLeast(1)

                    val hasProgress = if (isGainGoal) weightDiff > 0.05 else (weightDiff < -0.05 || measurementDiff < -0.05)
                    var autoPostSent = false

                    if (imageUri != null && hasProgress) {
                        try {
                            val context = getApplication<Application>()
                            val blurredUri = faceBlurer.blurFace(context, imageUri)
                            val descRes = if (isGainGoal) {
                                R.string.weight_success_share_desc_gain_fmt
                            } else {
                                R.string.weight_success_share_desc_loss_fmt
                            }
                            val autoDesc = context.getString(
                                descRes,
                                daysCount,
                                totalChangeStr,
                                startWeightStr,
                                currentWeightStr
                            )

                            val beforeUri = genuineBeforePhoto?.takeIf { it.isNotBlank() }?.toUri()

                            val createRequest = CreateGeneralPostRequest(
                                title = null,
                                description = autoDesc,
                                imageUri = beforeUri ?: blurredUri,
                                secondaryImageUri = if (beforeUri != null) blurredUri else null,
                                postType = PostType.WEIGHT_LOSS
                            )

                            communityPostRepository.createGeneralPost(createRequest)
                            autoPostSent = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            logAddedSuccessfully = true,
                            lastWeightChangeKg = weightDiff,
                            lastMeasurementChangeCm = measurementDiff,
                            photoBeforeUrl = genuineBeforePhoto,
                            autoPostSubmitted = autoPostSent,
                            startWeightKg = startW,
                            currentWeightKg = currentW,
                            daysCount = daysCount
                        )
                    }
                }

                is BaseResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error?.message ?: "Xatolik") }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetSuccessFlag() {
        _uiState.update { it.copy(logAddedSuccessfully = false) }
    }
}
