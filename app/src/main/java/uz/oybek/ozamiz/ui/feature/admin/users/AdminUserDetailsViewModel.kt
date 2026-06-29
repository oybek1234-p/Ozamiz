package uz.oybek.ozamiz.ui.feature.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
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
                // Fetch user Profile
                val loadedUser = UserRepository.getAdminUser(userId)
                _user.value = loadedUser
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun saveUserEdits(
        userId: String,
        weight: Double,
        targetWeight: Double,
        height: Int,
        age: Int,
        premium: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = _user.value
                val newPremiumDate = if (currentUser != null && currentUser.premium != premium) {
                    if (premium) System.currentTimeMillis() else 0L
                } else {
                    currentUser?.premiumDate ?: (if (premium) System.currentTimeMillis() else 0L)
                }

                val updates = mapOf(
                    "weight" to weight,
                    "targetWeight" to targetWeight,
                    "height" to height,
                    "age" to age,
                    "premium" to premium,
                    "premiumDate" to newPremiumDate
                )
                UserRepository.updateAdminUser(userId, updates)
                // Reload user to reflect changes
                loadUser(userId)
            } catch (e: Exception) {
                // Error handling...
            } finally {
                _isLoading.value = false
            }
        }
    }
}
