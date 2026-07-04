package uz.oybek.ozamiz.ui.feature.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.user.UserRepository

class AdminUserDetailsViewModel : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _recentFoodLog = MutableStateFlow<FoodLog?>(null)
    val recentFoodLog: StateFlow<FoodLog?> = _recentFoodLog.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val loadedUser = UserRepository.getAdminUser(userId)
                _user.value = loadedUser
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveAllFields(userId: String, updates: Map<String, Any?>) {
        if (userId.isBlank() || updates.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                UserRepository.updateAdminUser(userId, updates)
                loadUser(userId)
            } catch (e: Exception) {
                android.util.Log.e("AdminUserDetailsVM", "saveAllFields error for user $userId", e)
            } finally {
                _isLoading.value = false
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
            try {
                UserRepository.deleteUserAdmin(userId)
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("AdminUserDetailsVM", "deleteUser error for user $userId", e)
                onError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
