package uz.oybek.ozamiz.ui.feature.notification

import uz.oybek.ozamiz.data.user.UserProfileRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.notification.NotificationRepository
import uz.oybek.ozamiz.data.notification.model.NotificationModel
import uz.oybek.ozamiz.data.user.local.LocalUser

class NotificationsViewModel : ViewModel() {
    private val _notifications = MutableStateFlow<List<NotificationModel>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = NotificationRepository.getNotifications(LocalUser.user.uid)
            if (result.isSuccess) {
                _notifications.value = result.getOrDefault(emptyList())
            }
            _isLoading.value = false
        }
    }

    fun markAllAsRead() {
        UserProfileRepository.setUnreadNotificationsZero()
    }
}
