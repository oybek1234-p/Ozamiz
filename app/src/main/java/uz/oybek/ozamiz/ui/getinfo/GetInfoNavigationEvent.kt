package uz.oybek.ozamiz.ui.getinfo // Yoki mos paketga joylang

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.UserRepository
import uz.oybek.ozamiz.data.info.Gender
import uz.oybek.ozamiz.data.utils.NutritionCalculator

sealed class GetInfoNavigationEvent {
    data class ShowToast(val message: String) : GetInfoNavigationEvent()
    object NavigateToNextScreen : GetInfoNavigationEvent()
    object NavigateToHome : GetInfoNavigationEvent()
}

class GetInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _navigationEvent = MutableLiveData<Event<GetInfoNavigationEvent>>()
    val navigationEvent: LiveData<Event<GetInfoNavigationEvent>> = _navigationEvent

    private val _idealWeightRangeText = MutableLiveData<String?>(null)
    val idealWeightRangeText: LiveData<String?> = _idealWeightRangeText

    private var isNewUserSetup: Boolean = true // Profil tahrirlashdan farqlash uchun

    fun setSetupMode(isNew: Boolean) {
        isNewUserSetup = isNew
    }

    fun calculateAndSetIdealWeight(heightCm: Int?, gender: Int) {
        if (heightCm != null && heightCm > 0 && (gender == Gender.MALE || gender == Gender.FEMALE)) {
            val heightM = heightCm / 100.0

            val idealWeight = NutritionCalculator.getIdealWeight(heightM)
            val idealMin = idealWeight.first
            val idealMax = idealWeight.second
            _idealWeightRangeText.postValue(String.format("%.1f kg - %.1f kg", idealMin.toDouble(), idealMax.toDouble()))
        } else {
            _idealWeightRangeText.postValue(null)
        }
    }


    fun saveUserInfo(
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
        if (name.isNullOrBlank() || name.length < 3) {
            _navigationEvent.value =
                Event(GetInfoNavigationEvent.ShowToast("Iltimos, ismingizni to'liq kiriting (kamida 3 belgi)."))
            return
        }
        if (gender != Gender.MALE && gender != Gender.FEMALE) {
            _navigationEvent.value =
                Event(GetInfoNavigationEvent.ShowToast("Iltimos, jinsingizni tanlang."))
            return
        }
        val age = ageStr?.toIntOrNull()
        if (age == null || age <= 0 || age > 120) {
            _navigationEvent.value =
                Event(GetInfoNavigationEvent.ShowToast("Iltimos, yoshingizni to'g'ri kiriting."))
            return
        }
        if (activityLevel.isNullOrBlank()) {
            _navigationEvent.value =
                Event(GetInfoNavigationEvent.ShowToast("Iltimos, harakatni tanlang."))
            return
        }
        if (manzilSelected.isNullOrBlank()) {
            _navigationEvent.value =
                Event(GetInfoNavigationEvent.ShowToast("Iltimos, manzilni tanlang."))
            return
        }

        if (phoneNumber.isNullOrBlank() || phoneNumber.length < 9) {
            _navigationEvent.value =
                Event(GetInfoNavigationEvent.ShowToast("Iltimos, telefon raqamingizni to'g'ri kiriting."))
            return
        }
        val height = heightStr?.toIntOrNull()
        if (height == null || height < 50 || height > 250) {
            _navigationEvent.value =
                Event(GetInfoNavigationEvent.ShowToast("Iltimos, bo'yingizni to'g'ri kiriting (sm)."))
            return
        }
        val currentWeight = weightStr?.toDoubleOrNull()
        if (currentWeight == null || currentWeight < 20 || currentWeight > 500) {
            _navigationEvent.value =
                Event(GetInfoNavigationEvent.ShowToast("Iltimos, vazningizni to'g'ri kiriting (kg)."))
            return
        }

        val targetWeightKg = targetWeightStr?.toDoubleOrNull()

        if (targetWeightKg != null) {
            if (targetWeightKg < 20 || targetWeightKg > 500) {
                _navigationEvent.value =
                    Event(GetInfoNavigationEvent.ShowToast("Maqsad vaznni to'g'ri kiriting."))
                return
            }
            if (targetWeightKg >= currentWeight) {
                _navigationEvent.value =
                    Event(GetInfoNavigationEvent.ShowToast("Ozish uchun maqsad vazn joriy vazndan kichik bo'lishi kerak."))
                return
            }
            if (currentWeight - targetWeightKg > 30) { // Misol uchun, 30kg dan ko'p ozish
                _navigationEvent.value =
                    Event(GetInfoNavigationEvent.ShowToast("Bu maqsad juda keskin va ko'p vaqt talab qilishi mumkin. Kichikroq maqsad qo'yishni o'ylab ko'ring."))
                // return // Agar davom etishni bloklamoqchi bo'lsangiz
            }
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val userToUpdate = (LocalUser.user.takeIf { it.valid } ?: User()).copy(
                    name = name.trim(),
                    gender = gender,
                    age = age,
                    height = height,
                    weight = currentWeight,
                    targetWeight = targetWeightKg,
                    problems = problems ?: emptyList(),
                    manzil = manzilSelected,
                    phoneNumber = phoneNumber,
                    activityLevel = activityLevel
                )

                userToUpdate.targetCalories =
                    NutritionCalculator.calculateTargetCalories(userToUpdate)
                userToUpdate.targetWaterMl =
                    NutritionCalculator.calculateTargetWaterMl(userToUpdate.weight)
                userToUpdate.targetCaloriesBurned =
                    userToUpdate.targetCaloriesBurned.takeIf { it > 0 } ?: 300

                UserRepository.updateUser {
                    it.name = userToUpdate.name
                    it.gender = userToUpdate.gender
                    it.age = userToUpdate.age
                    it.targetWeight = userToUpdate.targetWeight
                    it.height = userToUpdate.height
                    it.weight = userToUpdate.weight
                    it.phoneNumber = userToUpdate.phoneNumber
                    it.activityLevel = userToUpdate.activityLevel
                    it.targetWeight = userToUpdate.targetWeight
                    it.problems = userToUpdate.problems
                    it.targetCalories = userToUpdate.targetCalories
                    it.targetWaterMl = userToUpdate.targetWaterMl
                    it.targetCaloriesBurned = userToUpdate.targetCaloriesBurned
                }

                _navigationEvent.postValue(Event(GetInfoNavigationEvent.ShowToast("Ma'lumotlar saqlandi!")))

                if (isNewUserSetup) {
                    _navigationEvent.postValue(Event(GetInfoNavigationEvent.NavigateToNextScreen))
                } else {
                    _navigationEvent.postValue(Event(GetInfoNavigationEvent.NavigateToHome))
                }

            } catch (e: Exception) {
                _navigationEvent.postValue(Event(GetInfoNavigationEvent.ShowToast("Saqlashda xatolik: ${e.message}")))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}