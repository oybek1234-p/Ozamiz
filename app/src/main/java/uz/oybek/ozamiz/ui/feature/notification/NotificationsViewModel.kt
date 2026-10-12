package uz.oybek.ozamiz.ui.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.notification.api.NotificationRepository
import uz.oybek.ozamiz.data.notification.model.NotificationModel
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val notificationsFlow: Flow<PagingData<NotificationModel>> =
        notificationRepository.getNotificationsPaging()
            .cachedIn(viewModelScope)

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.clearUnreadBadge()
        }
    }
}


