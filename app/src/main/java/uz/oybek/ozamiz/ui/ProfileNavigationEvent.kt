package uz.oybek.ozamiz.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow // LiveData'ni Flow'ga aylantirish uchun
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.UserRepository
import uz.oybek.ozamiz.reminders.ReminderScheduler
import uz.oybek.ozamiz.ui.getinfo.Event

sealed class ProfileNavigationEvent {
    data object NavigateToAuth : ProfileNavigationEvent()
    data class ShowToast(val message: String) : ProfileNavigationEvent()
}

class ProfileViewModel : ViewModel() {

    init {
        viewModelScope
    }
    // LocalUser.userLive (MutableLiveData<User?>) dan StateFlow<User> yaratish
    val userState: StateFlow<User> = LocalUser.userLive.asFlow() // LiveData'ni Flow'ga aylantiramiz
        .map { user -> user ?: User() } // Agar user null bo'lsa, bo'sh User() obyekti qaytaramiz
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L), // Aktiv bo'lmaganda 5 soniyadan keyin to'xtaydi
            initialValue = LocalUser.user ?: User() // Boshlang'ich qiymat
        )

    private val _navigationEvent = MutableLiveData<Event<ProfileNavigationEvent>>()
    val navigationEvent: LiveData<Event<ProfileNavigationEvent>> = _navigationEvent

    fun logout() {
        viewModelScope.launch {
            try {
                // Cancel all reminders before signing out
                // Scheduling with empty/cleared user data will effectively cancel them
                ReminderScheduler.scheduleAllActiveReminders(appContext)

                UserRepository.signOut() // This will clear LocalUser and trigger its observer
                _navigationEvent.postValue(Event(ProfileNavigationEvent.NavigateToAuth))
            } catch (e: Exception) {
                _navigationEvent.postValue(Event(ProfileNavigationEvent.ShowToast("Chiqishda xatolik: ${e.localizedMessage}")))
            }
        }
    }
}