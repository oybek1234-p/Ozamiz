package uz.oybek.ozamiz.ui.feature.onboarding.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.diet.DietEngine
import uz.oybek.ozamiz.data.profile.api.ProfileManagerRepo
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import uz.oybek.ozamiz.data.profile.model.User
import uz.oybek.ozamiz.data.profile.model.UserGoal
import uz.oybek.ozamiz.data.user.model.info.Gender
import javax.inject.Inject

sealed class GetInfoNavigationEvent {
    data class ShowToast(val message: String) : GetInfoNavigationEvent()
    object NavigateToCongratulation : GetInfoNavigationEvent()
    object NavigateToHome : GetInfoNavigationEvent()
}

@HiltViewModel
class GetInfoViewModel @Inject constructor(
    private val profileRepo: ProfileRepo,
    private val profileManagerRepo: ProfileManagerRepo,
    private val dietEngine: DietEngine
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _idealWeightRangeText = MutableStateFlow<String?>(null)
    val idealWeightRangeText: StateFlow<String?> = _idealWeightRangeText.asStateFlow()

    private val _navigationEvent = Channel<GetInfoNavigationEvent>(Channel.BUFFERED)
    val navigationEvent: Flow<GetInfoNavigationEvent> = _navigationEvent.receiveAsFlow()

    val user: User
        get() = profileRepo.currentUser

    private var isNewUserSetup: Boolean = true

    fun setSetupMode(isNew: Boolean) {
        isNewUserSetup = isNew
    }

    fun calculateAndSetIdealWeight(heightCm: Int?, gender: Int) {
        if (heightCm != null && heightCm > 0 && (gender == Gender.MALE || gender == Gender.FEMALE)) {
            val idealWeight = dietEngine.getIdealWeight(heightCm.toDouble())

            _idealWeightRangeText.value = String.format(
                "%.0f kg - %.0f kg",
                idealWeight.first.toDouble(),
                idealWeight.second.toDouble()
            )
        } else {
            _idealWeightRangeText.value = null
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
        manzil: String?,
        goal: String = UserGoal.LOSE_WEIGHT.key,
        targetCalories: Int = 0,
        targetProtein: Int = 0,
        targetFat: Int = 0,
        targetCarbs: Int = 0
    ) {
        if (_isLoading.value) return
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val existingUser = profileRepo.currentUser
                val userToUpdate = existingUser.copy(
                    name = name,
                    gender = gender,
                    age = age,
                    height = height,
                    weight = weight,
                    targetWeight = targetWeight,
                    goal = goal,
                    problems = problems ?: emptyList(),
                    phoneNumber = phoneNumber,
                    activityLevel = activityLevel,
                    region = manzil ?: existingUser.region,
                    targetCalories = targetCalories,
                    targetProtein = targetProtein,
                    targetFat = targetFat,
                    targetCarbs = targetCarbs,
                    registerDate = existingUser.registerDate
                )

                val plan = dietEngine.calculateDietPlan(userToUpdate)
                val finalUser = userToUpdate.copy(
                    targetCalories = if (targetCalories > 0) targetCalories else plan.targetCalories,
                    targetProtein = if (targetProtein > 0) targetProtein else plan.macros.protein,
                    targetFat = if (targetFat > 0) targetFat else plan.macros.fat,
                    targetCarbs = if (targetCarbs > 0) targetCarbs else plan.macros.carbs
                )

                profileManagerRepo.updateUser(onlyLocal = false) {
                    finalUser
                }
                _navigationEvent.send(GetInfoNavigationEvent.NavigateToHome)
            } catch (e: Exception) {
                _navigationEvent.send(GetInfoNavigationEvent.ShowToast("Xatolik yuz berdi: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
}