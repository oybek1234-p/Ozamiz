package uz.oybek.ozamiz.ui.getinfo // Yoki mos paketga joylang

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.UserRepository
import uz.oybek.ozamiz.data.info.Gender

sealed class GetInfoNavigationEvent {
    data class ShowToast(val message: String) : GetInfoNavigationEvent()
    object NavigateToNextScreen : GetInfoNavigationEvent() // Keyingi ekranga o'tish
}

class GetInfoViewModel : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Eventlarni boshqarish uchun (SingleLiveEvent yoki Event Wrapper)
    private val _navigationEvent = MutableLiveData<Event<GetInfoNavigationEvent>>()
    val navigationEvent: LiveData<Event<GetInfoNavigationEvent>> = _navigationEvent

    fun saveUserInfo(
        name: String?,
        gender: Int,
        ageStr: String?,
        heightStr: String?,
        weightStr: String?,
        problems: List<String>? // ChipGroup'dan keladigan Set
    ) {
        // --- Validatsiya ---
        if (name.isNullOrBlank() || name.length < 3) {
            _navigationEvent.value = Event(GetInfoNavigationEvent.ShowToast("Iltimos, ismingizni to'liq kiriting (kamida 3 belgi)."))
            return
        }
        if (gender != Gender.MALE && gender != Gender.FEMALE) {
            _navigationEvent.value = Event(GetInfoNavigationEvent.ShowToast("Iltimos, jinsingizni tanlang."))
            return
        }
        val age = ageStr?.toIntOrNull()
        if (age == null || age <= 0 || age > 100) { // Yoshi uchun oddiy tekshiruv
            _navigationEvent.value = Event(GetInfoNavigationEvent.ShowToast("Iltimos, yoshingizni to'g'ri kiriting."))
            return
        }
        val height = heightStr?.toIntOrNull()
        if (height == null || height < 50 || height > 250) { // Bo'y uchun taxminiy tekshiruv
            _navigationEvent.value = Event(GetInfoNavigationEvent.ShowToast("Iltimos, bo'yingizni to'g'ri kiriting (sm)."))
            return
        }
        val weight = weightStr?.toIntOrNull()
        if (weight == null || weight < 20 || weight > 300) { // Vazn uchun taxminiy tekshiruv
            _navigationEvent.value = Event(GetInfoNavigationEvent.ShowToast("Iltimos, vazningizni to'g'ri kiriting (kg)."))
            return
        }

        // --- Saqlash ---
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // UserRepository orqali Firebase'ga saqlash
                UserRepository.updateUser { currentUser ->
                    currentUser.name = name // Ism
                    currentUser.gender = gender
                    currentUser.age = age
                    currentUser.height = height
                    currentUser.weight = weight
                    currentUser.problems = problems?.toList()?: emptyList() // Muammolar (null bo'lsa bo'sh set)
                }
                _navigationEvent.postValue(Event(GetInfoNavigationEvent.ShowToast("Ma'lumotlar saqlandi!")))
                _navigationEvent.postValue(Event(GetInfoNavigationEvent.NavigateToNextScreen)) // Keyingi ekranga o'tish signali

            } catch (e: Exception) {
                throw e
                 _navigationEvent.postValue(Event(GetInfoNavigationEvent.ShowToast("Saqlashda xatolik: ${e.message}")))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
