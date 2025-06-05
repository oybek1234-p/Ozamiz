package uz.oybek.ozamiz.ui.getinfo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.play.integrity.internal.o
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.UserRepository
import uz.oybek.ozamiz.data.info.Gender
import uz.oybek.ozamiz.data.utils.NutritionCalculator
import uz.oybek.ozamiz.ui.showToast
import java.util.Date

sealed class GetInfoNavigationEvent {
    data class ShowToast(val message: String) : GetInfoNavigationEvent()
    object NavigateToCongratulation : GetInfoNavigationEvent() // Yangi navigatsiya
    object NavigateToHome : GetInfoNavigationEvent()
}

class GetInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _navigationEvent = MutableLiveData<Event<GetInfoNavigationEvent>>()
    val navigationEvent: LiveData<Event<GetInfoNavigationEvent>> = _navigationEvent

    private val _idealWeightRangeText = MutableLiveData<String?>(null)
    val idealWeightRangeText: LiveData<String?> = _idealWeightRangeText

    private var isNewUserSetup: Boolean = true

    fun setSetupMode(isNew: Boolean) {
        isNewUserSetup = isNew
    }

    fun calculateAndSetIdealWeight(heightCm: Int?, gender: Int) {
        if (heightCm != null && heightCm > 0 && (gender == Gender.MALE || gender == Gender.FEMALE)) {
            val heightM = heightCm / 100.0
            val idealWeight = NutritionCalculator.getIdealWeight(heightM)
            val idealMin = idealWeight.first
            val idealMax = idealWeight.second
            _idealWeightRangeText.postValue(String.format("%.0f kg - %.0f kg", idealMin.toDouble(), idealMax.toDouble()))
        } else {
            _idealWeightRangeText.postValue(null)
        }
    }

    fun saveUserInfoAndGeneratePlan(
        name: String?,
        gender: Int,
        ageStr: String?,
        heightStr: String?,
        weightStr: String?,
        targetWeightStr: String?,
        problems: List<String>?,
        manzilSelected: String?,
        phoneNumber: String?,
        activityLevel: String?
    ) {
        // Validation (existing logic)
        if (name.isNullOrBlank() || name.length < 3) {
            _navigationEvent.value = Event(GetInfoNavigationEvent.ShowToast("Iltimos, ismingizni to'liq kiriting (kamida 3 belgi)."))
            return
        }
        if (gender != Gender.MALE && gender != Gender.FEMALE) {
            _navigationEvent.value = Event(GetInfoNavigationEvent.ShowToast("Iltimos, jinsingizni tanlang."))
            return
        }
        val age = ageStr?.toIntOrNull()
        if (age == null || age <= 0 || age > 120) {
            _navigationEvent.value = Event(GetInfoNavigationEvent.ShowToast("Iltimos, yoshingizni to'g'ri kiriting."))
            return
        }
        if (activityLevel.isNullOrBlank()) {
            _navigationEvent.value = Event(GetInfoNavigationEvent.ShowToast("Iltimos, harakatni tanlang."))
            return
        }
        if (manzilSelected.isNullOrBlank()) {
            _navigationEvent.value = Event(GetInfoNavigationEvent.ShowToast("Iltimos, manzilni tanlang."))
            return
        }
        if (phoneNumber.isNullOrBlank() || phoneNumber.length < 9) { // Basic phone validation
            _navigationEvent.value = Event(GetInfoNavigationEvent.ShowToast("Iltimos, telefon raqamingizni to'g'ri kiriting."))
            return
        }
        val height = heightStr?.toIntOrNull()
        if (height == null || height < 50 || height > 250) {
            _navigationEvent.value = Event(GetInfoNavigationEvent.ShowToast("Iltimos, bo'yingizni to'g'ri kiriting (sm)."))
            return
        }
        val currentWeight = weightStr?.toDoubleOrNull()
        if (currentWeight == null || currentWeight < 20 || currentWeight > 500) {
            _navigationEvent.value = Event(GetInfoNavigationEvent.ShowToast("Iltimos, vazningizni to'g'ri kiriting (kg)."))
            return
        }
        val targetWeightKg = targetWeightStr?.toDoubleOrNull()
        if (targetWeightKg != null) {
            if (targetWeightKg < 20 || targetWeightKg > 500) {
                _navigationEvent.value = Event(GetInfoNavigationEvent.ShowToast("Maqsad vaznni to'g'ri kiriting."))
                return
            }
            if (targetWeightKg >= currentWeight) {
                _navigationEvent.value = Event(GetInfoNavigationEvent.ShowToast("Ozish uchun maqsad vazn joriy vazndan kichik bo'lishi kerak."))
                return
            }
            if (currentWeight - targetWeightKg > 30) {
                _navigationEvent.value = Event(GetInfoNavigationEvent.ShowToast("Bu maqsad juda keskin va ko'p vaqt talab qilishi mumkin. Kichikroq maqsad qo'yishni o'ylab ko'ring."))
                // No return here, just a warning
            }
        }
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val existingUser = LocalUser.user.takeIf { it.valid } ?: User()
                val userToUpdate = existingUser.copy(
                    name = name.trim(),
                    gender = gender,
                    age = age,
                    height = height,
                    weight = currentWeight,
                    targetWeight = targetWeightKg, // Can be null if not set
                    problems = problems ?: emptyList(),
                    manzil = manzilSelected,
                    phoneNumber = phoneNumber,
                    activityLevel = activityLevel,
                    registerDate = existingUser.registerDate ?: Date() // Preserve or set new if null
                )
                // Set startWeight logic
                if (targetWeightKg != null && targetWeightKg < currentWeight) {
                    // If a new valid target weight is set (for losing weight)
                    // OR if target weight changes and startWeight was not set or different from current
                    if (userToUpdate.startWeight == null || userToUpdate.startWeight != currentWeight || existingUser.targetWeight != targetWeightKg) {
                        userToUpdate.startWeight = currentWeight
                    }
                } else {
                    // If no target weight, or target is not for losing weight, clear startWeight
                    userToUpdate.startWeight = null
                }


                userToUpdate.targetCalories = NutritionCalculator.calculateTargetCalories(userToUpdate)
                userToUpdate.targetWaterMl = NutritionCalculator.calculateTargetWaterMl(userToUpdate.weight)
                userToUpdate.targetCaloriesBurned = userToUpdate.targetCaloriesBurned.takeIf { it > 0 } ?: 300

                UserRepository.updateUser { dbUser ->
                    dbUser.name = userToUpdate.name
                    dbUser.gender = userToUpdate.gender
                    dbUser.age = userToUpdate.age
                    dbUser.height = userToUpdate.height
                    dbUser.weight = userToUpdate.weight
                    dbUser.targetWeight = userToUpdate.targetWeight
                    dbUser.startWeight = userToUpdate.startWeight // Save startWeight to DB
                    dbUser.problems = userToUpdate.problems
                    dbUser.targetCalories = userToUpdate.targetCalories
                    dbUser.targetWaterMl = userToUpdate.targetWaterMl
                    dbUser.targetCaloriesBurned = userToUpdate.targetCaloriesBurned
                    dbUser.phoneNumber = userToUpdate.phoneNumber
                    dbUser.activityLevel = userToUpdate.activityLevel
                    dbUser.manzil = userToUpdate.manzil
                    if (dbUser.registerDate == null) { // Ensure registerDate is set if it was null
                        dbUser.registerDate = userToUpdate.registerDate
                    }
                }
                _isLoading.postValue(false)

                if (isNewUserSetup) {
                    _navigationEvent.postValue(Event(GetInfoNavigationEvent.NavigateToCongratulation))
                } else {
                    _navigationEvent.postValue(Event(GetInfoNavigationEvent.ShowToast("Ma'lumotlar yangilandi!")))
                    _navigationEvent.postValue(Event(GetInfoNavigationEvent.NavigateToHome))
                }

            } catch (e: Exception) {
                _navigationEvent.postValue(Event(GetInfoNavigationEvent.ShowToast("Saqlashda xatolik: ${e.message}")))
                _isLoading.postValue(false)
            }
        }
    }
}