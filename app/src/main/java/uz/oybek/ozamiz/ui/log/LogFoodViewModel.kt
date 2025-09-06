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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.ImageUploader
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.UserRepository
import uz.oybek.ozamiz.data.ai.GeminiAnalysisResult
import uz.oybek.ozamiz.data.ai.GeminiFoodAnalyzer
import uz.oybek.ozamiz.data.gamification.GamificationConstants
import uz.oybek.ozamiz.data.gamification.GamificationManager
import uz.oybek.ozamiz.data.info.FoodLog
import uz.oybek.ozamiz.data.info.MealsType
import uz.oybek.ozamiz.data.logs.DailyLogRepository
import uz.oybek.ozamiz.data.utils.NutritionCalculator
import uz.oybek.ozamiz.util.StreakManager
import java.util.Date

sealed class LogFoodUiState {
    data object Loading : LogFoodUiState()
    data class Inputting(val mealType: String) : LogFoodUiState()
    data class Editing(val log: FoodLog) : LogFoodUiState()
    data object Analyzing : LogFoodUiState()
    data class ShowResult(
        val analysisResult: GeminiAnalysisResult,
        val healthAssessmentText: String?,
        val isHealthyAssessment: Boolean?
    ) : LogFoodUiState()

    data object Saving : LogFoodUiState()
    data object Success : LogFoodUiState()
    data class Error(val message: String) : LogFoodUiState()
    data object ShowPremiumPrompt : LogFoodUiState()
}

class LogFoodViewModel(application: Application) : AndroidViewModel(application) {

    private val dailyLogRepository = DailyLogRepository()
    private val imageUploader = ImageUploader
    private val foodAnalyzer = GeminiFoodAnalyzer
    private val gamificationManager = GamificationManager()

    private val _uiState = MutableStateFlow<LogFoodUiState>(LogFoodUiState.Loading)
    val uiState: StateFlow<LogFoodUiState> = _uiState.asStateFlow()

    private var currentPendingLogData: FoodLog? = null
    private var currentAnalysisResult: GeminiAnalysisResult? = null
    private var currentUploadedUrlDuringAnalysis: String? = null

    var logToEdit: FoodLog? = null
    var mealType: String = ""
    var selectedImageUri: Uri? = null
    private var originalPhotoUrl: String? = null
    private var selectedDate: Date = Date() // Sana saqlash uchun

    fun loadArguments(arguments: Bundle?) {
        val defaultMealTypeFromArgs = arguments?.getString("mealType") ?: MealsType.Snack.id
        val logJson = arguments?.getString("logToEditJson")
        val timestamp = arguments?.getLong("selectedDateTimestamp", System.currentTimeMillis())
        selectedDate = Date(timestamp ?: System.currentTimeMillis()) // Argumentdan sanani o'qish
        mealType = defaultMealTypeFromArgs

        if (logJson != null) {
            try {
                logToEdit = Gson().fromJson(logJson, FoodLog::class.java)
                originalPhotoUrl = logToEdit?.photoUrl
                mealType = logToEdit?.mealType ?: defaultMealTypeFromArgs
                _uiState.value = LogFoodUiState.Editing(logToEdit!!)
            } catch (e: Exception) {
                _uiState.value = LogFoodUiState.Error("Log ma'lumotini o'qishda xatolik")
            }
        } else {
            logToEdit = null
            originalPhotoUrl = null
            _uiState.value = LogFoodUiState.Inputting(mealType)
        }
        currentPendingLogData = null
        currentAnalysisResult = null
        currentUploadedUrlDuringAnalysis = null
    }

    fun saveEditedLog(description: String) {
        if (logToEdit == null) return
        if (description.isBlank()) {
            _uiState.value =
                LogFoodUiState.Error(getApplication<Application>().getString(R.string.error_description_required))
            stateHandled()
            return
        }
        _uiState.value = LogFoodUiState.Saving
        viewModelScope.launch {
            var newPhotoUrl = originalPhotoUrl
            var deleteOldImage = false
            if (selectedImageUri != null) {
                when (val uploadResult = imageUploader.uploadImage(selectedImageUri!!)) {
                    is DataWrapper.Success -> {
                        newPhotoUrl = uploadResult.data
                        if (!originalPhotoUrl.isNullOrEmpty() && originalPhotoUrl != newPhotoUrl) {
                            deleteOldImage = true
                        }
                    }

                    is DataWrapper.Error -> {
                        _uiState.value =
                            LogFoodUiState.Error("Rasm yuklashda xato: ${uploadResult.error}")
                        return@launch
                    }

                    else -> {}
                }
            }
            val updatedLog =
                logToEdit!!.copy(description = description.trim(), photoUrl = newPhotoUrl)
            when (val result =
                dailyLogRepository.updateFoodLog(logToEdit!!.logId, updatedLog, selectedDate)) {
                is DataWrapper.Success -> {
                    if (deleteOldImage) {
                        imageUploader.deleteImage(originalPhotoUrl)
                    }
                    _uiState.value = LogFoodUiState.Success
                }

                is DataWrapper.Error -> {
                    if (selectedImageUri != null && newPhotoUrl != originalPhotoUrl) {
                        newPhotoUrl?.let { imageUploader.deleteImage(it) }
                    }
                    _uiState.value = LogFoodUiState.Error("Log saqlashda xato: ${result.error}")
                }

                else -> {}
            }
        }
    }

