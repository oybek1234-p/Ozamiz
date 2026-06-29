package uz.oybek.ozamiz.ui.feature.log.photo

import uz.oybek.ozamiz.data.community.AchievementPostHelper

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.logs.WeightLogRepository
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.logs.model.WeightLog
import uz.oybek.ozamiz.data.utils.DataWrapper

data class AddWeightPhotoUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedImageUri: Uri? = null,
    val logAddedSuccessfully: Boolean = false
)

class AddWeightPhotoViewModel(application: Application) : AndroidViewModel(application) {


    private val _uiState = MutableStateFlow(AddWeightPhotoUiState())
    val uiState: StateFlow<AddWeightPhotoUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    fun saveWeightPhotoLog(weightStr: String) {
        val weightKg = weightStr.toDoubleOrNull()
        val imageUri = _uiState.value.selectedImageUri
        val userId = LocalUser.user.uid

        if (userId.isBlank()) {
            _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.error_user_not_found)) }
            return
        }
        if (weightKg == null || weightKg < 30.0 || weightKg > 500.0) {
            _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.error_invalid_weight)) }
            return
        }
        /* Optional photo check removed as per user request */

        _uiState.update { it.copy(isLoading = true, error = null) }
//
        viewModelScope.launch {
            val result = WeightLogRepository.add(userId, weightKg, imageUri, shouldBlur = true)

            when (result) {
                is DataWrapper.Success -> {
                    val newLog = result.data
                    val previousWeight = LocalUser.user.weight
                    val newWeight = weightKg
                    val newAvatarUrl = newLog.photoUrl

                    LocalUser.update {
                        lastWeightLogTimestamp = System.currentTimeMillis()
                        weight = newWeight
                        if (newAvatarUrl.isNotEmpty()) {
                            avatarUrl = newAvatarUrl
                        }

                        if (startWeight == null || startWeight == -1.0) {
                            startWeight = newWeight
                        }
                    }

                    UserRepository.updateUser { userToUpdate ->
                        userToUpdate.lastWeightLogTimestamp = LocalUser.user.lastWeightLogTimestamp
                        userToUpdate.weight = LocalUser.user.weight
                        userToUpdate.startWeight = LocalUser.user.startWeight
                        if (newAvatarUrl.isNotEmpty()) {
                            userToUpdate.avatarUrl = newAvatarUrl
                        }
                    }

                    if (previousWeight > 0.0 && previousWeight != newWeight) {
                        AchievementPostHelper.onWeightDropped(previousWeight, newWeight)
                    }

                    _uiState.update { it.copy(isLoading = false, logAddedSuccessfully = true) }
                }

                is DataWrapper.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error) }
                }

                else -> {
                    _uiState.update { it.copy(isLoading = false, error = "Noma'lum xatolik") }
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