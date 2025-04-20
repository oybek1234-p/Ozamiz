package uz.oybek.ozamiz.ui.getinfo // Yoki loyihadagi mos paket

import MealsType
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.UserRepository
import java.util.Locale

open class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? = if (hasBeenHandled) null else {
        hasBeenHandled = true; content
    }
}

sealed class SetupReminderNavigationEvent {
    data class ShowToast(val message: String) : SetupReminderNavigationEvent()
    data object NavigateToHome : SetupReminderNavigationEvent()
}

class SetupRemindersViewModel : ViewModel() {

    private val _reminderListLiveData = MutableLiveData<MutableList<ReminderItem>>()
    val reminderListLiveData: LiveData<MutableList<ReminderItem>> = _reminderListLiveData

    private val _isLoadingLiveData = MutableLiveData<Boolean>(false)
    val isLoadingLiveData: LiveData<Boolean> = _isLoadingLiveData

    private val _navigationEventLiveData = MutableLiveData<Event<SetupReminderNavigationEvent>>()
    val navigationEventLiveData: LiveData<Event<SetupReminderNavigationEvent>> =
        _navigationEventLiveData

    private val defaultMealIds = setOf("breakfast", "snack1", "lunch", "snack2", "dinner")

    init {
        loadAndInitializeReminders()
    }

    private fun loadAndInitializeReminders() {
        _isLoadingLiveData.value = true
        viewModelScope.launch {
            val savedSettings: Map<String, Any?>? = LocalUser.user.reminderSettings

            if (savedSettings != null) {
                val default = getDefaultMealReminders()
                val reminders = savedSettings.map {
                    val isMeal = it.key in defaultMealIds
                    var item: ReminderItem? = null
                    item = if (isMeal) {
                        default.find { d -> d.id == it.key }
                    } else {
                        ExerciseType.getExerciseTypeById(it.key).let {
                            ReminderItem(
                                id = it.id,
                                name = it.name,
                                iconRes = it.iconRes,
                                type = ReminderType.EXERCISE,
                                description = it.description,
                                imageUrl = it.defaultImageUrl
                            )
                        }
                    }
                    item.apply {
                        parseAndSetTime(this!!, it.value as? String)
                    }
                }
                val list = reminders.filterNotNull().toMutableList()
                sortReminderList(list)
                _reminderListLiveData.postValue(list)
                _isLoadingLiveData.postValue(false)
            } else {
                val currentList = getDefaultMealReminders().toMutableList()

                applyDefaultTimes(currentList)
                sortReminderList(currentList)
                _reminderListLiveData.postValue(currentList)
                _isLoadingLiveData.postValue(false)
            }
        }
    }

    private fun getDefaultMealReminders(): List<ReminderItem> {
        return MealsType.mealTypes.map { it.toReminder() }.toMutableList().apply {
            add(ExerciseType.getExerciseTypeById("walking").let {
                ReminderItem(
                    id = it.id,
                    name = it.name,
                    iconRes = it.iconRes,
                    type = ReminderType.EXERCISE,
                    description = it.description,
                    imageUrl = it.defaultImageUrl
                )
            })
            add(ExerciseType.getExerciseTypeById("dance").let {
                ReminderItem(
                    id = it.id,
                    name = it.name,
                    iconRes = it.iconRes,
                    type = ReminderType.EXERCISE,
                    description = it.description,
                    imageUrl = it.defaultImageUrl
                )
            })
        }
    }

    private fun applyDefaultTimes(list: MutableList<ReminderItem>) {
        val defaultTimes = mapOf(
            "breakfast" to Pair(7, 0), "lunch" to Pair(13, 0), "dinner" to Pair(19, 0)
        )
        list.filter { it.type == ReminderType.MEAL }.forEach { item ->
            defaultTimes[item.id]?.let { time ->
                if (!item.isValidTimeSet()) {
                    item.hour = time.first
                    item.minute = time.second
                }
            }
        }
    }

    private fun parseAndSetTime(item: ReminderItem, timeString: String?) {
        item.hour = null
        item.minute = null
        if (!timeString.isNullOrEmpty()) {
            try {
                val parts = timeString.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull()
                    val minute = parts[1].toIntOrNull()
                    if (hour != null && minute != null) {
                        item.hour = hour
                        item.minute = minute
                    }
                }
            } catch (e: Exception) {
                //
            }
        }
    }

    fun userSetReminderTime(itemId: String, hour: Int, minute: Int) {
        val currentList = _reminderListLiveData.value ?: return
        val itemIndex = currentList.indexOfFirst { it.id == itemId }

        if (itemIndex != -1) {
            val item = currentList[itemIndex]
            item.hour = hour
            item.minute = minute

            sortReminderList(currentList)
            _reminderListLiveData.value = currentList

            if (item.type == ReminderType.MEAL) {
                applyAutomaticTimeSuggestions(itemId, hour, minute, currentList)
            }
        }
    }

    fun processExerciseEditResult(
        itemId: String?, action: String?, hour: Int = -1, minute: Int = -1
    ) {
        if (itemId == null || action == null) return

        val currentList = _reminderListLiveData.value ?: mutableListOf()
        val newList = currentList.toMutableList() // Ishlash uchun nusxa olish

        val existingIndex = newList.indexOfFirst { it.id == itemId }

        when (action) {
            "save" -> {
                val finalHour = if (hour == -1) null else hour
                val finalMinute = if (minute == -1) null else minute

                if (existingIndex != -1) {
                    val updatedItem =
                        newList[existingIndex].copy(hour = finalHour, minute = finalMinute)
                    newList[existingIndex] = updatedItem
                } else {
                    val exerciseType = ExerciseType.getExerciseTypeById(itemId)
                    val newItem = ReminderItem(
                        id = itemId,
                        name = exerciseType.name,
                        iconRes = exerciseType.iconRes,
                        type = ReminderType.EXERCISE,
                        description = exerciseType.description,
                        imageUrl = exerciseType.defaultImageUrl,
                        hour = finalHour,
                        minute = finalMinute
                    )
                    newList.add(newItem)
                }
            }

            "delete" -> {
                if (existingIndex != -1) {
                    newList.removeAt(existingIndex)
                }
            }
        }
        sortReminderList(newList)
        _reminderListLiveData.value = newList
    }


    private fun applyAutomaticTimeSuggestions(
        changedItemId: String,
        changedHour: Int,
        changedMinute: Int,
        listToUpdate: MutableList<ReminderItem>?
    ) {

    }

    private fun sortReminderList(list: MutableList<ReminderItem>) {
        list.sortWith(compareBy<ReminderItem, Int?>(nullsLast()) { it.hour }.thenBy(nullsLast()) { it.minute }
            .thenBy { it.id })
    }

    fun saveSettings() {
        val currentList = _reminderListLiveData.value ?: return
        val notSet = currentList.filter { it.hour == null || it.minute == null }
        if (notSet.isNotEmpty()) {
            _navigationEventLiveData.postValue(Event(SetupReminderNavigationEvent.ShowToast("${notSet.first().name} vaqtni belgilang!")))
            return
        }
        val settings = currentList.map {
            Pair(
                it.id, "${
                    String.format(
                        Locale.getDefault(), "%02d", it.hour
                    )
                }:${String.format(Locale.getDefault(), "%02d", it.minute)}"
            )
        }
        UserRepository.updateUser {
            it.reminderSettings = settings.toMap()
        }
        _navigationEventLiveData.postValue(Event(SetupReminderNavigationEvent.ShowToast("Sozlamalar saqlandi!")))
        _navigationEventLiveData.postValue(Event(SetupReminderNavigationEvent.NavigateToHome))
    }

}