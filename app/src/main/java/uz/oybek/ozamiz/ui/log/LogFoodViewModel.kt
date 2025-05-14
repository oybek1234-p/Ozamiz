// ----- C:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\ui\log\LogFoodViewModel.kt -----
// C:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\ui\log\LogFoodViewModel.kt
package uz.oybek.ozamiz.ui.log

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ServerValue
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.ImageUploader
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.ai.ChatGptFoodAnalyzer
import uz.oybek.ozamiz.data.ai.GeminiAnalysisResult
import uz.oybek.ozamiz.data.info.FoodLog
import uz.oybek.ozamiz.data.logs.DailyLogRepository

sealed class LogFoodUiState {
    data object Loading : LogFoodUiState()
    data class Inputting(val mealType: String) : LogFoodUiState()
    data class Editing(val log: FoodLog) : LogFoodUiState()
    data object Analyzing : LogFoodUiState()
    data class ShowResult(val result: GeminiAnalysisResult, val warnings: List<MealWarning>) :
        LogFoodUiState()

    data object Saving : LogFoodUiState()
    data object Success : LogFoodUiState() // Indicates successful save, fragment will navigate back
    data class Error(val message: String) : LogFoodUiState()
}


class LogFoodViewModel(application: Application) : AndroidViewModel(application) {

    private val dailyLogRepository = DailyLogRepository()
    private val imageUploader = ImageUploader
    private val foodAnalyzer = ChatGptFoodAnalyzer

    private val _uiState = MutableStateFlow<LogFoodUiState>(LogFoodUiState.Loading)
    val uiState: StateFlow<LogFoodUiState> = _uiState.asStateFlow()

    private var pendingLogData: FoodLog? = null
    private var analysisResultForSheet: GeminiAnalysisResult? = null
    private var generatedWarnings: List<MealWarning> = emptyList()

    var logToEdit: FoodLog? = null
    var mealType: String = ""
    var selectedImageUri: Uri? = null
    private var originalPhotoUrl: String? = null
    private var uploadedUrlDuringAnalysis: String? = null

    fun loadArguments(arguments: Bundle?) {
        val defaultMealType = arguments?.getString("mealType") ?: "snack"
        val logJson = arguments?.getString("logToEditJson")
        mealType = defaultMealType

        if (logJson != null) {
            try {
                logToEdit = Gson().fromJson(logJson, FoodLog::class.java)
                originalPhotoUrl = logToEdit?.photoUrl
                _uiState.value = LogFoodUiState.Editing(logToEdit!!)
            } catch (e: Exception) {
                _uiState.value = LogFoodUiState.Error("Log ma'lumotini o'qishda xatolik")
            }
        } else {
            logToEdit = null; originalPhotoUrl = null
            _uiState.value = LogFoodUiState.Inputting(mealType)
        }
        selectedImageUri = null; pendingLogData = null; analysisResultForSheet =
            null; generatedWarnings = emptyList(); uploadedUrlDuringAnalysis = null
    }

    fun saveEditedLog(description: String) {
        if (logToEdit == null) return
        if (description.isBlank()) {
            _uiState.value =
                LogFoodUiState.Error(getApplication<Application>().getString(R.string.error_description_required)); stateHandled(); return
        }
        _uiState.value = LogFoodUiState.Saving
        viewModelScope.launch {
            var newPhotoUrl = originalPhotoUrl;
            var deleteOldImage = false
            if (selectedImageUri != null) {
                when (val uploadResult = imageUploader.uploadImage(selectedImageUri!!)) {
                    is DataWrapper.Success -> {
                        newPhotoUrl =
                            uploadResult.data; if (!originalPhotoUrl.isNullOrEmpty() && originalPhotoUrl != newPhotoUrl) {
                            deleteOldImage = true
                        }
                    }

                    is DataWrapper.Error -> {
                        _uiState.value =
                            LogFoodUiState.Error("Rasm yuklashda xato: ${uploadResult.error}"); return@launch
                    }

                    else -> {} // Handle Loading? Not typical here.
                }
            }
            val updatedLog =
                logToEdit!!.copy(description = description.trim(), photoUrl = newPhotoUrl)
            when (val result = dailyLogRepository.updateFoodLog(logToEdit!!.logId, updatedLog)) {
                is DataWrapper.Success -> {
                    if (deleteOldImage) {
                        imageUploader.deleteImage(originalPhotoUrl)
                    }; _uiState.value = LogFoodUiState.Success
                }

                is DataWrapper.Error -> {
                    if (selectedImageUri != null && newPhotoUrl != originalPhotoUrl) {
                        newPhotoUrl?.let { imageUploader.deleteImage(it) }
                    }; _uiState.value = LogFoodUiState.Error("Log saqlashda xato: ${result.error}")
                }

                else -> {}
            }
        }
    }