    fun analyzeAndPrepareLog(description: String, currentMealTypeArg: String) {
        this.mealType = currentMealTypeArg

        val imageUriForAnalysis = selectedImageUri

        if (description.isBlank() && imageUriForAnalysis == null) {
            _uiState.value =
                LogFoodUiState.Error(getApplication<Application>().getString(R.string.error_description_required))
            stateHandled()
            return
        }
        _uiState.value = LogFoodUiState.Analyzing
        viewModelScope.launch {
            currentUploadedUrlDuringAnalysis = null
            if (imageUriForAnalysis != null) {
                when (val uploadResult = imageUploader.uploadImage(imageUriForAnalysis)) {
                    is DataWrapper.Success -> currentUploadedUrlDuringAnalysis = uploadResult.data
                    is DataWrapper.Error -> {
                        _uiState.value =
                            LogFoodUiState.Error("Rasm yuklashda xato: ${uploadResult.error}")
                        return@launch
                    }

                    else -> {}
                }
            }

            when (val analysisDataWrapper =
                foodAnalyzer.analyzeFood(imageUriForAnalysis, description.trim(), mealType)) {
                is DataWrapper.Success -> {
                    val analysisData = analysisDataWrapper.data
                    currentAnalysisResult = analysisData
                    currentPendingLogData = FoodLog(
                        description = description.trim(),
                        calories = analysisData.estimatedCalories,
                        protein = analysisData.estimatedProtein,
                        fat = analysisData.estimatedFat,
                        carbs = analysisData.estimatedCarbs,
                        mealType = mealType,
                        photoUrl = currentUploadedUrlDuringAnalysis,
                        analysisInsights = analysisData.healthFeedbackShort,
                        estimatedByAI = true,
                        timestamp = ServerValue.TIMESTAMP
                    )
                    _uiState.value = LogFoodUiState.ShowResult(
                        analysisData,
                        analysisData.healthFeedbackShort,
                        analysisData.isHealthyForWeightLoss
                    )
                }

                is DataWrapper.Error -> {
                    currentUploadedUrlDuringAnalysis?.let { imageUploader.deleteImage(it) }
                    _uiState.value = LogFoodUiState.Error(analysisDataWrapper.error)
                }

                else -> {}
            }
        }
    }

    fun confirmAndSaveLog() {
        val logDataToSave = currentPendingLogData
        if (logDataToSave == null) {
            _uiState.value = LogFoodUiState.Error("Saqlash uchun ma'lumot yo'q.")
            return
        }
        _uiState.value = LogFoodUiState.Saving
        viewModelScope.launch {
            when (val result = dailyLogRepository.addFoodLog(logDataToSave, selectedDate)) {
                is DataWrapper.Success -> {
                    onLogSaved()
                }

                is DataWrapper.Error -> {
                    currentUploadedUrlDuringAnalysis?.let { imageUploader.deleteImage(it) }
                    _uiState.value = LogFoodUiState.Error("Log saqlashda xatolik: ${result.error}")
                }

                else -> {}
            }
        }
    }

    fun onLogSaved() {
        viewModelScope.launch {
            val currentUser = LocalUser.user
            val streakUpdateResult = StreakManager.updateUserStreakOnAction(currentUser)

            if (streakUpdateResult.updatedUser.streakInfo != currentUser.streakInfo) {
                val userWithUpdatedStreak = streakUpdateResult.updatedUser
                LocalUser.user = userWithUpdatedStreak
                LocalUser.saveUser()
                UserRepository.updateUser { dbUser ->
                    dbUser.streakInfo = userWithUpdatedStreak.streakInfo
                }
            }

            _uiState.value = LogFoodUiState.Success
        }
    }

    fun cancelAnalysisOrResetState() {
        viewModelScope.launch {
            currentUploadedUrlDuringAnalysis?.let {
                imageUploader.deleteImage(it)
            }
            clearTemporaryAnalysisData()
            stateHandled()
        }
    }

    private fun clearTemporaryAnalysisData() {
        currentPendingLogData = null
        currentAnalysisResult = null
        currentUploadedUrlDuringAnalysis = null
    }

    fun stateHandled() {
        val currentState = _uiState.value
        if (currentState !is LogFoodUiState.Inputting && currentState !is LogFoodUiState.Editing && currentState !is LogFoodUiState.Analyzing) {
            _uiState.value = if (logToEdit != null) {
                LogFoodUiState.Editing(logToEdit!!)
            } else {
                LogFoodUiState.Inputting(mealType)
            }
        }
    }
}