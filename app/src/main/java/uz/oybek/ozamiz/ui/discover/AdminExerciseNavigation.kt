package uz.oybek.ozamiz.ui.discover

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
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

// AdminExerciseNavigation sealed class oldingidek AddEditWorkoutViewModel.kt da qoladi.
// Bu yerda faqat AddEditExerciseViewModel logikasi bo'ladi.

class AddEditExerciseViewModel : ViewModel() {

    private val repository = WorkoutRepository()
    private val gson = Gson()
    private val TAG = "AddEditExerciseVM"

    private val _uiState = MutableStateFlow(AddEditExerciseUiState())
    val uiState: StateFlow<AddEditExerciseUiState> = _uiState.asStateFlow()

    // Navigatsiya eventlari endi bu ViewModel uchun alohida emas,
    // chunki u DialogFragment yoki boshqa Fragmentdan chaqiriladi.
    // Natija FragmentResult orqali qaytariladi.
    private val _operationResultEvent = MutableLiveData<Event<Pair<String, Bundle>>>()
    val operationResultEvent: LiveData<Event<Pair<String, Bundle>>> = _operationResultEvent


    private var currentExercise = Exercise()
    private var initialExercise: Exercise? = null

    var selectedMediaUri: Uri? = null
    var selectedNameAudioUri: Uri? = null
    var selectedAboutAudioUri: Uri? = null

    private var deleteExistingNameAudio = false
    private var deleteExistingAboutAudio = false

    companion object {
        const val REQUEST_KEY_EXERCISE_MODIFIED = "ExerciseModifiedFromEditFragment" // Unique key
        const val RESULT_MODIFIED_EXERCISE_ID = "ModifiedExerciseIdResult"
        const val RESULT_WAS_DELETED = "WasExerciseDeletedResult"
    }


    fun initialize(exerciseJson: String?) {
        if (exerciseJson != null) {
            try {
                initialExercise = gson.fromJson(exerciseJson, Exercise::class.java)
                currentExercise = initialExercise!!.copy()
                _uiState.update {
                    it.copy(
                        exerciseToEdit = initialExercise,
                        title = "Mashqni Tahrirlash",
                        canDelete = true,
                        isLoading = false,
                        mediaUriToDisplay = currentExercise.mediaUrl?.let { Uri.parse(it) }
                    )
                }
            } catch (e: Exception) {
                handleException(e, "ExerciseJsonParseErrorAdmin")
                _uiState.update { it.copy(isLoading = false, errorMessage = "Mashq ma'lumotlarini o'qishda xatolik") }
            }
        } else {
            initialExercise = Exercise()
            currentExercise = Exercise()
            _uiState.update {
                it.copy(
                    exerciseToEdit = null,
                    title = "Yangi Mashq Qo'shish",
                    canDelete = false,
                    isLoading = false,
                    mediaUriToDisplay = null
                )
            }
        }
        selectedMediaUri = null
        selectedNameAudioUri = null
        selectedAboutAudioUri = null
        deleteExistingNameAudio = false
        deleteExistingAboutAudio = false
        _uiState.update { it.copy(isSaving = false, saveSuccess = false, deleteSuccess = false, errorMessage = null) }
    }

    fun updateExerciseField(updateAction: Exercise.() -> Exercise) {
        currentExercise = currentExercise.updateAction()
    }

    fun onMediaSelected(uri: Uri) {
        selectedMediaUri = uri
        _uiState.update { it.copy(mediaUriToDisplay = uri) }
    }

    fun onNameAudioSelected(uri: Uri) {
        selectedNameAudioUri = uri
        deleteExistingNameAudio = false
    }

    fun onAboutAudioSelected(uri: Uri) {
        selectedAboutAudioUri = uri
        deleteExistingAboutAudio = false
    }

    fun clearMedia() {
        selectedMediaUri = null
        currentExercise = currentExercise.copy(mediaUrl = null) // Mark for deletion if editing
        _uiState.update { it.copy(mediaUriToDisplay = null) }
    }

