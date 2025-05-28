package uz.oybek.ozamiz.ui.getinfo // Yoki loyihadagi mos paket

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.UserRepository
import uz.oybek.ozamiz.data.info.MealsType
import uz.oybek.ozamiz.reminders.ReminderScheduler
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

    private val defaultMealIds = MealsType.mealTypes.map { it.id }.toSet()

    init {
        loadAndInitializeReminders()
    }

    private fun loadAndInitializeReminders() {
        _isLoadingLiveData.value = true
        viewModelScope.launch {
            val user = LocalUser.user
            val savedSettings: Map<String, String?>? = user.reminderSettings

            val currentList = mutableListOf<ReminderItem>()

            // Add default meal reminders
            MealsType.mealTypes.forEach { mealType ->
                val reminder = mealType.toReminder()
                parseAndSetTime(reminder, savedSettings?.get(mealType.id))
                currentList.add(reminder)
            }
            applyDefaultTimesIfNeeded(currentList.filter { it.type == ReminderType.MEAL }.toMutableList())


            // Add saved exercise reminders
            savedSettings?.forEach { (id, timeString) ->
                if (!defaultMealIds.contains(id)) { // If not a meal, assume it's an exercise
                    val exerciseType = ExerciseType.getExerciseTypeById(id)
                    val exerciseReminder = ReminderItem(
                        id = exerciseType.id,
                        name = exerciseType.name,
                        iconRes = exerciseType.iconRes,
                        type = ReminderType.EXERCISE,
                        description = exerciseType.description,
                        imageUrl = exerciseType.defaultImageUrl
                    )
                    parseAndSetTime(exerciseReminder, timeString)
                    currentList.add(exerciseReminder)
                }
            }
            sortReminderList(currentList)
            _reminderListLiveData.postValue(currentList)
            _isLoadingLiveData.postValue(false)
        }
    }


    private fun applyDefaultTimesIfNeeded(mealReminders: MutableList<ReminderItem>) {
        val defaultTimes = mapOf(
            MealsType.Breakfast.id to Pair(7, 0),
            MealsType.Lunch.id to Pair(13, 0),
            MealsType.Dinner.id to Pair(19, 0)
        )
        mealReminders.forEach { item ->
            if (!item.isValidTimeSet()) { // Only apply if user hasn't set a time
                defaultTimes[item.id]?.let { time ->
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
                    if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                        item.hour = hour
                        item.minute = minute
                    }
                }
            } catch (e: Exception) {
                // Invalid format, leave time as null
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
            _reminderListLiveData.value = currentList // Notify UI
        }
    }

    fun processExerciseEditResult(
        itemId: String?, action: String?,
        name: String?, iconRes: Int?, hour: Int?, minute: Int?,
        description: String?, imageUrl: String?
    ) {
        if (itemId == null || action == null) return

        val currentList = _reminderListLiveData.value ?: mutableListOf()
        val newList = currentList.toMutableList()

        val existingIndex = newList.indexOfFirst { it.id == itemId }

        when (action) {
            "save" -> {
                if (hour == null || minute == null || hour == -1 || minute == -1) {
                    // Time not set or invalid, treat as disabled for this specific exercise
                    if (existingIndex != -1) {
                        newList[existingIndex] = newList[existingIndex].copy(hour = null, minute = null)
                    }
                    // If it's a new item and time isn't set, don't add it yet, or add as disabled.
                    // For simplicity, we'll assume time must be set to "save" an exercise reminder.
                    // If it can be saved without time, adjust logic.
                } else {
                    if (existingIndex != -1) { // Editing existing
                        newList[existingIndex] = newList[existingIndex].copy(
                            hour = hour,
                            minute = minute
                            // name, iconRes etc., if they can be changed in EditExerciseReminderFragment
                        )
                    } else if (name != null && iconRes != null) { // Adding new
                        val newItem = ReminderItem(
                            id = itemId, name = name, iconRes = iconRes,
                            type = ReminderType.EXERCISE, description = description,
                            imageUrl = imageUrl, hour = hour, minute = minute
                        )
                        newList.add(newItem)
                    }
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


    private fun sortReminderList(list: MutableList<ReminderItem>) {
        list.sortWith(compareBy<ReminderItem, Int?>(nullsLast()) { it.hour }
            .thenBy(nullsLast()) { it.minute }
            .thenBy { it.id }) // Sort by ID as a tie-breaker
    }

    fun saveSettings() {
        val currentList = _reminderListLiveData.value ?: return

        // Filter out reminders where time is not set (null hour or minute)
        // Only save reminders that have a valid time.
        val settingsToSave = currentList.filter { it.isValidTimeSet() }
            .associate {
                it.id to String.format(Locale.getDefault(), "%02d:%02d", it.hour, it.minute)
            }

        UserRepository.updateUser { user ->
            user.reminderSettings = settingsToSave
        }
        // Schedule alarms based on the saved settings
        ReminderScheduler.scheduleAllActiveReminders(appContext)

        _navigationEventLiveData.postValue(Event(SetupReminderNavigationEvent.ShowToast("Sozlamalar saqlandi!")))
        _navigationEventLiveData.postValue(Event(SetupReminderNavigationEvent.NavigateToHome))
    }
}