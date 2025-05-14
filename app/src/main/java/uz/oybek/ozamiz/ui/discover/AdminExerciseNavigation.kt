package uz.oybek.ozamiz.ui.discover

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uz.oybek.ozamiz.AudioUploader
import uz.oybek.ozamiz.ImageUploader
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.workouts.Exercise
import uz.oybek.ozamiz.data.workouts.WorkoutRepository
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.getinfo.Event

sealed class AdminExerciseNavigation {
    data class ShowToast(val message: String) : AdminExerciseNavigation()
    object NavigateBack : AdminExerciseNavigation()
}


class AddEditExerciseViewModel : ViewModel() {

    private val repository = WorkoutRepository()
    private val gson = Gson()
    private val TAG = "AddEditExerciseVM"

    private val _uiState = MutableStateFlow(AddEditExerciseUiState())
    val uiState: StateFlow<AddEditExerciseUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableLiveData<Event<AdminExerciseNavigation>>()
    val navigationEvent: LiveData<Event<AdminExerciseNavigation>> = _navigationEvent

    private var currentExercise = Exercise()
    private var initialExercise: Exercise? = null // To compare for changes

    var selectedMediaUri: Uri? = null
    var selectedNameAudioUri: Uri? = null
    var selectedAboutAudioUri: Uri? = null

    // Flags to track if existing audio should be deleted
    private var deleteExistingNameAudio = false
    private var deleteExistingAboutAudio = false

    fun initialize(exerciseJson: String?) {
        if (exerciseJson != null) {
            try {
                initialExercise = gson.fromJson(exerciseJson, Exercise::class.java)
                currentExercise = initialExercise!!.copy() // Start editing from a copy
                _uiState.update {
                    it.copy(
                        exerciseToEdit = initialExercise, // Pass the original for initial UI population
                        title = "Mashqni Tahrirlash",
                        canDelete = true,
                        isLoading = false // Reset loading state
                    )
                }
                Log.d(TAG, "Initialized for editing: ${initialExercise?.name}")
            } catch (e: Exception) {
                handleException(e, "ExerciseJsonParseErrorAdmin")
                _uiState.update { it.copy(isLoading = false, errorMessage = "Mashq ma'lumotlarini o'qishda xatolik") }
            }
        } else {
            initialExercise = Exercise() // Set initial to empty for comparison
            currentExercise = Exercise()
            _uiState.update {
                it.copy(
                    exerciseToEdit = null,
                    title = "Yangi Mashq Qo'shish",
                    canDelete = false,
                    isLoading = false
                )
            }
            Log.d(TAG, "Initialized for adding new exercise.")
        }
        // Reset selections and flags
        selectedMediaUri = null
        selectedNameAudioUri = null
        selectedAboutAudioUri = null
        deleteExistingNameAudio = false
        deleteExistingAboutAudio = false
        _uiState.update { it.copy(isSaving = false, saveSuccess = false, deleteSuccess = false, errorMessage = null) }
    }

    fun updateExerciseField(updateAction: Exercise.() -> Exercise) {
        currentExercise = currentExercise.updateAction()
        // Optionally update UI state if needed for real-time feedback/validation
    }

    fun onMediaSelected(uri: Uri) {
        selectedMediaUri = uri
        // Update UI state if needed to show selection immediately
        _uiState.update { it.copy(mediaUriToDisplay = uri) }
    }

    fun onNameAudioSelected(uri: Uri) {
        selectedNameAudioUri = uri
        deleteExistingNameAudio = false // Selecting new overrides deletion
        // Update UI state to show selected file name
    }

    fun onAboutAudioSelected(uri: Uri) {
        selectedAboutAudioUri = uri
        deleteExistingAboutAudio = false // Selecting new overrides deletion
        // Update UI state to show selected file name
    }

    fun clearMedia() {
        selectedMediaUri = null
        // Logic to handle deletion of existing media URL if needed (set a flag?)
        // currentExercise = currentExercise.copy(mediaUrl = null) // Update internal state
        // Update UI state
        _uiState.update { it.copy(mediaUriToDisplay = null) }
    }

    fun clearNameAudio() {
        selectedNameAudioUri = null
        if (!currentExercise.exerciseNameAudioUrl.isNullOrEmpty()) {
            deleteExistingNameAudio = true // Mark existing for deletion
        }
        // Update UI state
    }

    fun clearAboutAudio() {
        selectedAboutAudioUri = null
        if (!currentExercise.exerciseAboutAudioUrl.isNullOrEmpty()) {
            deleteExistingAboutAudio = true // Mark existing for deletion
        }
        // Update UI state
    }

