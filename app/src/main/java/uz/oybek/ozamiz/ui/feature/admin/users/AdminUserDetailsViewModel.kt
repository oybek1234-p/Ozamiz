package uz.oybek.ozamiz.ui.feature.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.profile.model.User
import uz.oybek.ozamiz.data.user.api.AdminUserRepo
import javax.inject.Inject

@HiltViewModel
class AdminUserDetailsViewModel @Inject constructor(
    private val adminUserRepo: AdminUserRepo
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _recentFoodLog = MutableStateFlow<FoodLog?>(null)
    val recentFoodLog: StateFlow<FoodLog?> = _recentFoodLog.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = adminUserRepo.getAdminUser(userId)) {
                is BaseResult.Success -> {
                    _user.value = result.data
                }
                is BaseResult.Failure -> {
                    _user.value = null
                }
            }
            _isLoading.value = false
        }
    }

    fun saveAllFields(userId: String, updates: Map<String, Any?>) {
        if (userId.isBlank() || updates.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            when (adminUserRepo.updateAdminUser(userId, updates)) {
                is BaseResult.Success -> {
                    loadUser(userId)
                }
                is BaseResult.Failure -> {
                    _isLoading.value = false
                }
            }
        }
    }

    fun deleteUser(userId: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        if (userId.isBlank()) {
            onError(IllegalArgumentException("Foydalanuvchi ID si bo'sh"))
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = adminUserRepo.deleteUserAdmin(userId)) {
                is BaseResult.Success -> {
                    _isLoading.value = false
                    onSuccess()
                }
                is BaseResult.Failure -> {
                    _isLoading.value = false
                    onError(result.error ?: Exception("Delete failed"))
                }
            }
        }
    }
}