    fun clearNameAudio() {
        selectedNameAudioUri = null
        if (!currentExercise.exerciseNameAudioUrl.isNullOrEmpty()) {
            deleteExistingNameAudio = true
        }
    }

    fun clearAboutAudio() {
        selectedAboutAudioUri = null
        if (!currentExercise.exerciseAboutAudioUrl.isNullOrEmpty()) {
            deleteExistingAboutAudio = true
        }
    }

    fun saveExercise(
        name: String,
        description: String?,
        durationStr: String?,
        repetitionsStr: String?
    ) {
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Mashq nomini kiriting") }
            return
        }
        val duration = durationStr?.toIntOrNull()
        val repetitions = repetitionsStr?.toIntOrNull()
        if ((duration == null || duration <= 0) && (repetitions == null || repetitions <= 0)) {
            _uiState.update { it.copy(errorMessage = "Davomiylik yoki takrorlash sonini kiriting") }
            return
        }
        if (selectedMediaUri == null && currentExercise.mediaUrl.isNullOrEmpty()) {
            _uiState.update { it.copy(errorMessage = "Mashq uchun GIF yoki rasm tanlang") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

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
                if (selectedMediaUri != null) {
                    when (val result = ImageUploader.uploadImage(selectedMediaUri!!)) {
                        is DataWrapper.Success -> uploadedMediaUrl = result.data
                        is DataWrapper.Error -> { mediaUploadSuccess = false; _uiState.update { it.copy(isSaving = false, errorMessage = "Media yuklashda xato: ${result.error}") } }
                        else -> {}
                    }
                    if (!mediaUploadSuccess) return@launch
                }

                if (selectedNameAudioUri != null) {
                    when (val result = AudioUploader.uploadAudio(selectedNameAudioUri!!)) {
                        is DataWrapper.Success -> uploadedNameAudioUrl = result.data
                        is DataWrapper.Error -> { nameAudioUploadSuccess = false; _uiState.update { it.copy(isSaving = false, errorMessage = "Nom audiosini yuklashda xato: ${result.error}") } }
                        else -> {}
                    }
                    if (!nameAudioUploadSuccess) { uploadedMediaUrl?.let { ImageUploader.deleteImage(it) }; return@launch }
                }

                if (selectedAboutAudioUri != null) {
                    when (val result = AudioUploader.uploadAudio(selectedAboutAudioUri!!)) {
                        is DataWrapper.Success -> uploadedAboutAudioUrl = result.data
                        is DataWrapper.Error -> { aboutAudioUploadSuccess = false; _uiState.update { it.copy(isSaving = false, errorMessage = "Haqida audiosini yuklashda xato: ${result.error}") } }
                        else -> {}
                    }
                    if (!aboutAudioUploadSuccess) { uploadedMediaUrl?.let { ImageUploader.deleteImage(it) }; uploadedNameAudioUrl?.let { AudioUploader.deleteAudio(it) }; return@launch }
                }

                val finalExercise = currentExercise.copy(
                    mediaUrl = uploadedMediaUrl ?: currentExercise.mediaUrl,
                    exerciseNameAudioUrl = if (deleteExistingNameAudio && selectedNameAudioUri == null) null else (uploadedNameAudioUrl ?: currentExercise.exerciseNameAudioUrl),
                    exerciseAboutAudioUrl = if (deleteExistingAboutAudio && selectedAboutAudioUri == null) null else (uploadedAboutAudioUrl ?: currentExercise.exerciseAboutAudioUrl)
                )

                val resultFirestore: DataWrapper<String> // ID ni olish uchun String
                val isUpdating = finalExercise.id.isNotBlank()

                if (!isUpdating) {
                    resultFirestore = repository.addExercise(finalExercise)
                } else {
                    // repository.updateExercise returns DataWrapper<Unit>, but we need the ID
                    val updateOpResult = repository.updateExercise(finalExercise)
                    resultFirestore = if (updateOpResult is DataWrapper.Success) DataWrapper.Success(finalExercise.id) else DataWrapper.Error((updateOpResult as DataWrapper.Error).error)
                }

                when (resultFirestore) {
                    is DataWrapper.Success -> {
                        val savedExerciseId = resultFirestore.data
                        if (selectedMediaUri != null && !oldMediaUrl.isNullOrEmpty() && oldMediaUrl != uploadedMediaUrl) { ImageUploader.deleteImage(oldMediaUrl) }
                        if ((selectedNameAudioUri != null || deleteExistingNameAudio) && !oldNameAudioUrl.isNullOrEmpty() && oldNameAudioUrl != uploadedNameAudioUrl) { AudioUploader.deleteAudio(oldNameAudioUrl) }
                        if ((selectedAboutAudioUri != null || deleteExistingAboutAudio) && !oldAboutAudioUrl.isNullOrEmpty() && oldAboutAudioUrl != uploadedAboutAudioUrl) { AudioUploader.deleteAudio(oldAboutAudioUrl) }

                        val message = if (initialExercise?.id.isNullOrEmpty()) "Mashq qo'shildi" else "Mashq yangilandi"
                        _operationResultEvent.postValue(Event(Pair(REQUEST_KEY_EXERCISE_MODIFIED, bundleOf(
                            RESULT_MODIFIED_EXERCISE_ID to savedExerciseId,
                            RESULT_WAS_DELETED to false,
                            "toastMessage" to message // Pass toast message
                        ))))
                        _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                    }
                    is DataWrapper.Error -> {
                        uploadedMediaUrl?.let { if (it != oldMediaUrl) ImageUploader.deleteImage(it) }
                        uploadedNameAudioUrl?.let { if (it != oldNameAudioUrl) AudioUploader.deleteAudio(it) }
                        uploadedAboutAudioUrl?.let { if (it != oldAboutAudioUrl) AudioUploader.deleteAudio(it) }
                        _uiState.update { it.copy(isSaving = false, errorMessage = resultFirestore.error) }
                    }
                    else -> {}
                }

            } catch (e: Exception) {
                handleException(e)
                uploadedMediaUrl?.let { if (it != oldMediaUrl) ImageUploader.deleteImage(it) }
                uploadedNameAudioUrl?.let { if (it != oldNameAudioUrl) AudioUploader.deleteAudio(it) }
                uploadedAboutAudioUrl?.let { if (it != oldAboutAudioUrl) AudioUploader.deleteAudio(it) }
                _uiState.update { it.copy(isSaving = false, errorMessage = "Saqlashda kutilmagan xatolik") }
            }
        }
    }

    fun deleteExercise() {
        val exerciseIdToDelete = currentExercise.id
        if (exerciseIdToDelete.isBlank()) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when(val result = repository.deleteExercise(exerciseIdToDelete)) {
                is DataWrapper.Success -> {
                    _operationResultEvent.postValue(Event(Pair(REQUEST_KEY_EXERCISE_MODIFIED, bundleOf(
                        RESULT_MODIFIED_EXERCISE_ID to exerciseIdToDelete,
                        RESULT_WAS_DELETED to true,
                        "toastMessage" to "Mashq o'chirildi"
                    ))))
                    _uiState.update { it.copy(isSaving = false, deleteSuccess = true) }
                }
                is DataWrapper.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "O'chirishda xatolik: ${result.error}") }
                }
                else -> {}
            }
        }
    }

    fun hasChanges(): Boolean {
        val dataChanged = currentExercise != initialExercise
        val mediaChanged = selectedMediaUri != null
        val nameAudioChanged = selectedNameAudioUri != null || (deleteExistingNameAudio && initialExercise?.exerciseNameAudioUrl != null)
        val aboutAudioChanged = selectedAboutAudioUri != null || (deleteExistingAboutAudio && initialExercise?.exerciseAboutAudioUrl != null)
        return dataChanged || mediaChanged || nameAudioChanged || aboutAudioChanged
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}