    // Update saveExercise to handle audio uploads
    fun saveExercise(
        name: String,
        description: String?,
        durationStr: String?,
        repetitionsStr: String?,
        // Removed voiceUrlStr - assuming deprecated
        // Keep other fields updated via updateExerciseField
    ) {
        Log.d(TAG, "saveExercise called. Name: $name")
        // --- Validation ---
        if (name.isBlank()) {
            _navigationEvent.postValue(Event(AdminExerciseNavigation.ShowToast("Mashq nomini kiriting")))
            return
        }
        val duration = durationStr?.toIntOrNull()
        val repetitions = repetitionsStr?.toIntOrNull()
        if ((duration == null || duration <= 0) && (repetitions == null || repetitions <= 0)) {
            _navigationEvent.postValue(Event(AdminExerciseNavigation.ShowToast("Davomiylik (sekund) yoki takrorlash sonini kiriting")))
            return
        }
        // Media check: Need either a selected URI or an existing URL
        if (selectedMediaUri == null && currentExercise.mediaUrl.isNullOrEmpty()) {
            _navigationEvent.postValue(Event(AdminExerciseNavigation.ShowToast("Mashq uchun GIF yoki rasm tanlang")))
            return
        }
        // --- End Validation ---

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        // Update currentExercise with latest text field values (already done via updateExerciseField)
        currentExercise = currentExercise.copy(
            name = name.trim(),
            description = description?.trim()?.ifBlank { null },
            durationSeconds = if (duration != null && duration > 0) duration else null,
            repetitions = if (repetitions != null && repetitions > 0) repetitions else null
        )

        viewModelScope.launch {
            var mediaUploadSuccess = true
            var nameAudioUploadSuccess = true
            var aboutAudioUploadSuccess = true
            var uploadedMediaUrl: String? = null
            var uploadedNameAudioUrl: String? = null
            var uploadedAboutAudioUrl: String? = null
            val oldMediaUrl = initialExercise?.mediaUrl
            val oldNameAudioUrl = initialExercise?.exerciseNameAudioUrl
            val oldAboutAudioUrl = initialExercise?.exerciseAboutAudioUrl

            try {
                // --- Upload Media (Image/GIF) ---
                if (selectedMediaUri != null) {
                    Log.d(TAG, "Uploading new media...")
                    when (val result = ImageUploader.uploadImage(selectedMediaUri!!)) {
                        is DataWrapper.Success -> uploadedMediaUrl = result.data
                        is DataWrapper.Error -> {
                            mediaUploadSuccess = false
                            _uiState.update { it.copy(isSaving = false, errorMessage = "Media yuklashda xato: ${result.error}") }
                        }
                        else -> {}
                    }
                    if (!mediaUploadSuccess) return@launch // Stop if media upload fails
                }

                // --- Upload Name Audio ---
                if (selectedNameAudioUri != null) {
                    Log.d(TAG, "Uploading new name audio...")
                    when (val result = AudioUploader.uploadAudio(selectedNameAudioUri!!)) {
                        is DataWrapper.Success -> uploadedNameAudioUrl = result.data
                        is DataWrapper.Error -> {
                            nameAudioUploadSuccess = false
                            _uiState.update { it.copy(isSaving = false, errorMessage = "Nom audiosini yuklashda xato: ${result.error}") }
                        }
                        else -> {}
                    }
                    if (!nameAudioUploadSuccess) {
                        // Rollback media upload if name audio failed
                        uploadedMediaUrl?.let { ImageUploader.deleteImage(it) }
                        return@launch
                    }
                }

                // --- Upload About Audio ---
                if (selectedAboutAudioUri != null) {
                    Log.d(TAG, "Uploading new about audio...")
                    when (val result = AudioUploader.uploadAudio(selectedAboutAudioUri!!)) {
                        is DataWrapper.Success -> uploadedAboutAudioUrl = result.data
                        is DataWrapper.Error -> {
                            aboutAudioUploadSuccess = false
                            _uiState.update { it.copy(isSaving = false, errorMessage = "Haqida audiosini yuklashda xato: ${result.error}") }
                        }
                        else -> {}
                    }
                    if (!aboutAudioUploadSuccess) {
                        // Rollback previous uploads
                        uploadedMediaUrl?.let { ImageUploader.deleteImage(it) }
                        uploadedNameAudioUrl?.let { AudioUploader.deleteAudio(it) }
                        return@launch
                    }
                }

                // --- Prepare final Exercise object ---
                val finalExercise = currentExercise.copy(
                    mediaUrl = uploadedMediaUrl ?: currentExercise.mediaUrl, // Use new or keep existing
                    exerciseNameAudioUrl = if (deleteExistingNameAudio) null else (uploadedNameAudioUrl ?: currentExercise.exerciseNameAudioUrl),
                    exerciseAboutAudioUrl = if (deleteExistingAboutAudio) null else (uploadedAboutAudioUrl ?: currentExercise.exerciseAboutAudioUrl)
                    // Ensure voiceUrl is handled correctly if deprecated or kept
                    // voiceUrl = ...
                )

                // --- Save to Firestore ---
                val resultFirestore: DataWrapper<*>
                val isUpdating = finalExercise.id.isNotBlank()
                Log.d(TAG, "Saving to Firestore. Is updating: $isUpdating, ID: ${finalExercise.id}")
                if (!isUpdating) {
                    resultFirestore = repository.addExercise(finalExercise)
                } else {
                    resultFirestore = repository.updateExercise(finalExercise)
                }

                // --- Handle Firestore Result & Cleanup ---
                when (resultFirestore) {
                    is DataWrapper.Success -> {
                        Log.d(TAG, "Firestore save successful.")
                        // Delete old files AFTER successful save
                        if (selectedMediaUri != null && !oldMediaUrl.isNullOrEmpty() && oldMediaUrl != uploadedMediaUrl) {
                            Log.d(TAG, "Deleting old media: $oldMediaUrl")
                            ImageUploader.deleteImage(oldMediaUrl)
                        }
                        if ((selectedNameAudioUri != null || deleteExistingNameAudio) && !oldNameAudioUrl.isNullOrEmpty() && oldNameAudioUrl != uploadedNameAudioUrl) {
                            Log.d(TAG, "Deleting old name audio: $oldNameAudioUrl")
                            AudioUploader.deleteAudio(oldNameAudioUrl)
                        }
                        if ((selectedAboutAudioUri != null || deleteExistingAboutAudio) && !oldAboutAudioUrl.isNullOrEmpty() && oldAboutAudioUrl != uploadedAboutAudioUrl) {
                            Log.d(TAG, "Deleting old about audio: $oldAboutAudioUrl")
                            AudioUploader.deleteAudio(oldAboutAudioUrl)
                        }
                        // Add deletion for old voiceUrl if needed

                        val message = if (initialExercise?.id.isNullOrEmpty()) "Mashq qo'shildi" else "Mashq yangilandi"
                        _navigationEvent.postValue(Event(AdminExerciseNavigation.ShowToast(message)))
                        _navigationEvent.postValue(Event(AdminExerciseNavigation.NavigateBack))
                        _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                    }
                    is DataWrapper.Error -> {
                        Log.e(TAG, "Firestore save failed: ${resultFirestore.error}")
                        // Rollback: Delete NEWLY uploaded files if Firestore save failed
                        uploadedMediaUrl?.let { if (it != oldMediaUrl) ImageUploader.deleteImage(it) }
                        uploadedNameAudioUrl?.let { if (it != oldNameAudioUrl) AudioUploader.deleteAudio(it) }
                        uploadedAboutAudioUrl?.let { if (it != oldAboutAudioUrl) AudioUploader.deleteAudio(it) }
                        _uiState.update { it.copy(isSaving = false, errorMessage = resultFirestore.error) }
                    }
                    is DataWrapper.Loading -> {} // Should not happen here
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during save/upload process", e)
                handleException(e)
                // Attempt to rollback any successful uploads during the failed process
                uploadedMediaUrl?.let { if (it != oldMediaUrl) ImageUploader.deleteImage(it) }
                uploadedNameAudioUrl?.let { if (it != oldNameAudioUrl) AudioUploader.deleteAudio(it) }
                uploadedAboutAudioUrl?.let { if (it != oldAboutAudioUrl) AudioUploader.deleteAudio(it) }
                _uiState.update { it.copy(isSaving = false, errorMessage = "Saqlashda kutilmagan xatolik") }
            }
        }
    }

    fun deleteExercise() {
        val exerciseIdToDelete = currentExercise.id // Use ID from the data being edited
        if (exerciseIdToDelete.isBlank()) return
        Log.d(TAG, "deleteExercise called for ID: $exerciseIdToDelete")

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            // Repository's deleteExercise now handles deleting files from storage
            when(val result = repository.deleteExercise(exerciseIdToDelete)) {
                is DataWrapper.Success -> {
                    Log.d(TAG, "Exercise deleted successfully.")
                    _navigationEvent.postValue(Event(AdminExerciseNavigation.ShowToast("Mashq o'chirildi")))
                    _navigationEvent.postValue(Event(AdminExerciseNavigation.NavigateBack))
                    _uiState.update { it.copy(isSaving = false, deleteSuccess = true) }
                }
                is DataWrapper.Error -> {
                    Log.e(TAG, "Error deleting exercise: ${result.error}")
                    _uiState.update { it.copy(isSaving = false, errorMessage = "O'chirishda xatolik: ${result.error}") }
                }
                is DataWrapper.Loading -> {}
            }
        }
    }


    fun hasChanges(): Boolean {
        // Compare currentExercise state with initialExercise state
        val dataChanged = currentExercise != initialExercise
        val mediaChanged = selectedMediaUri != null
        val nameAudioChanged = selectedNameAudioUri != null || deleteExistingNameAudio
        val aboutAudioChanged = selectedAboutAudioUri != null || deleteExistingAboutAudio

        Log.d(TAG, "hasChanges: dataChanged=$dataChanged, mediaChanged=$mediaChanged, nameAudioChanged=$nameAudioChanged, aboutAudioChanged=$aboutAudioChanged")
        return dataChanged || mediaChanged || nameAudioChanged || aboutAudioChanged
    }


    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}