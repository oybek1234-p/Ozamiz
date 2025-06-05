package uz.oybek.ozamiz.ui.log_photo

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.ImageUploader // ImageUploader importi
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.UserRepository
import uz.oybek.ozamiz.data.logs.WeightPhotoLogRepository
import uz.oybek.ozamiz.data.model.WeightPhotoLog
import java.util.Date
import java.util.UUID

data class AddWeightPhotoUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedImageUri: Uri? = null,
    val logAddedSuccessfully: Boolean = false
)

class AddWeightPhotoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeightPhotoLogRepository()

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
        if (weightKg == null || weightKg <= 0) {
            _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.error_invalid_weight_input)) }
            return
        }
        if (imageUri == null) {
            _uiState.update { it.copy(error = getApplication<Application>().getString(R.string.error_photo_required)) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            // Repository'ga addLog funksiyasini chaqirish
            val result = repository.addLog(userId, weightKg, imageUri)

            when(result) {
                is DataWrapper.Success -> {
                    // Muvaffaqiyatli saqlangandan so'ng LocalUserni yangilash
                    val newLog = result.data
                    LocalUser.user = LocalUser.user.copy(
                        lastWeightLogTimestamp = newLog.timestamp?.time ?: System.currentTimeMillis(),
                        avatarUrl = newLog.photoUrl // Profil rasmini yangilash
                    )
                    LocalUser.saveUser()

                    // Firebase'da foydalanuvchi ma'lumotlarini yangilash (agar kerak bo'lsa)
                    // Bu UserRepository orqali amalga oshirilishi mumkin
                    UserRepository.updateUser { userToUpdate ->
                        userToUpdate.lastWeightLogTimestamp = LocalUser.user.lastWeightLogTimestamp
                        userToUpdate.avatarUrl = LocalUser.user.avatarUrl
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