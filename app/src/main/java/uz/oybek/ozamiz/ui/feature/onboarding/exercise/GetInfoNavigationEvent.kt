package uz.oybek.ozamiz.ui.feature.onboarding.exercise

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.food.DietController
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.model.info.Gender

sealed class GetInfoNavigationEvent {
    data class ShowToast(val message: String) : GetInfoNavigationEvent()
    object NavigateToCongratulation : GetInfoNavigationEvent()
    object NavigateToHome : GetInfoNavigationEvent()
}

class GetInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _idealWeightRangeText = MutableLiveData<String?>(null)
    val idealWeightRangeText: LiveData<String?> = _idealWeightRangeText

    private val _navigationEvent = MutableLiveData<GetInfoNavigationEvent>()
    val navigationEvent: LiveData<GetInfoNavigationEvent> = _navigationEvent

    private var isNewUserSetup: Boolean = true

    fun setSetupMode(isNew: Boolean) {
        isNewUserSetup = isNew
    }

    fun calculateAndSetIdealWeight(heightCm: Int?, gender: Int) {
        if (heightCm != null && heightCm > 0 && (gender == Gender.MALE || gender == Gender.FEMALE)) {
            val idealWeight = DietController.getIdealWeight(heightCm.toDouble())

            _idealWeightRangeText.postValue(
                String.format(
                    "%.0f kg - %.0f kg",
                    idealWeight.first.toDouble(),
                    idealWeight.second.toDouble()
                )
            )
        } else {
            _idealWeightRangeText.postValue(null)
        }
    }

    fun saveUserInfoAndGeneratePlan(
        name: String,
        gender: Int,
        age: Int,
        height: Int,
        weight: Double,
        targetWeight: Double?,
        problems: List<String>?,
        phoneNumber: String,
        activityLevel: String,
        manzil: String?
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val existingUser = LocalUser.user
                val userToUpdate = existingUser.copy(
                    name = name,
                    gender = gender,
                    age = age,
                    height = height,
                    weight = weight,
                    targetWeight = targetWeight,
                    problems = problems ?: emptyList(),
                    phoneNumber = phoneNumber,
                    activityLevel = activityLevel,
                    registerDate = existingUser.registerDate
                )

                // Centralized calculation for calories, macros, and water
                DietController.updateTargetValues(userToUpdate)

                UserProfileRepository.updateUser {
                    this.name = userToUpdate.name
                    this.gender = userToUpdate.gender
                    this.age = userToUpdate.age
                    this.height = userToUpdate.height
                    this.weight = userToUpdate.weight
                    this.targetWeight = userToUpdate.targetWeight
                    this.startWeight = userToUpdate.startWeight
                    this.problems = userToUpdate.problems
                    this.targetCalories = userToUpdate.targetCalories
                    this.targetProtein = userToUpdate.targetProtein
                    this.targetFat = userToUpdate.targetFat
                    this.targetCarbs = userToUpdate.targetCarbs
                    this.targetWaterMl = userToUpdate.targetWaterMl
                    this.phoneNumber = userToUpdate.phoneNumber
                    this.activityLevel = userToUpdate.activityLevel
                    this.region = userToUpdate.region
                }
                _navigationEvent.postValue(GetInfoNavigationEvent.NavigateToHome)
            } catch (e: Exception) {
                _navigationEvent.postValue(GetInfoNavigationEvent.ShowToast("Xatolik yuz berdi: ${e.message}"))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}