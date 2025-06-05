// uz.oybek.ozamiz.ui.log.LogFoodViewModel.kt
package uz.oybek.ozamiz.ui.log

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.UserRepository
import uz.oybek.ozamiz.data.ai.GeminiAnalysisResult
import uz.oybek.ozamiz.data.ai.GeminiFoodAnalyzer
import uz.oybek.ozamiz.data.info.FoodLog
import uz.oybek.ozamiz.data.logs.DailyLogRepository
import uz.oybek.ozamiz.util.StreakManager
import java.util.concurrent.TimeUnit

sealed class LogFoodUiState {
    data object Loading : LogFoodUiState()
    data class Inputting(val mealType: String) : LogFoodUiState()
    data class Editing(val log: FoodLog) : LogFoodUiState()
    data object Analyzing : LogFoodUiState()
    data class ShowResult(val result: GeminiAnalysisResult, val warnings: List<MealWarning>) :
        LogFoodUiState()

    data object Saving : LogFoodUiState()
    data object Success : LogFoodUiState()
    data class Error(val message: String) : LogFoodUiState()
    data object ShowFirstLogCongratulation : LogFoodUiState()
    data class ShowStreakContinued(val currentStreak: Int) : LogFoodUiState()
    data object ShowPremiumPrompt : LogFoodUiState() // New state for premium prompt
}

class LogFoodViewModel(application: Application) : AndroidViewModel(application) {

    private val dailyLogRepository = DailyLogRepository()
    private val imageUploader = ImageUploader
    private val foodAnalyzer = GeminiFoodAnalyzer
    private val TAG = "LogFoodViewModel"

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
        Log.d(TAG, "loadArguments: mealType=$mealType, logJson=$logJson")

