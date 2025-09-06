package uz.oybek.ozamiz.ui.foods

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson // Gson importi
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.admin
import uz.oybek.ozamiz.data.foods.Recipe
import uz.oybek.ozamiz.data.foods.RecipeRepository
import uz.oybek.ozamiz.data.info.FoodLog
import uz.oybek.ozamiz.data.info.MealsType
import uz.oybek.ozamiz.data.logs.DailyLogRepository
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.getinfo.Event

// RecipeDetailFragment uchun UI state
data class RecipeDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val recipe: Recipe? = null,
    val isUserAdmin: Boolean = false,
    val isLoggingMeal: Boolean = false,
    val logSuccessMessage: String? = null,
    val logErrorMessage: String? = null
)

class RecipeDetailViewModel : ViewModel() {

    private val recipeRepository = RecipeRepository()
    private val dailyLogRepository = DailyLogRepository()
    private val gson = Gson() // Gson obyektini yaratish

    private val _uiState = MutableLiveData(RecipeDetailUiState())
    val uiState: LiveData<RecipeDetailUiState> = _uiState

    private val _navigationEvent = MutableLiveData<Event<RecipeDetailNavigationEvent>>()
    val navigationEvent: LiveData<Event<RecipeDetailNavigationEvent>> = _navigationEvent

    // O'ZGARISH: recipeId o'rniga recipeJson qabul qiladi
    fun loadRecipe(recipeJson: String?) {
        _uiState.value = _uiState.value?.copy(isLoading = true, errorMessage = null)
        if (recipeJson == null) {
            _uiState.postValue(
                _uiState.value?.copy(isLoading = false, errorMessage = "Retsept ma'lumoti topilmadi")
            )
            return
        }
        try {
            val recipe = gson.fromJson(recipeJson, Recipe::class.java)
            _uiState.postValue(
                _uiState.value?.copy(
                    isLoading = false,
                    recipe = recipe,
                    isUserAdmin = LocalUser.user.admin()
                )
            )
        } catch (e: Exception) {
            handleException(e, "RecipeDetailJsonParseError")
            _uiState.postValue(
                _uiState.value?.copy(isLoading = false, errorMessage = "Retsept ma'lumotini o'qishda xatolik")
            )
        }
    }


    fun onEditRecipeClick() {
        _uiState.value?.recipe?.let {
            _navigationEvent.value = Event(RecipeDetailNavigationEvent.NavigateToEditRecipe(it))
        }
    }

    fun onShareRecipeClick() {
        _uiState.value?.recipe?.let {
            _navigationEvent.value = Event(RecipeDetailNavigationEvent.ShareRecipe(it))
        }
    }

    fun onLogMealClick(mealType: String) {
        val currentRecipe = _uiState.value?.recipe ?: return
        _uiState.value = _uiState.value?.copy(isLoggingMeal = true, logErrorMessage = null, logSuccessMessage = null)

        viewModelScope.launch {
            val foodLog = FoodLog(
                description = currentRecipe.name,
                calories = currentRecipe.calories ?: 0,
                protein = currentRecipe.protein,
                fat = currentRecipe.fat,
                carbs = currentRecipe.carbs,
                mealType = mealType,
                photoUrl = currentRecipe.imageUrl,
                analysisInsights = null,
                estimatedByAI = false
            )
        }
    }

    fun onDeleteRecipeClick() {
        val recipeToDelete = _uiState.value?.recipe ?: return
        _uiState.value = _uiState.value?.copy(isLoading = true)
        viewModelScope.launch {
            when (val result = recipeRepository.deleteRecipe(recipeToDelete)) {
                is DataWrapper.Success -> {
                    _navigationEvent.postValue(Event(RecipeDetailNavigationEvent.NavigateBackWithMessage("Retsept o'chirildi")))
                }
                is DataWrapper.Error -> {
                    _uiState.postValue(
                        _uiState.value?.copy(isLoading = false, errorMessage = "O'chirishda xatolik: ${result.error}")
                    )
                }
                is DataWrapper.Loading -> {}
            }
        }
    }


    fun clearError() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }

    fun clearLogStatus() {
        _uiState.value = _uiState.value?.copy(logSuccessMessage = null, logErrorMessage = null)
    }

    // O'ZGARISH: Videoni ijro etish uchun yangi funksiya
    fun onPlayVideoClick() {
        _uiState.value?.recipe?.videoUrl?.let { videoUrl ->
            if (videoUrl.isNotBlank()) {
                // YouTube video ID sini ajratib olish
                val videoId = extractYoutubeVideoId(videoUrl)
                if (videoId != null) {
                    _navigationEvent.value = Event(RecipeDetailNavigationEvent.NavigateToYoutubePlayer(videoId))
                } else {
                    _uiState.value = _uiState.value?.copy(errorMessage = "Noto'g'ri YouTube video manzili")
                }
            }
        }
    }

    private fun extractYoutubeVideoId(youtubeUrl: String): String? {
        val patterns = arrayOf(
            "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*", // General pattern
            "(?<=\\/v\\/|\\/videos\\/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v=|&v=|\u200C\u200B?feature=player_embedded&v=|\u200C\u200B?v=|\u200C\u200B?list=)[^#\\&\\?\\n]*" // Wider pattern
        )
        for (pattern in patterns) {
            val compiledPattern = Regex(pattern)
            val matcher = compiledPattern.find(youtubeUrl)
            if (matcher != null && matcher.value.length == 11) { // Standard YouTube ID length
                return matcher.value
            }
        }
        return null // No valid ID found
    }
}

sealed class RecipeDetailNavigationEvent {
    data class NavigateToEditRecipe(val recipe: Recipe) : RecipeDetailNavigationEvent()
    data class ShareRecipe(val recipe: Recipe) : RecipeDetailNavigationEvent()
    data class ShowMealTypeSelection(val availableTypes: List<MealsType>) : RecipeDetailNavigationEvent()
    data class NavigateBackWithMessage(val message: String) : RecipeDetailNavigationEvent()
    data class NavigateToYoutubePlayer(val videoId: String) : RecipeDetailNavigationEvent() // O'ZGARISH
}