    fun analyzeAndPrepareLog(description: String) {
        val imageUriForAnalysis = selectedImageUri ?: originalPhotoUrl?.let { Uri.parse(it) }

        if (description.isBlank()) {
            _uiState.value =
                LogFoodUiState.Error(getApplication<Application>().getString(R.string.error_description_required)); stateHandled(); return
        }
        _uiState.value = LogFoodUiState.Analyzing
        viewModelScope.launch {
            uploadedUrlDuringAnalysis = null // Reset before attempting upload
            if (selectedImageUri != null) {
                when (val uploadResult = imageUploader.uploadImage(selectedImageUri!!)) {
                    is DataWrapper.Success -> uploadedUrlDuringAnalysis = uploadResult.data
                    is DataWrapper.Error -> {
                        _uiState.value =
                            LogFoodUiState.Error("Rasm yuklashda xato: ${uploadResult.error}"); return@launch
                    }

                    else -> {}
                }
            }

            when (val analysisDataWrapper =
                foodAnalyzer.analyzeFood(imageUriForAnalysis, description.trim())) {
                is DataWrapper.Success -> {
                    val analysisData = analysisDataWrapper.data
                    analysisResultForSheet = analysisData
                    pendingLogData = FoodLog(
                        description = description.trim(),
                        calories = analysisData.estimatedCalories,
                        protein = analysisData.estimatedProtein,
                        fat = analysisData.estimatedFat,
                        carbs = analysisData.estimatedCarbs,
                        mealType = mealType,
                        photoUrl = uploadedUrlDuringAnalysis ?: originalPhotoUrl,
                        analysisInsights = analysisData.insights,
                        estimatedByAI = true,
                        timestamp = ServerValue.TIMESTAMP
                    )
                    generatedWarnings = generateMealWarnings(analysisData)
                    // Set ShowResult state, this will trigger showing the bottom sheet in the fragment
                    _uiState.value = LogFoodUiState.ShowResult(analysisData, generatedWarnings)
                }

                is DataWrapper.Error -> {
                    uploadedUrlDuringAnalysis?.let { imageUploader.deleteImage(it) } // Rollback upload on analysis error
                    _uiState.value = LogFoodUiState.Error(analysisDataWrapper.error)
                }

                is DataWrapper.Loading -> {}
            }
        }
    }

    private fun generateMealWarnings(result: GeminiAnalysisResult): List<MealWarning> {
        val warnings = mutableListOf<MealWarning>()
        if (result.estimatedFat != null && result.estimatedFat > 25.0) {
            warnings.add(
                MealWarning(
                    "Bu ovqatda yog' miqdori yuqori bo'lishi mumkin.", isSerious = true
                )
            )
        }
        if (result.estimatedCalories > 600) {
            warnings.add(MealWarning("Kaloriya miqdori yuqori, porsiyaga e'tibor bering."))
        }
        if (result.recognizedIngredients.any {
                it.contains("non", ignoreCase = true) || it.contains(
                    "bread", ignoreCase = true
                )
            }) {
            warnings.add(
                MealWarning("Ovqat tarkibida non borligi aniqlandi.")
            )
        }
        return warnings
    }

    // This function is now called by the CalorieResultBottomSheet
    fun confirmAndSaveLog() {
        if (pendingLogData == null) {
            // If called unexpectedly, revert to input state
            _uiState.value = if (logToEdit != null) LogFoodUiState.Editing(logToEdit!!) else LogFoodUiState.Inputting(mealType)
            _uiState.update { LogFoodUiState.Error("Saqlash uchun ma'lumot yo'q.") } // Show error in bottom sheet
            return
        }
        // Set Saving state - the BottomSheet will observe this
        _uiState.value = LogFoodUiState.Saving
        viewModelScope.launch {
            when (val result = dailyLogRepository.addFoodLog(pendingLogData!!)) {
                is DataWrapper.Success -> {
                    // Set Success state - BottomSheet will observe and dismiss
                    _uiState.value = LogFoodUiState.Success
                }
                is DataWrapper.Error -> {
                    // Rollback image upload only if it was newly uploaded
                    uploadedUrlDuringAnalysis?.let { imageUploader.deleteImage(it) }
                    // Set Error state - BottomSheet will observe and show error
                    _uiState.value = LogFoodUiState.Error("Log saqlashda xatolik: ${result.error}")
                }
                else -> {}
            }
            pendingLogData = null; analysisResultForSheet = null; generatedWarnings =
            emptyList(); uploadedUrlDuringAnalysis = null
        }
    }

    fun cancelAnalysisOrResetState() {
        viewModelScope.launch { // Ensure image deletion runs in coroutine
            uploadedUrlDuringAnalysis?.let { imageUploader.deleteImage(it) } // Rollback upload if analysis was cancelled
            pendingLogData = null; analysisResultForSheet = null; generatedWarnings =
                emptyList(); uploadedUrlDuringAnalysis = null
            stateHandled() // Go back to inputting/editing state
        }
    }

    // Called by Fragment/BottomSheet when state needs to be reset to Inputting/Editing
    fun stateHandled() {
        val currentState = _uiState.value
        // Only reset if we are in a final or intermediate state that requires going back
        if (currentState is LogFoodUiState.ShowResult ||
            currentState is LogFoodUiState.Error ||
            currentState is LogFoodUiState.Success || // Also reset after success is handled by UI
            currentState is LogFoodUiState.Saving ) { // Reset if saving is interrupted/cancelled

            _uiState.value = if (logToEdit != null) {
                LogFoodUiState.Editing(logToEdit!!)
            } else {
                LogFoodUiState.Inputting(mealType)
            }
        }
    }
}