        if (logJson != null) {
            try {
                logToEdit = Gson().fromJson(logJson, FoodLog::class.java)
                originalPhotoUrl = logToEdit?.photoUrl
                _uiState.value = LogFoodUiState.Editing(logToEdit!!)
                Log.d(TAG, "loadArguments: Editing log - ${logToEdit?.description}")
            } catch (e: Exception) {
                _uiState.value = LogFoodUiState.Error("Log ma'lumotini o'qishda xatolik")
                Log.e(TAG, "loadArguments: Error parsing logJson", e)
            }
        } else {
            logToEdit = null; originalPhotoUrl = null
            _uiState.value = LogFoodUiState.Inputting(mealType)
            Log.d(TAG, "loadArguments: Inputting new log for mealType=$mealType")
        }
        selectedImageUri = null; pendingLogData = null; analysisResultForSheet =
            null; generatedWarnings = emptyList(); uploadedUrlDuringAnalysis = null
    }

    fun saveEditedLog(description: String) {
        Log.d(TAG, "saveEditedLog: description=$description")
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
                Log.d(TAG, "saveEditedLog: Uploading new image for edit.")
                when (val uploadResult = imageUploader.uploadImage(selectedImageUri!!)) {
                    is DataWrapper.Success -> {
                        newPhotoUrl =
                            uploadResult.data; if (!originalPhotoUrl.isNullOrEmpty() && originalPhotoUrl != newPhotoUrl) {
                            deleteOldImage = true
                        }
                        Log.d(
                            TAG,
                            "saveEditedLog: New image uploaded: $newPhotoUrl, old to delete: $originalPhotoUrl ($deleteOldImage)"
                        )
                    }

                    is DataWrapper.Error -> {
                        _uiState.value =
                            LogFoodUiState.Error("Rasm yuklashda xato: ${uploadResult.error}"); return@launch
                    }

                    else -> {}
                }
            }
            val updatedLog =
                logToEdit!!.copy(description = description.trim(), photoUrl = newPhotoUrl)
            Log.d(TAG, "saveEditedLog: Updating log in repository: ${updatedLog.logId}")
            when (val result = dailyLogRepository.updateFoodLog(logToEdit!!.logId, updatedLog)) {
                is DataWrapper.Success -> {
                    if (deleteOldImage) {
                        Log.d(TAG, "saveEditedLog: Deleting old image: $originalPhotoUrl")
                        imageUploader.deleteImage(originalPhotoUrl)
                    }
                    _uiState.value = LogFoodUiState.Success
                    Log.d(TAG, "saveEditedLog: Successfully updated.")
                }

                is DataWrapper.Error -> {
                    if (selectedImageUri != null && newPhotoUrl != originalPhotoUrl) {
                        newPhotoUrl?.let { imageUploader.deleteImage(it) }
                    }; _uiState.value = LogFoodUiState.Error("Log saqlashda xato: ${result.error}")
                    Log.e(TAG, "saveEditedLog: Error updating log: ${result.error}")
                }

                else -> {}
            }
        }
    }

    fun analyzeAndPrepareLog(description: String, type: String) {
        val user = LocalUser.user
        val daysSinceRegistration = user.registerDate?.let {
            TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it.time)
        } ?: 0L // Default to 0 if registerDate is null
        val currentStreak = user.streakInfo?.currentStreak ?: 0

        if (!user.premium && daysSinceRegistration > 3 && currentStreak > 3) {
            _uiState.value = LogFoodUiState.ShowPremiumPrompt
            return
        }

        val imageUriForAnalysis = selectedImageUri ?: originalPhotoUrl?.let { Uri.parse(it) }

        if (description.isBlank() && selectedImageUri == null) {
            _uiState.value =
                LogFoodUiState.Error(getApplication<Application>().getString(R.string.error_description_required)); stateHandled(); return
        }
        _uiState.value = LogFoodUiState.Analyzing
        viewModelScope.launch {
            uploadedUrlDuringAnalysis = null
            if (selectedImageUri != null) {
                when (val uploadResult = imageUploader.uploadImage(selectedImageUri!!)) {
                    is DataWrapper.Success -> {
                        uploadedUrlDuringAnalysis = uploadResult.data
                    }

                    is DataWrapper.Error -> {
                        _uiState.value =
                            LogFoodUiState.Error("Rasm yuklashda xato: ${uploadResult.error}"); return@launch
                    }

                    else -> {}
                }
            }

            Log.d(TAG, "analyzeAndPrepareLog: Sending to foodAnalyzer.")
            when (val analysisDataWrapper =
                foodAnalyzer.analyzeFood(imageUriForAnalysis, description.trim(), type)) {
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
                    _uiState.value = LogFoodUiState.ShowResult(analysisData, generatedWarnings)
                    Log.d(TAG, "analyzeAndPrepareLog: Analysis success, showing result.")
                }

                is DataWrapper.Error -> {
                    uploadedUrlDuringAnalysis?.let { imageUploader.deleteImage(it) }
                    _uiState.value = LogFoodUiState.Error(analysisDataWrapper.error)
                    Log.e(TAG, "analyzeAndPrepareLog: Analysis error: ${analysisDataWrapper.error}")
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
                    "Yog'li ovqatlarni yeyishni kamaytiring)", isSerious = true
                )
            )
        }
        if (result.recognizedIngredients.any {
                it.contains("non", ignoreCase = true) || it.contains(
                    "bread", ignoreCase = true
                )
            }) {
            warnings.add(
                MealWarning("Oq noni iloji bo'lsa butunlay cheklang!")
            )
            warnings.add(
                MealWarning("Qora non kuniga 1-2 bo'lak yeyish mumkin")
            )
        }
        return warnings
    }

    fun confirmAndSaveLog() {
        if (pendingLogData == null) {
            _uiState.value =
                if (logToEdit != null) LogFoodUiState.Editing(logToEdit!!) else LogFoodUiState.Inputting(
                    mealType
                )
            _uiState.update { LogFoodUiState.Error("Saqlash uchun ma'lumot yo'q.") }
            Log.w(TAG, "confirmAndSaveLog: pendingLogData is null.")
            return
        }
        _uiState.value = LogFoodUiState.Saving
        viewModelScope.launch {
            when (val result = dailyLogRepository.addFoodLog(pendingLogData!!)) {
                is DataWrapper.Success -> {
                    handleStreakAfterLogSave()
                }

                is DataWrapper.Error -> {
                    uploadedUrlDuringAnalysis?.let { imageUploader.deleteImage(it) }
                    _uiState.value = LogFoodUiState.Error("Log saqlashda xatolik: ${result.error}")
                }

                else -> {}
            }
            pendingLogData = null; analysisResultForSheet = null; generatedWarnings =
            emptyList(); uploadedUrlDuringAnalysis = null
        }
    }

    private fun handleStreakAfterLogSave() {
        viewModelScope.launch {
            val currentUser = LocalUser.user
            val streakUpdateResult = StreakManager.updateUserStreakOnAction(currentUser)

            if (streakUpdateResult.updatedUser.streakInfo != currentUser.streakInfo) {
                UserRepository.updateUser { user ->
                    user.streakInfo = streakUpdateResult.updatedUser.streakInfo
                }
                LocalUser.user = streakUpdateResult.updatedUser
                LocalUser.saveUser()
            }

            if (StreakManager.shouldShowFirstLogCongratulation(streakUpdateResult)) {
                _uiState.value = LogFoodUiState.ShowFirstLogCongratulation
            } else if (streakUpdateResult.streakContinued && (streakUpdateResult.updatedUser.streakInfo?.currentStreak
                    ?: 0) > 1
            ) {
                _uiState.value =
                    LogFoodUiState.ShowStreakContinued(streakUpdateResult.updatedUser.streakInfo!!.currentStreak)
            } else {
                _uiState.value = LogFoodUiState.Success
            }
        }
    }


    fun cancelAnalysisOrResetState() {
        Log.d(TAG, "cancelAnalysisOrResetState called.")
        viewModelScope.launch {
            uploadedUrlDuringAnalysis?.let {
                Log.d(TAG, "Deleting uploaded image during analysis cancellation: $it")
                imageUploader.deleteImage(it)
            }
            pendingLogData = null; analysisResultForSheet = null; generatedWarnings =
            emptyList(); uploadedUrlDuringAnalysis = null
            stateHandled()
        }
    }

    fun stateHandled() {
        Log.d(TAG, "stateHandled called. Current state: ${_uiState.value}")
        val currentState = _uiState.value
        // Reset to inputting or editing state after certain terminal/intermediate states.
        if (currentState is LogFoodUiState.ShowResult || currentState is LogFoodUiState.Error || currentState is LogFoodUiState.Success || currentState is LogFoodUiState.Saving || currentState is LogFoodUiState.ShowFirstLogCongratulation || currentState is LogFoodUiState.ShowStreakContinued || currentState is LogFoodUiState.ShowPremiumPrompt || // Also reset after premium prompt
            currentState is LogFoodUiState.Analyzing
        ) { // Reset if analysis was interrupted somehow
            _uiState.value = if (logToEdit != null) {
                Log.d(TAG, "stateHandled: Resetting to Editing state.")
                LogFoodUiState.Editing(logToEdit!!)
            } else {
                Log.d(TAG, "stateHandled: Resetting to Inputting state for mealType=$mealType.")
                LogFoodUiState.Inputting(mealType)
            }
        }
    }
}